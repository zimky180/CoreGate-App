package com.zimky.coregateUS.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import top.yukonga.miuix.kmp.basic.Text as MiuixText

/** New content rises from below; unchanged values never start a transition. */
@Composable
fun RisingContent(text: String, modifier: Modifier = Modifier, content: @Composable (String) -> Unit) {
    AnimatedContent(
        targetState = text,
        modifier = modifier.clipToBounds(),
        transitionSpec = {
            (slideInVertically(tween(320, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(220))) togetherWith
                (slideOutVertically(tween(320, easing = FastOutSlowInEasing)) { -it } + fadeOut(tween(180)))
        }, label = "risingText"
    ) { shown -> content(shown) }
}

/** Shared read-only text; input fields keep their native editing behavior. */
@Composable
fun RollingText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE
) {
    RisingContent(text, modifier) { shown ->
        MiuixText(text = shown, color = color, fontSize = fontSize, fontWeight = fontWeight,
            fontFamily = fontFamily, letterSpacing = letterSpacing, textAlign = textAlign,
            lineHeight = lineHeight, overflow = overflow, maxLines = maxLines)
    }
}
