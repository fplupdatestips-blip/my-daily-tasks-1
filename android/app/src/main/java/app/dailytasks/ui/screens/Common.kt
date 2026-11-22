package app.dailytasks.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dailytasks.data.SyncState
import app.dailytasks.ui.TaskImages
import app.dailytasks.ui.appIcon
import app.dailytasks.ui.theme.LocalAppColors
import app.dailytasks.ui.theme.taskColors

@Composable
fun TaskIconBox(icon: String, color: String, size: Dp = 40.dp, iconSize: Dp = 20.dp, image: String? = null) {
    val c = LocalAppColors.current
    val (tint, fg) = taskColors(color, c.dark)
    val picture = remember(image) { image?.let { TaskImages.decode(it) } }
    Box(Modifier.size(size).clip(RoundedCornerShape(size * 0.3f)).background(tint), contentAlignment = Alignment.Center) {
        if (picture != null) {
            Image(picture, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(appIcon(icon), contentDescription = null, tint = fg, modifier = Modifier.size(iconSize))
        }
    }
}

@Composable
fun SyncIcon(sync: SyncState) {
    val c = LocalAppColors.current
    val (name, label) = when (sync) {
        SyncState.SYNCED -> "cloud" to "All changes synced"
        SyncState.SYNCING -> "cloudSync" to "Syncing"
        SyncState.OFFLINE -> "cloudOff" to "Offline. Changes will sync when you're back online"
    }
    Box(Modifier.size(48.dp).semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Icon(appIcon(name, 1.8f), contentDescription = null, tint = c.text2, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun SettingsButton(onClick: () -> Unit) {
    val c = LocalAppColors.current
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(appIcon("settings", 1.8f), contentDescription = "Settings", tint = c.text2, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String? = null, actions: @Composable () -> Unit = {}) {
    val c = LocalAppColors.current
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            subtitle?.let { Text(it, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = c.text2) }
            Text(title, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = c.text, modifier = Modifier.semantics { heading() })
        }
        Row(verticalAlignment = Alignment.CenterVertically) { actions() }
    }
}

@Composable
fun GroupTitle(text: String, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    Text(
        text.uppercase(),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        color = c.text2,
        modifier = modifier.padding(start = 4.dp, top = 4.dp).semantics { heading() },
    )
}
