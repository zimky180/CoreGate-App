package com.zimky.coregateUS.page

import com.zimky.coregateUS.ui.component.FloatingBottomBar
import com.zimky.coregateUS.ui.component.FloatingBottomBarItem
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.layout.imePadding
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.ButtonDefaults

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zimky.coregateUS.core.CoreConfig
import com.zimky.coregateUS.core.RootShell
import com.zimky.coregateUS.core.SysProbe
import com.zimky.coregateUS.ui.PageHeading
import com.zimky.coregateUS.ui.SectionHeading
import com.zimky.coregateUS.ui.BottomSpacer
import com.zimky.coregateUS.ui.Gap
import com.zimky.coregateUS.ui.HGap
import com.zimky.coregateUS.ui.HairLine
import com.zimky.coregateUS.ui.HintText
import com.zimky.coregateUS.ui.NavRow
import com.zimky.coregateUS.ui.PAGE_BOTTOM_INSET
import com.zimky.coregateUS.ui.Semantic
import com.zimky.coregateUS.ui.SubText
import com.zimky.coregateUS.ui.SwitchRow
import com.zimky.coregateUS.ui.Toaster
import top.yukonga.miuix.kmp.basic.Badge
import com.zimky.coregateUS.ui.LiquidCard as Card
import com.zimky.coregateUS.ui.RollingText as Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 第 2 页：录屏/相机恢复、充电策略、轮询间隔、当前场景判断。
 *
 * 与旧 PolicyPage 对齐的三条关键行为（一条都不能动）：
 *  1. 开关是「由 cfg 单向驱动」，onCheckedChange 里只做 save，不需要 syncing 标记；
 *  2. 轮询间隔由保存按钮或输入法完成键提交，且与当前值相同则不写盘（避免无意义 root）；
 *  3. 输入框获得焦点期间，外部刷新回来的数字不覆盖用户正在编辑的内容。
 *
 * 视觉：Miuix [Card] + [Badge]，与 KernelSU 管理器首页同一套组件语言。
 */
