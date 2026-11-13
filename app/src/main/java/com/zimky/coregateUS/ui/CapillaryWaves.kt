package com.zimky.coregateUS.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/** A dispersive wave packet: short trailing crests follow the leading water ridge.
 *
 * @param spread 涟漪尺寸档位。1f = 巨型（音乐播放器展开，波峰贯穿整卡）；
 *               0.45f 左右 = 小型（普通卡片点按，只在水滴附近荡开）。
 *               半径与环间距同步缩放，视觉密度保持一致。
 */
internal fun DrawScope.drawCapillaryWaves(
    origin: Offset,
    progress: Float,
    tint: Color,
    returning: Boolean = false,
    intensity: Float = 1f,
    spread: Float = 1f
) {
    val t = progress.coerceIn(0f, 1f)
    val envelope = sin(PI * t).toFloat() * intensity
    if (envelope <= 0.001f || size.minDimension <= 0f) return
    val reach = maxOf(
        hypot(origin.x, origin.y), hypot(size.width - origin.x, origin.y),
        hypot(origin.x, size.height - origin.y),
        hypot(size.width - origin.x, size.height - origin.y)
    )
    val travel = if (returning) 1f - t else t
    // spread 决定波峰能铺多远：小卡片用小档位，波峰不会强行吃满整卡
    val front = reach * 1.35f * spread * sqrt(travel)
    val spacing = (10f * density) * spread + reach * 0.016f * spread
    repeat(4) { index ->
        val radius = front - spacing * index * (1f + index * 0.14f)
        if (radius > 0f) {
            val attenuation = envelope * (1f - index / 4f)
            // 宽而亮的波脊 + 细高光/暗影成对，铺满卡片后仍有水的厚度感
            drawCircle(tint.copy(alpha = 0.025f * attenuation), radius, origin,
                style = Stroke((3f * density)))
            drawCircle(Color.White.copy(alpha = 0.10f * attenuation), radius, origin,
                style = Stroke((1.1f * density)))
            drawCircle(tint.copy(alpha = 0.09f * attenuation),
                (radius - (2.0f * density)).coerceAtLeast(0f), origin,
                style = Stroke((0.9f * density)))
        }
    }
}
