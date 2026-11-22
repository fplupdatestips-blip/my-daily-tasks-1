package app.dailytasks.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dailytasks.core.Describe
import app.dailytasks.core.Period
import app.dailytasks.core.Reminder
import app.dailytasks.core.Repeat
import app.dailytasks.core.RepeatType
import app.dailytasks.core.Subtask
import app.dailytasks.core.Task
import app.dailytasks.core.TimeOfDay
import app.dailytasks.ui.TASK_ICONS
import app.dailytasks.ui.appIcon
import app.dailytasks.ui.theme.LocalAppColors
import app.dailytasks.ui.theme.TaskColorKeys
import app.dailytasks.ui.theme.taskColors
import app.dailytasks.ui.theme.taskSwatch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

private data class Preset(val key: String, val label: String, val make: (LocalDate) -> Repeat)

private val PRESETS = listOf(
    Preset("someday", "No date yet") { Repeat(RepeatType.SOMEDAY) },
    Preset("none", "Does not repeat") { Repeat(RepeatType.NONE, dueDate = it) },
    Preset("daily", "Every day") { Repeat(RepeatType.DAILY, interval = 1, startDate = it) },
    Preset("weekdays", "Weekdays") { Repeat(RepeatType.WEEKLY, daysOfWeek = listOf(1, 2, 3, 4, 5), startDate = it) },
    Preset("weekly", "Specific days") { Repeat(RepeatType.WEEKLY, daysOfWeek = listOf(it.dayOfWeek.value), startDate = it) },
    Preset("everyN", "Every few days") { Repeat(RepeatType.DAILY, interval = 3, startDate = it) },
    Preset("quota", "Times a week") { Repeat(RepeatType.QUOTA, count = 3, period = Period.WEEK, startDate = it) },
    Preset("monthly", "Monthly") { Repeat(RepeatType.MONTHLY, dayOfMonth = it.dayOfMonth, startDate = it) },
    Preset("yearly", "Yearly") { Repeat(RepeatType.YEARLY, month = it.monthValue, dayOfMonth = it.dayOfMonth, startDate = it) },
    Preset("after", "After last done") { Repeat(RepeatType.AFTER_DONE, interval = 7, startDate = it) },
)

private fun presetKey(r: Repeat): String = when (r.type) {
    RepeatType.NONE -> "none"
    RepeatType.SOMEDAY -> "someday"
    RepeatType.DAILY -> if ((r.interval ?: 1) <= 1) "daily" else "everyN"
    RepeatType.WEEKLY -> if (r.daysOfWeek?.sorted() == listOf(1, 2, 3, 4, 5)) "weekdays" else "weekly"
    RepeatType.QUOTA -> "quota"
    RepeatType.MONTHLY -> "monthly"
    RepeatType.YEARLY -> "yearly"
    RepeatType.AFTER_DONE -> "after"
}

