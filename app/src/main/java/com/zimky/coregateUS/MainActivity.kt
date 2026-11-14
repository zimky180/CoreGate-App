package com.zimky.coregateUS

import androidx.compose.ui.graphics.drawscope.clipPath
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.runtime.CompositionLocalProvider
import com.zimky.coregateUS.ui.theme.LocalAppSettings
import com.zimky.coregateUS.ui.theme.LocalSetAppSettings
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.foundation.layout.size
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.draw.clip

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.zimky.coregateUS.core.CoreConfig
import com.zimky.coregateUS.core.FreezeData
import com.zimky.coregateUS.core.RootShell
import com.zimky.coregateUS.core.SysProbe
import com.zimky.coregateUS.page.FreqPage
import com.zimky.coregateUS.page.FreezePage
import com.zimky.coregateUS.page.ScanDetailsPage
import com.zimky.coregateUS.page.ProcessManagerPage
import com.zimky.coregateUS.page.LockPage
import com.zimky.coregateUS.page.PolicyPage
import com.zimky.coregateUS.page.SettingsPage
import com.zimky.coregateUS.ui.HomeMusicPlayer
import com.zimky.coregateUS.ui.StartupIntro
import com.zimky.coregateUS.ui.ToastHost
import com.zimky.coregateUS.ui.component.RootGate
import com.zimky.coregateUS.ui.component.FloatingBottomBar
import com.zimky.coregateUS.ui.component.FloatingBottomBarItem
import com.zimky.coregateUS.ui.theme.AppSettings
import com.zimky.coregateUS.ui.theme.MiuixKernelSUTheme
import kotlinx.coroutines.delay
import com.zimky.coregateUS.ui.LiquidCard as Card
import top.yukonga.miuix.kmp.basic.Scaffold
import com.zimky.coregateUS.ui.RollingText as Text
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.blur
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.graphics.RectangleShape
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * MainActivity —— 全原生壳，零 WebView（Compose + Miuix 版）。
 *
 * 与旧 View 版行为一一对齐，三条硬性约束照旧：
 *   1) root 调用串行 + 合并。同一时刻只有一个 probe 在跑，期间来的请求只置「再跑一次」，
 *      手动刷新还有 1.2 秒最小间隔 —— 这是 v2.0.6 修掉安全弹窗的核心逻辑。
 *   2) 页面切换不销毁重建。四页 Composable 全部常驻在 composition 里，
 *      切 Tab 只改是否可见，滚动位置与输入内容都不丢。
 *   3) 页面只负责显示，轮询统一在 Activity 层做，两条线不打架。
 */
