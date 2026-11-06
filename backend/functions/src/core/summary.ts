/**
 * The model ends a reading with a one-line summary for the share card, after this marker. It comes
 * from the same model call, so it adds a few tokens rather than a second request.
 */
export const SUMMARY_MARKER = "[สรุป]";

/** Longer than this won't fit the card, and a summary cut off mid-sentence is what it exists to avoid. */
export const MAX_SUMMARY_CHARS = 100;

function cleanSummary(raw: string): string | null {
  const text = raw.replace(/[*#_`]/g, "").replace(/\s+/g, " ").trim();
  return text.length > 0 && text.length <= MAX_SUMMARY_CHARS ? text : null;
}

/** Length of the longest end of [text] that could still grow into the marker. */
function partialMarkerLength(text: string): number {
  for (let n = Math.min(SUMMARY_MARKER.length - 1, text.length); n > 0; n--) {
    if (text.endsWith(SUMMARY_MARKER.slice(0, n))) return n;
  }
  return 0;
}

/** Separates the reading the seeker sees from the summary as the stream arrives. */
export class SummarySplitter {
  private held = "";
  private summary: string | null = null;

  /** Returns the text that is safe to show now; it may be empty. */
  push(chunk: string): string {
    if (this.summary !== null) {
      this.summary += chunk;
      return "";
    }
    const text = this.held + chunk;
    const at = text.indexOf(SUMMARY_MARKER);
    if (at >= 0) {
      this.held = "";
      this.summary = text.slice(at + SUMMARY_MARKER.length);
      return text.slice(0, at);
    }
    const keep = partialMarkerLength(text);
    this.held = text.slice(text.length - keep);
    return text.slice(0, text.length - keep);
  }

  /** Call when the stream ends. [text] is anything still held back that turned out not to be the marker. */
  finish(): { text: string; summary: string | null } {
    const text = this.held;
    this.held = "";
    return { text, summary: this.summary === null ? null : cleanSummary(this.summary) };
  }
}
