package com.zimky.coregateUS.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Foreground-only, on-demand stream. No music files or background service. */
private class HomeStream(context: Context) {
    private val app = context.applicationContext
    private val audio = requireNotNull(app.getSystemService(AudioManager::class.java))
    private var media: MediaPlayer? = null
    private var prepared = false
    private var wantsPlay = false
    private var source = ""
    var playing by mutableStateOf(false)
        private set
    var loading by mutableStateOf(false)
        private set
    var position by mutableIntStateOf(0)
        private set
    var duration by mutableIntStateOf(0)
        private set
    var message by mutableStateOf("添加一首喜欢的歌")
        private set
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(attributes).setOnAudioFocusChangeListener { change ->
            if (change != AudioManager.AUDIOFOCUS_GAIN) pause()
        }.build()

    fun toggle(url: String) {
        if (playing || loading) { pause(); return }
        if (url.isBlank()) return
        wantsPlay = true
        if (prepared && source == url) { start(); return }
        releaseMedia()
        wantsPlay = true
        source = url
        loading = true
        message = "正在连接…"
        val player = MediaPlayer()
        media = player
        try {
            player.setAudioAttributes(attributes)
            player.setOnPreparedListener {
                if (media === it) {
                    prepared = true
                    loading = false
                    duration = it.duration.coerceAtLeast(0)
                    message = "已就绪"
                    if (wantsPlay) start()
                }
            }
            player.setOnCompletionListener {
                if (media === it) {
                    playing = false
                    wantsPlay = false
                    position = duration
                    message = "播放结束"
                    audio.abandonAudioFocusRequest(focus)
                }
            }
            player.setOnErrorListener { current, _, _ ->
                if (media === current) fail("加载失败，请检查网络或音频直链")
                true
            }
            player.setOnInfoListener { current, what, _ ->
                if (media === current && playing) {
                    if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) message = "缓冲中…"
                    if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) message = "正在播放"
                }
                false
            }
            player.setDataSource(app, Uri.parse(url))
            player.prepareAsync()
        } catch (_: Exception) { fail("无法打开这个音频地址") }
    }

    private fun start() {
        if (!prepared) return
        if (audio.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            wantsPlay = false
            message = "暂时无法播放，请稍后重试"
            return
        }
        try {
            if (duration > 0 && position >= duration) media?.seekTo(0)
            media?.start()
            playing = true
            message = "正在播放"
        } catch (_: Exception) { fail("播放中断，请重试") }
    }

    fun pause() {
        wantsPlay = false
        if (loading) { releaseMedia(); message = "已取消加载"; return }
        try { if (playing) media?.pause() } catch (_: Exception) { releaseMedia() }
        playing = false
        message = if (prepared) "已暂停" else message
        audio.abandonAudioFocusRequest(focus)
    }

    fun tick() {
        if (prepared) try { position = media?.currentPosition ?: 0 } catch (_: Exception) { }
    }

    fun seek(fraction: Float) {
        if (!prepared || duration <= 0) return
        try {
            position = (duration * fraction).toInt().coerceIn(0, duration)
            media?.seekTo(position)
        } catch (_: Exception) { }
    }

    fun fail(text: String) { releaseMedia(); message = text }
    fun reset() { releaseMedia(); message = "点击播放，在线加载" }
    private fun releaseMedia() {
        val previous = media
        media = null
        prepared = false
        playing = false
        loading = false
        wantsPlay = false
        position = 0
        duration = 0
        previous?.release()
        audio.abandonAudioFocusRequest(focus)
    }
    fun close() { releaseMedia() }
}

