import { Prompt } from "./prompt";

export class GeminiError extends Error {
  constructor(
    readonly code: "blocked" | "rate_limited" | "auth" | "network" | "bad_response" | "empty",
    message: string,
  ) {
    super(message);
  }
}

export interface Usage {
  tokensIn: number;
  tokensOut: number;
}

export interface StreamOptions {
  apiKey: string;
  model: string;
  maxOutputTokens?: number;
  temperature?: number;
  signal?: AbortSignal;
  fetchImpl?: typeof fetch;
  baseUrl?: string;
}

const BLOCKING_FINISH = new Set(["SAFETY", "PROHIBITED_CONTENT", "BLOCKLIST", "SPII", "RECITATION"]);

interface Chunk {
  candidates?: { content?: { parts?: { text?: string; thought?: boolean }[] }; finishReason?: string }[];
  promptFeedback?: { blockReason?: string };
  usageMetadata?: { promptTokenCount?: number; candidatesTokenCount?: number; thoughtsTokenCount?: number };
}

export function requestBody(prompt: Prompt, maxOutputTokens: number, temperature: number) {
  return {
    systemInstruction: { parts: [{ text: prompt.systemInstruction }] },
    contents: [{ role: "user", parts: [{ text: prompt.userMessage }] }],
    generationConfig: {
      temperature,
      topP: 0.95,
      maxOutputTokens,
      // Readings need little reasoning; low thinking keeps output tokens (cost) down.
      // Same setting the app used when it called Gemini directly.
      thinkingConfig: { thinkingLevel: "low" },
    },
    safetySettings: [
      { category: "HARM_CATEGORY_HARASSMENT", threshold: "BLOCK_MEDIUM_AND_ABOVE" },
      { category: "HARM_CATEGORY_HATE_SPEECH", threshold: "BLOCK_MEDIUM_AND_ABOVE" },
      { category: "HARM_CATEGORY_SEXUALLY_EXPLICIT", threshold: "BLOCK_MEDIUM_AND_ABOVE" },
      { category: "HARM_CATEGORY_DANGEROUS_CONTENT", threshold: "BLOCK_MEDIUM_AND_ABOVE" },
    ],
  };
}

/**
 * Streams Gemini's answer (REST + server-sent events). Yields text chunks; resolves the
 * returned usage once the stream ends. Throws [GeminiError] on any failure.
 */
export async function* streamGemini(prompt: Prompt, opts: StreamOptions): AsyncGenerator<string, Usage> {
  const doFetch = opts.fetchImpl ?? fetch;
  const base = opts.baseUrl ?? "https://generativelanguage.googleapis.com/v1beta";
  let response: Response;
  try {
    response = await doFetch(`${base}/models/${encodeURIComponent(opts.model)}:streamGenerateContent?alt=sse`, {
      method: "POST",
      headers: { "content-type": "application/json", "x-goog-api-key": opts.apiKey },
      body: JSON.stringify(requestBody(prompt, opts.maxOutputTokens ?? 1024, opts.temperature ?? 0.7)),
      signal: opts.signal,
    });
  } catch (e) {
    throw new GeminiError("network", `fetch failed: ${(e as Error).message}`);
  }
  if (!response.ok || !response.body) {
    const body = await response.text().catch(() => "");
    if (response.status === 429) throw new GeminiError("rate_limited", body.slice(0, 300));
    if (response.status === 401 || response.status === 403) throw new GeminiError("auth", body.slice(0, 300));
    throw new GeminiError("bad_response", `HTTP ${response.status}: ${body.slice(0, 300)}`);
  }

  const usage: Usage = { tokensIn: 0, tokensOut: 0 };
  const decoder = new TextDecoder();
  const reader = response.body.getReader();
  let buffer = "";
  let produced = false;

  const handle = (line: string): string => {
    if (!line.startsWith("data:")) return "";
    let chunk: Chunk;
    try {
      chunk = JSON.parse(line.slice(5).trim()) as Chunk;
    } catch {
      return "";
    }
    if (chunk.promptFeedback?.blockReason) throw new GeminiError("blocked", chunk.promptFeedback.blockReason);
    if (chunk.usageMetadata) {
      usage.tokensIn = chunk.usageMetadata.promptTokenCount ?? usage.tokensIn;
      usage.tokensOut =
        (chunk.usageMetadata.candidatesTokenCount ?? 0) + (chunk.usageMetadata.thoughtsTokenCount ?? 0) || usage.tokensOut;
    }
    const candidate = chunk.candidates?.[0];
    const text = (candidate?.content?.parts ?? [])
      .filter((p) => p.thought !== true)
      .map((p) => p.text ?? "")
      .join("");
    if (candidate?.finishReason && BLOCKING_FINISH.has(candidate.finishReason) && !produced && !text) {
      throw new GeminiError("blocked", candidate.finishReason);
    }
    return text;
  };

  try {
    while (true) {
      const { value, done } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });
      let newline: number;
      while ((newline = buffer.indexOf("\n")) >= 0) {
        const line = buffer.slice(0, newline).replace(/\r$/, "");
        buffer = buffer.slice(newline + 1);
        const text = handle(line);
        if (text) {
          produced = true;
          yield text;
        }
      }
    }
    const tail = handle(buffer.trim());
    if (tail) {
      produced = true;
      yield tail;
    }
  } catch (e) {
    if (e instanceof GeminiError) throw e;
    throw new GeminiError("network", `stream interrupted: ${(e as Error).message}`);
  }
  if (!produced) throw new GeminiError("empty", "model returned no text");
  return usage;
}

/** Approximate cost in Thai baht, for the cost log (prices per 1M tokens, USD; update with Google's price page). */
export const PRICES_USD_PER_M: Record<string, { in: number; out: number }> = {
  "gemini-flash-lite-latest": { in: 0.1, out: 0.4 },
  "gemini-flash-latest": { in: 0.3, out: 2.5 },
  "gemini-pro-latest": { in: 1.25, out: 10 },
};

export function estimateCostThb(model: string, usage: Usage, usdToThb = 35): number {
  const p = PRICES_USD_PER_M[model] ?? PRICES_USD_PER_M["gemini-flash-latest"];
  return ((usage.tokensIn * p.in + usage.tokensOut * p.out) / 1_000_000) * usdToThb;
}