class MainActivity : ComponentActivity() {
    private var introIconBounds: androidx.compose.ui.geometry.Rect? = null
    private var systemIntroComplete by mutableStateOf(false)
    private var introIconDrawn = false
    private var introStartBounds by mutableStateOf<androidx.compose.ui.geometry.Rect?>(null)
    private var introOverlayRemoved by mutableStateOf(false)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialAppearance = AppSettings.load(this)
        // Keep the actual system splash animation, then move its icon to the
        // measured Compose icon bounds before starting the full-screen particles.
        systemIntroComplete = savedInstanceState != null
        splashScreen.setOnExitAnimationListener { splash ->
            if (initialAppearance.skipStartupIntro) {
                systemIntroComplete = true
                introOverlayRemoved = true
                splash.remove()
                return@setOnExitAnimationListener
            }
            val icon = splash.iconView
            val scale = runCatching {
                android.provider.Settings.Global.getFloat(contentResolver,
                    android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
            }.getOrDefault(1f).coerceAtLeast(0f)
            val elapsed = splash.iconAnimationStart?.let {
                java.time.Duration.between(it, java.time.Instant.now()).toMillis()
            } ?: 680L
            val remaining = if (scale == 0f || savedInstanceState != null) 0L
                else ((splash.iconAnimationDuration?.toMillis() ?: 680L) * scale - elapsed).toLong().coerceIn(0L, 1200L)
            splash.postDelayed({
                if (icon != null && icon.width > 0 && icon.height > 0) {
                    val location = IntArray(2)
                    icon.getLocationOnScreen(location)
                    introStartBounds = androidx.compose.ui.geometry.Rect(
                        location[0].toFloat(), location[1].toFloat(),
                        location[0] + icon.width.toFloat(), location[1] + icon.height.toFloat())
                }
                systemIntroComplete = true
                // The application draws the same chip at the system icon's screen bounds.
                // Remove the overlay only after that frame exists; all subsequent movement
                // belongs to this single Compose image, including its final resting frame.
                val deadline = android.os.SystemClock.uptimeMillis() + 500L
                val finish = object : Runnable {
                    override fun run() {
                        if (introIconDrawn || android.os.SystemClock.uptimeMillis() >= deadline) {
                            // The matching Compose frame is already drawn. Swap on this
                            // vsync rather than holding the chip for an extra fade-out.
                            introOverlayRemoved = true
                            splash.remove()
                        } else splash.postOnAnimation(this)
                    }
                }
                splash.postOnAnimation(finish)
            }, remaining)
        }

        setContent {
            var introVisible by rememberSaveable { mutableStateOf(savedInstanceState == null && !initialAppearance.skipStartupIntro) }
            var introMotionComplete by remember { mutableStateOf(savedInstanceState != null || initialAppearance.skipStartupIntro) }
            var appearance by remember { mutableStateOf(initialAppearance) }
            val liveThemeRegions = remember { androidx.compose.runtime.mutableStateMapOf<Any, androidx.compose.ui.geometry.Rect>() }
            val themeLayer = androidx.compose.ui.graphics.rememberGraphicsLayer()
            var oldThemeSnapshot by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
            var themeJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
            val themeScope = rememberCoroutineScope()
            
            val themeOverlayAlpha = remember { androidx.compose.animation.core.Animatable(0f) }
            Box(Modifier.fillMaxSize()) {
            CompositionLocalProvider(
                com.zimky.coregateUS.ui.theme.LocalThemeLiveRegions provides liveThemeRegions,
                LocalAppSettings provides appearance,
                LocalSetAppSettings provides updateAppearance@{ next ->
                    if (next != appearance) {
                        val themeChanged = next.colorMode != appearance.colorMode ||
                            next.keyColor != appearance.keyColor ||
                            next.paletteStyle != appearance.paletteStyle || next.colorSpec != appearance.colorSpec
                        if (!themeChanged) {
                            appearance = next
                            next.save(this@MainActivity)
                            return@updateAppearance
                        }
                        themeJob?.cancel()
                        themeJob = themeScope.launch {
                            // Independent pixels: live child layers cannot serve as a frozen frame.
                            val snapshot = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                                themeLayer.toImageBitmap()
                            }
                            try {
                                themeOverlayAlpha.snapTo(1f)
                                oldThemeSnapshot = snapshot
                                appearance = next
                                next.save(this@MainActivity)
                                androidx.compose.runtime.withFrameNanos { }
                                themeOverlayAlpha.animateTo(0f, tween(420))
                            } finally {
                                oldThemeSnapshot = null
                            }
                        }
                    }
                }
            ) {
            Box(Modifier.fillMaxSize().drawWithContent {
                themeLayer.record { this@drawWithContent.drawContent() }
                drawLayer(themeLayer)
            }) {
            MiuixKernelSUTheme(appSettings = appearance, startupActive = introVisible) {
                // 开屏 root 门槛：拿不到 root 就根本进不来（拦截式，不是提醒式）。
                // 放在主题内部，保证检测页与主界面共用同一套配色，不会闪一次白屏。
                if (introMotionComplete || !introVisible) RootGate { AppRoot() }
            }
            }
            }
            // 开发者调试叠加层（手指实时坐标）。
            //
            // 必须挂在**最外层** Box 里、且 zIndex 高于所有页面与二级页（最高是 2f），
            // 否则会被 Scaffold / 二级页盖住，量不到被遮挡区域的坐标。
            // 组件内部自带 enabled 判断，关闭时连 pointerInput 都不挂，零开销零干扰。
            //
            // V2.4.1 两处改动（对应用户要求「5 秒后完全启动调试模式」）：
            //  1) 延迟挂载：打开开关后不立即接管屏幕，先给 debugArmIn 秒的缓冲，
            //     时间到了才真正 enabled=true。否则手指刚离开开关，
            //     整屏就被吞掉，用户连「我刚才点了什么」都看不出来。
            //  2) 底部放行区归零：旧版留 112dp 让底栏仍可点，导致「调试模式没完全生效」。
            //     现在整屏只量不点，导航栏同样不可用 —— 这正是「完全启动」的含义。
            //     退出调试只能靠杀掉应用重进（AppSettings 不跨进程保留，见 AppSettings.kt）。
            if (appearance.debugOverlay) {
                var debugArmed by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay(DEBUG_ARM_MS)
                    debugArmed = true
                }
                com.zimky.coregateUS.ui.DebugPointerOverlay(
                    enabled = debugArmed,
                    // 不再放行底部：调试模式一旦完全启动，全屏只量不点。
                    bottomReservedPx = 0f,
                    modifier = Modifier.fillMaxSize().zIndex(99f)
                )
            }
            oldThemeSnapshot?.let { snapshot ->
                Box(Modifier.fillMaxSize().graphicsLayer { alpha = themeOverlayAlpha.value }
                    .drawWithContent {
                        val livePath = androidx.compose.ui.graphics.Path().apply {
                            // Match the live controls' CircleShape: rectangular holes expose
                            // the new page background at all four corners during the fade.
                            liveThemeRegions.values.forEach { bounds ->
                                val radius = minOf(bounds.width, bounds.height) / 2f
                                addRoundRect(androidx.compose.ui.geometry.RoundRect(
                                    bounds, androidx.compose.ui.geometry.CornerRadius(radius, radius)
                                ))
                            }
                        }
                        clipPath(livePath, androidx.compose.ui.graphics.ClipOp.Difference) {
                            drawImage(snapshot)
                        }
                    })
            }
            if (introVisible) {
                StartupIntro(
                    started = systemIntroComplete,
                    moving = introOverlayRemoved,
                    startBounds = introStartBounds,
                    onIconBounds = { introIconBounds = it },
                    onIconDrawn = { introIconDrawn = true },
                    onMotionFinished = { introMotionComplete = true },
                    onFinished = { introVisible = false }
                )
            }
            }
        }
    }
}

