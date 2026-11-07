package com.iching.ai.feature.share

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.Trigram
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** What the upper trigram paints in the sky of a share card. */
enum class SkyMotif { SUN_AND_CLOUDS, CLOUD_BANKS, LIGHTNING, RAIN, FAR_PEAKS, WIND_AND_BIRDS, RED_SUN, MOON_AND_STARS }

/** What the lower trigram paints on the ground of a share card. */
enum class GroundMotif { SEA_OF_CLOUDS, FIELDS, SPROUTS, WAVES, NEAR_PEAKS, BAMBOO, FLAMES, STILL_LAKE }

fun skyMotif(trigram: Trigram): SkyMotif = when (trigram) {
    Trigram.QIAN -> SkyMotif.SUN_AND_CLOUDS
    Trigram.KUN -> SkyMotif.CLOUD_BANKS
    Trigram.ZHEN -> SkyMotif.LIGHTNING
    Trigram.KAN -> SkyMotif.RAIN
    Trigram.GEN -> SkyMotif.FAR_PEAKS
    Trigram.XUN -> SkyMotif.WIND_AND_BIRDS
    Trigram.LI -> SkyMotif.RED_SUN
    Trigram.DUI -> SkyMotif.MOON_AND_STARS
}

fun groundMotif(trigram: Trigram): GroundMotif = when (trigram) {
    Trigram.QIAN -> GroundMotif.SEA_OF_CLOUDS
    Trigram.KUN -> GroundMotif.FIELDS
    Trigram.ZHEN -> GroundMotif.SPROUTS
    Trigram.KAN -> GroundMotif.WAVES
    Trigram.GEN -> GroundMotif.NEAR_PEAKS
    Trigram.XUN -> GroundMotif.BAMBOO
    Trigram.LI -> GroundMotif.FLAMES
    Trigram.DUI -> GroundMotif.STILL_LAKE
}

/** Colours of the scene; the card passes its sepia tones. */
data class SceneInk(val ink: Color, val wash: Color, val paper: Color, val accent: Color)

/**
 * An ink-wash landscape for one hexagram: the upper trigram is the sky, the lower one the ground,
 * so each of the 64 hexagrams gets its own picture without any image files.
 */
@Composable
fun TrigramScene(hexagram: Hexagram, ink: SceneInk, modifier: Modifier = Modifier) {
    val sky = skyMotif(hexagram.upperTrigram)
    val ground = groundMotif(hexagram.lowerTrigram)
    Canvas(modifier.clipToBounds()) {
        drawSky(sky, ink)
        drawGround(ground, ink)
    }
}

// ---- Sky: drawn in the top ~55% of the canvas ----

