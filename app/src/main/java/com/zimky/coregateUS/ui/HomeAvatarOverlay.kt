package com.zimky.coregateUS.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.zimky.coregateUS.core.CustomAvatar
import com.zimky.coregateUS.core.QqAvatar
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Only layout references are stored. Scroll-dependent positions are resolved in the draw pass. */
class HomeAvatarAnchor {
    var root by mutableStateOf<LayoutCoordinates?>(null)
    var avatar by mutableStateOf<LayoutCoordinates?>(null)
}

private data class AvatarFrame(val center: Offset, val radius: Float, val target: Offset, val absorbing: Boolean)

@Composable
fun BoxScope.HomeAvatarOverlay(scroll: ScrollState, anchor: HomeAvatarAnchor) {
    val context = LocalContext.current.applicationContext
    val owner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val background = MiuixTheme.colorScheme.surfaceContainerHigh.toArgb()
    val renderer = remember(density.density) { TelegramAvatarRenderer(density.density) }
    DisposableEffect(renderer) { onDispose { renderer.release() } }
    var avatar by remember { mutableStateOf<ImageBitmap?>(null) }
    var saving by remember { mutableStateOf(false) }
    var selectionVersion by remember { mutableIntStateOf(0) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null && !saving) {
            selectionVersion++
            saving = true
            scope.launch {
                try {
                    val selected = CustomAvatar.save(context, uri)
                    if (selected != null) avatar = selected else Toaster.show("图片读取失败，请重新选择", true)
                } finally { saving = false }
            }
        }
    }
    LaunchedEffect(owner, context) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            if (avatar == null) {
                val version = selectionVersion
                val loaded = CustomAvatar.load(context) ?: QqAvatar.load()
                if (version == selectionVersion && !saving) avatar = loaded
            }
        }
    }
    val bitmap = remember(avatar) { avatar?.asAndroidBitmap() }
    val statusHeight = WindowInsets.statusBars.getTop(density).toFloat()
    val avatarRadius = with(density) { 26.dp.toPx() }
    val endRadius = with(density) { 12.dp.toPx() }
    val approachHeight = with(density) { 100.dp.toPx() }
    val endTop = with(density) { (-24).dp.toPx() }
    val currentSaving by rememberUpdatedState(saving)

    fun frame(): AvatarFrame? {
        val root = anchor.root?.takeIf { it.isAttached } ?: return null
        val slot = anchor.avatar?.takeIf { it.isAttached } ?: return null
        val position = root.localPositionOf(slot, Offset(slot.size.width / 2f, slot.size.height / 2f))
        // A single local coordinate space, read after layout. No window-position + scroll feedback loop.
        val originInWindow = root.localToWindow(Offset.Zero)
        val cutout = ViewCompat.getRootWindowInsets(view)?.displayCutout?.boundingRects
            ?.filter { it.top <= statusHeight && it.width() < view.width / 2 }
            ?.minByOrNull { kotlin.math.abs(it.centerX() - view.width / 2) }
        val target = Offset(
            (cutout?.exactCenterX() ?: (view.width / 2f)) - originInWindow.x,
            (cutout?.bottom?.toFloat() ?: 0f) - originInWindow.y
        )
        val startY = position.y + scroll.value
        val travel = (startY - target.y - endTop - endRadius).coerceAtLeast(avatarRadius)
        val progress = (scroll.value / travel).coerceIn(0f, 1f)
        val approachTravel = (startY - target.y - approachHeight).coerceAtLeast(avatarRadius)
        val horizontal = (scroll.value / approachTravel).coerceIn(0f, 1f)
        val eased = horizontal * horizontal * (3f - 2f * horizontal)
        val cx = position.x + (target.x - position.x) * eased
        val cy = startY - travel * progress
        val shrink = ((target.y + with(density) { 12.dp.toPx() } - cy) / with(density) { 24.dp.toPx() }).coerceIn(0f, 1f)
        val radius = avatarRadius + (endRadius - avatarRadius) * shrink
        val absorbing = horizontal >= 1f && cy - radius - target.y <= with(density) { 24.dp.toPx() }
        return AvatarFrame(Offset(cx, cy), radius, target, absorbing)
    }

    Canvas(Modifier.fillMaxSize()) {
        val f = frame() ?: return@Canvas
        drawIntoCanvas { composeCanvas ->
            val canvas = composeCanvas.nativeCanvas
            if (!f.absorbing) {
                renderer.drawCircle(canvas, bitmap, f.center.x, f.center.y, f.radius, background)
            } else {
                canvas.save()
                canvas.translate(0f, f.target.y)
                renderer.drawAbsorption(canvas, bitmap, f.target.x,
                    f.center.y - f.radius - f.target.y, f.radius, size.width)
                canvas.restore()
            }
        }
    }
    // Restrict hit testing to the moving avatar; the drawing layer never intercepts page gestures.
    Box(Modifier.size(52.dp).graphicsLayer {
        val f = frame()
        translationX = if (f == null || f.absorbing) -10000f else f.center.x - avatarRadius
        translationY = if (f == null || f.absorbing) -10000f else f.center.y - avatarRadius
        val scale = (f?.radius ?: avatarRadius) / avatarRadius
        scaleX = scale
        scaleY = scale
    }.pointerInput(scroll, anchor) {
        detectTapGestures {
            val f = frame()
            if (!currentSaving && f != null && !f.absorbing) {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }
    }.semantics { contentDescription = "主页头像，点击从相册更换；上滑吸附，回滑还原" })
}