package com.zimky.coregateUS.page

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.zimky.coregateUS.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import com.zimky.coregateUS.ui.RollingText as Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zimky.coregateUS.core.RootProcesses
import com.zimky.coregateUS.core.RootShell
import com.zimky.coregateUS.core.SysProbe
import com.zimky.coregateUS.ui.ConfirmDialog
import com.zimky.coregateUS.ui.LiquidCard as Card
import com.zimky.coregateUS.ui.Toaster
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ScanDetailsPage(snapshot: SysProbe.Snapshot?, packagesRaw: String, onBack: () -> Unit, onRefresh: () -> Unit) {
    val cs = MiuixTheme.colorScheme
    var query by remember { mutableStateOf("") }
    val scan = snapshot?.scanRaw.orEmpty().split('|', limit = 5)
    val packages = scan.getOrNull(4).orEmpty().split(',').filter { it.isNotBlank() }.distinct().sorted()
    val context = LocalContext.current
    val gamePackages = remember(packagesRaw) { packagesRaw.lineSequence().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.toSet() }
    var category by remember { mutableStateOf(1) }
    val apps by produceState<Map<String, Pair<String, ImageBitmap?>>>(emptyMap(), packages) {
        value = withContext(Dispatchers.IO) {
            packages.associateWith { pkg ->
                val pm = context.packageManager
                val info = runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull()
                val label = info?.let { runCatching { pm.getApplicationLabel(it).toString() }.getOrNull() } ?: pkg
                val icon = info?.let { runCatching { pm.getApplicationIcon(it).toBitmap(96, 96).asImageBitmap() }.getOrNull() }
                label to icon
            }
        }
    }
    val filtered = packages.filter { pkg ->
        (if (category == 0) pkg in gamePackages else pkg !in gamePackages) &&
            (pkg.contains(query.trim(), true) || apps[pkg]?.first?.contains(query.trim(), true) == true)
    }
    val mode = if (scan.firstOrNull() == "all") "前台 + 全部后台" else "仅前台"
    val state = when {
        snapshot == null -> "等待模块状态"
        !snapshot.daemonAlive -> "守护进程已停止 · 以下为最近记录"
        scan.getOrNull(3) == "screen_off" -> "本轮息屏 · 已跳过包名扫描"
        else -> "守护进程运行中 · $mode"
    }
    val time = scan.getOrNull(1)?.toLongOrNull()?.let {
        java.text.SimpleDateFormat("MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(it * 1000L))
    } ?: "暂无扫描时间"
    Column(Modifier.fillMaxSize().background(cs.surface).statusBarsPadding().navigationBarsPadding()) {
        DetailHeader("包名扫描", "每一轮识别，都有迹可循", onBack, "刷新", onRefresh)
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Overview("最近一次扫描", packages.size, "个包名", state, "更新于 $time") }
            item { SearchField(query, { query = it }, "搜索应用名称或包名") }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterPill("扫描包名 ${packages.count { it !in gamePackages }}", category == 1) { category = 1 }
                FilterPill("匹配包名 ${packages.count { it in gamePackages }}", category == 0) { category = 0 }
            } }
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("识别记录", color = cs.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("${filtered.size} 个结果", color = cs.onSurfaceVariantSummary, fontSize = 12.sp)
                }
            }
            if (filtered.isEmpty()) item { EmptyResult(if (packages.isEmpty()) "暂时没有扫描记录" else "没有匹配的包名", if (packages.isEmpty()) "刷新状态，查看模块最近一轮扫描结果" else "试试其他关键词") }
            items(filtered, key = { it }) { pkg ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        LetterBadge((apps[pkg]?.first ?: pkg).take(1), bitmap = apps[pkg]?.second)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(apps[pkg]?.first ?: pkg, color = cs.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(5.dp))
                            Text(pkg, color = cs.onSurfaceVariantSummary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProcessManagerPage(sh: RootShell, onBack: () -> Unit, onChanged: () -> Unit) {
    val cs = MiuixTheme.colorScheme
    var entries by remember { mutableStateOf<List<RootProcesses.Entry>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var filter by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<RootProcesses.Entry?>(null) }
    var killing by remember { mutableStateOf<Int?>(null) }
    fun refresh() {
        if (loading) return
        loading = true
        RootProcesses.load(sh) { list, failure -> entries = list; error = failure; loading = false }
    }
    LaunchedEffect(Unit) { refresh() }
    val search = query.trim()
    val filtered = entries.filter { entry ->
        (filter == 0 || (filter == 1 && entry.coreDaemon) || (filter == 2 && entry.protected)) &&
            (search.isEmpty() || entry.name.contains(search, true) || entry.command.contains(search, true) || entry.pid.toString().contains(search))
    }
    Column(Modifier.fillMaxSize().background(cs.surface).statusBarsPadding().navigationBarsPadding()) {
        DetailHeader("进程管理", "运行中的脚本与 LSP 框架", onBack, if (loading) "读取中…" else "刷新", { refresh() })
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Overview("脚本与框架快照", entries.size, "个进程", "守护进程 ${entries.count { it.coreDaemon }} · 受保护 ${entries.count { it.protected }}", "仅显示实际执行的 .sh 脚本与 LSP，隐藏命令行及内部辅助脚本") }
            item { SearchField(query, { query = it }, "搜索名称、PID 或命令") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("全部进程", "守护进程", "受保护").forEachIndexed { index, text -> FilterPill(text, filter == index) { filter = index } }
                }
            }
            error?.let { failure -> item { EmptyResult("读取失败", failure) } }
            if (error == null && filtered.isEmpty()) item { EmptyResult(if (loading) "正在读取进程" else "没有匹配的进程", if (loading) "正在读取脚本与框架进程" else "调整搜索词或筛选条件后再试") }
            items(filtered, key = { "${it.pid}:${it.started}" }) { entry ->
                val identity = "${entry.pid}:${entry.started}"
                val open = expanded == identity
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.animateContentSize().clickable { expanded = if (open) null else identity }.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LetterBadge("SH", imageRes = when { entry.coreDaemon -> R.drawable.process_coregate; entry.lsp -> R.drawable.process_lsp; else -> null })
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(if (entry.lsp) "LSP 框架" else entry.scriptPath.substringAfterLast('/'), color = cs.onSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(5.dp))
                                Text("PID ${entry.pid}", color = cs.onSurfaceVariantSummary, fontSize = 12.sp)
                            }
                            TextButton(if (entry.protected) "已保护" else if (killing == entry.pid) "结束中…" else "结束", onClick = { if (!entry.protected && killing == null) pending = entry }, enabled = !entry.protected && killing == null)
                        }
                        Spacer(Modifier.height(12.dp))
                        val tag = when {
                            entry.lsp -> "LSP 框架 · 已保护"
                            entry.pid <= 1 -> "系统关键进程 · 已保护"
                            entry.coreDaemon -> "CoreGate.sh · 已保护，前往设置页「停止 CoreGate」操作"
                            entry.script -> "脚本进程"
                            else -> "运行进程"
                        }
                        Text(tag, color = cs.primary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(6.dp))
                        Text(entry.command.ifBlank { "[内核线程]" }, color = cs.onSurfaceVariantSummary, fontSize = 12.sp, maxLines = if (open) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(8.dp))
                        Text(if (open) "父进程 ${entry.parent} · 点击收起" else "点击展开命令详情", color = cs.onSurfaceVariantSummary.copy(alpha = 0.7f), fontSize = 11.sp)
                    }
                }
            }
        }
    }
    pending?.let { target ->
        ConfirmDialog("确认结束进程", "是否结束 ${target.name}（PID ${target.pid}）？\n${target.command}\n结束后相关功能可能停止。", "确认结束", onCancel = { pending = null }, onConfirm = {
            pending = null; killing = target.pid
            RootProcesses.kill(sh, target) { success, message -> killing = null; Toaster.show(message, !success); refresh(); onChanged() }
        })
    }
}

