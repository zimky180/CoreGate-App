package com.zimky.coregateUS.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.zimky.coregateUS.BuildConfig
import com.zimky.coregateUS.core.ApkUpdates
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Version summary, release metadata, changes and actions follow the reference update prompt. */
@Composable
fun ApkUpdateCard() {
    val cs = MiuixTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<ApkUpdates.Result?>(null) }
    var showDetails by remember { mutableStateOf(false) }
    fun openLink(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            .onFailure { Toaster.show("无法打开链接，请检查浏览器是否可用。", true) }
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("GitHub 检查更新", color = cs.onSurface, fontSize = 16.sp)
            SubText("当前 APK：${BuildConfig.VERSION_NAME}")
            val message = when (val value = result) {
                is ApkUpdates.Result.Available -> "发现新版本 ${value.release.version}"
                is ApkUpdates.Result.Current -> "当前 APK 已是最新或更新版本（GitHub：${value.version}）"
                is ApkUpdates.Result.Failed -> value.message
                ApkUpdates.Result.NoRelease -> "GitHub 暂无可安装的正式版 APK 发布。"
                null -> "检查 GitHub Releases 中的 APK 更新。"
            }
            HintText(if (busy) "正在检查…" else message)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(if (busy) "检查中…" else "检查 APK 更新", onClick = {
                    if (!busy) {
                        busy = true
                        scope.launch {
                            try {
                                result = ApkUpdates.check()
                                showDetails = result is ApkUpdates.Result.Available
                            } finally { busy = false }
                        }
                    }
                }, colors = ButtonDefaults.textButtonColorsPrimary())
                if (result is ApkUpdates.Result.Available) {
                    TextButton("查看更新", onClick = { showDetails = true })
                }
            }
            TextButton("前往 GitHub 发布页", onClick = { openLink(ApkUpdates.RELEASES_URL) })
        }
    }
    val release = (result as? ApkUpdates.Result.Available)?.release
    if (showDetails && release != null) {
        Dialog(onDismissRequest = { showDetails = false }) {
            Card(Modifier.fillMaxWidth(), cornerRadius = 28.dp) {
                Column(Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()).padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("发现 APK 更新", color = cs.onSurface, fontSize = 20.sp)
                    Text("${BuildConfig.VERSION_NAME} → ${release.version}", color = cs.primary, fontSize = 15.sp)
                    KeyValueRow("发布名称", release.title)
                    KeyValueRow("发布时间", release.published.replace('T', ' ').removeSuffix("Z") + " UTC")
                    KeyValueRow("安装包", release.assetName)
                    HairLine()
                    Text("更新内容", color = cs.onSurface, fontSize = 15.sp)
                    Text(release.notes.ifBlank { "此版本未提供更新说明。" },
                        color = cs.onSurfaceVariantSummary, fontSize = 13.sp, lineHeight = 20.sp)
                    TextButton("查看完整发布记录", onClick = { openLink(release.pageUrl) })
                    TextButton("下载 APK", onClick = { openLink(release.apkUrl) },
                        modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.textButtonColorsPrimary())
                    TextButton("稍后", onClick = { showDetails = false }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}