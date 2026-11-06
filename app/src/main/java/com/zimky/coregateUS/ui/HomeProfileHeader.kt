package com.zimky.coregateUS.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import com.zimky.coregateUS.core.CustomAvatar
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.zimky.coregateUS.core.QqAvatar
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import java.util.Calendar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** The empty slot stays in the heading; the avatar itself is drawn outside the scroll clip. */
@Composable
fun HomeProfileHeader(onAvatarBounds: (Rect) -> Unit) {
    val cs = MiuixTheme.colorScheme
    val owner = LocalLifecycleOwner.current
    var hour by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    LaunchedEffect(owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                delay(30_000)
            }
        }
    }
    val greeting = when (hour) {
        in 5..10 -> "早上好！"
        in 11..17 -> "中午好！"
        else -> "晚上好！"
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("核心门控", modifier = Modifier.weight(1f), color = cs.onSurface,
                fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.size(44.dp).onGloballyPositioned { onAvatarBounds(Rect(it.positionInWindow(), androidx.compose.ui.geometry.Size(it.size.width.toFloat(), it.size.height.toFloat()))) })
            Text(greeting, color = cs.onSurface, fontSize = 14.sp,
                fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text("运行概览与核心控制", color = cs.onSurfaceVariantSummary,
            fontSize = 13.sp, lineHeight = 19.sp)
    }
}

/** Scroll-driven, reversible motion toward the real top cutout, with a top-center fallback. */
@Composable
fun BoxScope.HomeAvatarOverlay(scroll: ScrollState, avatarBounds: Rect?, rootOrigin: Offset) {
    var avatar by remember { mutableStateOf<ImageBitmap?>(null) }
    val owner = LocalLifecycleOwner.current
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var selectionVersion by remember { mutableIntStateOf(0) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null && !saving) {
            selectionVersion++
            saving = true
            scope.launch {
                try {
                    val selected = CustomAvatar.save(context, uri)
                    if (selected != null) avatar = selected
                    else Toaster.show("图片读取失败，请重新选择", true)
                } finally {
                    saving = false
                }
            }
        }
    }
    LaunchedEffect(owner, context) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            if (avatar == null) {
                val version = selectionVersion
                val loaded = CustomAvatar.load(context) ?: QqAvatar.load()
                if (selectionVersion == version && !saving) avatar = loaded
            }
        }
    }
    val bounds = avatarBounds ?: return
    val density = LocalDensity.current
    val view = LocalView.current
    val cs = MiuixTheme.colorScheme
    val avatarSize = with(density) { 44.dp.toPx() }
    val statusHeight = WindowInsets.statusBars.getTop(density).toFloat()
    Box(Modifier.size(44.dp)
        .graphicsLayer {
            // bounds follows the scrolled slot. Add its displacement back to recover the origin.
            val startX = bounds.center.x - rootOrigin.x
            val startY = bounds.center.y - rootOrigin.y + scroll.value
            val cutout = ViewCompat.getRootWindowInsets(view)?.displayCutout?.boundingRects
                ?.filter { it.top <= statusHeight && it.width() < view.width / 2 }
                ?.minByOrNull { kotlin.math.abs(it.centerX() - view.width / 2) }
            val targetX = cutout?.exactCenterX()?.minus(rootOrigin.x)
                ?: (view.width / 2f - rootOrigin.x)
            val targetY = cutout?.exactCenterY()?.minus(rootOrigin.y)
                ?: (statusHeight / 2f - rootOrigin.y)
            val distance = (startY - targetY).coerceAtLeast(with(density) { 72.dp.toPx() })
            val progress = (scroll.value / distance).coerceIn(0f, 1f)
            val eased = progress * progress * (3f - 2f * progress)
            translationX = startX + (targetX - startX) * eased - avatarSize / 2f
            translationY = startY + (targetY - startY) * eased - avatarSize / 2f
            // Compress horizontally first, then disappear inside the cutout at the end.
            scaleX = (1f - eased * 0.98f).coerceAtLeast(0.02f)
            scaleY = (1f - eased * 0.88f).coerceAtLeast(0.02f)
            alpha = if (progress < 0.78f) 1f else ((1f - progress) / 0.22f).coerceIn(0f, 1f)
            clip = true
            shape = CircleShape
        }
        .background(cs.surfaceContainerHigh, CircleShape)
        .border(1.dp, cs.outline.copy(alpha = 0.5f), CircleShape)
        .clickable(enabled = !saving && scroll.value == 0, role = Role.Button,
            onClickLabel = "从相册选择头像") {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        .semantics { contentDescription = "头像，点击从相册选择" }) {
        avatar?.let { image ->
            Image(bitmap = image, contentDescription = null,
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
}