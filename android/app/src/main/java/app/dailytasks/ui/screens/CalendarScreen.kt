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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import app.dailytasks.core.RepeatEngine
import app.dailytasks.core.TimeOfDay
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
    var view by rememberSaveable { mutableStateOf("week") }
    var weekKey by rememberSaveable { mutableStateOf(RepeatEngine.startOfWeek(today, weekStart).toString()) }
    val weekFirst = RepeatEngine.startOfWeek(LocalDate.parse(weekKey), weekStart)
    val weekDays = (0L until 7L).map { weekFirst.plusDays(it) }
    val weekItems = remember(weekKey, weekStart, data, today) { weekDays.map { CalendarItems.forDay(data.allTasks, data.completions, it, today, weekStart) } }
    val weekLabel = run {
        val last = weekDays[6]
        val m1 = Describe.monthLong(weekFirst.monthValue).take(3)
        if (weekFirst.monthValue == last.monthValue) "$m1 ${weekFirst.dayOfMonth} – ${last.dayOfMonth}"
        else "$m1 ${weekFirst.dayOfMonth} – ${Describe.monthLong(last.monthValue).take(3)} ${last.dayOfMonth}"
    }
    val atToday = if (view == "week") today in weekDays else month == YearMonth.from(today)

    val rows = remember(month, weekStart) { CalendarItems.monthGrid(month, weekStart) }
    val byDay = remember(rows, data, today) {
        rows.flatten().filterNotNull().associateWith { CalendarItems.forDay(data.allTasks, data.completions, it, today, weekStart) }
    }
    val dayItems = remember(byDay, selected, data, today) {
        byDay[selected] ?: CalendarItems.forDay(data.allTasks, data.completions, selected, today, weekStart)
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
                Modifier.padding(horizontal = 8.dp).fillMaxWidth().background(c.surface, RoundedCornerShape(20.dp)).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.height(40.dp).border(1.dp, c.outline, RoundedCornerShape(12.dp))
                            .clickable(enabled = !atToday, role = Role.Button) {
                                monthKey = YearMonth.from(today).toString()
                                selectedKey = today.toString()
                                weekKey = RepeatEngine.startOfWeek(today, weekStart).toString()
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Today", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (atToday) c.text2 else c.text) }
                    val unit = if (view == "week") "week" else "month"
                    IconButton(onClick = { if (view == "week") weekKey = weekFirst.minusWeeks(1).toString() else monthKey = month.minusMonths(1).toString() }, modifier = Modifier.size(40.dp)) {
                        Icon(appIcon("chevronLeft"), contentDescription = "Previous $unit", tint = c.text2, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { if (view == "week") weekKey = weekFirst.plusWeeks(1).toString() else monthKey = month.plusMonths(1).toString() }, modifier = Modifier.size(40.dp)) {
                        Icon(appIcon("chevronRight"), contentDescription = "Next $unit", tint = c.text2, modifier = Modifier.size(18.dp))
                    }
                    Box(Modifier.weight(1f))
                    Row(Modifier.background(c.surface2, RoundedCornerShape(12.dp)).padding(3.dp)) {
                        listOf("week" to "Week", "month" to "Month").forEach { (k, label) ->
                            val on = view == k
                            Box(
                                Modifier.height(34.dp).background(if (on) c.surface else Color.Transparent, RoundedCornerShape(9.dp))
                                    .clickable(role = Role.Tab) { view = k }.semantics { this.selected = on }.padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) { Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (on) c.text else c.text2) }
                        }
                    }
                }
                Text(
                    if (view == "week") weekLabel else "${Describe.monthLong(month.monthValue)} ${month.year}",
                    fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = c.text,
                    modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 4.dp),
                )
                if (view == "week") {
                    WeekStrip(weekDays, weekItems, today, onOpen, onOpenDay)
                } else {
                Row(Modifier.fillMaxWidth()) {
                    (0 until 7).forEach { i ->
                        val iso = (weekStart - 1 + i) % 7 + 1
                        Text(
                            Describe.weekdayShort(iso), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = c.text2,
                            textAlign = TextAlign.Center, modifier = Modifier.weight(1f).padding(bottom = 4.dp),
                        )
                    }
                }
                Column(Modifier.fillMaxWidth().border(1.dp, c.divider, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp))) {
                    rows.forEachIndexed { ri, row ->
                        if (ri > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(c.divider))
                        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                            row.forEachIndexed { ci, d ->
                                if (ci > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(c.divider))
                                Box(Modifier.weight(1f).fillMaxHeight()) {
                                    if (d != null) DayCell(d, byDay[d] ?: emptyList(), today, d == selected) { selectedKey = d.toString() }
                                    else Box(Modifier.fillMaxSize().background(c.surface2.copy(alpha = 0.6f)))
                                }
                            }
                        }
                    }
                }
                }
            }
        }
        if (view == "month") item {
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
        if (view == "month") items(dayItems, key = { it.task.id }) { item ->
            Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { DayRow(item, selected, today) { onOpen(item.task) } }
        }
    }
}

