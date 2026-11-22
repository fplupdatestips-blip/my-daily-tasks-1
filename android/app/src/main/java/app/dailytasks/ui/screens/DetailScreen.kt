package app.dailytasks.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dailytasks.AppViewModel
import app.dailytasks.core.CompletionStatus
import app.dailytasks.core.Describe
import app.dailytasks.core.PAUSED_FOREVER
import app.dailytasks.core.RepeatEngine
import app.dailytasks.core.Task
import app.dailytasks.core.TodayItems
import app.dailytasks.ui.appIcon
import app.dailytasks.ui.theme.LocalAppColors
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun DetailScreen(vm: AppViewModel, task: Task, today: LocalDate, onBack: () -> Unit, onEdit: () -> Unit) {
    val c = LocalAppColors.current
    val data by vm.data.collectAsStateWithLifecycle()
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var confirmDelete by remember { mutableStateOf(false) }
    val marks = remember(data.completions, task.id) { TodayItems.marksByTask(data.completions)[task.id] ?: emptyList() }
    val byDate = marks.associate { it.date to it.status }
    val calendarRule = RepeatEngine.isCalendarRule(task.repeat)
    val streak = RepeatEngine.streak(task.repeat, today, marks)
    val paused = task.pausedUntil?.let { !it.isBefore(today) } == true

    val rate = run {
        var sched = 0
        var done = 0
        for (i in 0L until 30L) {
            val d = today.minusDays(i)
            if (task.repeat.startDate != null && d < task.repeat.startDate) break
            val st = byDate[d]
            if (calendarRule) {
                if (!RepeatEngine.isScheduled(task.repeat, d) || st == CompletionStatus.SKIPPED || (d == today && st != CompletionStatus.DONE)) continue
                sched++
            }
            if (st == CompletionStatus.DONE) done++
        }
        if (calendarRule) (if (sched > 0) "${done * 100 / sched}%" else "–") else "$done"
    }

    Column(Modifier.fillMaxSize().background(c.bg).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(appIcon("back", 2f), contentDescription = "Back", tint = c.text) }
            Box(Modifier.weight(1f))
            IconButton(onClick = onEdit) { Icon(appIcon("edit"), contentDescription = "Edit", tint = c.text2) }
            IconButton(onClick = { vm.saveTask(task.copy(pausedUntil = if (paused) null else PAUSED_FOREVER)) }) {
                Icon(appIcon(if (paused) "play" else "pause"), contentDescription = if (paused) "Resume" else "Pause", tint = c.text2)
            }
            IconButton(onClick = { vm.saveTask(task.copy(archived = !task.archived)) }) {
                Icon(appIcon("archive"), contentDescription = if (task.archived) "Unarchive" else "Archive", tint = c.text2)
            }
            IconButton(onClick = { confirmDelete = true }) { Icon(appIcon("trash"), contentDescription = "Delete", tint = c.text2) }
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                TaskIconBox(task.icon, task.color, 56.dp, 26.dp, task.image)
                Column {
                    Text(task.name, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = c.text)
                    val bits = listOfNotNull(
                        Describe.repeat(task.repeat),
                        task.timeOfDay.name.lowercase().replaceFirstChar { it.uppercase() },
                        task.time?.let { Describe.time(it) },
                        if (paused) "Paused" else null,
                        if (task.archived) "Archived" else null,
                    )
                    Text(bits.joinToString(" · "), fontSize = 14.sp, color = c.text2)
                }
            }
            if (task.description.isNotBlank()) Text(task.description, fontSize = 15.sp, color = c.text, lineHeight = 22.sp)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (calendarRule) {
                    Stat("${streak.current}", "Current streak", c.streak, Modifier.weight(1f))
                    Stat("${streak.best}", "Best streak", c.text, Modifier.weight(1f))
                    Stat(rate, "Last 30 days", c.done, Modifier.weight(1f))
                } else {
                    Stat(rate, "Done in 30 days", c.text, Modifier.weight(1f))
                    Stat("${marks.count { it.status == CompletionStatus.DONE }}", "Done in total", c.text, Modifier.weight(1f))
                    Stat("${task.subtasks.size}", "Subtasks", c.text, Modifier.weight(1f))
                }
            }

            if (task.subtasks.isNotEmpty()) {
                Column(Modifier.fillMaxWidth().background(c.surface, RoundedCornerShape(16.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GroupTitle("Subtasks")
                    task.subtasks.forEach { Text("•  ${it.title}", fontSize = 15.sp, color = c.text) }
                }
            }

            // Month calendar: tap a past day to backfill.
            Column(Modifier.fillMaxWidth().background(c.surface, RoundedCornerShape(20.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { month = month.minusMonths(1) }) { Icon(appIcon("chevronLeft"), contentDescription = "Previous month", tint = c.text2) }
                    Text("${Describe.monthLong(month.monthValue)} ${month.year}", fontWeight = FontWeight.SemiBold, color = c.text, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    IconButton(onClick = { month = month.plusMonths(1) }) { Icon(appIcon("chevronRight"), contentDescription = "Next month", tint = c.text2) }
                }
                val fdow = data.settings.firstDayOfWeek
                Row {
                    (0 until 7).forEach { i ->
                        val wd = (fdow - 1 + i) % 7 + 1
                        Text(Describe.weekdayShort(wd), fontSize = 12.sp, color = c.text2, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
                val first = month.atDay(1)
                val lead = (first.dayOfWeek.value - fdow + 7) % 7
                val cells: List<LocalDate?> = List(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
                cells.chunked(7).forEach { week ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (0 until 7).forEach { i ->
                            val d = week.getOrNull(i)
                            Box(Modifier.weight(1f).height(40.dp)) {
                                if (d != null) DayCell(d, today, byDate[d], calendarRule, task, marks) { vm.toggleDay(task, d) }
                            }
                        }
                    }
                }
                Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Legend(c.done, "Done")
                    Legend(c.divider, "Skipped")
                    Text("× Missed", fontSize = 12.sp, color = c.missed)
                }
                Text("Tap a past day to change it", fontSize = 12.sp, color = c.text2)
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GroupTitle("History")
                Column(Modifier.fillMaxWidth().background(c.surface, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 4.dp)) {
                    val history = marks.filter { it.status != CompletionStatus.PARTIAL }.sortedByDescending { it.date }.take(20)
                    if (history.isEmpty()) Text("Nothing yet.", color = c.text2, modifier = Modifier.padding(vertical = 12.dp))
                    history.forEachIndexed { i, h ->
                        if (i > 0) HorizontalDivider(color = c.divider)
                        Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(Describe.longLabel(h.date), color = c.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
                            Text(if (h.status == CompletionStatus.DONE) "Done" else "Skipped", fontSize = 13.sp, color = if (h.status == CompletionStatus.DONE) c.done else c.text2)
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete “${task.name}”?") },
            text = { Text("Its history is deleted too. This can’t be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.deleteTask(task.id)
                    onBack()
                }) { Text("Delete", color = c.missed) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Stat(value: String, label: String, color: Color, modifier: Modifier) {
    val c = LocalAppColors.current
    Column(modifier.background(c.surface, RoundedCornerShape(16.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 13.sp, color = c.text2)
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    val c = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(12.dp).background(color, RoundedCornerShape(3.dp)))
        Text(label, fontSize = 12.sp, color = c.text2)
    }
}

@Composable
private fun DayCell(
    d: LocalDate,
    today: LocalDate,
    status: CompletionStatus?,
    calendarRule: Boolean,
    task: Task,
    marks: List<app.dailytasks.core.DayMark>,
    onToggle: () -> Unit,
) {
    val c = LocalAppColors.current
    val scheduled = if (calendarRule) RepeatEngine.isScheduled(task.repeat, d)
    else RepeatEngine.dueInfo(task.repeat, d, marks.filter { it.date < d }).due
    val state = when {
        status == CompletionStatus.DONE -> "done"
        status == CompletionStatus.SKIPPED -> "skipped"
        d.isAfter(today) -> "upcoming"
        d == today -> "today, not done yet"
        scheduled -> "missed"
        else -> "not scheduled"
    }
    val shape = RoundedCornerShape(10.dp)
    val base = Modifier.fillMaxSize()
    val styled = when (state) {
        "done" -> base.background(c.done, shape)
        "skipped" -> base.background(c.divider, shape)
        "today, not done yet" -> base.border(2.dp, c.primary, shape)
        else -> base
    }
    val fg = when (state) {
        "done" -> c.onDone
        "skipped" -> c.text2
        "missed" -> c.missed
        "today, not done yet" -> c.primary
        "upcoming" -> c.outline
        else -> c.text2
    }
    Box(
        styled.then(if (!d.isAfter(today)) Modifier.clickable(onClick = onToggle) else Modifier)
            .semantics { contentDescription = "${Describe.longLabel(d)}, $state" },
        contentAlignment = Alignment.Center,
    ) {
        Text("${d.dayOfMonth}" + if (state == "missed") "×" else "", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}