@Composable
fun BoxScope.HomeMusicPlayer(active: Boolean) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("home_music", Context.MODE_PRIVATE) }
    val stream = remember(context) { HomeStream(context) }
    var url by remember { mutableStateOf(prefs.getString("url", "").orEmpty()) }
    var title by remember { mutableStateOf(prefs.getString("title", "我的音乐").orEmpty()) }
    var expanded by rememberSaveable { mutableStateOf(true) }
    var appeared by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf(false) }
    val owner = LocalLifecycleOwner.current
    val cs = MiuixTheme.colorScheme
    LaunchedEffect(Unit) { delay(400); appeared = true }
    LaunchedEffect(active) { if (!active) { stream.pause(); edit = false } }
    LaunchedEffect(stream.playing) {
        while (stream.playing) { stream.tick(); delay(500) }
    }
    LaunchedEffect(stream.loading) {
        if (stream.loading) { delay(30_000); if (stream.loading) stream.fail("连接超时，请检查音频地址后重试") }
    }
    DisposableEffect(owner, stream) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) stream.pause()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); stream.close() }
    }
    Box(Modifier.align(Alignment.BottomEnd).padding(bottom = PAGE_BOTTOM_INSET + 16.dp)) {
        AnimatedVisibility(visible = appeared && expanded,
            enter = slideInHorizontally { it } + fadeIn(),
            exit = slideOutHorizontally { it } + fadeOut()) {
            Card(Modifier.padding(horizontal = 16.dp).widthIn(max = 300.dp)
                .shadow(14.dp, RoundedCornerShape(24.dp))) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).clickable { edit = true }) {
                            Text(if (url.isBlank()) "随身听" else title.ifBlank { "我的音乐" },
                                color = cs.onSurface, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(stream.message, color = cs.onSurfaceVariantSummary, fontSize = 12.sp,
                                maxLines = 2)
                        }
                        TextButton("收起", onClick = { expanded = false })
                    }
                    if (stream.duration > 0) {
                        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(20.dp)
                            .pointerInput(stream.duration) {
                                detectTapGestures { point ->
                                    stream.seek((point.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f))
                                }
                            }) {
                            val start = androidx.compose.ui.geometry.Offset(0f, size.height / 2)
                            val end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2)
                            drawLine(cs.outline.copy(alpha = 0.3f), start, end, 4.dp.toPx(), androidx.compose.ui.graphics.StrokeCap.Round)
                            val fraction = (stream.position.toFloat() / stream.duration).coerceIn(0f, 1f)
                            drawLine(cs.primary, start, end.copy(x = size.width * fraction), 4.dp.toPx(), androidx.compose.ui.graphics.StrokeCap.Round)
                        }
                        Text("${musicTime(stream.position)} / ${musicTime(stream.duration)}",
                            fontSize = 11.sp, color = cs.onSurfaceVariantSummary)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(if (stream.loading) "取消" else if (stream.playing) "暂停" else "播放",
                            onClick = { if (url.isBlank()) edit = true else stream.toggle(url) })
                        TextButton("选择音源", onClick = { edit = true })
                    }
                }
            }
        }
        if (!expanded) {
            Box(Modifier.clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
                .background(cs.primary.copy(alpha = 0.16f))
                .pointerInput(Unit) {
                    var drag = 0f
                    detectHorizontalDragGestures(onDragStart = { drag = 0f },
                        onDragEnd = { if (drag < -20.dp.toPx()) expanded = true }) { change, delta ->
                        change.consume(); drag += delta
                    }
                }.clickable { expanded = true }.padding(horizontal = 14.dp, vertical = 16.dp)) {
                Text("‹ ♪", color = cs.primary, fontSize = 20.sp)
            }
        }
    }
    if (edit) {
        var draftUrl by remember { mutableStateOf(url) }
        var draftTitle by remember { mutableStateOf(title) }
        var error by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { edit = false }) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("网络音源", color = cs.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("填入 HTTPS 音频直链，例如 MP3、M4A。歌曲按需在线播放，不打包进安装包。",
                        color = cs.onSurfaceVariantSummary, fontSize = 13.sp)
                    MusicInput(draftTitle, { draftTitle = it.take(80) }, "歌曲名称")
                    MusicInput(draftUrl, { draftUrl = it.take(4096); error = "" }, "https://… 音频直链")
                    if (error.isNotBlank()) Text(error, color = cs.error, fontSize = 12.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton("取消", onClick = { edit = false })
                        TextButton("保存", onClick = {
                            val candidate = draftUrl.trim()
                            val uri = Uri.parse(candidate)
                            if (uri.scheme != "https" || uri.host.isNullOrBlank() || !uri.userInfo.isNullOrBlank()) {
                                error = "请填写有效的 HTTPS 音频直链"
                            } else {
                                stream.reset()
                                url = candidate
                                title = draftTitle.trim().ifBlank { "我的音乐" }
                                prefs.edit().putString("url", url).putString("title", title).apply()
                                edit = false
                            }
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicInput(value: String, onChange: (String) -> Unit, hint: String) {
    val cs = MiuixTheme.colorScheme
    BasicTextField(value = value, onValueChange = onChange, singleLine = true,
        textStyle = TextStyle(color = cs.onSurface, fontSize = 14.sp),
        cursorBrush = SolidColor(cs.primary),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(cs.surfaceContainerHigh).padding(14.dp),
        decorationBox = { inner -> Box {
            if (value.isEmpty()) Text(hint, color = cs.onSurfaceVariantSummary, fontSize = 14.sp)
            inner()
        } })
}

private fun musicTime(milliseconds: Int): String {
    val seconds = milliseconds.coerceAtLeast(0) / 1000
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}
