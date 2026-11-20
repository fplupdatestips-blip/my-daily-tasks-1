package app.dailytasks.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.os.Build
import app.dailytasks.AppViewModel
import app.dailytasks.AuthUi
import app.dailytasks.DataState
import app.dailytasks.core.Describe
import app.dailytasks.core.RepeatEngine
import app.dailytasks.core.Task
import app.dailytasks.core.TodayItems
import app.dailytasks.ui.appIcon
import app.dailytasks.ui.theme.LocalAppColors
import java.time.LocalDate

@Composable
fun TasksScreen(vm: AppViewModel, data: DataState, today: LocalDate, onOpen: (Task) -> Unit, onSettings: () -> Unit) {
    val c = LocalAppColors.current
    var filter by rememberSaveable { mutableStateOf("Active") }
    val list = data.tasks.filter {
        val paused = it.pausedUntil?.let { p -> !p.isBefore(today) } == true
        when (filter) {
            "Archived" -> it.archived
            "Paused" -> !it.archived && paused
            else -> !it.archived && !paused
        }
    }.sortedBy { it.sortOrder }

    LazyColumn(contentPadding = PaddingValues(bottom = 112.dp)) {
        item {
            ScreenHeader("Tasks") {
                SyncIcon(data.sync)
                SettingsButton(onSettings)
            }
        }
        item {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Active", "Paused", "Archived").forEach { f ->
                    val sel = filter == f
                    Box(
                        Modifier.heightIn(min = 40.dp)
                            .background(if (sel) c.primaryContainer else c.surface, RoundedCornerShape(10.dp))
                            .clickable { filter = f }.padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(f, color = if (sel) c.onPrimaryContainer else c.text, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal, fontSize = 14.sp) }
                }
            }
        }
        if (list.isEmpty()) item { Text("No ${filter.lowercase()} tasks.", color = c.text2, modifier = Modifier.fillMaxWidth().padding(40.dp)) }
        itemsIndexed(list, key = { _, t -> t.id }) { i, t ->
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth().background(c.surface, RoundedCornerShape(16.dp))
                    .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TaskIconBox(t.icon, t.color)
                Column(Modifier.weight(1f).clickable { onOpen(t) }) {
                    Text(t.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        Describe.repeat(t.repeat) + if (t.subtasks.isNotEmpty()) " · ${t.subtasks.size} subtasks" else "",
                        fontSize = 13.sp, color = c.text2, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                if (filter == "Active") {
                    IconButton(onClick = { if (i > 0) swap(vm, t, list[i - 1]) }, enabled = i > 0) {
                        Icon(appIcon("chevronUp"), contentDescription = "Move ${t.name} up", tint = c.text2, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { if (i < list.size - 1) swap(vm, t, list[i + 1]) }, enabled = i < list.size - 1) {
                        Icon(appIcon("chevronDown"), contentDescription = "Move ${t.name} down", tint = c.text2, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

private fun swap(vm: AppViewModel, a: Task, b: Task) {
    vm.saveTask(a.copy(sortOrder = b.sortOrder))
    vm.saveTask(b.copy(sortOrder = a.sortOrder))
}

@Composable
fun ProgressScreen(data: DataState, today: LocalDate, onSettings: () -> Unit) {
    val c = LocalAppColors.current
    val week = remember(data, today) {
        val start = RepeatEngine.startOfWeek(today, data.settings.firstDayOfWeek)
        (0L until 7L).map { i ->
            val d = start.plusDays(i)
            val items = if (d.isAfter(today)) emptyList() else TodayItems.forDay(data.tasks, data.completions, d, today, data.settings.firstDayOfWeek)
            Triple(d, items.size, items.count { it.done })
        }
    }
    val total = week.sumOf { it.second }
    val done = week.sumOf { it.third }
    val max = maxOf(1, week.maxOf { it.second })
    val marks = TodayItems.marksByTask(data.completions)
    val streaks = data.tasks.filter { !it.archived }
        .map { it to RepeatEngine.streak(it.repeat, today, marks[it.id] ?: emptyList()) }
        .filter { it.second.current > 0 }
        .sortedByDescending { it.second.current }
        .take(5)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 112.dp)) {
        ScreenHeader("Progress") {
            SyncIcon(data.sync)
            SettingsButton(onSettings)
        }
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.fillMaxWidth().background(c.surface, RoundedCornerShape(20.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("This week", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = c.text, modifier = Modifier.weight(1f))
                    Text(if (total > 0) "${done * 100 / total}% · $done of $total" else "No tasks yet", fontSize = 14.sp, color = c.text2)
                }
                Row(
                    Modifier.fillMaxWidth().height(160.dp).semantics {
                        contentDescription = week.joinToString(", ") { (d, t, dn) -> "${Describe.weekdayShort(d.dayOfWeek.value)} $dn of $t" }
                    },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    week.forEach { (d, t, dn) ->
                        Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                            Text(if (t > 0) "$dn" else "", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = c.text2)
                            val barH = maxOf(4f, 120f * t / max).dp
                            Box(
                                Modifier.fillMaxWidth().height(barH).background(c.ringTrack.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.BottomCenter,
                            ) {
                                if (t > 0 && dn > 0) Box(Modifier.fillMaxWidth().height(barH * (dn.toFloat() / t)).background(c.done, RoundedCornerShape(8.dp)))
                            }
                            Text(
                                Describe.weekdayShort(d.dayOfWeek.value).take(1), fontSize = 12.sp,
                                fontWeight = if (d == today) FontWeight.Bold else FontWeight.Normal, color = if (d == today) c.primary else c.text2,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
            Column(Modifier.fillMaxWidth().background(c.surface, RoundedCornerShape(20.dp)).padding(16.dp)) {
                Text("Longest active streaks", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = c.text, modifier = Modifier.padding(bottom = 8.dp))
                if (streaks.isEmpty()) Text("Tick a repeating task a few days in a row to start a streak.", fontSize = 14.sp, color = c.text2)
                streaks.forEachIndexed { i, (t, s) ->
                    if (i > 0) HorizontalDivider(color = c.divider)
                    Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(t.name, color = c.text, modifier = Modifier.weight(1f))
                        Icon(appIcon("flame", 2f), contentDescription = null, tint = c.streak, modifier = Modifier.size(16.dp))
                        Text(" ${s.current}", color = c.streak, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val c = LocalAppColors.current
    val data by vm.data.collectAsStateWithLifecycle()
    val prefs by vm.devicePrefs.collectAsStateWithLifecycle()
    val auth by vm.auth.collectAsStateWithLifecycle()
    val user = (auth as? AuthUi.SignedIn)?.user

    @Composable
    fun Card(title: String, content: @Composable () -> Unit) {
        Column(Modifier.fillMaxWidth().background(c.surface, RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 8.dp)) {
            GroupTitle(title, Modifier.padding(top = 4.dp, bottom = 4.dp))
            content()
        }
    }

    @Composable
    fun Choice(label: String, options: List<Pair<String, String>>, value: String, onPick: (String) -> Unit) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Text(label, color = c.text, fontSize = 15.sp)
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                options.forEach { (k, l) ->
                    val sel = k == value
                    Box(
                        Modifier.heightIn(min = 40.dp).background(if (sel) c.primaryContainer else c.surface2, RoundedCornerShape(10.dp))
                            .clickable { onPick(k) }.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(l, fontSize = 13.sp, color = if (sel) c.onPrimaryContainer else c.text, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal) }
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().background(c.bg).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(appIcon("back", 2f), contentDescription = "Back", tint = c.text) }
            Text("Settings", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = c.text)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card("Appearance") {
                Choice("Theme on this phone", listOf("system" to "System", "light" to "Light", "dark" to "Dark"), prefs.theme) { vm.setTheme(it) }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Wallpaper colors", color = c.text, fontSize = 15.sp)
                            Text("Use colors from your wallpaper", color = c.text2, fontSize = 13.sp)
                        }
                        Switch(checked = prefs.dynamicColor, onCheckedChange = { vm.setDynamicColor(it) }, colors = SwitchDefaults.colors(checkedTrackColor = c.primary))
                    }
                }
            }
            Card("Day") {
                Choice("First day of the week", listOf("1" to "Monday", "6" to "Saturday", "7" to "Sunday"), data.settings.firstDayOfWeek.toString()) {
                    vm.saveSettings(data.settings.copy(firstDayOfWeek = it.toInt()))
                }
                Choice("My day ends at", (0..4).map { "$it" to if (it == 0) "Midnight" else "$it AM" }, data.settings.dayEndsAt.toString()) {
                    vm.saveSettings(data.settings.copy(dayEndsAt = it.toInt()))
                }
                Text("Ticks before this time count for the day before.", fontSize = 13.sp, color = c.text2, modifier = Modifier.padding(bottom = 8.dp))
            }
            Card("Account") {
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(user?.displayName ?: "Signed in", color = c.text, fontSize = 15.sp)
                        Text(user?.email ?: "", color = c.text2, fontSize = 13.sp)
                    }
                    TextButton(onClick = { vm.signOut(); onBack() }) { Text("Sign out", color = c.primary, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}
