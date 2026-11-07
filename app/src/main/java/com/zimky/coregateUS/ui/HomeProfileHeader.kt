package com.zimky.coregateUS.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import com.zimky.coregateUS.core.CustomAvatar
import kotlinx.coroutines.launch
import android.graphics.BitmapShader
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import kotlin.math.min
import kotlin.math.max
import androidx.compose.ui.graphics.ImageBitmap
import com.zimky.coregateUS.core.QqAvatar
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
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
import java.time.LocalDate
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** The centered slot reserves space; the avatar is drawn outside the scroll clip. */
@Composable
fun HomeProfileHeader(onAvatarBounds: (Rect) -> Unit) {
    val cs = MiuixTheme.colorScheme
    val owner = LocalLifecycleOwner.current
    var hour by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    var today by remember { mutableStateOf(LocalDate.now()) }
    LaunchedEffect(owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                today = LocalDate.now()
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
        // Reserve a separate centered row; the overlay uses the actual cutout x.
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Spacer(Modifier.size(44.dp).onGloballyPositioned {
                onAvatarBounds(Rect(it.positionInWindow(), androidx.compose.ui.geometry.Size(it.size.width.toFloat(), it.size.height.toFloat())))
            })
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("核心门控", modifier = Modifier.weight(1f), color = cs.onSurface,
                fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(greeting, color = cs.onSurface, fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text("每日一言 · ${today.monthValue}月${today.dayOfMonth}日", color = cs.primary,
            fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        Text(DailyQuote.forDate(today), color = cs.onSurfaceVariantSummary,
            fontSize = 15.sp, lineHeight = 24.sp, modifier = Modifier.fillMaxWidth())
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
    val connector = remember { TelegramMetaball() }
    val silhouette = remember { Path() }
    val circle = remember { Path() }
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG) }
    val matrix = remember { Matrix() }
    val bitmap = remember(avatar) { avatar?.asAndroidBitmap() }
    val shader = remember(bitmap) { bitmap?.let { BitmapShader(it, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP) } }
    val cutout = ViewCompat.getRootWindowInsets(view)?.displayCutout?.boundingRects
        ?.filter { it.top <= statusHeight && it.width() < view.width / 2 }
        ?.minByOrNull { kotlin.math.abs(it.centerX() - view.width / 2) }
    val targetX = cutout?.exactCenterX()?.minus(rootOrigin.x) ?: (view.width / 2f - rootOrigin.x)
    val targetY = cutout?.exactCenterY()?.minus(rootOrigin.y) ?: (statusHeight / 2f - rootOrigin.y)
    val notchRadius = cutout?.let { min(it.width(), it.height()) / 2f }
        ?: with(density) { 7.dp.toPx() }
    val startY = bounds.center.y - rootOrigin.y + scroll.value
    val distance = (startY - targetY).coerceAtLeast(avatarSize)
    // Read scrolling during drawing, avoiding composition/layout work on every frame.
    Canvas(Modifier.fillMaxSize()) {
        val progress = (scroll.value / distance).coerceIn(0f, 1f)
        if (progress >= 1f) return@Canvas
        val eased = progress * progress * (3f - 2f * progress)
        val center = Offset(targetX, startY - distance * progress)
        val radius = avatarSize / 2f + (notchRadius * 0.88f - avatarSize / 2f) * eased
        val anchor = Offset(targetX, targetY)
        val separation = (center - anchor).getDistance()
        val reach = notchRadius + radius * 2.25f
        // Narrow continuously at the disconnect distance instead of popping a bridge.
        val contact = ((reach - separation) / radius.coerceAtLeast(1f)).coerceIn(0f, 1f)
        val stickiness = contact * contact * (3f - 2f * contact) * 0.5f
        drawIntoCanvas { composeCanvas ->
            val canvas = composeCanvas.nativeCanvas
            silhouette.rewind()
            silhouette.addCircle(center.x, center.y, radius, Path.Direction.CW)
            if (stickiness > 0f && connector.update(anchor, center, notchRadius, radius, stickiness)) {
                silhouette.op(connector.path, Path.Op.UNION)
            }
            if (stickiness > 0f) {
                circle.rewind()
                circle.addCircle(anchor.x, anchor.y, notchRadius, Path.Direction.CW)
                silhouette.op(circle, Path.Op.UNION)
            }
            paint.shader = null
            paint.color = cs.surfaceContainerHigh.toArgb()
            canvas.drawPath(silhouette, paint)
            if (bitmap != null && shader != null) {
                val scale = radius * 2f / min(bitmap.width, bitmap.height)
                matrix.reset()
                matrix.setScale(scale, scale)
                matrix.postTranslate(center.x - bitmap.width * scale / 2f, center.y - bitmap.height * scale / 2f)
                shader.setLocalMatrix(matrix)
                paint.shader = shader
                paint.color = android.graphics.Color.WHITE
                canvas.drawPath(silhouette, paint)
            }
            // Blend the stretched photo into the physical black camera opening.
            if (stickiness > 0f) {
                paint.shader = LinearGradient(0f, targetY + notchRadius * 0.5f,
                    0f, max(targetY + notchRadius + 1f, center.y + radius * 0.35f),
                    android.graphics.Color.BLACK, android.graphics.Color.TRANSPARENT, Shader.TileMode.CLAMP)
                canvas.drawPath(silhouette, paint)
            }
            paint.shader = null
        }
    }
    // Only this small slot handles taps; the drawing layer leaves page scrolling alone.
    Box(Modifier.size(44.dp).graphicsLayer {
        translationX = targetX - avatarSize / 2f
        translationY = startY - avatarSize / 2f
    }.clickable(enabled = !saving && scroll.value == 0, role = Role.Button,
        onClickLabel = "从相册选择头像") {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }.semantics { contentDescription = "头像，点击从相册选择" })
}