@Composable
fun PolicyPage(
    cfg: CoreConfig,
    snapshot: SysProbe.Snapshot?,
    packagesRaw: String,
    sh: RootShell,
    onRefresh: () -> Unit,
    save: (CoreConfig, String?, ((Boolean, String) -> Unit)?) -> Unit,
    /** 冻结管理二级页里的「已冻结」数量快照，null 表示还没读到 */
    frozenCount: Int?,
    onOpenFreeze: (Rect) -> Unit
) {
    val interactive = cfg.loaded && cfg.policyMasterEnabled
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    var freezeOrigin by remember { mutableStateOf(Rect.Zero) }
    var detectSaving by remember { mutableStateOf(false) }
    val cs = MiuixTheme.colorScheme
    val surface = cs.surface
    val backdrop = rememberLayerBackdrop { drawRect(surface) }
    val s = snapshot
    val daemonAlive = s?.daemonAlive == true
    val total = s?.cores?.size ?: 0
    val onlineCount = s?.onlineCount ?: 0
    val allOnline = total > 0 && onlineCount == total
    val lockedCount = (total - onlineCount).coerceAtLeast(0)
    val pkg = s?.statePkg?.takeIf { it.isNotBlank() }

    // 冻结摘要：与主页卡片三严格同一套措辞，用户在两页看到同一件事必须读到同一句话
    val freezeSummary = when {
        frozenCount == null -> "还没探到"
        frozenCount == 0 -> "当前没有冻结任何服务"
        else -> "已冻结 $frozenCount 个服务"
    }
    // 输入框文本与焦点状态
    var intervalText by remember { mutableStateOf(cfg.pollInterval.toString()) }
    var inputFocused by remember { mutableStateOf(false) }
    var intervalSaving by remember { mutableStateOf(false) }
    fun saveInterval() {
        if (!cfg.loaded || intervalSaving) return
        commitInterval(intervalText, cfg) { text, next ->
            intervalText = text
            if (next != null) {
                intervalSaving = true
                save(next, null) { ok, msg ->
                    intervalSaving = false
                    Toaster.show(if (ok) "轮询间隔已设为 ${next.pollInterval} 秒" else "保存失败：$msg", !ok)
                }
            }
        }
        keyboard?.hide()
        focus.clearFocus()
    }
    // 游戏状态轮询间隔：与日常间隔完全同一套三状态约束，默认 30，范围 5~600
    var gameIntervalText by remember { mutableStateOf(cfg.gamePollInterval.toString()) }
    var gameInputFocused by remember { mutableStateOf(false) }
    var gameIntervalSaving by remember { mutableStateOf(false) }
    fun saveGameInterval() {
        if (!cfg.loaded || gameIntervalSaving) return
        commitGameInterval(gameIntervalText, cfg) { text, next ->
            gameIntervalText = text
            if (next != null) {
                gameIntervalSaving = true
                save(next, null) { ok, msg ->
                    gameIntervalSaving = false
                    Toaster.show(if (ok) "游戏间隔已设为 ${next.gamePollInterval} 秒" else "保存失败：$msg", !ok)
                }
            }
        }
        keyboard?.hide()
        focus.clearFocus()
    }

    // 外部刷新回来的配置变化：仅在未聚焦时同步显示值
    if (!inputFocused && intervalText != cfg.pollInterval.toString()) {
        intervalText = cfg.pollInterval.toString()
    }
    // 同上约束：用户正在编辑游戏间隔时，外部刷新不得覆盖
    if (!gameInputFocused && gameIntervalText != cfg.gamePollInterval.toString()) {
        gameIntervalText = cfg.gamePollInterval.toString()
    }


    Column(
        modifier = Modifier
            // 一级页必须自己铺满整屏：
            // 这四页是常驻 composition 的，靠 PageSlot 的 zIndex 叠在一起。
            // 根 Column 若只写 fillMaxWidth，竖向高度会随内容收缩，
            // 内容不足一屏时下面那块就不属于本页 —— 既会透出下层页，
            // 点击也会漏到别的页去（真机反馈的「按钮点不着」）。
            // fillMaxSize 同时给 verticalScroll 一个有界高度的视口。
            .fillMaxSize()
            .imePadding()
            .background(cs.surface)
            .verticalScroll(rememberScrollState())
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = PAGE_BOTTOM_INSET),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        PageHeading("策略", "按使用场景自动调整核心")

        // ── 卡片零：策略总开关（V2.4.7 需求2）────────────────────────
        // 关掉后策略页全部功能失效：App 侧所有策略开关置灰不可改，
        // sh 侧跳过可选策略的额外读取与判定；基础轮询、游戏识别、限频和冻结独立运行。
        // 重新打开会直接恢复关闭前的配置（配置值本身从未被清空）。
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                SwitchRow(
                    title = "启用策略功能",
                    desc = if (cfg.policyMasterEnabled) {
                        "策略已启用，模块会按下面的开关自动调整核心。"
                    } else {
                        "策略已关闭：跳过温度、充电、录屏/相机恢复和游戏保持锁核策略。基础轮询、前台与游戏识别、日常限频和冻结仍独立运行。"
                    },
                    trailing = {
                        Switch(
                            checked = cfg.policyMasterEnabled,
                            onCheckedChange = { on ->
                                save(cfg.copy(policyMasterEnabled = on), null) { ok, msg ->
                                    if (ok) onRefresh() else Toaster.show("保存失败：$msg", true)
                                }
                                Toaster.show(if (on) "策略功能已启用" else "策略功能已关闭")
                            },
                            colors = switchColors()
                        )
                    }
                )
                if (!interactive) {
                    Gap(10.dp)
                    HintText(
                        "关闭期间 sh 进程不再读取温度、充电状态与前台应用，功耗最低。重新打开后恢复关闭前的全部策略配置。",
                        color = Semantic.WARN
                    )
                }
            }
        }

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // ── 卡片四：当前场景判断 ─────────────────────────────────────
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                Text(text = "当前场景判断", color = cs.onSurface, fontSize = 16.sp)
                Gap(12.dp)

                if (s == null) {
                    StateBadge("读取中", cs.onSurfaceVariantSummary)
                    Gap(10.dp)
                    SubText("正在读取当前场景。")
                } else if (!daemonAlive) {
                    StateBadge("未运行", cs.error)
                    Gap(10.dp)
                    SubText("守护进程未运行，场景判断暂停。")
                } else if (allOnline) {
                    StateBadge("已放开", Semantic.ACC)
                    Gap(10.dp)
                    SubText(
                        if (pkg != null) {
                            "全部核心在线。恢复条件可能来自游戏、录屏、充电或温度策略；前台记录：$pkg。"
                        } else {
                            "全部核心在线：可能来自自动恢复、温度策略，或锁核总开关已关闭。"
                        }
                    )
                } else {
                    BreathingBadge("锁核中", cs.primary)
                    Gap(10.dp)
                    SubText(
                        if (pkg != null) {
                            "当前按日常锁核或温度锁核策略执行。前台记录：$pkg。"
                        } else {
                            "前台应用无法读取（ColorOS 限制），当前按日常或温度锁核策略执行。"
                        }
                    )
                    Gap(10.dp)
                    SubText("已封锁 $lockedCount 个核心以节省功耗。")
                }
            }
        }

        // Only recovery and scene policies follow the master switch.
        Box(modifier = Modifier.fillMaxWidth().alpha(if (interactive) 1f else 0.42f)) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeading("自动恢复", "满足已开启的条件时恢复全部核心")

                // ── 卡片一：录屏 / 相机自动恢复 ──────────────────────────────
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                        SwitchRow(
                            title = "录屏或拍照时恢复全部核心",
                            desc = "检测到名单内的录屏或相机应用时恢复全部核心。",
                            trailing = {
                                Switch(
                                    checked = cfg.policyMasterEnabled && cfg.captureBoost,
                                    enabled = interactive,
                                    onCheckedChange = { on ->
                                        save(cfg.copy(captureBoost = on), null) { ok, _ ->
                                            if (ok) Toaster.show(if (on) "已开启录屏/相机自动恢复" else "已关闭录屏/相机自动恢复")
                                        }
                                    },
                                    colors = switchColors()
                                )
                            }
                        )

                        Gap(16.dp)
                        HairLine()
                        Gap(16.dp)

                        SwitchRow(
                            title = "充电时不锁核",
                            desc = "充电期间保持全部核心在线，拔下后恢复日常策略。",
                            trailing = {
                                Switch(
                                    checked = cfg.policyMasterEnabled && cfg.noLockCharging,
                                    enabled = interactive,
                                    onCheckedChange = { on ->
                                        save(cfg.copy(noLockCharging = on), null) { ok, _ ->
                                            if (ok) Toaster.show(if (on) "已开启充电不锁核" else "已关闭充电不锁核")
                                        }
                                    },
                                    colors = switchColors()
                                )
                            }
                        )

                        Gap(10.dp)
                        HintText("默认关闭：即便在充电，也照常锁核。")
                    }
                }

                TemperaturePolicyCard(cfg, snapshot, save)

            }
            if (!interactive) {
                // Hit-test the overlay, leaving events unconsumed for parent scrolling.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) awaitPointerEvent()
                            }
                        }
                )
            }
        }
                SectionHeading("检测节奏")
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                        Text(text = "检测范围", color = cs.onSurface, fontSize = 16.sp)
                        Gap(12.dp)
                        FloatingBottomBar(
                            modifier = Modifier.fillMaxWidth(),
                            selectedIndex = if (cfg.fgOnlyDetect) 0 else 1,
                            onSelected = { index ->
                                val only = index == 0
                                if (cfg.loaded && !detectSaving && cfg.fgOnlyDetect != only) {
                                    detectSaving = true
                                    save(cfg.copy(fgOnlyDetect = only), null) { ok, msg ->
                                        detectSaving = false
                                        Toaster.show(if (ok) "检测范围已更新" else "保存失败：$msg", !ok)
                                    }
                                }
                            },
                            backdrop = backdrop, tabsCount = 2, compact = true
                        ) { activate ->
                            listOf("只读前台", "前台+后台").forEachIndexed { index, label ->
                                val selected = cfg.fgOnlyDetect == (index == 0)
                                FloatingBottomBarItem(selected = selected, onClick = {
                                    if (cfg.loaded && !detectSaving) activate(index)
                                }) {
                                    Text(label, fontSize = 13.sp, maxLines = 1,
                                        color = if (selected) cs.primary else cs.onSurfaceVariantSummary)
                                }
                            }
                        }
                        Gap(10.dp)
                        HintText("只读前台包含可见小窗、分屏和画中画；前台+后台额外检测游戏名单内的存活进程。录屏与相机始终只认前台。")
                    }
                }


                // ── 卡片三：轮询间隔 ─────────────────────────────────────────
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                        Text(text = "日常检测", color = cs.onSurface, fontSize = 16.sp)
                        Gap(10.dp)
                        SubText("间隔越短，场景切换响应越快，唤醒次数也越多。")
                        Gap(12.dp)

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                enabled = cfg.loaded,
                                value = intervalText,
                                onValueChange = { v ->
                                    // 只接受数字，最多 3 位，避免用户输入出奇怪内容
                                    intervalText = v.filter { it.isDigit() }.take(3)
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .width(84.dp)
                                    .onFocusChanged { state ->
                                        inputFocused = state.isFocused
                                    },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { saveInterval() }),
                                shape = CircleShape,
                                textStyle = TextStyle(fontSize = 14.sp, color = cs.onSurface),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = cs.onSurface,
                                    unfocusedTextColor = cs.onSurface,
                                    focusedBorderColor = cs.primary,
                                    unfocusedBorderColor = cs.outline,
                                    focusedContainerColor = cs.surfaceContainerHigh,
                                    unfocusedContainerColor = cs.surfaceContainerHigh,
                                    cursorColor = cs.primary
                                )
                            )
                            HGap(10.dp)
                            Text(text = "秒", color = cs.onSurfaceVariantSummary, fontSize = 14.sp)
                            HGap(12.dp)
                            TextButton(
                                text = if (intervalSaving) "保存中" else "保存",
                                cornerRadius = 100.dp,
                                colors = ButtonDefaults.textButtonColorsPrimary(),
                                onClick = { saveInterval() }, enabled = cfg.loaded && !intervalSaving
                            )
                        }

                        Gap(10.dp)
                        HintText("允许范围 5 ~ 600 秒，默认 10 秒。留空或填非法值将回退为 10。")
                    }
                }

                // ── 卡片三之二：游戏状态轮询间隔 ─────────────────────────────
                // 与上面日常间隔刻意拆成两张卡：两者用途不同（日常场景切换 vs 游戏检测），
                // 用户要的是「两种间隔都能自己调」。约束与日常间隔完全一致。
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                        Text(text = "游戏间隔", color = cs.onSurface, fontSize = 16.sp)
                        Gap(10.dp)
                        SubText("检测到游戏时的轮询节奏，间隔越短越跟手，唤醒次数也越多。")
                        Gap(12.dp)

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                enabled = cfg.loaded,
                                value = gameIntervalText,
                                onValueChange = { v ->
                                    // 只接受数字，最多 3 位，避免用户输入出奇怪内容
                                    gameIntervalText = v.filter { it.isDigit() }.take(3)
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .width(84.dp)
                                    .onFocusChanged { state ->
                                        gameInputFocused = state.isFocused
                                    },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { saveGameInterval() }),
                                shape = CircleShape,
                                textStyle = TextStyle(fontSize = 14.sp, color = cs.onSurface),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = cs.onSurface,
                                    unfocusedTextColor = cs.onSurface,
                                    focusedBorderColor = cs.primary,
                                    unfocusedBorderColor = cs.outline,
                                    focusedContainerColor = cs.surfaceContainerHigh,
                                    unfocusedContainerColor = cs.surfaceContainerHigh,
                                    cursorColor = cs.primary
                                )
                            )
                            HGap(10.dp)
                            Text(text = "秒", color = cs.onSurfaceVariantSummary, fontSize = 14.sp)
                            HGap(12.dp)
                            TextButton(
                                text = if (gameIntervalSaving) "保存中" else "保存",
                                cornerRadius = 100.dp,
                                colors = ButtonDefaults.textButtonColorsPrimary(),
                                onClick = { saveGameInterval() }, enabled = cfg.loaded && !gameIntervalSaving
                            )
                        }

                        Gap(10.dp)
                        HintText("允许范围 5 ~ 600 秒，默认 30 秒。留空或填非法值将回退为 30。")
                    }
                }

                SectionHeading("更多策略")

                // ── 卡片五：更多策略（点进各自的二级页）───────────────────────
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {

                        NavRow(
                            title = "冻结管理",
                            summary = freezeSummary,
                            modifier = Modifier.onGloballyPositioned { freezeOrigin = it.boundsInRoot() },
                            onClick = { if (cfg.loaded) onOpenFreeze(freezeOrigin) }
                        )


                        Gap(10.dp)
                        HintText("冻结管理仅适用于 ColorOS，其他厂商系统无效果。")
                        Gap(6.dp)
                        HintText("以上项默认关闭，改之前请先看清页面的说明。")
                    }
                }



        }
        BottomSpacer()
    }
}

