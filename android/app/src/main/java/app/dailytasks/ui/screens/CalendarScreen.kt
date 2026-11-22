package app.dailytasks.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dailytasks.DataState
import app.dailytasks.core.CalendarItems
import app.dailytasks.core.Describe
import app.dailytasks.core.Task
import app.dailytasks.core.TodayItem
import app.dailytasks.ui.appIcon
import app.dailytasks.ui.theme.LocalAppColors
import app.dailytasks.ui.theme.taskColors
import java.time.LocalDate
import java.time.YearMonth

/** Month view of every task. Tap a day to see its tasks below the grid. */
@Composable
fun CalendarScreen(
    data: DataState,
    today: LocalDate,
    onOpen: (Task) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onSettings: () -> Unit,
) {
    val c = LocalAppColors.current
    val weekStart = data.settings.firstDayOfWeek
    var monthKey by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    var selectedKey by rememberSaveable { mutableStateOf(today.toString()) }
    val month = YearMonth.parse(monthKey)
    val selected = LocalDate.parse(selectedKey)

    val rows = remember(month, weekStart) { CalendarItems.monthGrid(month, weekStart) }
    val byDay = remember(rows, data, today) {
        rows.flatten().filterNotNull().associateWith { CalendarItems.forDay(data.tasks, data.completions, it, today, weekStart) }
    }
    val dayItems = remember(byDay, selected, data, today) {
        byDay[selected] ?: CalendarItems.forDay(data.tasks, data.completions, selected, today, weekStart)
    }
    val doneCount = dayItems.count { it.done }

    LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            ScreenHeader("Calendar") {
                SyncIcon(data.sync)
                SettingsButton(onSettings)
            }
        }
        item {
            Column(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().background(c.surface, RoundedCornerShape(20.dp)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { monthKey = month.minusMonths(1).toString() }, modifier = Modifier.size(40.dp)) {
                        Icon(appIcon("chevronLeft"), contentDescription = "Previous month", tint = c.text2, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        "${Describe.monthLong(month.monthValue)} ${month.year}",
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = c.text,
                        modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                    )
                    IconButton(onClick = { monthKey = month.plusMonths(1).toString() }, modifier = Modifier.size(40.dp)) {
                        Icon(appIcon("chevronRight"), contentDescription = "Next month", tint = c.text2, modifier = Modifier.size(18.dp))
                    }
                }
                if (month != YearMonth.from(today)) {
                    TextButton(
                        onClick = {
                            monthKey = YearMonth.from(today).toString()
                            selectedKey = today.toString()
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) { Text("Back to today", color = c.primary, fontWeight = FontWeight.SemiBold) }
                }
                Row(Modifier.fillMaxWidth()) {
                    (0 until 7).forEach { i ->
                        val iso = (weekStart - 1 + i) % 7 + 1
                        Text(
                            Describe.weekdayShort(iso), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = c.text2,
                            textAlign = TextAlign.Center, modifier = Modifier.weight(1f).padding(bottom = 4.dp),
                        )
                    }
                }
                rows.forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { d ->
                            Box(Modifier.weight(1f).padding(2.dp)) {
                                if (d != null) DayCell(d, byDay[d] ?: emptyList(), today, d == selected) { selectedKey = d.toString() }
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.padding(start = 20.dp, end = 16.dp, top = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (selected == today) "Today" else Describe.longLabel(selected), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = c.text)
                    Text(
                        when {
                            dayItems.isEmpty() -> "Nothing due"
                            else -> "${dayItems.size} ${if (dayItems.size == 1) "task" else "tasks"}" + if (!selected.isAfter(today)) " · $doneCount done" else ""
                        },
                        fontSize = 14.sp, color = c.text2,
                    )
                }
                Box(
                    Modifier.height(40.dp).border(1.dp, c.outline, RoundedCornerShape(20.dp)).clickable(role = Role.Button) { onOpenDay(selected) }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Open day", color = c.primary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
            }
        }
        items(dayItems, key = { it.task.id }) { item ->
            Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { DayRow(item, selected, today) { onOpen(item.task) } }
        }
    }
}

@Composable
private fun DayCell(d: LocalDate, items: List<TodayItem>, today: LocalDate, sel: Boolean, onClick: () -> Unit) {
    val c = LocalAppColors.current
    val allDone = items.isNotEmpty() && items.all { it.done || it.skipped } && !d.isAfter(today)
    val bg = when {
        sel -> c.primary
        d == today -> c.primaryContainer
        else -> Color.Transparent
    }
    val fg = when {
        sel -> c.onPrimary
        d == today -> c.onPrimaryContainer
        d.isBefore(today) -> c.text2
        else -> c.text
    }
    val label = (if (d == today) "Today, " else "") + Describe.longLabel(d) +
        ", ${items.size} ${if (items.size == 1) "task" else "tasks"}" + if (allDone) ", all done" else ""
    Column(
        Modifier.fillMaxWidth().height(56.dp).background(bg, RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = label
                selected = sel
            }
            .padding(top = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("${d.dayOfMonth}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = fg)
        if (allDone) {
            Box(Modifier.size(16.dp).background(if (sel) c.onPrimary else c.done, CircleShape), contentAlignment = Alignment.Center) {
                Icon(appIcon("check", 3.5f), contentDescription = null, tint = if (sel) c.primary else c.onDone, modifier = Modifier.size(10.dp))
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                items.take(3).forEach { i ->
                    val dot = if (sel) c.onPrimary else taskColors(i.task.color, c.dark).second
                    Box(Modifier.size(6.dp).background(if (i.done || i.skipped) dot.copy(alpha = 0.35f) else dot, CircleShape))
                }
                if (items.size > 3) Text("+${items.size - 3}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = fg)
            }
        }
    }
}

@Composable
private fun DayRow(item: TodayItem, date: LocalDate, today: LocalDate, onOpen: () -> Unit) {
    val c = LocalAppColors.current
    val task = item.task
    val status = when {
        item.done -> "Done"
        item.skipped -> "Skipped"
        item.late != null -> item.late
        date.isBefore(today) -> "Not done"
        else -> null
    }
    val meta = listOfNotNull(Describe.repeat(task.repeat, true), task.time?.let { Describe.time(it) }).joinToString(" · ")
    Row(
        Modifier.fillMaxWidth().background(c.surface, RoundedCornerShape(16.dp)).clickable(role = Role.Button, onClick = onOpen)
            .padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TaskIconBox(task.icon, task.color)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                task.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = if (item.done) c.text2 else c.text,
                textDecoration = if (item.done) TextDecoration.LineThrough else null, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (task.description.isNotBlank()) Text(task.description, fontSize = 14.sp, color = c.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (meta.isNotBlank()) Text(meta, fontSize = 12.sp, color = c.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (status != null) {
            val (sbg, sfg) = when {
                item.done -> c.done to c.onDone
                item.late != null || status == "Not done" -> c.amberTint to c.streak
                else -> c.surface2 to c.text2
            }
            Text(status, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = sfg, modifier = Modifier.background(sbg, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp))
        }
    }
}
