package com.zimky.coregateUS.page

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.zimky.coregateUS.ui.*
import com.zimky.coregateUS.ui.contributors.*
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Reference: Duck-Detector-Refactoring ContributorsSection and its Miuix author details layout. */
@Composable
fun ContributorsPage(onBack: () -> Unit) {
    val cs = MiuixTheme.colorScheme
    val context = LocalContext.current
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .statusBarsPadding().padding(horizontal = 16.dp).padding(top = 8.dp, bottom = PAGE_BOTTOM_INSET),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TextButton("‹ 返回", onClick = onBack)
        PageHeading("贡献者", "感谢一起完善 CoreGate 的伙伴")
        SectionHeading("贡献者", "3")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ContributorWallCanvas(authors = contributorProfiles, onSelect = { selectedId = it.login })
                HintText("拖动或双指缩放头像墙，点击头像查看贡献者。")
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                contributorProfiles.forEachIndexed { index, profile ->
                    if (index > 0) HairLine()
                    NavRow("${index + 1}. ${profile.name}", profile.contributionSummary,
                        onClick = { selectedId = profile.login })
                }
            }
        }
        BottomSpacer()
    }
    contributorProfiles.firstOrNull { it.login == selectedId }?.let { profile ->
        Dialog(onDismissRequest = { selectedId = null }) {
            Card(Modifier.fillMaxWidth(), cornerRadius = 28.dp) {
                Column(Modifier.heightIn(max = 540.dp).verticalScroll(rememberScrollState()).padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(profile.name, color = cs.onSurface, fontSize = 20.sp)
                    AuthorAvatar(profile, Modifier.size(96.dp))
                    Text(profile.contributionSummary, modifier = Modifier.fillMaxWidth(),
                        color = cs.onSurfaceVariantSummary, fontSize = 14.sp)
                    val qq = profile.qq?.takeIf { it.matches(Regex("[1-9][0-9]{4,11}")) }
                    Text(if (qq == null) "QQ 号待补充" else "QQ：$qq",
                        modifier = Modifier.fillMaxWidth(), color = cs.onSurfaceVariantSummary, fontSize = 13.sp)
                    TextButton("查看 QQ", onClick = {
                        if (qq == null) {
                            Toaster.show("这位贡献者的 QQ 号尚未填写。")
                        } else {
                            val uri = Uri.Builder().scheme("mqqwpa").authority("im").path("chat")
                                .appendQueryParameter("chat_type", "wpa")
                                .appendQueryParameter("uin", qq).appendQueryParameter("version", "1").build()
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                                .onFailure { Toaster.show("无法打开 QQ，请确认已安装 QQ。号码：$qq", true) }
                        }
                    }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.textButtonColorsPrimary())
                    TextButton("关闭", onClick = { selectedId = null }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}