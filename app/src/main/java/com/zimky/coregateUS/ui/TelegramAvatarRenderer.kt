/*
 * Adapted from Telegram for Android ProfileMetaballView.onDraw,
 * initRenderNode and drawWithRenderNode at Git tree
 * 3de37c8cfb039452be7398909461da6288babf15.
 * Copyright Telegram contributors. GNU GPL v2; see LICENSES/Telegram-GPL-2.0.txt.
 * Changes: static local bitmap instead of ImageReceiver; external scroll geometry;
 * RenderNode only (CoreGate minSdk 33); right-side approach before aligned absorption.
 */
package com.zimky.coregateUS.ui

import android.graphics.*
import kotlin.math.max
import kotlin.math.min
import androidx.compose.ui.geometry.Offset

/** Owns the whole absorption drawing pass, including the original clipping and blur stages. */
internal class TelegramAvatarRenderer(private val density: Float) {
    private fun dp(value: Float) = value * density
    private fun mix(a: Float, b: Float, t: Float) = a + (b - a) * t
    private val connector = TelegramMetaball()
    private val clipPath = Path()
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
    private val photoPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val matrix = Matrix()
    private val gradient = LinearGradient(0f, 0f, 0f, dp(16f), Color.BLACK, Color.TRANSPARENT, Shader.TileMode.CLAMP)
    private val node = RenderNode("CoreGateTelegramAvatar")
    private var recordedBitmap: Bitmap? = null
    private var installedBlur = -1

    private fun record(bitmap: Bitmap) {
        if (recordedBitmap === bitmap && node.hasDisplayList()) return
        val size = 56
        val top = 24
        node.setPosition(0, 0, size, size + top)
        val canvas = node.beginRecording()
        val scale = size.toFloat() / min(bitmap.width, bitmap.height)
        canvas.save()
        canvas.translate((size - bitmap.width * scale) / 2f, top.toFloat() + (size - bitmap.height * scale) / 2f)
        canvas.scale(scale, scale)
        canvas.drawBitmap(bitmap, 0f, 0f, photoPaint)
        canvas.restore()
        // Upstream mirrors the avatar above its top edge to fill the stretched bridge.
        canvas.save()
        canvas.scale(1f, -1.5f)
        canvas.translate(0f, -top / 1.5f)
        canvas.translate((size - bitmap.width * scale) / 2f, (size - bitmap.height * scale) / 2f)
        canvas.scale(scale, scale)
        canvas.drawBitmap(bitmap, 0f, 0f, photoPaint)
        canvas.restore()
        node.endRecording()
        recordedBitmap = bitmap
    }

    fun drawCircle(canvas: Canvas, bitmap: Bitmap?, cx: Float, cy: Float, radius: Float, background: Int) {
        clipPath.rewind()
        clipPath.addCircle(cx, cy, radius, Path.Direction.CW)
        circlePaint.color = background
        canvas.drawPath(clipPath, circlePaint)
        circlePaint.color = Color.BLACK
        if (bitmap == null) return
        canvas.save()
        canvas.clipPath(clipPath)
        val scale = radius * 2f / min(bitmap.width, bitmap.height)
        canvas.translate(cx - bitmap.width * scale / 2f, cy - bitmap.height * scale / 2f)
        canvas.scale(scale, scale)
        canvas.drawBitmap(bitmap, 0f, 0f, photoPaint)
        canvas.restore()
    }

    /** Original onDraw coordinates: anchor is the tangent point of the large black circle. */
    fun drawAbsorption(canvas: Canvas, bitmap: Bitmap?, cx: Float, top: Float, vr: Float, width: Float) {
        val r = width
        val vy = top
        val isNear = vr <= dp(32f)
        if (vr > dp(40f)) return
        val fraction = max(0f, vr - dp(18f)) / dp(14f)
        val alphaFraction = fraction.coerceIn(0f, 1f)
        val alpha = if (isNear) (255f * alphaFraction).toInt() else 255
        val blurRadius = if (isNear) 2 + ((1f - fraction) * 20).toInt() else 1
        if (installedBlur != blurRadius) {
            installedBlur = blurRadius
            val blur = max(blurRadius / 2f - 1f, 0.5f)
            node.setRenderEffect(RenderEffect.createBlurEffect(blur, blur, Shader.TileMode.CLAMP))
        }
        val c = (vy / dp(24f)).coerceIn(-1f, 1f)
        var v = ((1f - c / 1.3f) / 2f).coerceIn(0f, 0.8f)
        val near = if (isNear) 1f else 1f - (vr - dp(32f)) / dp(8f)
        val exit = min((vy + vr * 2f) / dp(6f), 1f)
        if (!isNear) v = min(mix(0f, 0.2f, near), v)
        val center1 = Offset(cx, -r + dp(1f))
        val center2 = Offset(cx, vy + vr)
        // Before contact the same layer owns the normal circle. No second avatar is cross-faded in.
        if (!connector.update(center1, center2, r, vr, v)) {
            drawCircle(canvas, bitmap, cx, vy + vr, vr, Color.BLACK)
            return
        }
        val path = connector.path
        val decelerateNear = if (isNear) 1f else near * near
        if (bitmap != null && isNear) path.addRoundRect(cx - vr, vy, cx + vr, vy + vr * 2f, vr, vr, Path.Direction.CCW)
        canvas.save()
        if (!isNear) {
            clipPath.rewind()
            clipPath.addRect(cx - width / 2f, 0f, cx + width / 2f, dp(10f) * decelerateNear, Path.Direction.CW)
            clipPath.addCircle(cx, 0f, dp(20f) * decelerateNear, Path.Direction.CW)
            val clipRadius = vr + dp(12f) * decelerateNear
            clipPath.addCircle(cx, center2.y, clipRadius, Path.Direction.CW)
            clipPath.addCircle(cx, center2.y - clipRadius + dp(4f) * decelerateNear, dp(8f) * decelerateNear, Path.Direction.CW)
            canvas.clipPath(clipPath)
        }
        canvas.drawPath(path, circlePaint)
        canvas.drawCircle(cx, -r + dp(1f) * max(1f, near * 2f) * exit, r, circlePaint)
        canvas.restore()
        if (alpha > 0 && bitmap != null && canvas.isHardwareAccelerated) {
            record(bitmap)
            canvas.save()
            clipPath.rewind()
            if (isNear) {
                clipPath.addRoundRect(cx - vr, vy, cx + vr, vy + vr * 2f, vr, vr, Path.Direction.CW)
                clipPath.addRect(cx - width / 2f, 0f, cx + width / 2f, vy + dp(16f), Path.Direction.CW)
            } else {
                val clipRadius = vr + dp(12f) * decelerateNear
                clipPath.addCircle(cx, center2.y, clipRadius, Path.Direction.CW)
                clipPath.addCircle(cx, center2.y - clipRadius + dp(4f) * decelerateNear, dp(8f) * decelerateNear, Path.Direction.CW)
            }
            canvas.clipPath(clipPath)
            canvas.save()
            canvas.clipPath(path)
            val scale = vr * 2f / 56f
            canvas.translate(cx - vr, vy - 24f * scale)
            canvas.scale(scale, scale)
            node.alpha = alphaFraction
            canvas.drawRenderNode(node)
            canvas.restore()
            matrix.setTranslate(0f, max(0f, vy - dp(14f) - dp(15f) * (1f - near)))
            gradient.setLocalMatrix(matrix)
            gradientPaint.shader = gradient
            canvas.drawPath(path, gradientPaint)
            canvas.restore()
        }
    }

    fun release() { node.discardDisplayList(); recordedBitmap = null }
}