@Composable
internal fun DetailHeader(title: String, subtitle: String, onBack: () -> Unit, action: String, onAction: () -> Unit) {
    val cs = MiuixTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton("‹ 返回", onClick = onBack)
            TextButton(action, onClick = onAction)
        }
        Spacer(Modifier.height(12.dp))
        Text(title, color = cs.onSurface, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = cs.onSurfaceVariantSummary, fontSize = 13.sp)
    }
}

@Composable
internal fun Overview(label: String, count: Int, unit: String, status: String, note: String) {
    val cs = MiuixTheme.colorScheme
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Brush.linearGradient(listOf(cs.primary.copy(alpha = 0.16f), cs.primary.copy(alpha = 0.025f)))).padding(22.dp)) {
        Text(label, color = cs.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(count.toString(), color = cs.onSurface, fontSize = 48.sp, fontWeight = FontWeight.Bold)
            Text(unit, Modifier.padding(start = 10.dp, bottom = 12.dp), color = cs.onSurfaceVariantSummary, fontSize = 13.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(status, color = cs.onSurface, fontSize = 13.sp)
        Spacer(Modifier.height(5.dp))
        Text(note, color = cs.onSurfaceVariantSummary, fontSize = 11.sp)
    }
}

@Composable
internal fun SearchField(value: String, onChange: (String) -> Unit, hint: String) {
    val cs = MiuixTheme.colorScheme
    BasicTextField(value = value, onValueChange = onChange, singleLine = true, textStyle = TextStyle(color = cs.onSurface, fontSize = 14.sp), cursorBrush = SolidColor(cs.primary), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(cs.onSurface.copy(alpha = 0.055f)).padding(horizontal = 18.dp, vertical = 16.dp), decorationBox = { inner ->
        Box { if (value.isEmpty()) Text(hint, color = cs.onSurfaceVariantSummary, fontSize = 14.sp); inner() }
    })
}

@Composable
internal fun FilterPill(text: String, selected: Boolean, onClick: () -> Unit) {
    val cs = MiuixTheme.colorScheme
    Text(text, modifier = Modifier.clip(RoundedCornerShape(50)).background(if (selected) cs.primary.copy(alpha = 0.15f) else cs.onSurface.copy(alpha = 0.045f)).clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 10.dp), color = if (selected) cs.primary else cs.onSurfaceVariantSummary, fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
}

@Composable
internal fun LetterBadge(text: String, imageRes: Int? = null, bitmap: ImageBitmap? = null) {
    val cs = MiuixTheme.colorScheme
    Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(cs.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
        when {
            bitmap != null -> Image(bitmap, contentDescription = text, modifier = Modifier.fillMaxSize())
            imageRes != null -> Image(painterResource(imageRes), contentDescription = text, modifier = Modifier.fillMaxSize())
            else -> Text(text, color = cs.primary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun EmptyResult(title: String, message: String) {
    val cs = MiuixTheme.colorScheme
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = cs.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(message, color = cs.onSurfaceVariantSummary, fontSize = 12.sp)
        }
    }
}
