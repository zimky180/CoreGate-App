package com.zimky.coregateUS.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zimky.coregateUS.ui.RollingText as Text
import java.util.Locale

/**
 * 开发者调试叠加层 —— 手指实时坐标显示。
 *
 * 用户原话：「加一个调试模式，仅开发人员开启、不懂勿碰」「手指实时显示坐标」。
 *
 * 设计要点（每条都对应一个具体坑）：
 *  1) 【只在开启时才存在】未开启直接 return，连 pointerInput 都不挂——
 *     否则整屏多一个吞事件的层，会把底下四个页面的点击全部吃掉。
 *  2) 【底部导航区必须放行】v2.3.8 修：旧版整屏 consume，导致开启调试后
 *     连「关闭调试」的开关都点不到，用户被彻底锁死在调试模式里。
 *     现在底部 [bottomReservedPx] 高度内的指针事件**不消费**，直接穿透，
 *     所以导航栏与页面底部按钮始终可以点，随时能关掉调试。
 *  3) 【坐标要给两套单位】px 给自动化点击/截图分析用，dp 给写代码用，
 *     两者换算依赖 density，所以用 LocalDensity 现算，不写死。
 *  4) 【松手保留最后坐标】抬手后不立刻清空，方便看一眼再抄下来；
 *     用 pressed 单独记录按压态，避免抬起后数字消失。
 *  5) 【显示层不参与命中】坐标牌本身用 Box + background，不加 clickable，
 *     不干扰整屏手势。
 *  6) 【坐标牌不吃触摸】坐标牌是纯绘制层；v2.3.8 起它被移到顶部，
 *     不再占底部区域，避免与放行区重叠。
 */
@Composable
fun DebugPointerOverlay(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    /** 底部放行区高度（px）。该高度内的触摸不消费，直接穿透到下层界面。 */
    bottomReservedPx: Float = 0f
) {
    if (!enabled) return

    // 最后一次触点位置（px）。null 表示还没碰过屏幕。
    var pos by remember { mutableStateOf<Offset?>(null) }
    // 当前是否正按着（用于区分「实时」与「已松手」）
    var pressed by remember { mutableStateOf(false) }
    // 屏幕尺寸，用于显示右侧/下方剩余距离
    var sizePx by remember { mutableStateOf(Offset.Zero) }

    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                sizePx = Offset(this.size.width.toFloat(), this.size.height.toFloat())
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        val p = change.position
                        // 底部放行区：只记录坐标，不 consume，让导航栏 / 底部按钮照常响应。
                        val inReplyZone = p.y >= sizePx.y - bottomReservedPx
                        when (event.type) {
                            PointerEventType.Press, PointerEventType.Move -> {
                                pos = p
                                pressed = true
                                // 放行区不消费；其余区域吃掉事件，防止穿透到下层界面
                                if (!inReplyZone) change.consume()
                            }
                            PointerEventType.Release -> {
                                pos = p
                                pressed = false
                                if (!inReplyZone) change.consume()
                            }
                            else -> {}
                        }
                    }
                }
            }
    ) {
        val p = pos ?: return@Box
        val d = density.density
        val px = p.x.toInt()
        val py = p.y.toInt()
        val dpx = p.x / d
        val dpy = p.y / d
        val fromRight = (sizePx.x - p.x).toInt()
        val fromBottom = (sizePx.y - p.y).toInt()

        // 十字准星：让「到底指在哪个像素」一眼看清
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // 竖线
            Box(
                modifier = Modifier
                    .padding(start = (dpx - 0.5f).dp)
                    .width(1.dp)
                    .height(with(density) { sizePx.y.toDp() })
                    .background(Color(0xFFFF5252).copy(alpha = 0.55f))
            )
            // 横线
            Box(
                modifier = Modifier
                    .padding(top = (dpy - 0.5f).dp)
                    .height(1.dp)
                    .width(with(density) { sizePx.x.toDp() })
                    .background(Color(0xFFFF5252).copy(alpha = 0.55f))
            )
            // 触点圆环
            Box(
                modifier = Modifier
                    .padding(start = (dpx - 9f).dp, top = (dpy - 9f).dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF5252).copy(alpha = 0.35f))
            )
        }

        // 坐标牌：贴顶部居中偏左，永不与手指区域/底部放行区重叠
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 12.dp, top = 96.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                .background(Color(0xE6101010))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(
                text = "$px , $py  px",
                color = Color(0xFFFFD54F),
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = String.format(Locale.ROOT, "%.1f , %.1f  dp", dpx, dpy),
                color = Color(0xFF81D4FA),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "距右 $fromRight  距底 $fromBottom  px",
                color = Color(0xFFB0BEC5),
                fontSize = 11.5.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "density ${String.format(Locale.ROOT, "%.2f", d)}",
                    color = Color(0xFF90A4AE),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (pressed) "按压中" else "已松手",
                    color = if (pressed) Color(0xFF69F0AE) else Color(0xFF90A4AE),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}