package com.zimky.coregateUS.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Queue / editor surface.
 *
 * Built exactly like [MusicDroplet]: a single anchored surface whose *measured*
 * size is interpolated between the small entry square and the full card, driven
 * by one spring so rapid taps retarget from the current position and velocity.
 *
 * V2.4.2: the card now returns to the place it grew out of. [origin] is the
 * bounds of the "歌单" button in root coordinates; while collapsing the whole
 * surface is translated and scaled onto that rectangle instead of shrinking
 * into a ball in the bottom-right corner. Same idea as `LiquidReveal`, but
 * applied to the card itself rather than a full-screen layer.
 */
@Composable
internal fun MusicSheet(
    visible: Boolean,
    origin: Rect = Rect.Zero,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val settings = com.zimky.coregateUS.ui.theme.LocalAppSettings.current
    val waterRipples = settings.waterRipples
    val cs = MiuixTheme.colorScheme
    // Follow the real accent seed, not a muted tonal primary.
    val tint = if (settings.keyColor != 0) Color(settings.keyColor) else cs.primary
    val edge = cs.onSurface.copy(alpha = 0.10f)
    val surface = lerp(cs.surfaceContainerHigh, tint, 0.035f)
    val flow = remember { Animatable(if (visible) 1f else 0f) }
    val wave = remember { Animatable(1f) }
    var firstFrame by remember { mutableStateOf(true) }
    // Where this surface currently sits, in root coordinates, plus its measured
    // size. Needed to compute the offset that brings it back onto [origin].
    var selfOffset by remember { mutableStateOf(Offset.Zero) }
    var selfSize by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(visible, waterRipples) {
        if (firstFrame) { firstFrame = false; return@LaunchedEffect }
        if (waterRipples) launch {
            wave.snapTo(0f)
            wave.animateTo(1f, tween(950, easing = LinearEasing))
        } else wave.snapTo(1f)
        // Retarget from the current position and velocity on rapid taps.
        flow.animateTo(if (visible) 1f else 0f,
            spring(dampingRatio = 0.92f, stiffness = 72f))
    }
    val raw = flow.value
    val p = raw.coerceIn(0f, 1f)
    if (!visible && p <= 0.001f) return
    val density = LocalDensity.current
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(
        (with(density) { (22.dp.toPx() + 2.dp.toPx() * p) / density.density }).dp
    )
    Layout(
        modifier = Modifier
            .onGloballyPositioned {
                selfOffset = it.positionInRoot()
                selfSize = it.size
            }
            .shadow(10.dp, shape, clip = false)
            .graphicsLayer {
                this.shape = shape
                clip = true
                transformOrigin = TransformOrigin(0f, 0f)
                // Fly back onto the "歌单" button while collapsing. `home` is 0
                // when fully open and 1 when fully collapsed, so every transform
                // below is exactly inert at rest.
                val hasHome = origin.width > 0f && origin.height > 0f && selfSize.width > 0
                val home = if (hasHome) (1f - p).coerceIn(0f, 1f) else 0f
                // Collapse only: dissolve the shell over the last stretch so it
                // fades away instead of blinking out once `p` reaches zero.
                // Inert while expanding (visible == true) - opening is untouched.
                if (!visible) {
                    alpha = ((p - 0.02f) / 0.26f).coerceIn(0f, 1f)
                }
                if (home > 0f) {
                    val curW = selfSize.width.toFloat()
                    val curH = selfSize.height.toFloat()
                    val dx = origin.center.x - selfOffset.x - curW * 0.5f
                    val dy = origin.center.y - selfOffset.y - curH * 0.5f
                    translationX = dx * home
                    translationY = dy * home
                    val sx = origin.width / curW
                    val sy = origin.height / curH
                    scaleX = 1f + (sx - 1f) * home
                    scaleY = 1f + (sy - 1f) * home
                } else {
                    scaleY = 1f + (raw - p) * 0.12f
                }
            }
            .background(surface.copy(alpha = 0.98f))
            .liquidSheen()
            .border(
                0.75.dp,
                Brush.verticalGradient(listOf(edge, edge.copy(alpha = 0.025f))),
                shape
            )
            .drawWithContent {
                drawContent()
                if (waterRipples) drawCapillaryWaves(
                    Offset(size.width - 28.dp.toPx(), size.height - 24.dp.toPx()),
                    wave.value, tint, returning = !visible
                )
            },
        content = {
            Box(
                Modifier
                    .pointerInput(visible, p >= 0.92f) {
                        // Swallow input while flowing so nothing fires mid-flight.
                        if (!visible || p < 0.92f) awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                    .graphicsLayer {
                        alpha = ((p - 0.45f) / 0.55f).coerceIn(0f, 1f)
                        transformOrigin = TransformOrigin(1f, 1f)
                        scaleX = 0.94f + p * 0.06f
                        scaleY = scaleX
                    }
            ) { content() }
            // Small tail handle: doubles as the tap-to-close target while it
            // is still the only thing on screen.
            Box(
                Modifier
                    .size(64.dp, 56.dp)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Final)
                                if (event.changes.any { it.changedToUp() || it.pressed }) {
                                    onDismiss()
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {}
        }
    ) { measurables, constraints ->
        val panelWidth = 320.dp.roundToPx().coerceAtMost(constraints.maxWidth)
        val full = measurables[0].measure(
            if (constraints.hasBoundedWidth) Constraints.fixedWidth(panelWidth) else Constraints()
        )
        val anchor = measurables[1].measure(Constraints())
        // When we know where we came from, the collapsed footprint is the
        // button itself, so the shrink lands exactly on the "歌单" button
        // instead of degenerating into a small ball at the corner.
        val hasHome = origin.width > 0f && origin.height > 0f
        val minW = if (hasHome) origin.width.toInt() else anchor.width
        val minH = if (hasHome) origin.height.toInt() else anchor.height
        val width = (minW + (full.width - minW) * p).toInt()
        val height = (minH + (full.height - minH) * p).toInt()
        val actualWidth = constraints.constrainWidth(width)
        val actualHeight = constraints.constrainHeight(height)
        layout(actualWidth, actualHeight) {
            if (p > 0f) full.placeRelative(actualWidth - full.width, actualHeight - full.height)
            if (!hasHome && p < 0.16f) {
                anchor.placeRelative(actualWidth - anchor.width, actualHeight - anchor.height)
            }
        }
    }
}