private fun DrawScope.drawSky(motif: SkyMotif, c: SceneInk) {
    val w = size.width
    val h = size.height
    when (motif) {
        SkyMotif.SUN_AND_CLOUDS -> {
            val center = Offset(w * 0.5f, h * 0.24f)
            drawCircle(c.wash.copy(alpha = 0.18f), radius = h * 0.2f, center = center)
            drawCircle(c.ink.copy(alpha = 0.55f), radius = h * 0.15f, center = center, style = Stroke(w * 0.006f))
            drawCircle(c.wash.copy(alpha = 0.35f), radius = h * 0.11f, center = center)
            scrollCloud(Offset(w * 0.2f, h * 0.36f), w * 0.2f, c)
            scrollCloud(Offset(w * 0.78f, h * 0.3f), w * 0.17f, c)
        }
        SkyMotif.CLOUD_BANKS -> {
            for (i in 0 until 4) {
                val y = h * (0.1f + i * 0.09f)
                val band = Path().apply {
                    moveTo(0f, y)
                    var x = 0f
                    val step = w / 6f
                    while (x < w) {
                        quadraticTo(x + step / 2, y - h * 0.035f, x + step, y)
                        x += step
                    }
                    lineTo(w, y + h * 0.05f)
                    lineTo(0f, y + h * 0.05f)
                    close()
                }
                drawPath(band, c.wash.copy(alpha = 0.10f + i * 0.04f))
            }
        }
        SkyMotif.LIGHTNING -> {
            darkCloud(Offset(w * 0.32f, h * 0.12f), w * 0.3f, c)
            darkCloud(Offset(w * 0.7f, h * 0.1f), w * 0.26f, c)
            val bolt = Path().apply {
                moveTo(w * 0.52f, h * 0.16f)
                lineTo(w * 0.45f, h * 0.3f)
                lineTo(w * 0.53f, h * 0.3f)
                lineTo(w * 0.44f, h * 0.48f)
            }
            drawPath(bolt, c.accent, style = Stroke(w * 0.012f, cap = StrokeCap.Round, join = StrokeJoin.Miter))
            val fork = Path().apply {
                moveTo(w * 0.72f, h * 0.15f)
                lineTo(w * 0.76f, h * 0.24f)
                lineTo(w * 0.71f, h * 0.26f)
                lineTo(w * 0.75f, h * 0.36f)
            }
            drawPath(fork, c.accent.copy(alpha = 0.7f), style = Stroke(w * 0.007f, cap = StrokeCap.Round))
        }
        SkyMotif.RAIN -> {
            darkCloud(Offset(w * 0.3f, h * 0.1f), w * 0.32f, c)
            darkCloud(Offset(w * 0.72f, h * 0.13f), w * 0.28f, c)
            for (i in 0 until 22) {
                val x = w * (0.06f + (i * 0.043f) % 0.9f)
                val y = h * (0.2f + ((i * 37) % 23) / 100f)
                drawLine(
                    c.ink.copy(alpha = 0.35f),
                    Offset(x, y),
                    Offset(x - w * 0.02f, y + h * 0.07f),
                    strokeWidth = w * 0.004f,
                    cap = StrokeCap.Round,
                )
            }
        }
        SkyMotif.FAR_PEAKS -> {
            peakRange(
                baseY = h * 0.5f,
                peaks = listOf(0.08f to 0.32f, 0.3f to 0.18f, 0.52f to 0.28f, 0.75f to 0.14f, 0.95f to 0.3f),
                color = c.wash.copy(alpha = 0.22f),
            )
            drawRect(
                Brush.verticalGradient(listOf(c.paper.copy(alpha = 0f), c.paper), startY = h * 0.36f, endY = h * 0.5f),
                topLeft = Offset(0f, h * 0.36f),
                size = Size(w, h * 0.15f),
            )
        }
        SkyMotif.WIND_AND_BIRDS -> {
            for (i in 0 until 4) {
                val y = h * (0.12f + i * 0.08f)
                val gust = Path().apply {
                    moveTo(w * (0.05f + i * 0.06f), y)
                    cubicTo(w * 0.3f, y - h * 0.05f, w * 0.5f, y + h * 0.05f, w * (0.78f - i * 0.04f), y)
                    // A curl at the tip of each gust.
                    quadraticTo(w * (0.86f - i * 0.04f), y - h * 0.04f, w * (0.8f - i * 0.04f), y - h * 0.05f)
                }
                drawPath(gust, c.ink.copy(alpha = 0.4f), style = Stroke(w * 0.005f, cap = StrokeCap.Round))
            }
            listOf(Offset(0.3f, 0.08f), Offset(0.38f, 0.05f), Offset(0.62f, 0.09f)).forEach { (fx, fy) ->
                bird(Offset(w * fx, h * fy), w * 0.035f, c.ink.copy(alpha = 0.7f))
            }
        }
        SkyMotif.RED_SUN -> {
            val center = Offset(w * 0.5f, h * 0.24f)
            for (i in 0 until 16) {
                val angle = (i / 16f) * 2 * PI
                val inner = h * 0.17f
                val outer = h * (if (i % 2 == 0) 0.25f else 0.21f)
                drawLine(
                    c.accent.copy(alpha = 0.55f),
                    Offset(center.x + inner * cos(angle).toFloat(), center.y + inner * sin(angle).toFloat()),
                    Offset(center.x + outer * cos(angle).toFloat(), center.y + outer * sin(angle).toFloat()),
                    strokeWidth = w * 0.006f,
                    cap = StrokeCap.Round,
                )
            }
            drawCircle(c.accent.copy(alpha = 0.85f), radius = h * 0.13f, center = center)
        }
        SkyMotif.MOON_AND_STARS -> {
            val center = Offset(w * 0.68f, h * 0.2f)
            drawCircle(c.wash.copy(alpha = 0.35f), radius = h * 0.1f, center = center)
            // The paper colour bites a crescent out of the full disc.
            drawCircle(c.paper, radius = h * 0.09f, center = center + Offset(h * 0.045f, -h * 0.02f))
            listOf(Offset(0.15f, 0.1f), Offset(0.3f, 0.25f), Offset(0.45f, 0.08f), Offset(0.88f, 0.36f), Offset(0.2f, 0.4f))
                .forEach { (fx, fy) -> star(Offset(w * fx, h * fy), w * 0.014f, c.ink.copy(alpha = 0.55f)) }
        }
    }
}