/** 轮询间隔。对齐 WebUI 的 8 秒（v2.0.6 从 5 秒放宽后的值）。 */
private const val POLL_MS = 8_000L

/** 手动刷新冷却，防抖。 */
private const val MIN_REFRESH_GAP = 1_200L

/**
 * 调试模式的「武装」延迟（毫秒）。V2.4.1 新增。
 *
 * 打开开发者调试模式后，先等这么久才真正接管屏幕。
 * 理由：调试层一旦 enabled 就会吞掉整屏触摸（导航栏也不再放行），
 * 若在按下开关的同一帧生效，用户的手指还停在开关位置上，
 * 会以为「点了没反应」或者「卡住了」，实际是刚开始量坐标。
 * 给 5 秒缓冲，用户能看清界面、确认自己开了什么。
 */
private const val DEBUG_ARM_MS = 5_000L

/**
 * 二级页的形态。用字符串而不是 sealed class，是为了能直接存进 rememberSaveable，
 * 旋转屏幕后仍停在同一页。
 */
private object SubPage {
    const val NONE = ""
    const val FREEZE = "freeze"
    const val SCAN = "scan"
    const val PROCESSES = "processes"
}

/** 底栏四栏文案。 */
private val TAB_TITLES = listOf("锁核", "策略", "限频", "设置")

/**
 * 单页槽位。V2.4.1：由「瞬间显隐」改为 KernelSU 式左右横滑。
 *
 * 旧实现的问题（用户原话「导航栏界面启动是瞬间有点晃眼」）：
 *   只在 visible 翻转时做 180ms 透明度淡入，且不可见页直接把布局尺寸压成 0，
 *   切栏时是「上一帧 A、下一帧 B」的硬切，视觉上是一次闪跳。
 *
 * 新实现对齐 KSU 的 HorizontalPager 观感，但**不引入 Pager**：
 *   四页的既有 contract 是「常驻 composition、滚动位置与状态全部保留」，
 *   换成 HorizontalPager 会让页面在滑出屏幕后被弃置重组合，滚动条会跳回顶部，
 *   根页还会重跑 root 探测 —— 这与本项目「少发 su、别狂刷」的硬约束冲突。
 *
 * 做法（纯平移，零重建）：
 *   pagerPosition 是一条跟随 currentTab 的浮点动画（见 AppRoot），
 *   本页按 (index - pagerPosition) 乘以屏宽做横向位移：
 *     - 当前页 → 位移 0，不透明；
 *     - 右侧待进入的页 → 位移 +屏宽，等被滑入；
 *     - 左侧已离开的页 → 位移 -屏宽，滑出屏幕。
 *   只对「与当前位置距离 < 1 屏」的页做绘制与命中，其余页照旧不参与布局，
 *   避免四页同时保留大图层导致滚动掉帧。
 *
 * 边缘 30% 做线性淡入淡出，模拟 KSU 里页面交接时的轻微渐隐，消除「晃眼」。
 */