@Composable
private fun DayCell(d: LocalDate, items: List<TodayItem>, today: LocalDate, sel: Boolean, onClick: () -> Unit) {
    val c = LocalAppColors.current
    val allDone = items.isNotEmpty() && items.all { it.done || it.skipped } && !d.isAfter(today)
    val label = (if (d == today) "Today, " else "") + Describe.longLabel(d) +
        ", ${items.size} ${if (items.size == 1) "task" else "tasks"}" + (if (allDone) ", all done" else "") +
        if (items.isNotEmpty()) ": " + items.joinToString(", ") { it.task.name } else ""
    val shown = if (items.size > 3) items.take(2) else items
    val more = items.size - shown.size
    Column(
        Modifier.fillMaxSize().heightIn(min = 92.dp)
            .background(if (sel) c.primaryContainer.copy(alpha = 0.6f) else Color.Transparent)
            .then(if (sel) Modifier.border(2.dp, c.primary) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = label
                selected = sel
            }
            .padding(3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(bottom = 2.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            if (allDone) {
                Box(Modifier.padding(end = 2.dp).size(14.dp).background(c.done, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(appIcon("check", 3.5f), contentDescription = null, tint = c.onDone, modifier = Modifier.size(9.dp))
                }
            }
            Box(
                Modifier.size(22.dp).background(if (d == today) c.primary else Color.Transparent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${d.dayOfMonth}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    color = when {
                        d == today -> c.onPrimary
                        d.isBefore(today) -> c.text2
                        else -> c.text
                    },
                )
            }
        }
        shown.forEach { i ->
            val (tint, fg) = taskColors(i.task.color, c.dark)
            val finished = i.done || i.skipped
            Text(
                i.task.shortName ?: i.task.name,
                fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Medium,
                color = if (finished) fg.copy(alpha = 0.5f) else fg,
                textDecoration = if (i.done) TextDecoration.LineThrough else null,
                maxLines = 1, softWrap = false, overflow = TextOverflow.Clip,
                modifier = Modifier.fillMaxWidth().background(if (finished) tint.copy(alpha = 0.5f) else tint, RoundedCornerShape(6.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
        if (more > 0) Text("+$more more", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = c.text2, modifier = Modifier.padding(start = 4.dp))
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
        TaskIconBox(task.icon, task.color, image = task.image)
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

/** A week as seven side-by-side columns (scroll sideways), one card per task. */
@Composable
private fun WeekStrip(days: List<LocalDate>, items: List<List<TodayItem>>, today: LocalDate, onOpen: (Task) -> Unit, onOpenDay: (LocalDate) -> Unit) {
    val c = LocalAppColors.current
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        days.forEachIndexed { i, d ->
            val its = items[i]
            val isToday = d == today
            Column(
                Modifier.width(140.dp).fillMaxHeight().heightIn(min = 360.dp)
                    .background(c.bg, RoundedCornerShape(16.dp))
                    .border(if (isToday) 1.5.dp else 1.dp, if (isToday) c.primary else c.divider, RoundedCornerShape(16.dp)),
            ) {
                Column(
                    Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "Open this day") { onOpenDay(d) }
                        .semantics(mergeDescendants = true) { contentDescription = (if (isToday) "Today, " else "") + Describe.longLabel(d) + ", ${its.size} tasks" }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(Describe.weekdayShort(d.dayOfWeek.value), fontSize = 12.sp, color = c.text2, fontWeight = FontWeight.Medium)
                    Box(Modifier.size(30.dp).background(if (isToday) c.primary else Color.Transparent, CircleShape), contentAlignment = Alignment.Center) {
                        Text("${d.dayOfMonth}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = if (isToday) c.onPrimary else if (d.isBefore(today)) c.text2 else c.text)
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.divider))
                Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    its.forEach { WeekCard(it, d, today) { onOpen(it.task) } }
                }
            }
        }
    }
}

@Composable
private fun WeekCard(item: TodayItem, date: LocalDate, today: LocalDate, onClick: () -> Unit) {
    val c = LocalAppColors.current
    val t = item.task
    val missed = !item.done && !item.skipped && date.isBefore(today)
    val (label, icon) = when {
        item.done -> "Done" to "check"
        item.skipped -> "Skipped" to "skip"
        missed -> "Missed" to "close"
        else -> "To do" to "bell"
    }
    val pillBg = when { item.done -> c.done.copy(alpha = 0.15f); missed -> c.amberTint; else -> c.surface2 }
    val pillFg = when { item.done -> c.done; missed -> c.streak; else -> c.text2 }
    val time = t.time ?: when (t.timeOfDay) {
        TimeOfDay.MORNING -> "Morning"
        TimeOfDay.AFTERNOON -> "Afternoon"
        TimeOfDay.EVENING -> "Evening"
        TimeOfDay.ANYTIME -> "Any time"
    }
    Row(
        Modifier.fillMaxWidth()
            .background(if (item.done) c.done.copy(alpha = 0.10f) else c.surface, RoundedCornerShape(12.dp))
            .border(1.dp, if (item.done) c.done.copy(alpha = 0.3f) else c.divider, RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "${t.name}, $time, $label" }
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TaskIconBox(t.icon, t.color, 34.dp, 17.dp, t.image)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(time, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = c.text, maxLines = 1)
            Text(
                t.shortName ?: t.name, fontSize = 12.sp, color = c.text2, maxLines = 1, overflow = TextOverflow.Ellipsis,
                textDecoration = if (item.done) TextDecoration.LineThrough else null,
            )
            Row(
                Modifier.background(pillBg, RoundedCornerShape(6.dp)).padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Icon(appIcon(icon, 2.6f), contentDescription = null, tint = pillFg, modifier = Modifier.size(10.dp))
                Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = pillFg, maxLines = 1)
            }
        }
    }
}
