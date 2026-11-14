package com.zimky.coregateUS.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zimky.coregateUS.ui.RollingText as Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** A local drag preview; seeks only on release, never for each pointer event. */
@Composable
internal fun MusicSeekBar(position: Int, duration: Int, playing: Boolean, onSeek: (Float) -> Unit) {
    val cs = MiuixTheme.colorScheme
    var dragging by remember { mutableStateOf(false) }
    var preview by remember { mutableFloatStateOf(0f) }
    var width by remember { mutableIntStateOf(1) }
    val latestSeek by rememberUpdatedState(onSeek)
    val actual = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val smooth by animateFloatAsState(if (dragging) preview else actual,
        tween(if (playing && !dragging) 220 else 150), label = "musicProgress")
    val knob by animateFloatAsState(if (dragging) 8f else 5f, spring(), label = "musicThumb")
    val surge = remember { Animatable(0f) }
    LaunchedEffect(playing, duration) {
        if (playing && duration > 0) {
            while (true) {
                surge.snapTo(0f)
                surge.animateTo(1f, tween(1500, easing = LinearEasing))
            }
        } else surge.snapTo(0f)
    }
    val waveStrength by animateFloatAsState(if (playing && duration > 0) 1f else 0f,
        tween(240), label = "musicWaveStrength")
    val inset = 12.dp
    val density = androidx.compose.ui.platform.LocalDensity.current
    val insetPx = with(density) { inset.toPx() }
    fun pick(x: Float) { preview = ((x - insetPx) / (width - 2 * insetPx).coerceAtLeast(1f)).coerceIn(0f, 1f) }
    Column {
        Canvas(Modifier.fillMaxWidth().height(44.dp).onSizeChanged { width = it.width }
            .semantics {
                contentDescription = "歌曲进度"
                progressBarRangeInfo = ProgressBarRangeInfo(if (dragging) preview else actual, 0f..1f)
                setProgress { value ->
                    if (duration <= 0 || !value.isFinite()) false
                    else { latestSeek(value.coerceIn(0f, 1f)); true }
                }
            }
            .pointerInput(duration, width) {
                detectTapGestures { offset -> if (duration > 0) { pick(offset.x); latestSeek(preview) } }
            }
            .pointerInput(duration, width) {
                detectHorizontalDragGestures(
                    onDragStart = { if (duration > 0) { dragging = true; pick(it.x) } },
                    onDragEnd = { if (dragging) { latestSeek(preview); dragging = false } },
                    onDragCancel = { dragging = false }
                ) { change, _ -> if (dragging) { change.consume(); pick(change.position.x) } }
            }) {
            val fraction = if (dragging) preview else smooth
            val start = Offset(insetPx, center.y)
            val end = Offset((size.width - insetPx).coerceAtLeast(insetPx), center.y)
            val thumb = Offset(start.x + (end.x - start.x) * fraction, center.y)
            drawLine(cs.outline.copy(alpha = 0.25f), start, end, 4.dp.toPx(), StrokeCap.Round)
            if (fraction > 0f) drawLine(cs.primary, start, thumb, 4.dp.toPx(), StrokeCap.Round)
            // A narrow bright crest races toward the thumb, with two tapered spikes.
            if (waveStrength > 0f && fraction > 0f) {
                val phase = surge.value
                val travel = thumb.x - start.x
                val head = start.x + travel * phase
                val tail = 46.dp.toPx()
                val envelope = kotlin.math.sin(phase * Math.PI).toFloat().coerceAtLeast(0f) * waveStrength
                clipRect(left = start.x, top = 0f, right = thumb.x.coerceAtLeast(start.x), bottom = size.height) {
                    val glow = Brush.horizontalGradient(
                        listOf(Color.Transparent, cs.primary.copy(alpha = 0.35f * envelope),
                            cs.primary.copy(alpha = envelope)), head - tail, head)
                    drawLine(glow, Offset(head - tail, center.y), Offset(head, center.y),
                        9.dp.toPx(), StrokeCap.Round)
                    val spike = Path().apply {
                        moveTo(head - tail, center.y)
                        lineTo(head - 22.dp.toPx(), center.y)
                        lineTo(head - 15.dp.toPx(), center.y - 8.dp.toPx() * envelope)
                        lineTo(head - 10.dp.toPx(), center.y + 6.dp.toPx() * envelope)
                        lineTo(head - 5.dp.toPx(), center.y - 12.dp.toPx() * envelope)
                        lineTo(head, center.y)
                    }
                    drawPath(spike, cs.primary.copy(alpha = 0.18f * envelope), style = Stroke(7.dp.toPx()))
                    drawPath(spike, cs.primary.copy(alpha = envelope), style = Stroke(1.7.dp.toPx()))
                    drawCircle(cs.onSurface.copy(alpha = 0.75f * envelope), 2.dp.toPx(), Offset(head, center.y))
                }
            }
            drawCircle(cs.primary.copy(alpha = if (dragging) 0.16f else 0.07f), (knob + 6f).dp.toPx(), thumb)
            drawCircle(cs.primary, knob.dp.toPx(), thumb)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(musicTime(if (dragging) (preview * duration).toInt() else position), color = cs.onSurfaceVariantSummary, fontSize = 11.sp)
            Text(musicTime(duration), color = cs.onSurfaceVariantSummary, fontSize = 11.sp)
        }
    }
}

internal fun musicTime(milliseconds: Int): String {
    val seconds = milliseconds.coerceAtLeast(0) / 1000
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}