@Composable
private fun PageSlot(
    index: Int,
    position: Float,
    content: @Composable () -> Unit
) {
    val surface = MiuixTheme.colorScheme.surface
    // 与当前页的屏幕距离：0 = 正在显示，±1 = 已在屏幕外一整屏
    val distance = index - position
    val visible = kotlin.math.abs(distance) < 1f
    // 边缘渐隐：距离 0.7 之后开始透明，1 时完全透明
    val alpha = (1f - kotlin.math.abs(distance) * 1.42f).coerceIn(0f, 1f)

    androidx.compose.ui.layout.Layout(content = {
        Box(Modifier.fillMaxSize().background(surface)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { this.alpha = alpha }
            ) { content() }
        }
    }) { measurables, constraints ->
        if (visible) {
            val page = measurables.first().measure(constraints)
            // 横向整屏位移，纵向不动。roundToInt 避免像素缝隙
            val dx = (distance * constraints.maxWidth).toInt()
            layout(page.width, page.height) { page.placeRelative(dx, 0) }
        } else {
            // 屏幕外的页不测量、不布局，也不接收触摸
            layout(constraints.maxWidth, constraints.maxHeight) {}
        }
    }
}

@Composable
private fun AppRoot() {
    val main = remember { Handler(Looper.getMainLooper()) }

    // ---- 宿主状态：配置 / 包名列表 / 最近一次快照 ----
    var cfg by remember { mutableStateOf(CoreConfig()) }
    var packagesRaw by remember { mutableStateOf("") }
    var snapshot by remember { mutableStateOf<SysProbe.Snapshot?>(null) }
    var currentTab by rememberSaveable { mutableStateOf(0) }

    /**
     * 底栏切页的横向动画位置（浮点）。V2.4.1 新增。
     *
     * currentTab 是离散整数，直接用它做位移只会得到「0 或 ±1 屏」两种硬切结果。
     * 这里再挂一条 animateFloatAsState，把整数目标值补间成连续浮点：
     *   点「限频」→ 0f 平滑滚到 2f，中间会路过 1f，于是「策略」页也跟着滑一下，
     *   与 KernelSU 里 HorizontalPager 的跟手滚动手感一致。
     *
     * 缓动曲线用 FastOutSlowInEasing 而非 Linear：
     *   起手快、收尾缓，跨三栏（0→3）时不至于中间一段拖得太久；
     *   320ms 是 KSU 页面切换的观感时长，比原来的 180ms 淡入更有「滑动」的实感。
     *
     * 注意：这不是手势驱动的 Pager，参数完全由底栏点击驱动，
     * 所以不需要 nestedScroll / 拖动阈值那一套，也就不会与页内竖向滚动打架。
     */
    val pagerPosition by androidx.compose.animation.core.animateFloatAsState(
        targetValue = currentTab.toFloat(),
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 320,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "pagerPosition"
    )

    // 当前展开的二级页；SubPage.NONE 表示停在四栏主界面
    var subPage by rememberSaveable { mutableStateOf(SubPage.NONE) }
    var retainedSubPage by rememberSaveable { mutableStateOf(SubPage.FREEZE) }
    LaunchedEffect(subPage) {
        if (subPage != SubPage.NONE) retainedSubPage = subPage
    }
    val detailPage = if (subPage != SubPage.NONE) subPage else retainedSubPage
    var subPageOrigin by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }

    // 策略页要显示的「冻结了多少个」，由二级页与后台探测回报。
    // null = 还没探到（主页与策略页都会显示「还没探到」），0 是有效结果，不能和 null 混。
    var frozenCount by remember { mutableStateOf<Int?>(null) }

    // 刷新：串行 + 合并
    var refreshing by remember { mutableStateOf(false) }
    var refreshAgain by remember { mutableStateOf(false) }
    var lastRefreshAt by remember { mutableStateOf(0L) }
    var alive by remember { mutableStateOf(true) }
    var configRevision by remember { mutableStateOf(0L) }
    var pendingSaves by remember { mutableStateOf(0) }
    var freezeProbeRunning by remember { mutableStateOf(false) }
    var configLoading by remember { mutableStateOf(false) }
    var lastFreezeProbeAt by remember { mutableStateOf(0L) }

    // 底栏 backdrop。
    //
    // ⚠️ 这里曾经踩过一个坑：只创建 backdrop 而不给内容层挂 layerBackdrop，
    // 底栏的 drawBackdrop 就取不到任何内容像素，blur/lens/vibrancy 全部失效，
    // 底栏退化成一块不透明的纯色块 —— 也就是用户说的「不像 KSU，那条黑带子是死的」。
    //
    // KSU 的做法是两件套，缺一不可：
    //   1) 内容层（HorizontalPager）挂 Modifier.layerBackdrop(backdrop)  ← 采集
    //   2) 底栏交给 Scaffold 的 bottomBar 槽位，内部 drawBackdrop(backdrop) ← 消费
    // 本项目第 1 步之前完全没做，第 2 步用的是裸 Box 手动 align。
    // 现在两步都补齐：内容 Box 挂 layerBackdrop，底栏进 Scaffold 槽位。
    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    /**
     * 拉一次状态。
     *
     * 串行：同一时刻只允许一个 probe 在跑，期间来的请求只标记「再跑一次」。
     * 冷却：fromUser=true 时也照样有 1.2 秒最小间隔，因为狂点刷新正是
     *       v2.0.6 里弹安全警告的元凶。
     */
    fun refresh(fromUser: Boolean) {
        if (!alive) return
        val now = System.currentTimeMillis()
        if (fromUser && now - lastRefreshAt < MIN_REFRESH_GAP) {
            refreshAgain = true
            return
        }
        if (refreshing) {
            refreshAgain = true
            return
        }
        refreshing = true
        lastRefreshAt = now
        SysProbe.probe(RootShell) { s ->
            refreshing = false
            snapshot = s
            if (refreshAgain) {
                refreshAgain = false
                // 稍等一下再补，别把两次 probe 挤在一起
                main.postDelayed({ refresh(false) }, 300L)
            }
        }
    }

    /**
     * 探一次冻结实况，喂给策略页的摘要。
     *
     * 跑一条命令：`--freeze-status` 给冻结页的计数。
     *
     * 为什么不在 refresh() 里做：refresh 走的是 SysProbe，它读的是 sysfs 与
     * /data 下的状态文件，实现上完全独立；把冻结状态塞进同一个回调会让
     * 「一次刷新 = 两次 root」：用户狂点刷新时更容易被安全机制判为入侵。
     * 这里是独立的、低频的路径（只在启动与相关页改动后调用）。
     */
    fun probeFreeze() {
        if (!alive || freezeProbeRunning || System.currentTimeMillis() - lastFreezeProbeAt < 1500L) return
        freezeProbeRunning = true
        com.zimky.coregateUS.core.FreezeStatus.read { code, out, _ ->
            freezeProbeRunning = false
            lastFreezeProbeAt = System.currentTimeMillis()
            if (code == 0 && out.contains("[actual]") && out.contains("[groups]")) {
                frozenCount = FreezeData.parse(out).scopedFrozenCount
            }
        }
    }

    /** 读配置。root 授权弹窗被拒时 loaded 会是 false，需要能重试。 */
    fun loadConfig() {
        if (configLoading) return
        configLoading = true
        val revision = configRevision
        CoreConfig.load(RootShell) { c, pkgs, _ ->
            configLoading = false
            if (revision == configRevision && pendingSaves == 0 && c.loaded) {
                cfg = c
                packagesRaw = pkgs
            }
            refresh(false)
            probeFreeze()
        }
    }

    // 首次进入：先读配置，读到后再拉一次状态
    LaunchedEffect(Unit) {
        loadConfig()
    }

    // 生命周期：回到前台立刻刷一次
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // 自愈：首次启动时若还没来得及点「允许」，配置就会永远读不到。
                // 每次回前台发现没读到就重试一次（root 已授权时这是零成本）。
                if (pendingSaves == 0) loadConfig()
                refresh(false)
            }
        }
        alive = true
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            alive = false
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 轮询循环：只在 resumed 时真的去拉，切后台就空转不消耗 root
    LaunchedEffect(lifecycleOwner) {
        while (alive) {
            delay(POLL_MS)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                refresh(false)
            }
        }
    }

    /**
     * 保存配置。写盘成功后本地副本同步更新（避免界面回跳），
     * 再延迟一点补一次刷新，让守护进程的新状态尽快反映到界面。
     */
    fun save(newCfg: CoreConfig, packages: String?, done: ((Boolean, String) -> Unit)?) {
        if (!cfg.loaded) {
            done?.invoke(false, "配置尚未成功读取，请先重试读取")
            com.zimky.coregateUS.ui.Toaster.show("配置未读取，已阻止覆盖保存", true)
            return
        }
        configRevision += 1
        pendingSaves += 1
        val previousCfg = cfg
        val intended = newCfg.copy(loaded = true)
        cfg = intended
        val cmd = CoreConfig.buildSaveCmd(newCfg, packages)
        RootShell.execSave(cmd) { code, out, err ->
            pendingSaves -= 1
            val ok = code == 0 && out.lineSequence().any { it.trim() == "OK" }
            if (ok) {
                if (cfg == intended) cfg = intended
                if (packages != null) packagesRaw = packages
                refresh(false)
                // 配置可能正好改了冻结分组，摘要要跟着变，否则策略页还停在旧数字上
                main.postDelayed({ probeFreeze() }, 900L)
            }
            if (!ok && cfg == intended) cfg = previousCfg
            if (!ok) com.zimky.coregateUS.ui.Toaster.show("配置保存失败，请重试", true)
            done?.invoke(
                ok,
                if (ok) "OK" else err.takeIf { it.isNotEmpty() } ?: "退出码 $code"
            )
        }
    }

    fun closeSubPage() {
        subPage = SubPage.NONE
        loadConfig()
    }
    BackHandler(enabled = subPage != SubPage.NONE) { closeSubPage() }

    // 根布局：Scaffold(bottomBar = …)，与 KernelSU 官方 ui/MainActivity.kt 的
    // `Scaffold(bottomBar = bottomBar) { pagerContent(innerPadding.calculateBottomPadding()) }`
    // 逐条对齐。底栏进槽位有两个必须的理由：
    //   1) 槽位横向撑满，FloatingBottomBar 内部的 width(IntrinsicSize.Min) 才能拿到确定宽度，
    //      不会塌缩成圆（v2.2.0 初版「文字竖排」的缺陷就是缺这个约束）；
    //   2) 浮层不再由业务代码手动 align，层级由 Scaffold 保证，backdrop 绘制次序稳定。
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background),
        bottomBar = {
            // 歌单改用浮层占位，底栏不再跟随撤下（v2.3.3 原版行为）。
            if (subPage == SubPage.NONE) Box(modifier = Modifier.fillMaxWidth()) {
                val navBottomInset = WindowInsets.navigationBars
                    .asPaddingValues()
                    .calculateBottomPadding()
                // 底部间距照抄 KSU：有导航栏 inset 时 8.dp + inset，否则 28.dp
                val barBottomPadding = if (navBottomInset != 0.dp) 8.dp + navBottomInset else 28.dp
                FloatingBottomBar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = 28.dp, end = 28.dp, bottom = barBottomPadding),
                    selectedIndex = currentTab,
                    onSelected = { currentTab = it },
                    backdrop = backdrop,
                    tabsCount = TAB_TITLES.size,
                    isBlurEnabled = true
                ) { activateTab ->
                    TAB_TITLES.forEachIndexed { i, title ->
                        FloatingBottomBarItem(
                            selected = currentTab == i,
                            onClick = { activateTab(i) },
                            modifier = Modifier.defaultMinSize(minWidth = 76.dp)
                        ) {
                            Icon(
                                imageVector = listOf(Icons.Outlined.Memory, Icons.Outlined.Tune,
                                    Icons.Outlined.Speed, Icons.Outlined.Settings)[i],
                                contentDescription = title,
                                tint = MiuixTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        // 内容区：四页常驻，只显示当前那个。
        //
        // 缺陷 1 修复要点：内容层必须挂 Modifier.layerBackdrop(backdrop)。
        // layerBackdrop 让本节点绘制时把像素写进 backdrop 的离屏缓冲，
        // 底栏的 drawBackdrop(backdrop) 才能读到「底栏背后的界面」并做实时采样模糊。
        // 之前缺了这一句，backdrop 里只有 rememberLayerBackdrop 那次 drawRect(surfaceColor)，
        // 底栏于是永远是一块纯色，跟内容毫无互动 —— 这就是「不像 KSU」的根因。
        //
        // innerPadding 在这里只做「读取」而不用来缩进内容 —— 与 KSU 一致：
        // 内容铺满全屏、流到底栏背后，底栏的 blur 才有东西可采。
        // 各页自己的滚动内容由 PAGE_BOTTOM_INSET 兜住，不会被底栏遮住。
        // 用 calculateBottomPadding() 取值即可，避免「取了 innerPadding 却不用」的告警。
        @Suppress("UNUSED_VARIABLE")
        val bottomPad = innerPadding.calculateBottomPadding()

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
            ) {
                PageSlot(index = 0, position = pagerPosition) {
                    // 冻结摘要由 probeFreeze() 统一探测后下传，LockPage 自己不发 root。
                    // 入口和策略页指向同一批二级页，用户在哪一栏都能进。
                    LockPage(
                        cfg = cfg,
                        snapshot = snapshot,
                        packagesRaw = packagesRaw,
                        sh = RootShell,
                        onRefresh = { refresh(true) },
                        save = ::save,
                        frozenCount = frozenCount,
                        onOpenFreeze = { origin -> subPageOrigin = origin; subPage = SubPage.FREEZE },
                        onOpenScan = { origin -> subPageOrigin = origin; subPage = SubPage.SCAN },
                        onOpenProcesses = { origin -> subPageOrigin = origin; subPage = SubPage.PROCESSES }
                    )
                }
                PageSlot(index = 1, position = pagerPosition) {
                    PolicyPage(
                        cfg = cfg,
                        snapshot = snapshot,
                        packagesRaw = packagesRaw,
                        sh = RootShell,
                        onRefresh = { refresh(true) },
                        save = ::save,
                        frozenCount = frozenCount,
                        onOpenFreeze = { origin -> subPageOrigin = origin; subPage = SubPage.FREEZE }
                    )
                }
                PageSlot(index = 2, position = pagerPosition) {
                    FreqPage(cfg, snapshot, packagesRaw, RootShell, { refresh(false) }, ::save)
                }
                PageSlot(index = 3, position = pagerPosition) {
                    SettingsPage(cfg, snapshot, packagesRaw, RootShell, { refresh(true) }, ::save)
                }

                // 二级页：盖在四个常驻页之上。
                //
                // 为什么不塞进 PageSlot：
                //   四个 PageSlot 的 contract 是「常驻不销毁、只切透明度」，
                //   把二级页塞进去等于让它在切页时被组合/弃置，破坏该 constraint。
                //   所以这里在 PageSlot 之外单独一层，用 zIndex(2f) 压住可见页（可见页是 1f）。
                //
                // 二级页同样铺满全屏，状态栏留白由页内滚动内容提供。
                com.zimky.coregateUS.ui.LiquidReveal(
                    visible = subPage != SubPage.NONE,
                    origin = subPageOrigin,
                    modifier = Modifier.zIndex(2f)
                ) {
                    // 二级页必须是「完完全全的第二屏」。
                    //
                    // 用户反馈：「打开的第二页跟第一页重叠，是能互相看见的，不好看」。
                    // 根因是本层 Box 之前只有 zIndex(2f) + statusBarsPadding()：
                    //   1) 没有铺底色 → 下层可见页（zIndex 1f）的内容直接透出来；
                    //   2) 没有拦指针事件 → 点空白处会穿到下层页的按钮上，
                    //      表现成「看着是第二页，点下去却触发了第一页的东西」。
                    //
                    // 修法：整屏铺不透明主题底色（surface）+ 整块吞掉指针事件。
                    // 底色用 MiuixTheme.colorScheme.surface，与四个常驻页 PageSlot 的底色一致，
                    // 切换时不会有色差;铺满 fillMaxSize 后连状态栏区域也不透。
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(2f)
                            .background(MiuixTheme.colorScheme.surface)
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) {}
                    ) {
                        when (detailPage) {
                            SubPage.SCAN -> ScanDetailsPage(
                                snapshot = snapshot,
                                packagesRaw = packagesRaw,
                                onBack = { closeSubPage() },
                                onRefresh = { refresh(true) }
                            )
                            SubPage.PROCESSES -> ProcessManagerPage(
                                sh = RootShell,
                                onBack = { closeSubPage() },
                                onChanged = { refresh(true) }
                            )
                            else -> FreezePage(
                                cfg = cfg,
                                sh = RootShell,
                                onBack = { closeSubPage() },
                                onChanged = { loadConfig() }
                            )
                        }
                    }
                }
            }

            HomeMusicPlayer()

            // 顶部仍显示系统状态栏；界面在其背后滚动，薄雾保留内容层次。
            if (currentTab != 0 || subPage != SubPage.NONE) Box(Modifier.align(Alignment.TopCenter).fillMaxWidth()
                .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                .drawBackdrop(backdrop = backdrop, shape = { RectangleShape },
                    effects = { blur(18.dp.toPx(), 18.dp.toPx()) },
                    onDrawSurface = { drawRect(surfaceColor.copy(alpha = 0.36f)) }))

            // 读取失败提示条：只在配置真的读不到时出现。
            //
            // 对应需求：「如果配置无法读取，就多写个提示请检查是否给予 root 权限不就行了」。
            // 这是「首次授权已完成、但后来 root 被撤销」的唯一反馈通道 ——
            // 因为第一屏按设计永不再出现，不能再把用户踢回开屏页。
            //
            // 判定依据直接用 cfg.loaded（CoreConfig.load 在 su 失败 / 配置为空时会给 false），
            // 不额外探测 root，避免多一次 su 调用被判为入侵。
            // 授权恢复后下一次 ON_RESUME 自愈重试成功，loaded 变 true，提示条自动消失。
            if (!cfg.loaded) {
                NoRootHint(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 12.dp, start = 20.dp, end = 20.dp)
                )
            }

            // 轻提示层：盖在内容与底栏之上，屏幕正中淡入淡出
            //
            // 用户要求：「弹窗你别挂到最下面啊，你整到屏幕中间的那种弹窗」。
            // 之前贴在顶部 statusBars 下方，视觉上像一条通知条；改成整体居中后
            // 更像一个「提示」，也不会被悬浮底栏或页面标题挤到边缘。
            ToastHost(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
            )
        }
    }

}
/**
 * 主页内的「配置读取失败」提示条。
 *
 * 设计定位（对应用户原话）：
 *   「首次打开后就是完完全全的告诉你请输入 root，然后首次授予完之后第一屏永不再重现。
 *     如果配置无法读取，就多写个提示请检查是否给予 root 权限不就行了」
 *
 * 所以本组件承担的是「首次授权已完成、后来 root 被撤销」的**唯一**反馈通道：
 * 第一屏按设计永不再出现，撤销后不能把用户踢回开屏页，只能在这里点一句。
 *
 * 判定依据来自外部（MainActivity 用 cfg.loaded），本组件只负责显示，
 * 不自己探测 root、不自己发 su —— 探测越多越容易被系统安全机制误判为入侵。
 *
 * 恢复路径：用户在 KernelSU / Magisk 里重新授予后，回到前台触发 ON_RESUME，
 * MainActivity 里已有的 `if (!cfg.loaded) loadConfig()` 会自愈重试，
 * 成功则 loaded=true，本提示条自动消失，不需要「重试」按钮。
 *
 * 视觉对齐 RootGate 的 GateCard：同一张 Card、同一套字号与配色，
 * 避免「提示条」与「第一屏」像两个不同的 App。
 */
@Composable
private fun NoRootHint(modifier: Modifier = Modifier) {
    val cs = MiuixTheme.colorScheme
    Card(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            // 左侧强调竖条：一眼看出这是需要处理的状态，而不是普通说明
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(34.dp)
                    .clip(CircleShape)
                    .background(cs.primary)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "配置读取失败",
                    color = cs.primary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "请检查是否已给予 CoreGate root 权限。授权后回到前台会自动重新读取，无需重启。",
                    color = cs.onSurfaceVariantSummary,
                    fontSize = 11.5.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}