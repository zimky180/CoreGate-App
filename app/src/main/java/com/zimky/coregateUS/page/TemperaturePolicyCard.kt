package com.zimky.coregateUS.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Switch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zimky.coregateUS.core.CoreConfig
import com.zimky.coregateUS.core.SysProbe
import com.zimky.coregateUS.ui.*
import com.zimky.coregateUS.ui.component.FloatingBottomBar
import com.zimky.coregateUS.ui.component.FloatingBottomBarItem
import top.yukonga.miuix.kmp.basic.Badge
import com.zimky.coregateUS.ui.LiquidCard as Card
import com.zimky.coregateUS.ui.RollingText as Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.Locale

/** 游戏与电池温度场景，配置交由模块执行。 */
@Composable
internal fun TemperaturePolicyCard(
    cfg: CoreConfig,
    snapshot: SysProbe.Snapshot?,
    save: (CoreConfig, String?, ((Boolean, String) -> Unit)?) -> Unit
) {
    val cs = MiuixTheme.colorScheme
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    var thresholdText by remember { mutableStateOf(cfg.temperatureThreshold.toString()) }
    var focused by remember { mutableStateOf(false) }
    var pending by remember { mutableIntStateOf(0) }
    val busy = pending > 0
    var gameDraft by remember(cfg.gameLockCores) {
        mutableStateOf(cfg.gameLockCores.split(',').mapNotNull { it.toIntOrNull() }.toSet())
    }
    var extraDraft by remember(cfg.temperatureLockCores) {
        mutableStateOf(CoreConfig.normalizeTemperatureCores(cfg.temperatureLockCores).split(',').mapNotNull { it.toIntOrNull() }.toSet())
    }
    LaunchedEffect(cfg.temperatureThreshold, focused) {
        if (!focused) thresholdText = cfg.temperatureThreshold.toString()
    }
    fun persist(next: CoreConfig) {
        if (!cfg.loaded || !cfg.policyMasterEnabled) return
        pending += 1
        Toaster.show("场景策略已更新")
        save(next, null) { ok, msg ->
            pending = (pending - 1).coerceAtLeast(0)
            if (!ok) Toaster.show("保存失败：$msg", true)
        }
    }
    fun commitThreshold() {
        if (!cfg.loaded || !cfg.policyMasterEnabled) return
        val threshold = CoreConfig.clampTemperature(thresholdText.toIntOrNull() ?: 40)
        thresholdText = threshold.toString()
        if (threshold != cfg.temperatureThreshold) persist(cfg.copy(temperatureThreshold = threshold))
        keyboard?.hide()
        focus.clearFocus()
    }
    val parts = snapshot?.temperatureRaw.orEmpty().split('|')
    val stamp = parts.getOrNull(4)?.toLongOrNull() ?: 0L
    val age = System.currentTimeMillis() / 1000 - stamp
    val fresh = snapshot?.daemonAlive == true && stamp > 0 && age in 0..(cfg.pollInterval + 60).toLong()
    val sourceMatches = parts.getOrNull(5) == "battery"
    val matches = parts.getOrNull(2) == cfg.temperatureMode &&
        parts.getOrNull(3)?.toIntOrNull() == cfg.temperatureThreshold
    val status = when {
        !cfg.policyMasterEnabled || !cfg.temperatureEnabled -> "已关闭"
        !cfg.enabled -> "模块已停用"
        snapshot?.daemonAlive != true -> "等待模块运行"
        !fresh -> "等待模块更新"
        !sourceMatches -> "请更新配套模块"
        !matches || parts.getOrNull(6) != cfg.temperatureLockCores -> "等待策略同步"
        parts[0] == "scene_skipped" -> "自动恢复中，暂停额外锁核"
        parts[0] == "no_extra" -> "尚未选择额外核心"
        parts[0] == "active" -> "已触发"
        parts[0] == "watching" -> "监测中"
        parts[0] == "unavailable" -> "传感器不可用"
        else -> "等待模块执行"
    }
    val temp = if (fresh && sourceMatches && cfg.temperatureEnabled) parts.getOrNull(1)?.toIntOrNull() else null
    // V2.3.6：CPU / SoC 热点温度已整体移除，只保留电池温度。
    // 温度串第 9 段仍保留空位（恒为空串），以免破坏按 | 切分的旧逻辑。
    val surface by rememberUpdatedState(cs.surfaceContainer)
    val backdrop = rememberLayerBackdrop { drawRect(surface) }
    val available = cfg.loaded && cfg.policyMasterEnabled

    SectionHeading("场景策略", "游戏保持锁核与电池温度策略")
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SwitchRow(
                title = "游戏保持锁核",
                desc = "进游戏时只锁下面勾选的核心，其余全放开。",
                trailing = {
                    Switch(checked = cfg.policyMasterEnabled && cfg.gameLockEnable, enabled = available && cfg.policyMasterEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                            checkedTrackColor = cs.primary,
                            checkedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                            uncheckedThumbColor = cs.onSurfaceVariantSummary,
                            uncheckedTrackColor = cs.surfaceContainerHigh,
                            uncheckedBorderColor = cs.outline),
                        onCheckedChange = { persist(cfg.copy(gameLockEnable = it)) })
                }
            )
            val gameUsable = available && cfg.gameLockEnable
            if (cfg.gameLockEnable) {
                SubText("勾选的核心进游戏后被封锁，并显示为红色。")
                val gameCores = snapshot?.cores.orEmpty().sortedBy { it.id }.filter { it.id > 0 }
                if (gameCores.isEmpty()) HintText("等待读取核心列表后可自定义。")
                gameCores.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { core ->
                            val selected = core.id in gameDraft
                            TextButton(text = "CPU${core.id}${if (selected) " ✓" else ""}",
                                enabled = gameUsable, modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.textButtonColors(textColor = if (selected) cs.primary else cs.onSurfaceVariantSummary),
                                onClick = { gameDraft = if (selected) gameDraft - core.id else gameDraft + core.id })
                        }
                    }
                }
                SubText("已保存的游戏核心：${cfg.gameLockCores.ifBlank { "未选择（游戏时全部解封）" }}")
                TextButton(text = "保存游戏核心", enabled = gameUsable && gameDraft.sorted().joinToString(",") != cfg.gameLockCores,
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = { persist(cfg.copy(gameLockCores = gameDraft.sorted().joinToString(","))) })
            }
            HintText("少锁一颗大核可能影响游戏性能。高温额外锁核只在日常场景叠加。")
            HairLine()
            SwitchRow(
                title = "启用温度策略",
                desc = "达到阈值时执行所选动作，降温后恢复当前场景策略。",
                trailing = {
                    Switch(checked = cfg.policyMasterEnabled && cfg.temperatureEnabled, enabled = available && cfg.policyMasterEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                            checkedTrackColor = cs.primary,
                            checkedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                            uncheckedThumbColor = cs.onSurfaceVariantSummary,
                            uncheckedTrackColor = cs.surfaceContainerHigh,
                            uncheckedBorderColor = cs.outline),
                        onCheckedChange = { persist(cfg.copy(temperatureEnabled = it)) })
                }
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column {
                        Text(if (temp != null) String.format(Locale.ROOT, "%.1f ℃", temp / 1000.0) else "— ℃",
                            color = cs.primary, fontSize = 26.sp)
                        Text("电池温度", color = cs.onSurfaceVariantSummary, fontSize = 12.sp)
                    }
                }
                Badge(containerColor = cs.primary.copy(alpha = 0.12f), contentColor = cs.primary) {
                    AnimatedGradientText(status, cs.primary)
                }
            }
            HairLine()
            Text("高温时的动作", color = cs.onSurface, fontSize = 14.sp)
            Box(Modifier.fillMaxWidth()) {
                
                FloatingBottomBar(
                    modifier = Modifier.fillMaxWidth(),
                    selectedIndex = if (cfg.temperatureMode == "unlock") 1 else 0,
                    onSelected = { index ->
                        val mode = if (index == 1) "unlock" else "lock"
                        if (available && mode != cfg.temperatureMode) persist(cfg.copy(temperatureMode = mode))
                    },
                    backdrop = backdrop, tabsCount = 2, compact = true
                ) { activate ->
                    listOf("高温锁核", "高温解封").forEachIndexed { index, label ->
                        val selected = (cfg.temperatureMode == "unlock") == (index == 1)
                        FloatingBottomBarItem(selected = selected, onClick = { if (available) activate(index) }) {
                            Text(label, fontSize = 13.sp, maxLines = 1,
                                color = if (selected) cs.primary else cs.onSurfaceVariantSummary)
                        }
                    }
                }
            }
            SubText(if (cfg.temperatureMode == "unlock")
                "达到阈值后放开全部核心并撤销限频。"
                else "达到阈值后额外封锁下面选中的核心。")
            HintText("只看电池温度，读取稳定不会漏判。")
            if (cfg.temperatureMode == "lock") {
                Text("高温额外锁核", color = cs.onSurface, fontSize = 14.sp)
                SubText("日常封锁：${if (cfg.lockCores == "auto") "自动选择" else cfg.lockCores}。以下额外选择，重复不累加，CPU0 始终保留。")
                val cores = snapshot?.cores.orEmpty().sortedBy { it.id }.filter { it.id > 0 }
                if (cores.isEmpty()) HintText("等待读取核心列表后可自定义。")
                cores.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { core ->
                            val selected = core.id in extraDraft
                            TextButton(
                                text = "CPU${core.id}${if (selected) " ✓" else ""}",
                                enabled = available,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.textButtonColors(textColor = if (selected) cs.primary else cs.onSurfaceVariantSummary),
                                onClick = { extraDraft = if (selected) extraDraft - core.id else extraDraft + core.id }
                            )
                        }
                    }
                }
                SubText("已保存的额外核心：${cfg.temperatureLockCores.ifBlank { "未选择（高温时不会额外锁核）" }}")
                TextButton(text = "保存额外核心", enabled = available && extraDraft.sorted().joinToString(",") != cfg.temperatureLockCores,
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = { persist(cfg.copy(temperatureLockCores = extraDraft.sorted().joinToString(","))) })
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = thresholdText,
                    onValueChange = { thresholdText = it.filter(Char::isDigit).take(3) },
                    label = { Text("触发阈值", fontSize = 12.sp, color = cs.onSurfaceVariantSummary) },
                    shape = CircleShape,
                    singleLine = true, enabled = available,
                    modifier = Modifier.width(96.dp).onFocusChanged { focused = it.isFocused },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { commitThreshold() }),
                    textStyle = TextStyle(fontSize = 14.sp, color = cs.onSurface),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = cs.primary, unfocusedBorderColor = cs.outline,
                        focusedContainerColor = cs.surfaceContainerHigh,
                        unfocusedContainerColor = cs.surfaceContainerHigh, cursorColor = cs.primary)
                )
                Text("℃", color = cs.onSurfaceVariantSummary, fontSize = 16.sp)
                TextButton(text = if (busy) "保存中" else "保存阈值", enabled = available,
                    cornerRadius = 100.dp,
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = { commitThreshold() })
            }
            HintText("35～60℃，默认 40℃。降至阈值以下 3℃时退出温度策略，按轮询间隔检测。")
            HintText(if (cfg.temperatureMode == "lock")
                "高温额外锁核叠加于日常或游戏自定义名单；未设置游戏名单时保持游戏全解封。已开启的录屏/相机恢复及充电恢复优先，未选额外核心时不新增封锁。"
                else "高温解封恢复全部核心并撤销限频；总开关与安全模式优先。")
            HintText("读取不到有效电池温度时撤销温度策略；系统自身温控继续生效。配置由守护进程执行，关闭 App 后仍生效。")
        }
    }
}