// ---- Ground: drawn in the bottom ~50% of the canvas ----

private fun DrawScope.drawGround(motif: GroundMotif, c: SceneInk) {
    val w = size.width
    val h = size.height
    when (motif) {
        GroundMotif.SEA_OF_CLOUDS -> {
            for (row in 0 until 3) {
                val y = h * (0.66f + row * 0.11f)
                val r = w * (0.09f - row * 0.01f)
                var x = -r * (row % 2)
                while (x < w + r) {
                    drawCircle(c.paper, radius = r, center = Offset(x, y))
                    drawArc(
                        c.ink.copy(alpha = 0.45f), 180f, 180f, false,
                        topLeft = Offset(x - r, y - r), size = Size(r * 2, r * 2), style = Stroke(w * 0.005f),
                    )
                    // The inward curl that makes a cloud scroll.
                    drawArc(
                        c.ink.copy(alpha = 0.3f), 200f, 160f, false,
                        topLeft = Offset(x - r * 0.5f, y - r * 0.5f), size = Size(r, r), style = Stroke(w * 0.004f),
                    )
                    x += r * 1.7f
                }
            }
        }
        GroundMotif.FIELDS -> {
            for (i in 0 until 5) {
                val top = h * (0.6f + i * 0.08f)
                val field = Path().apply {
                    moveTo(0f, top + h * 0.02f * (i % 2))
                    quadraticTo(w * 0.5f, top - h * 0.03f, w, top + h * 0.02f * ((i + 1) % 2))
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(field, c.wash.copy(alpha = 0.12f + i * 0.05f))
            }
            for (i in 0 until 7) {
                val x = w * (0.1f + i * 0.13f)
                drawLine(c.ink.copy(alpha = 0.18f), Offset(w * 0.5f, h * 0.62f), Offset(x * 1.4f - w * 0.2f, h), w * 0.003f)
            }
        }
        GroundMotif.SPROUTS -> {
            groundBand(h * 0.82f, c)
            listOf(0.15f, 0.3f, 0.46f, 0.62f, 0.8f).forEachIndexed { i, fx ->
                val x = w * fx
                val tall = h * (0.12f + (i % 3) * 0.05f)
                val stem = Path().apply {
                    moveTo(x, h * 0.84f)
                    quadraticTo(x - w * 0.02f, h * 0.84f - tall / 2, x + w * 0.01f, h * 0.84f - tall)
                }
                drawPath(stem, c.ink.copy(alpha = 0.7f), style = Stroke(w * 0.007f, cap = StrokeCap.Round))
                leaf(Offset(x + w * 0.01f, h * 0.84f - tall), w * 0.05f, -35f, c.ink.copy(alpha = 0.6f))
                leaf(Offset(x, h * 0.84f - tall * 0.6f), w * 0.045f, 200f, c.ink.copy(alpha = 0.5f))
            }
        }
        GroundMotif.WAVES -> {
            for (row in 0 until 4) {
                val y = h * (0.66f + row * 0.09f)
                val r = w * 0.06f
                var x = if (row % 2 == 0) 0f else r
                while (x < w + r) {
                    for (ring in 0 until 3) {
                        val rr = r * (1f - ring * 0.3f)
                        drawArc(
                            c.ink.copy(alpha = 0.55f - ring * 0.12f), 180f, 180f, false,
                            topLeft = Offset(x - rr, y - rr), size = Size(rr * 2, rr * 2), style = Stroke(w * 0.004f),
                        )
                    }
                    x += r * 2f
                }
            }
        }
        GroundMotif.NEAR_PEAKS -> {
            peakRange(
                baseY = h,
                peaks = listOf(0.0f to 0.62f, 0.22f to 0.48f, 0.45f to 0.66f, 0.68f to 0.44f, 0.92f to 0.6f),
                color = c.ink.copy(alpha = 0.55f),
            )
            // A band of mist across the foot of the mountains.
            drawRect(
                Brush.verticalGradient(listOf(c.paper.copy(alpha = 0f), c.paper.copy(alpha = 0.85f)), startY = h * 0.78f, endY = h * 0.92f),
                topLeft = Offset(0f, h * 0.78f),
                size = Size(w, h * 0.22f),
            )
        }
        GroundMotif.BAMBOO -> {
            groundBand(h * 0.9f, c)
            listOf(0.12f to 0.42f, 0.24f to 0.5f, 0.78f to 0.46f, 0.9f to 0.56f).forEach { (fx, topF) ->
                val x = w * fx
                val top = h * topF
                val lean = w * 0.05f
                drawLine(c.ink.copy(alpha = 0.7f), Offset(x, h * 0.92f), Offset(x + lean, top), w * 0.012f, cap = StrokeCap.Round)
                var y = h * 0.86f
                while (y > top + h * 0.05f) {
                    val t = (h * 0.92f - y) / (h * 0.92f - top)
                    val nx = x + lean * t
                    drawLine(c.paper, Offset(nx - w * 0.008f, y), Offset(nx + w * 0.008f, y), w * 0.004f)
                    y -= h * 0.08f
                }
                leaf(Offset(x + lean, top + h * 0.03f), w * 0.07f, 10f, c.ink.copy(alpha = 0.6f))
                leaf(Offset(x + lean * 0.8f, top + h * 0.08f), w * 0.06f, 25f, c.ink.copy(alpha = 0.5f))
                leaf(Offset(x + lean * 0.6f, top + h * 0.14f), w * 0.06f, 160f, c.ink.copy(alpha = 0.45f))
            }
        }
        GroundMotif.FLAMES -> {
            groundBand(h * 0.9f, c)
            listOf(0.2f to 0.14f, 0.38f to 0.22f, 0.55f to 0.28f, 0.72f to 0.2f, 0.86f to 0.12f).forEach { (fx, tall) ->
                flame(Offset(w * fx, h * 0.9f), w * 0.09f, h * tall, c.accent.copy(alpha = 0.75f))
                flame(Offset(w * fx, h * 0.9f), w * 0.045f, h * tall * 0.55f, c.paper.copy(alpha = 0.8f))
            }
        }
        GroundMotif.STILL_LAKE -> {
            drawRect(
                Brush.verticalGradient(listOf(c.wash.copy(alpha = 0.08f), c.wash.copy(alpha = 0.28f)), startY = h * 0.7f, endY = h),
                topLeft = Offset(0f, h * 0.7f),
                size = Size(w, h * 0.3f),
            )
            drawLine(c.ink.copy(alpha = 0.5f), Offset(0f, h * 0.7f), Offset(w, h * 0.7f), w * 0.004f)
            for (i in 0 until 6) {
                val y = h * (0.75f + i * 0.04f)
                val inset = w * (0.25f - i * 0.03f)
                drawLine(c.ink.copy(alpha = 0.25f), Offset(inset, y), Offset(w - inset * 1.4f, y), w * 0.003f, cap = StrokeCap.Round)
            }
            listOf(0.08f, 0.12f, 0.16f, 0.86f, 0.9f).forEachIndexed { i, fx ->
                val x = w * fx
                val top = h * (0.5f + (i % 3) * 0.04f)
                drawLine(c.ink.copy(alpha = 0.65f), Offset(x, h * 0.74f), Offset(x + w * 0.01f, top), w * 0.005f, cap = StrokeCap.Round)
                drawOval(c.ink.copy(alpha = 0.6f), topLeft = Offset(x - w * 0.002f, top - h * 0.01f), size = Size(w * 0.022f, h * 0.06f))
            }
        }
    }
}

// ---- Shared brush strokes ----

private fun DrawScope.scrollCloud(center: Offset, width: Float, c: SceneInk) {
    val r = width / 4f
    listOf(-1.4f, 0f, 1.4f).forEachIndexed { i, dx ->
        val cc = Offset(center.x + dx * r, center.y - if (i == 1) r * 0.5f else 0f)
        drawCircle(c.paper, radius = r, center = cc)
        drawArc(
            c.ink.copy(alpha = 0.5f), 150f, 240f, false,
            topLeft = cc - Offset(r, r), size = Size(r * 2, r * 2), style = Stroke(size.width * 0.005f),
        )
        drawArc(
            c.ink.copy(alpha = 0.35f), 180f, 200f, false,
            topLeft = cc - Offset(r * 0.5f, r * 0.5f), size = Size(r, r), style = Stroke(size.width * 0.004f),
        )
    }
}

private fun DrawScope.darkCloud(center: Offset, width: Float, c: SceneInk) {
    val r = width / 4f
    listOf(-1.3f to 0f, 0f to -0.4f, 1.3f to 0f, 0.6f to 0.3f, -0.6f to 0.3f).forEach { (dx, dy) ->
        drawCircle(lerp(c.paper, c.wash, 0.3f), radius = r, center = Offset(center.x + dx * r, center.y + dy * r))
    }
}

private fun DrawScope.peakRange(baseY: Float, peaks: List<Pair<Float, Float>>, color: Color) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(-w * 0.05f, baseY)
        peaks.forEachIndexed { i, (fx, fy) ->
            val x = w * fx
            val y = h * fy
            val prevX = if (i == 0) -w * 0.05f else w * peaks[i - 1].first
            // Saddle between peaks, then a slightly convex climb to the summit.
            quadraticTo((prevX + x) / 2, baseY - (baseY - y) * 0.35f, x, y)
        }
        lineTo(w * 1.05f, baseY)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.groundBand(top: Float, c: SceneInk) {
    val path = Path().apply {
        moveTo(0f, top)
        quadraticTo(size.width * 0.5f, top - size.height * 0.03f, size.width, top)
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(path, c.wash.copy(alpha = 0.25f))
}

private fun DrawScope.leaf(base: Offset, length: Float, angleDeg: Float, color: Color) {
    val a = angleDeg * PI.toFloat() / 180f
    val tip = base + Offset(length * cos(a), length * sin(a))
    val normal = Offset(-sin(a), cos(a)) * (length * 0.25f)
    val mid = (base + tip) / 2f
    val path = Path().apply {
        moveTo(base.x, base.y)
        quadraticTo((mid + normal).x, (mid + normal).y, tip.x, tip.y)
        quadraticTo((mid - normal).x, (mid - normal).y, base.x, base.y)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.flame(base: Offset, width: Float, height: Float, color: Color) {
    val path = Path().apply {
        moveTo(base.x - width / 2, base.y)
        cubicTo(base.x - width * 0.6f, base.y - height * 0.5f, base.x - width * 0.1f, base.y - height * 0.6f, base.x, base.y - height)
        cubicTo(base.x + width * 0.1f, base.y - height * 0.6f, base.x + width * 0.6f, base.y - height * 0.5f, base.x + width / 2, base.y)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.bird(center: Offset, span: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x - span, center.y)
        quadraticTo(center.x - span / 2, center.y - span * 0.5f, center.x, center.y)
        quadraticTo(center.x + span / 2, center.y - span * 0.5f, center.x + span, center.y)
    }
    drawPath(path, color, style = Stroke(size.width * 0.005f, cap = StrokeCap.Round))
}

private fun DrawScope.star(center: Offset, r: Float, color: Color) {
    drawLine(color, center - Offset(r, 0f), center + Offset(r, 0f), size.width * 0.003f, cap = StrokeCap.Round)
    drawLine(color, center - Offset(0f, r), center + Offset(0f, r), size.width * 0.003f, cap = StrokeCap.Round)
}
