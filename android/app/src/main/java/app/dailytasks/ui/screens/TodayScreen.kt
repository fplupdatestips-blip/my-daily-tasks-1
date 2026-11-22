package app.dailytasks.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dailytasks.AppViewModel
import app.dailytasks.DataState
import app.dailytasks.core.Describe
import app.dailytasks.core.RepeatEngine
import app.dailytasks.core.RepeatType
import app.dailytasks.core.Task
import app.dailytasks.core.TimeOfDay
import app.dailytasks.core.TodayItem
import app.dailytasks.core.TodayItems
import app.dailytasks.ui.appIcon
import app.dailytasks.ui.theme.LocalAppColors
import java.time.LocalDate

private val GROUPS = listOf(
    TimeOfDay.MORNING to "Morning",
    TimeOfDay.AFTERNOON to "Afternoon",
    TimeOfDay.EVENING to "Evening",
    TimeOfDay.ANYTIME to "Anytime",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodayScreen(
    vm: AppViewModel,
    data: DataState,
    today: LocalDate,
    date: LocalDate,
    setDate: (LocalDate) -> Unit,
    onOpen: (Task) -> Unit,
    onEdit: (Task?, Task?) -> Unit,
    onUndo: (String, () -> Unit) -> Unit,
    onSettings: () -> Unit,
) {
    val c = LocalAppColors.current
    val view = LocalView.current
    val weekStart = data.settings.firstDayOfWeek
    val items = remember(data, date, today) { TodayItems.forDay(data.tasks, data.completions, date, today, weekStart) }
    val open = items.filter { !it.done && !it.skipped }
    val finished = items.filter { it.done || it.skipped }
    val doneCount = items.count { it.done }
    var showDone by rememberSaveable { mutableStateOf(false) }
    var dismissedMissed by rememberSaveable(today) { mutableStateOf(false) }
    val missedYesterday = remember(data, today, date) {
        if (date != today) 0 else TodayItems.forDay(data.tasks, data.completions, today.minusDays(1), today, weekStart)
            .count { !it.done && !it.skipped && it.task.repeat.type != RepeatType.NONE }
    }

    val toggle: (TodayItem) -> Unit = { item ->
        val undo = vm.toggleDone(item, date)
        if (!item.done) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            onUndo("Done: ${item.task.name}", undo)
        }
    }
    val skip: (TodayItem) -> Unit = { item ->
        onUndo(if (item.skipped) "Unskipped ${item.task.name}" else "Skipped ${item.task.name}", vm.skip(item, date))
    }
    val sub: (TodayItem, String) -> Unit = { item, id ->
        val (undo, completed) = vm.toggleSubtask(item, id, date)
        if (completed) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            onUndo("Done: ${item.task.name}", undo)
        }
    }

    @Composable
    fun Card(item: TodayItem) = TaskCard(item, onToggle = { toggle(item) }, onSubtask = { sub(item, it) }, onOpen = { onOpen(item.task) }, onSkip = { skip(item) }, onEdit = { onEdit(item.task, null) })

    LazyColumn(contentPadding = PaddingValues(bottom = 112.dp)) {
        item {
            ScreenHeader(
                title = when {
                    date == today -> "Today"
                    date < today -> "Past day"
                    else -> "Upcoming"
                },
                subtitle = Describe.longLabel(date),
            ) {
                if (date != today) TextButton(onClick = { setDate(today) }) { Text("Back to today", color = c.primary, fontWeight = FontWeight.SemiBold) }
                SyncIcon(data.sync)
                SettingsButton(onSettings)
            }
        }
        item { WeekStrip(data, today, date, setDate) }

        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (items.isNotEmpty()) ProgressCard(doneCount, items.size, open.size)
                if (missedYesterday > 0 && !dismissedMissed) {
                    Row(Modifier.fillMaxWidth().background(c.surface, RoundedCornerShape(16.dp)).padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("You missed $missedYesterday ${if (missedYesterday == 1) "task" else "tasks"} yesterday.", color = c.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = { setDate(today.minusDays(1)) }) { Text("Review", color = c.primary, fontWeight = FontWeight.SemiBold) }
                        IconButton(onClick = { dismissedMissed = true }) { Icon(appIcon("close"), contentDescription = "Dismiss", tint = c.text2, modifier = Modifier.size(16.dp)) }
                    }
                }
                if (items.isEmpty() && data.tasks.isEmpty()) {
                    Column(Modifier.fillMaxWidth().background(c.surface, RoundedCornerShape(24.dp)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Add your first daily task", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = c.text)
                        Text("Start from one of these, or tap the add button to make your own.", fontSize = 14.sp, color = c.text2, textAlign = TextAlign.Center)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Templates.list.forEach { t ->
                                Row(
                                    Modifier.heightIn(min = 44.dp).border(1.dp, c.outline, RoundedCornerShape(12.dp)).clickable { onEdit(null, t) }.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(appIcon(t.icon), contentDescription = null, tint = c.text, modifier = Modifier.size(16.dp))
                                    Text(t.name, fontSize = 14.sp, color = c.text)
                                }
                            }
                        }
                    }
                } else if (items.isEmpty()) {
                    Text(
                        if (date == today) "Nothing due today." else "Nothing due on ${Describe.longLabel(date)}.",
                        color = c.text2, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    )
                }
            }
        }

        GROUPS.forEach { (key, label) ->
            val list = open.filter { it.task.timeOfDay == key }
            if (list.isNotEmpty()) {
                item(key = "g-$key") { GroupTitle(label, Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)) }
                items(list, key = { it.task.id }) { item ->
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { Card(item) }
                }
            }
        }

        if (finished.isNotEmpty()) {
            item(key = "done-header") {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth().height(52.dp)
                        .background(c.surface, RoundedCornerShape(16.dp)).clickable { showDone = !showDone }.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.size(24.dp).background(c.done, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(appIcon("check", 3f), contentDescription = null, tint = c.onDone, modifier = Modifier.size(14.dp))
                    }
                    Text("Done · ${finished.size}", color = c.text2, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Icon(appIcon(if (showDone) "chevronUp" else "chevronDown"), contentDescription = if (showDone) "Hide done tasks" else "Show done tasks", tint = c.text2)
                }
            }
            if (showDone) {
                items(finished, key = { "d-" + it.task.id }) { item ->
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { Card(item) }
                }
            }
        }
    }
}

