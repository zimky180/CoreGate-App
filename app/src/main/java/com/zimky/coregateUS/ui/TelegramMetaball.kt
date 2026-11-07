/*
 * Adapted from Telegram for Android, ProfileMetaballView.createMetaballPath.
 * Upstream: https://github.com/DrKLO/Telegram/blob/master/TMessagesProj/src/main/java/org/telegram/ui/Components/ProfileMetaballView.java
 * Copyright belongs to the Telegram contributors. Licensed under GNU GPL v2;
 * see LICENSES/Telegram-GPL-2.0.txt. This adaptation retains that license.
 * Changes: Kotlin port, reusable Android Path, clamped inverse cosine inputs.
 */
package com.zimky.coregateUS.ui

import android.graphics.Path
import androidx.compose.ui.geometry.Offset
import kotlin.math.*

/** Telegram's two-circle connector; circles are added by the caller. */
internal class TelegramMetaball {
    val path = Path()

    fun update(c1: Offset, c2: Offset, radius1: Float, radius2: Float, v: Float): Boolean {
        path.rewind()
        val d = (c2 - c1).getDistance()
        val maxDist = radius1 + radius2 * 2.25f
        if (radius1 <= 0f || radius2 <= 0f || d > maxDist || d <= abs(radius1 - radius2)) return false
        fun safeAcos(value: Float) = acos(value.coerceIn(-1f, 1f))
        var u1 = 0f
        var u2 = 0f
        if (d < radius1 + radius2) {
            u1 = safeAcos((radius1 * radius1 + d * d - radius2 * radius2) / (2f * radius1 * d))
            u2 = safeAcos((radius2 * radius2 + d * d - radius1 * radius1) / (2f * radius2 * d))
        }
        val angle = atan2(c2.y - c1.y, c2.x - c1.x)
        val spread = safeAcos((radius1 - radius2) / d)
        val pi = PI.toFloat()
        val angle1 = angle + u1 + (spread - u1) * v
        val angle2 = angle - u1 - (spread - u1) * v
        val angle3 = angle + pi - u2 - (pi - u2 - spread) * v
        val angle4 = angle - pi + u2 + (pi - u2 - spread) * v
        fun vector(center: Offset, a: Float, r: Float) = center + Offset(cos(a) * r, sin(a) * r)
        val p1 = vector(c1, angle1, radius1)
        val p2 = vector(c1, angle2, radius1)
        val p3 = vector(c2, angle3, radius2)
        val p4 = vector(c2, angle4, radius2)
        val totalRadius = radius1 + radius2
        val d2 = min(v * 20f, (p1 - p3).getDistance() / totalRadius) * min(1f, d * 2f / totalRadius)
        val r1 = radius1 * d2
        val r2 = radius2 * d2
        val halfPi = pi / 2f
        val h1 = vector(p1, angle1 - halfPi, r1)
        val h2 = vector(p2, angle2 + halfPi, r1)
        val h3 = vector(p3, angle3 + halfPi, r2)
        val h4 = vector(p4, angle4 - halfPi, r2)
        val a1 = vector(p3, angle3 + halfPi, r2 * 0.55f)
        val a2 = vector(p4, angle4 - halfPi, r2 * 0.55f)
        path.moveTo(p1.x, p1.y)
        path.cubicTo(h1.x, h1.y, h3.x, h3.y, p3.x, p3.y)
        path.cubicTo(a1.x, a1.y, a2.x, a2.y, p4.x, p4.y)
        path.cubicTo(h4.x, h4.y, h2.x, h2.y, p2.x, p2.y)
        path.close()
        return true
    }
}
