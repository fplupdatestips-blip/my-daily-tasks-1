package app.dailytasks.data

import app.dailytasks.core.Completion
import app.dailytasks.core.CompletionStatus
import app.dailytasks.core.Period
import app.dailytasks.core.Reminder
import app.dailytasks.core.Repeat
import app.dailytasks.core.RepeatType
import app.dailytasks.core.Settings
import app.dailytasks.core.Subtask
import app.dailytasks.core.Task
import app.dailytasks.core.TimeOfDay
import java.time.LocalDate

// Firestore <-> model. Field names match web/src/core/model.ts exactly, so
// the phone and the web read each other's documents.

private fun Any?.asLong(): Long? = (this as? Number)?.toLong()
private fun Any?.asInt(): Int? = (this as? Number)?.toInt()
private fun Any?.asDate(): LocalDate? = (this as? String)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

private inline fun <reified T : Enum<T>> Any?.asEnum(default: T): T =
    (this as? String)?.let { s -> enumValues<T>().firstOrNull { it.name == s } } ?: default

@Suppress("UNCHECKED_CAST")
private fun Any?.asMap(): Map<String, Any?> = this as? Map<String, Any?> ?: emptyMap()

private fun Any?.asList(): List<Any?> = this as? List<Any?> ?: emptyList()

fun taskFromDoc(id: String, d: Map<String, Any?>): Task {
    val r = d["repeat"].asMap()
    return Task(
        id = id,
        name = d["name"] as? String ?: "",
        description = d["description"] as? String ?: "",
        icon = d["icon"] as? String ?: "check",
        image = d["image"] as? String,
        color = d["color"] as? String ?: "teal",
        timeOfDay = d["timeOfDay"].asEnum(TimeOfDay.ANYTIME),
        time = d["time"] as? String,
        repeat = Repeat(
            type = r["type"].asEnum(RepeatType.DAILY),
            dueDate = r["dueDate"].asDate(),
            interval = r["interval"].asInt(),
            daysOfWeek = (r["daysOfWeek"] as? List<*>)?.mapNotNull { it.asInt() },
            dayOfMonth = r["dayOfMonth"].asInt(),
            ordinal = r["ordinal"].asInt(),
            weekday = r["weekday"].asInt(),
            month = r["month"].asInt(),
            count = r["count"].asInt(),
            period = (r["period"] as? String)?.let { p -> Period.entries.firstOrNull { it.name == p } },
            startDate = r["startDate"].asDate(),
            endDate = r["endDate"].asDate(),
        ),
        subtasks = d["subtasks"].asList().map { s ->
            val m = s.asMap()
            Subtask(m["id"] as? String ?: "", m["title"] as? String ?: "", m["done"] as? Boolean ?: false)
        },
        reminders = d["reminders"].asList().map { s ->
            val m = s.asMap()
            Reminder(m["localTime"] as? String ?: "09:00", m["enabled"] as? Boolean ?: true)
        },
        sortOrder = d["sortOrder"].asLong() ?: 0,
        createdAt = d["createdAt"].asLong() ?: 0,
        archived = d["archived"] as? Boolean ?: false,
        pausedUntil = d["pausedUntil"].asDate(),
    )
}

fun taskToDoc(t: Task): Map<String, Any?> {
    val r = t.repeat
    val repeat = buildMap<String, Any?> {
        put("type", r.type.name)
        r.dueDate?.let { put("dueDate", it.toString()) }
        r.interval?.let { put("interval", it) }
        r.daysOfWeek?.let { put("daysOfWeek", it) }
        r.dayOfMonth?.let { put("dayOfMonth", it) }
        r.ordinal?.let { put("ordinal", it) }
        r.weekday?.let { put("weekday", it) }
        r.month?.let { put("month", it) }
        r.count?.let { put("count", it) }
        r.period?.let { put("period", it.name) }
        r.startDate?.let { put("startDate", it.toString()) }
        r.endDate?.let { put("endDate", it.toString()) }
    }
    return mapOf(
        "id" to t.id,
        "name" to t.name,
        "description" to t.description,
        "icon" to t.icon,
        "image" to t.image,
        "color" to t.color,
        "timeOfDay" to t.timeOfDay.name,
        "time" to t.time,
        "repeat" to repeat,
        "subtasks" to t.subtasks.map { s ->
            if (r.type == RepeatType.NONE || r.type == RepeatType.SOMEDAY) mapOf("id" to s.id, "title" to s.title, "done" to s.done) else mapOf("id" to s.id, "title" to s.title)
        },
        "reminders" to t.reminders.map { mapOf("localTime" to it.localTime, "enabled" to it.enabled) },
        "sortOrder" to t.sortOrder,
        "createdAt" to t.createdAt,
        "archived" to t.archived,
        "pausedUntil" to t.pausedUntil?.toString(),
    )
}

fun completionFromDoc(id: String, d: Map<String, Any?>): Completion? {
    val date = d["date"].asDate() ?: return null
    return Completion(
        id = id,
        taskId = d["taskId"] as? String ?: return null,
        date = date,
        status = d["status"].asEnum(CompletionStatus.DONE),
        subtasksDone = d["subtasksDone"].asList().mapNotNull { it as? String },
        completedAt = d["completedAt"].asLong() ?: 0,
        deviceId = d["deviceId"] as? String,
    )
}

fun completionToDoc(c: Completion): Map<String, Any?> = mapOf(
    "id" to c.id,
    "taskId" to c.taskId,
    "date" to c.date.toString(),
    "status" to c.status.name,
    "subtasksDone" to c.subtasksDone,
    "completedAt" to c.completedAt,
    "deviceId" to c.deviceId,
)

fun settingsFromDoc(d: Map<String, Any?>?): Settings = Settings(
    firstDayOfWeek = d?.get("firstDayOfWeek").asInt() ?: 1,
    dayEndsAt = d?.get("dayEndsAt").asInt() ?: 0,
    fplDeadlines = d?.get("fplDeadlines") as? Boolean ?: true,
    matchCoverage = d?.get("matchCoverage") as? Boolean ?: true,
)

fun settingsToDoc(s: Settings): Map<String, Any?> = mapOf(
    "firstDayOfWeek" to s.firstDayOfWeek,
    "dayEndsAt" to s.dayEndsAt,
    "fplDeadlines" to s.fplDeadlines,
    "matchCoverage" to s.matchCoverage,
)
