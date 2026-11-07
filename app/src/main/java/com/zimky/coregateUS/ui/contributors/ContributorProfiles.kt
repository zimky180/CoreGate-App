package com.zimky.coregateUS.ui.contributors

import com.zimky.coregateUS.R
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal data class AuthorProfile(
    val login: String,
    val name: String,
    val contributionSummary: String,
    val qq: String? = null,
    @DrawableRes val avatarRes: Int? = null
)

/** Fixed order: owner, administrators, testing contributors. QQ numbers supplied by the owner. */
internal val contributorProfiles = listOf(
    AuthorProfile("owner", "zimky", "项目创始人 · 独立开发与维护\n从零构建 CoreGate，负责整体架构、模块逻辑与全部 UI 实现，持续承担版本迭代、问题修复与项目维护。",
        qq = "1805920232", avatarRes = R.drawable.contributor_owner),
    AuthorProfile("friend-1", "寒玉", "社区管理 · 需求与反馈\n汇集用户需求，整理问题与 Bug 反馈，协助开发者跟进处理，连接用户与项目。",
        qq = "490601192", avatarRes = R.drawable.contributor_feedback),
    AuthorProfile("friend-2", "热情", "社区管理 · 交流与互动\n组织日常交流、带动群内互动，让用户有一个轻松活跃的交流空间。",
        qq = "591570259", avatarRes = R.drawable.contributor_community),
    AuthorProfile("tester-dongan", "冬安", "测试贡献 · 问题反馈\n参与测试版体验，提供宝贵的 Bug 反馈，帮助发现问题并完善使用体验。",
        qq = "2408646386", avatarRes = R.drawable.contributor_dongan),
    AuthorProfile("tester-canghai", "沧海", "测试贡献 · 问题反馈\n参与测试版体验，提供宝贵的 Bug 反馈，帮助发现问题并完善使用体验。",
        qq = "3560356758", avatarRes = R.drawable.contributor_canghai),
    AuthorProfile("tester-jiekai", "解开超载的心", "测试贡献 · 问题反馈\n参与测试版体验，提供宝贵的 Bug 反馈，帮助发现问题并完善使用体验。",
        qq = "3543500780", avatarRes = R.drawable.contributor_jiekai)
)

@Composable
internal fun AuthorAvatar(profile: AuthorProfile, modifier: Modifier = Modifier) {
    val avatar = profile.avatarRes
    if (avatar == null) {
        Box(modifier.clip(CircleShape).background(MiuixTheme.colorScheme.surfaceContainerHigh))
    } else {
        Image(painterResource(avatar), contentDescription = profile.name,
            modifier = modifier.clip(CircleShape), contentScale = ContentScale.Crop)
    }
}