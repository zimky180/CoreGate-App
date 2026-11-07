package com.zimky.coregateUS.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.zimky.coregateUS.core.CustomAvatar
import com.zimky.coregateUS.core.QqAvatar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun HomeProfileHeader() {
    val cs = MiuixTheme.colorScheme
    val lifecycle = LocalLifecycleOwner.current
    var hour by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    var today by remember { mutableStateOf(LocalDate.now()) }
    var quoteOffset by rememberSaveable(today.toString()) { mutableIntStateOf(0) }
    LaunchedEffect(lifecycle) {
        lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                today = LocalDate.now()
                delay(30_000)
            }
        }
    }
    val greeting = when (hour) { in 5..10 -> "早上好！"; in 11..17 -> "中午好！"; else -> "晚上好！" }
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("核心门控", color = cs.onSurface, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text(greeting, color = cs.onSurface, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            }
            HomeAvatar()
        }
        Column(Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "换一句") {
            quoteOffset = (quoteOffset + 1) % DailyQuote.count
        }.padding(top = 6.dp)) {
            Text("每日一言 · ${today.monthValue}月${today.dayOfMonth}日 · 点按换句", color = cs.primary, fontSize = 12.sp)
            Text(DailyQuote.forDate(today, quoteOffset), color = cs.onSurfaceVariantSummary,
                fontSize = 15.sp, lineHeight = 24.sp)
        }
    }
}

@Composable
private fun HomeAvatar() {
    var avatar by remember { mutableStateOf<ImageBitmap?>(null) }
    val owner = LocalLifecycleOwner.current
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var selectionVersion by remember { mutableIntStateOf(0) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null && !saving) {
            selectionVersion++
            saving = true
            scope.launch {
                try {
                    val selected = CustomAvatar.save(context, uri)
                    if (selected != null) avatar = selected
                    else Toaster.show("图片读取失败，请重新选择", true)
                } finally {
                    saving = false
                }
            }
        }
    }
    LaunchedEffect(owner, context) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            if (avatar == null) {
                val version = selectionVersion
                val loaded = CustomAvatar.load(context) ?: QqAvatar.load()
                if (selectionVersion == version && !saving) avatar = loaded
            }
        }
    }
    val cs = MiuixTheme.colorScheme
    Box(Modifier.size(52.dp).clip(CircleShape).background(cs.surfaceContainerHigh)
        .border(1.dp, cs.outline.copy(alpha = 0.5f), CircleShape)
        .clickable(enabled = !saving, role = Role.Button, onClickLabel = "从相册选择头像") {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }, contentAlignment = Alignment.Center) {
        if (avatar != null) Image(bitmap = avatar!!, contentDescription = "头像，点击更换",
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text("头像", color = cs.primary, fontSize = 12.sp)
    }
}
