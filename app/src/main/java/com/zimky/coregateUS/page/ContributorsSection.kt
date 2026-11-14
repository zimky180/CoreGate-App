package com.zimky.coregateUS.page

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.zimky.coregateUS.R
import com.zimky.coregateUS.ui.*
import com.zimky.coregateUS.ui.contributors.*
import com.zimky.coregateUS.ui.LiquidCard as Card
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Reference: Duck-Detector-Refactoring ContributorsSection and its Miuix author details layout. */
@Composable
fun ContributorsSection() {
    val cs = MiuixTheme.colorScheme
    val context = LocalContext.current
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeading("贡献者", contributorProfiles.size.toString())
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ContributorWallCanvas(authors = contributorProfiles, onSelect = { selectedId = it.login })
                HintText("拖动或双指缩放头像墙，点击头像查看贡献者。")
            }
        }
    }
    contributorProfiles.firstOrNull { it.login == selectedId }?.let { profile ->
        Dialog(onDismissRequest = { selectedId = null }) {
            Card(Modifier.fillMaxWidth(), cornerRadius = 28.dp) {
                Column(Modifier.heightIn(max = 540.dp).verticalScroll(rememberScrollState()).padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AnimatedGradientText(
                        text = profile.name, fontSize = 24.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        gradientColors = listOf(
                            androidx.compose.ui.graphics.lerp(cs.onSurface, androidx.compose.ui.graphics.Color(0xFF408CFF), 0.72f),
                            androidx.compose.ui.graphics.lerp(cs.onSurface, androidx.compose.ui.graphics.Color(0xFFAA73ED), 0.72f),
                            androidx.compose.ui.graphics.lerp(cs.onSurface, androidx.compose.ui.graphics.Color(0xFF24B7BF), 0.72f),
                            androidx.compose.ui.graphics.lerp(cs.onSurface, androidx.compose.ui.graphics.Color(0xFF408CFF), 0.72f)
                        )
                    )
                    // V2.4.1：名字右侧平台图标。规则是「有什么给什么」——
                    // 有 QQ 才出 QQ 图标，有酷安才出酷安图标，两样都没有就整行不渲染，
                    // 不留空占位、不勾勒灰色假图标。
                    val hasQqIcon = !profile.qq.isNullOrBlank()
                    val hasCoolapkIcon = !profile.coolapkUid.isNullOrBlank()
                    if (hasQqIcon || hasCoolapkIcon) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            if (hasQqIcon) {
                                Image(painterResource(R.drawable.ic_qq_logo), contentDescription = "QQ",
                                    modifier = Modifier.size(22.dp))
                            }
                            if (hasCoolapkIcon) {
                                Image(painterResource(R.drawable.ic_coolapk_logo), contentDescription = "酷安",
                                    modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                    AuthorAvatar(profile, Modifier.size(96.dp))
                    Text(profile.contributionSummary, modifier = Modifier.fillMaxWidth(),
                        color = cs.onSurfaceVariantSummary, fontSize = 14.sp)
                    val qq = profile.qq?.takeIf { it.matches(Regex("[1-9][0-9]{4,11}")) }
                    // V2.4.1：占位值为 PENDING_*，说明 UID 还没拿到。
                    // 这种情况下按「没有酷安」处理，绝不让 PENDING_OWNER 这种字符串
                    // 流到界面上或者被拼进 URL。
                    val coolapk = profile.coolapkUid
                        ?.takeIf { it.isNotBlank() && !it.startsWith("PENDING_") }
                    if (qq != null) Text("QQ：$qq",
                        modifier = Modifier.fillMaxWidth(), color = cs.onSurfaceVariantSummary, fontSize = 13.sp)
                    if (coolapk != null) {
                        Text("酷安 UID：$coolapk", modifier = Modifier.fillMaxWidth(),
                            color = cs.onSurfaceVariantSummary, fontSize = 13.sp)
                    }
                    // V2.4.1：按钮顺序固定为
                    // ①查看 QQ ②查看酷安 ③复制 QQ 号 ④复制酷安 UID ⑤关闭
                    // 不用 getOrNull() 之类做动态升降位，避免同一位置在不同人身上是不同功能。
                    if (qq != null) TextButton("查看 QQ", onClick = {
                        if (qq == null) {
                            Toaster.show("这位贡献者的 QQ 号尚未填写。")
                        } else {
                            val uri = Uri.Builder().scheme("mqqwpa").authority("im").path("chat")
                                .appendQueryParameter("chat_type", "wpa")
                                .appendQueryParameter("uin", qq)
                                .appendQueryParameter("version", "1").build()
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage("com.tencent.mobileqq"))
                            }.onFailure {
                                Toaster.show("无法打开 QQ，请确认已安装；可复制号码后搜索添加。", true)
                            }
                        }
                    }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.textButtonColorsPrimary())
                    if (coolapk != null) {
                        // 绿色：复用语义色 Semantic.OK，跟 QQ 的主色按钮区分开，
                        // 一眼能看出「这是酷安的入口」。
                        TextButton("查看酷安", onClick = {
                            val uri = Uri.parse("coolmarket://u/$coolapk")
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage("com.coolapk.market"))
                            }.onFailure {
                                // 兜底走网页版，酷安没装也能看主页。
                                // 链接统一从 profile.coolapkUrl 取，不在 UI 层重新拼一遍，
                                // 保证「显示的 UID」和「跳转的链接」永远是同一套规则算出来的。
                                val webUrl = profile.coolapkUrl
                                if (webUrl == null) {
                                    Toaster.show("无法打开酷安，可复制 UID 后在酷安内搜索。", true)
                                } else {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)))
                                    }.onFailure {
                                        Toaster.show("无法打开酷安，可复制 UID 后在酷安内搜索。", true)
                                    }
                                }
                            }
                        }, modifier = Modifier.fillMaxWidth(),
                            // 注意：Miuix 的参数名是 textColor，不是 Material 的 contentColor。
                            colors = ButtonDefaults.textButtonColors(textColor = Semantic.OK))
                    }
                    if (qq != null) {
                        TextButton("复制 QQ 号", onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("QQ", qq))
                            Toaster.show("QQ 号已复制，可到 QQ 搜索添加。")
                        }, modifier = Modifier.fillMaxWidth())
                    }
                    if (coolapk != null) {
                        TextButton("复制酷安 UID", onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("酷安 UID", coolapk))
                            Toaster.show("酷安 UID 已复制，可到酷安搜索用户。")
                        }, modifier = Modifier.fillMaxWidth())
                    }
                    TextButton("关闭", onClick = { selectedId = null }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}