fun newId(): String = UUID.randomUUID().toString().replace("-", "").take(20)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(task: Task?, preset: Task?, today: LocalDate, nextSortOrder: Long, onCancel: () -> Unit, onSave: (Task) -> Unit) {
    val c = LocalAppColors.current
    var t by remember {
        mutableStateOf(
            task ?: (preset ?: Task(id = "", name = "")).let { p ->
                p.copy(
                    id = newId(),
                    sortOrder = nextSortOrder,
                    createdAt = System.currentTimeMillis(),
                    repeat = if (p.repeat.type == RepeatType.NONE || p.repeat.type == RepeatType.SOMEDAY) p.repeat else p.repeat.copy(startDate = p.repeat.startDate ?: today),
                )
            },
        )
    }
    var iconsOpen by remember { mutableStateOf(false) }
    var newSub by remember { mutableStateOf("") }
    var more by remember { mutableStateOf(t.repeat.endDate != null) }
    var timeDialog by remember { mutableStateOf<((String) -> Unit)?>(null) }
    var timeInitial by remember { mutableStateOf("09:00") }
    var dateDialog by remember { mutableStateOf<((LocalDate) -> Unit)?>(null) }
    var dateInitial by remember { mutableStateOf(today) }
    val nameFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (task == null) nameFocus.requestFocus() }

    fun setR(f: (Repeat) -> Repeat) { t = t.copy(repeat = f(t.repeat)) }
    fun pickTime(initial: String?, onPick: (String) -> Unit) { timeInitial = initial ?: "09:00"; timeDialog = onPick }
    fun pickDate(initial: LocalDate?, onPick: (LocalDate) -> Unit) { dateInitial = initial ?: today; dateDialog = onPick }
    fun addSub() {
        if (newSub.isNotBlank()) {
            t = t.copy(subtasks = t.subtasks + Subtask(newId(), newSub.trim()))
            newSub = ""
        }
    }
    val canSave = t.name.isNotBlank()
    val save = {
        if (canSave) {
            val subs = (t.subtasks + if (newSub.isNotBlank()) listOf(Subtask(newId(), newSub.trim())) else emptyList()).filter { it.title.isNotBlank() }
            onSave(t.copy(name = t.name.trim(), description = t.description.trim(), subtasks = subs))
        }
    }

    Column(Modifier.fillMaxSize().background(c.surface).statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCancel) { Icon(appIcon("close", 2f), contentDescription = "Close", tint = c.text2) }
            Text(if (task == null) "New task" else "Edit task", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = c.text, modifier = Modifier.weight(1f))
            Button(
                onClick = save, enabled = canSave,
                colors = ButtonDefaults.buttonColors(containerColor = c.primary, contentColor = c.onPrimary),
                modifier = Modifier.padding(end = 8.dp),
            ) { Text("Save", fontWeight = FontWeight.SemiBold) }
        }
        HorizontalDivider(color = c.divider)

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            OutlinedTextField(
                value = t.name, onValueChange = { t = t.copy(name = it) }, label = { Text("Name") }, singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth().focusRequester(nameFocus),
            )
            Column {
                OutlinedTextField(
                    value = t.description, onValueChange = { t = t.copy(description = it) }, label = { Text("Description") },
                    minLines = 2, maxLines = 5, modifier = Modifier.fillMaxWidth(),
                )
                Text("Shown under the task name on Today", fontSize = 12.sp, color = c.text2, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
            }

            // Icon and color
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val (tint, fg) = taskColors(t.color, c.dark)
                    Box(
                        Modifier.size(56.dp).background(tint, RoundedCornerShape(16.dp)).clickable(role = Role.Button) { iconsOpen = !iconsOpen }
                            .semantics { contentDescription = "Choose icon, currently ${t.icon}" },
                        contentAlignment = Alignment.Center,
                    ) { Icon(appIcon(t.icon), contentDescription = null, tint = fg, modifier = Modifier.size(26.dp)) }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TaskColorKeys.forEach { k ->
                            val sel = t.color == k
                            Box(
                                Modifier.size(44.dp).then(if (sel) Modifier.border(3.dp, c.text, CircleShape) else Modifier).padding(if (sel) 5.dp else 0.dp)
                                    .background(taskSwatch(k), CircleShape).clickable(role = Role.RadioButton) { t = t.copy(color = k) }
                                    .semantics { contentDescription = k; selected = sel },
                            )
                        }
                    }
                }
                if (iconsOpen) {
                    FlowRow(Modifier.background(c.surface2, RoundedCornerShape(16.dp)).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TASK_ICONS.forEach { i ->
                            Box(
                                Modifier.size(44.dp).background(if (t.icon == i) c.primaryContainer else c.surface2, RoundedCornerShape(12.dp))
                                    .clickable(role = Role.RadioButton) { t = t.copy(icon = i); iconsOpen = false }
                                    .semantics { contentDescription = i; selected = t.icon == i },
                                contentAlignment = Alignment.Center,
                            ) { Icon(appIcon(i), contentDescription = null, tint = if (t.icon == i) c.onPrimaryContainer else c.text) }
                        }
                    }
                }
            }

            // Repeats
            Section("Repeats") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PRESETS.forEach { p ->
                        Chip(p.label, presetKey(t.repeat) == p.key) {
                            if (presetKey(t.repeat) != p.key) {
                                val r = p.make(today)
                                t = t.copy(repeat = if (r.type == RepeatType.NONE || r.type == RepeatType.SOMEDAY) r else r.copy(startDate = t.repeat.startDate ?: today))
                            }
                        }
                    }
                }
                RuleFields(t.repeat, ::setR, pickDate = { d, f -> pickDate(d, f) })
                val note = when (t.repeat.type) {
                    RepeatType.NONE -> " If it is not done, it moves to the next day until it is."
                    RepeatType.SOMEDAY -> " It waits in the Someday list (Tasks tab) until you give it a date."
                    RepeatType.AFTER_DONE -> " The next one is due that many days after you last tick it."
                    else -> ""
                }
                Text(Describe.repeat(t.repeat) + "." + note, fontSize = 14.sp, color = c.text2)
            }

            // Subtasks
            Section("Subtasks") {
                Column(Modifier.border(1.dp, c.divider, RoundedCornerShape(12.dp))) {
                    t.subtasks.forEachIndexed { i, s ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = s.title,
                                onValueChange = { v -> t = t.copy(subtasks = t.subtasks.mapIndexed { k, x -> if (k == i) x.copy(title = v) else x }) },
                                singleLine = true, modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                            )
                            IconButton(onClick = { t = t.copy(subtasks = t.subtasks.toMutableList().apply { add(i - 1, removeAt(i)) }) }, enabled = i > 0) {
                                Icon(appIcon("chevronUp"), contentDescription = "Move ${s.title} up", tint = c.text2, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { t = t.copy(subtasks = t.subtasks.toMutableList().apply { add(i + 1, removeAt(i)) }) }, enabled = i < t.subtasks.size - 1) {
                                Icon(appIcon("chevronDown"), contentDescription = "Move ${s.title} down", tint = c.text2, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { t = t.copy(subtasks = t.subtasks.filterIndexed { k, _ -> k != i }) }) {
                                Icon(appIcon("close"), contentDescription = "Remove ${s.title}", tint = c.text2, modifier = Modifier.size(16.dp))
                            }
                        }
                        HorizontalDivider(color = c.divider)
                    }
                    Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newSub, onValueChange = { newSub = it }, placeholder = { Text("Add subtask") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { addSub() }),
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { addSub() }, enabled = newSub.isNotBlank()) {
                            Icon(appIcon("plus", 2.2f), contentDescription = "Add subtask", tint = c.primary)
                        }
                    }
                }
                if (t.subtasks.isNotEmpty()) {
                    Text(
                        "Ticking the last subtask completes the task." + if (t.repeat.type != RepeatType.NONE && t.repeat.type != RepeatType.SOMEDAY) " Subtasks reset each day." else "",
                        fontSize = 12.sp, color = c.text2,
                    )
                }
            }

            // Time of day
            Section("Time of day") {
                val times = TimeOfDay.entries
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    times.forEachIndexed { i, x ->
                        SegmentedButton(
                            selected = t.timeOfDay == x,
                            onClick = { t = t.copy(timeOfDay = x) },
                            shape = SegmentedButtonDefaults.itemShape(i, times.size),
                            icon = {},
                        ) { Text(x.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 13.sp, maxLines = 1) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Exact time", color = c.text, modifier = Modifier.weight(1f))
                    TextButton(onClick = { pickTime(t.time) { v -> t = t.copy(time = v) } }) {
                        Text(t.time?.let { Describe.time(it) } ?: "None", color = c.primary, fontWeight = FontWeight.SemiBold)
                    }
                    if (t.time != null) IconButton(onClick = { t = t.copy(time = null) }) {
                        Icon(appIcon("close"), contentDescription = "Clear time", tint = c.text2, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Reminders
            Column(Modifier.fillMaxWidth().background(c.surface2, RoundedCornerShape(16.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(appIcon("bell"), contentDescription = null, tint = c.text2)
                    Column {
                        Text("Reminders", fontSize = 16.sp, color = c.text)
                        Text("Notifications arrive in the next update", fontSize = 13.sp, color = c.text2)
                    }
                }
                t.reminders.forEachIndexed { i, r ->
                    Row(Modifier.padding(start = 36.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { pickTime(r.localTime) { v -> t = t.copy(reminders = t.reminders.mapIndexed { k, x -> if (k == i) x.copy(localTime = v) else x }) } }) {
                            Text(Describe.time(r.localTime), color = c.text, fontSize = 16.sp)
                        }
                        IconButton(onClick = { t = t.copy(reminders = t.reminders.filterIndexed { k, _ -> k != i }) }) {
                            Icon(appIcon("close"), contentDescription = "Remove reminder", tint = c.text2, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                TextButton(onClick = { pickTime(t.time) { v -> t = t.copy(reminders = t.reminders + Reminder(v)) } }, modifier = Modifier.padding(start = 24.dp)) {
                    Text("+ Add reminder", color = c.primary, fontWeight = FontWeight.SemiBold)
                }
            }

            // More
            Column {
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { more = !more }, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("More options", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = c.text)
                        Text("Start and end date", fontSize = 13.sp, color = c.text2)
                    }
                    Icon(appIcon(if (more) "chevronUp" else "chevronDown"), contentDescription = null, tint = c.text2)
                }
                if (more && t.repeat.type != RepeatType.NONE && t.repeat.type != RepeatType.SOMEDAY) {
                    DateRow("Starts", t.repeat.startDate) { pickDate(t.repeat.startDate) { d -> setR { it.copy(startDate = d) } } }
                    DateRow("Ends", t.repeat.endDate, onClear = { setR { it.copy(endDate = null) } }) { pickDate(t.repeat.endDate) { d -> setR { it.copy(endDate = d) } } }
                }
            }
        }
    }

    timeDialog?.let { onPick ->
        val (h, m) = timeInitial.split(":").map { it.toInt() }
        val state = rememberTimePickerState(initialHour = h, initialMinute = m)
        AlertDialog(
            onDismissRequest = { timeDialog = null },
            confirmButton = {
                TextButton(onClick = {
                    onPick("%02d:%02d".format(state.hour, state.minute))
                    timeDialog = null
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { timeDialog = null }) { Text("Cancel") } },
            text = { TimePicker(state = state) },
        )
    }
    dateDialog?.let { onPick ->
        val state = rememberDatePickerState(initialSelectedDateMillis = dateInitial.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { dateDialog = null },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    dateDialog = null
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { dateDialog = null }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val c = LocalAppColors.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.text2)
        content()
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Box(
        Modifier.heightIn(min = 40.dp)
            .then(if (selected) Modifier.background(c.primaryContainer, RoundedCornerShape(10.dp)) else Modifier.border(1.dp, c.outline, RoundedCornerShape(10.dp)))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 14.sp, color = if (selected) c.onPrimaryContainer else c.text, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun DateRow(label: String, value: LocalDate?, onClear: (() -> Unit)? = null, onPick: () -> Unit) {
    val c = LocalAppColors.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = c.text, modifier = Modifier.weight(1f))
        TextButton(onClick = onPick) { Text(value?.let { Describe.longLabel(it) } ?: "None", color = c.primary, fontWeight = FontWeight.SemiBold) }
        if (value != null && onClear != null) IconButton(onClick = onClear) {
            Icon(appIcon("close"), contentDescription = "Clear $label", tint = c.text2, modifier = Modifier.size(16.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun RuleFields(r: Repeat, setR: ((Repeat) -> Repeat) -> Unit, pickDate: (LocalDate?, (LocalDate) -> Unit) -> Unit) {
    val c = LocalAppColors.current
    @Composable
    fun NumberField(value: Int?, label: String, min: Int, max: Int, onChange: (Int) -> Unit) {
        var text by remember(value) { mutableStateOf((value ?: min).toString()) }
        OutlinedTextField(
            value = text,
            onValueChange = { v ->
                text = v.filter { it.isDigit() }.take(3)
                text.toIntOrNull()?.let { onChange(it.coerceIn(min, max)) }
            },
            label = { Text(label) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(110.dp),
        )
    }

    when {
        r.type == RepeatType.NONE -> DateRow("Due", r.dueDate) { pickDate(r.dueDate) { d -> setR { it.copy(dueDate = d) } } }
        r.type == RepeatType.DAILY && (r.interval ?: 1) > 1 ->
            NumberField(r.interval, "Every N days", 2, 365) { n -> setR { it.copy(interval = n) } }
        r.type == RepeatType.AFTER_DONE ->
            NumberField(r.interval, "Days after done", 1, 365) { n -> setR { it.copy(interval = n) } }
        r.type == RepeatType.WEEKLY -> FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..7).forEach { d ->
                val on = d in (r.daysOfWeek ?: emptyList())
                Box(
                    Modifier.size(44.dp).then(if (on) Modifier.background(c.primary, CircleShape) else Modifier.border(1.dp, c.outline, CircleShape))
                        .clickable(role = Role.Checkbox) {
                            val s = (r.daysOfWeek ?: emptyList()).toMutableSet()
                            if (on) s.remove(d) else s.add(d)
                            if (s.isNotEmpty()) setR { it.copy(daysOfWeek = s.sorted()) }
                        }
                        .semantics { contentDescription = Describe.weekdayLong(d); selected = on },
                    contentAlignment = Alignment.Center,
                ) { Text(Describe.weekdayShort(d).take(2), color = if (on) c.onPrimary else c.text, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal) }
            }
        }
        r.type == RepeatType.QUOTA -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(r.count, "Times", 1, 31) { n -> setR { it.copy(count = n) } }
            Chip("a week", r.period != Period.MONTH) { setR { it.copy(period = Period.WEEK) } }
            Chip("a month", r.period == Period.MONTH) { setR { it.copy(period = Period.MONTH) } }
        }
        r.type == RepeatType.MONTHLY -> {
            val byWeekday = (r.ordinal ?: 0) != 0 && (r.weekday ?: 0) != 0
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("By date", !byWeekday) { setR { it.copy(ordinal = null, weekday = null, dayOfMonth = it.dayOfMonth ?: 1) } }
                    Chip("By weekday", byWeekday) { setR { it.copy(ordinal = 1, weekday = 1) } }
                }
                if (byWeekday) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Picker("Which", listOf(1 to "First", 2 to "Second", 3 to "Third", 4 to "Fourth", -1 to "Last"), r.ordinal ?: 1) { v -> setR { it.copy(ordinal = v) } }
                        Picker("Weekday", (1..7).map { it to Describe.weekdayLong(it) }, r.weekday ?: 1) { v -> setR { it.copy(weekday = v) } }
                    }
                } else {
                    NumberField(r.dayOfMonth, "Day of month", 1, 31) { n -> setR { it.copy(dayOfMonth = n) } }
                }
            }
        }
        r.type == RepeatType.YEARLY -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(r.dayOfMonth, "Day", 1, 31) { n -> setR { it.copy(dayOfMonth = n) } }
            Picker("Month", (1..12).map { it to Describe.monthLong(it) }, r.month ?: 1) { v -> setR { it.copy(month = v) } }
        }
        else -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Picker(label: String, options: List<Pair<Int, String>>, value: Int, onPick: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
        OutlinedTextField(
            value = options.firstOrNull { it.first == value }?.second ?: "",
            onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            modifier = Modifier.width(160.dp).menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (v, l) ->
                DropdownMenuItem(text = { Text(l) }, onClick = { onPick(v); open = false })
            }
        }
    }
}
