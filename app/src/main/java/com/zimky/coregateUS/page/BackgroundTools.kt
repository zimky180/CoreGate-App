package com.zimky.coregateUS.page

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.zimky.coregateUS.core.SysProbe
import com.zimky.coregateUS.ui.KeyValueRow
import com.zimky.coregateUS.ui.NavRow

/** Rows are hosted inside the same card as freeze management. */
@Composable
fun BackgroundTools(snapshot: SysProbe.Snapshot?, onOpenScan: (Rect) -> Unit, onOpenProcesses: (Rect) -> Unit) {
    var scanOrigin by remember { mutableStateOf(Rect.Zero) }
    var processOrigin by remember { mutableStateOf(Rect.Zero) }
    val scan = snapshot?.scanRaw.orEmpty().split('|', limit = 5)
    val count = scan.getOrNull(4).orEmpty().split(',').filter { it.isNotBlank() }.distinct().size
    val mode = if (scan.firstOrNull() == "all") "前台 + 全部后台" else "仅前台"
    NavRow("本轮包名扫描", if (snapshot?.daemonAlive == true) "$mode · $count 个" else "查看最近记录", onClick = { onOpenScan(scanOrigin) }, modifier = Modifier.onGloballyPositioned { scanOrigin = it.boundsInRoot() })
    NavRow("进程管理", "运行中的脚本 / LSP 框架", onClick = { onOpenProcesses(processOrigin) }, modifier = Modifier.onGloballyPositioned { processOrigin = it.boundsInRoot() })
    if (snapshot != null && !snapshot.daemonAlive) KeyValueRow("CoreGate.sh", "已死亡 · 轮巡已停止", gradientValue = true)
}