@Composable
private fun WeekStrip(data: DataState, today: LocalDate, date: LocalDate, setDate: (LocalDate) -> Unit) {
    val c = LocalAppColors.current
    val start = RepeatEngine.startOfWeek(date, data.settings.firstDayOfWeek)
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { setDate(date.minusDays(7)) }, modifier = Modifier.size(40.dp)) {
            Icon(appIcon("chevronLeft"), contentDescription = "Previous week", tint = c.text2, modifier = Modifier.size(18.dp))
        }
        (0L until 7L).forEach { i ->
            val d = start.plusDays(i)
            val sel = d == date
            val its = remember(data, d, today) { TodayItems.forDay(data.tasks, data.completions, d, today, data.settings.firstDayOfWeek) }
            val all = its.isNotEmpty() && its.all { it.done || it.skipped } && !d.isAfter(today)
            val bg = if (sel) c.primary else Color.Transparent
            val fg = when {
                sel -> c.onPrimary
                d == today -> c.primary
                else -> c.text2
            }
            Column(
                Modifier.weight(1f).height(60.dp).padding(horizontal = 2.dp).background(bg, RoundedCornerShape(16.dp))
                    .clickable(role = Role.Button) { setDate(d) }
                    .semantics { contentDescription = (if (d == today) "Today, " else "") + Describe.longLabel(d) + if (all) ", all done" else "" },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(Describe.weekdayShort(d.dayOfWeek.value).take(1), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = fg)
                Text("${d.dayOfMonth}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = fg)
                Box(Modifier.padding(top = 2.dp).size(5.dp).background(if (all && !sel) c.done else Color.Transparent, CircleShape))
            }
        }
        IconButton(onClick = { setDate(date.plusDays(7)) }, modifier = Modifier.size(40.dp)) {
            Icon(appIcon("chevronRight"), contentDescription = "Next week", tint = c.text2, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ProgressCard(done: Int, total: Int, left: Int) {
    val c = LocalAppColors.current
    val frac by animateFloatAsState(if (total == 0) 0f else done.toFloat() / total, tween(300), label = "ring")
    Row(
        Modifier.fillMaxWidth().background(c.primaryContainer, RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$done of $total tasks done" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(52.dp)) {
                val stroke = 6.dp.toPx()
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(c.ringTrack, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                if (frac > 0f) drawArc(c.primary, -90f, 360f * frac, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Text("$done/$total", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.onPrimaryContainer)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(if (left == 0) "All done for the day" else "$done of $total done", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = c.onPrimaryContainer)
            Text(if (left == 0) "Nice work." else "$left left", fontSize = 14.sp, color = c.onPrimaryContainer)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskCard(
    item: TodayItem,
    onToggle: () -> Unit,
    onSubtask: (String) -> Unit,
    onOpen: () -> Unit,
    onSkip: () -> Unit,
    onEdit: () -> Unit,
) {
    val c = LocalAppColors.current
    val t = item.task
    val subs = t.subtasks
    val subsDone = subs.count { it.id in item.subtasksDone }
    var expanded by rememberSaveable(t.id) { mutableStateOf(subsDone > 0 && !item.done) }
    var menu by remember { mutableStateOf(false) }
    val meta = listOfNotNull(Describe.repeat(t.repeat, short = true), t.time?.let { Describe.time(it) }).joinToString(" · ")
    val state = when {
        item.done -> "done"
        item.skipped -> "skipped"
        else -> "not done"
    }

    Column(
        Modifier.fillMaxWidth().background(c.surface, RoundedCornerShape(16.dp))
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(if (item.skipped) "Unskip" else "Skip today") { onSkip(); true },
                    CustomAccessibilityAction("Edit") { onEdit(); true },
                )
            },
    ) {
        Row(Modifier.padding(start = 12.dp, end = 4.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TaskIconBox(t.icon, t.color, image = t.image)
            Box(Modifier.weight(1f)) {
                Column(
                    Modifier.fillMaxWidth().combinedClickable(onClick = onOpen, onLongClick = { menu = true }, onLongClickLabel = "More actions"),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    if (item.late != null && !item.done) {
                        Text(
                            item.late!!, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = c.streak,
                            modifier = Modifier.background(c.amberTint, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    Text(
                        t.name, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                        color = if (item.done) c.text2 else c.text,
                        textDecoration = if (item.done) TextDecoration.LineThrough else null,
                    )
                    if (t.description.isNotBlank()) {
                        Text(t.description, fontSize = 13.sp, color = c.text2, lineHeight = 17.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (item.skipped) "Skipped" else meta, fontSize = 13.sp, color = c.text2)
                        if (subs.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Icon(appIcon("subtasks", 2f), contentDescription = null, tint = c.text, modifier = Modifier.size(14.dp))
                                Text("$subsDone of ${subs.size}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.text)
                            }
                        }
                        if (item.streak > 0) {
                            Row(Modifier.semantics(mergeDescendants = true) { contentDescription = "${item.streak} day streak" }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Icon(appIcon("flame", 2f), contentDescription = null, tint = c.streak, modifier = Modifier.size(14.dp))
                                Text("${item.streak}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.streak)
                            }
                        }
                    }
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(if (item.done) "Mark not done" else "Mark done") }, onClick = { menu = false; onToggle() })
                    DropdownMenuItem(text = { Text(if (item.skipped) "Unskip" else "Skip today") }, onClick = { menu = false; onSkip() })
                    DropdownMenuItem(text = { Text("Edit") }, onClick = { menu = false; onEdit() })
                    DropdownMenuItem(text = { Text("Details") }, onClick = { menu = false; onOpen() })
                }
            }
            CheckCircle(
                checked = item.done,
                onClick = onToggle,
                description = "${t.name}, $state",
            )
        }
        if (subs.isNotEmpty()) {
            Row(
                Modifier.padding(start = 60.dp, bottom = if (expanded) 0.dp else 8.dp).heightIn(min = 40.dp).clickable(role = Role.Button) { expanded = !expanded }.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(if (expanded) "Hide subtasks" else "Show subtasks", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.primary)
                Icon(appIcon(if (expanded) "chevronUp" else "chevronDown"), contentDescription = null, tint = c.primary, modifier = Modifier.size(16.dp))
            }
            if (expanded) {
                Column(Modifier.padding(start = 64.dp, end = 8.dp, bottom = 8.dp)) {
                    subs.forEach { s ->
                        val d = s.id in item.subtasksDone
                        HorizontalDivider(color = c.divider)
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable(role = Role.Checkbox) { onSubtask(s.id) }
                                .semantics(mergeDescendants = true) { stateDescription = if (d) "done" else "not done" },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(Modifier.width(40.dp), contentAlignment = Alignment.Center) {
                                Box(
                                    Modifier.size(22.dp).then(
                                        if (d) Modifier.background(c.done, RoundedCornerShape(6.dp)) else Modifier.border(2.dp, c.outline, RoundedCornerShape(6.dp)),
                                    ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (d) Icon(appIcon("check", 3f), contentDescription = null, tint = c.onDone, modifier = Modifier.size(13.dp))
                                }
                            }
                            Text(s.title, fontSize = 14.sp, color = if (d) c.text2 else c.text, textDecoration = if (d) TextDecoration.LineThrough else null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CheckCircle(checked: Boolean, onClick: () -> Unit, description: String) {
    val c = LocalAppColors.current
    val fill by animateColorAsState(if (checked) c.done else Color.Transparent, tween(200), label = "check")
    Box(
        Modifier.size(48.dp).clickable(role = Role.Checkbox, onClickLabel = if (checked) "Mark not done" else "Mark done", onClick = onClick)
            .semantics {
                contentDescription = description
                stateDescription = if (checked) "done" else "not done"
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(32.dp).background(fill, CircleShape).then(if (checked) Modifier else Modifier.border(2.dp, c.outline, CircleShape)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(appIcon("check", 3f), contentDescription = null, tint = c.onDone, modifier = Modifier.size(18.dp))
        }
    }
}