/** 状态徽标：静态，白色文字，与 KernelSU 徽标一致。 */
@Composable
private fun StateBadge(text: String, color: Color) {
    Badge(containerColor = color, contentColor = Color.White) {
        Text(text = text, fontSize = 12.sp, color = Color.White)
    }
}

/** 呼吸徽标：锁核进行中时用轻微明暗变化提示「正在工作」（0.55~1.0，周期 1.4 秒）。 */
@Composable
private fun BreathingBadge(text: String, color: Color) {
    val transition = rememberInfiniteTransition(label = "policyBadgeBreath")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "policyBadgeAlpha"
    )
    Box(modifier = Modifier.alpha(alpha)) {
        Badge(containerColor = color, contentColor = Color.White) {
            Text(text = text, fontSize = 12.sp, color = Color.White)
        }
    }
}

/**
 * 提交轮询间隔。
 *
 * @param text 输入框当前文本
 * @param cfg  当前配置
 * @param done text = 回填输入框的规范化文本；newCfg = 需要写盘的新配置（null 表示不需要写）
 */
private fun commitInterval(
    text: String,
    cfg: CoreConfig,
    done: (text: String, newCfg: CoreConfig?) -> Unit
) {
    val raw = text.trim().toIntOrNull()
    // 留空或非法：回退默认 10
    val normalized = if (raw == null) 10 else CoreConfig.clampInterval(raw)
    // 与当前配置一致：不写盘，只把输入框回填成规范值
    if (normalized == cfg.pollInterval) {
        done(normalized.toString(), null)
        return
    }
    done(normalized.toString(), cfg.copy(pollInterval = normalized))
}

/**
 * 游戏状态轮询间隔（game_poll_interval）提交：与 commitInterval 完全同构，
 * 只有默认值不同（30）以及写入的字段不同。
 */
private fun commitGameInterval(
    text: String,
    cfg: CoreConfig,
    done: (text: String, newCfg: CoreConfig?) -> Unit
) {
    val raw = text.trim().toIntOrNull()
    // 留空或非法：回退默认 30
    val normalized = if (raw == null) 30 else CoreConfig.clampInterval(raw)
    // 与当前配置一致：不写盘，只把输入框回填成规范值
    if (normalized == cfg.gamePollInterval) {
        done(normalized.toString(), null)
        return
    }
    done(normalized.toString(), cfg.copy(gamePollInterval = normalized))
}

/** 开关配色：统一走 Miuix 主题色。 */
@Composable
private fun switchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = MiuixTheme.colorScheme.primary,
    checkedBorderColor = Color.Transparent,
    uncheckedThumbColor = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    uncheckedTrackColor = MiuixTheme.colorScheme.surfaceContainerHigh,
    uncheckedBorderColor = MiuixTheme.colorScheme.outline
)