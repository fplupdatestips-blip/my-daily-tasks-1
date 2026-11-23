package app.dailytasks.core

import java.time.LocalDate

// Mirrors web/src/core/model.ts and the Firestore documents under
// users/{uid}/... Keep field names identical on both sides.
// Dates are ISO "yyyy-MM-dd" strings in storage and LocalDate in code.

/** SOMEDAY: no date yet, never due until it is given one. */
enum class RepeatType { NONE, SOMEDAY, DAILY, WEEKLY, MONTHLY, YEARLY, QUOTA, AFTER_DONE }

enum class Period { WEEK, MONTH }

data class Repeat(
    val type: RepeatType = RepeatType.DAILY,
    /** NONE: the day the one-off task is due. */
    val dueDate: LocalDate? = null,
    /** NONE only: shown on dueDate and never moved to the next day (matches, deadlines). */
    val fixed: Boolean = false,
    /** DAILY: every N days. AFTER_DONE: N days after the last completion. */
    val interval: Int? = null,
    /** WEEKLY: ISO weekdays, 1 = Monday ... 7 = Sunday. */
    val daysOfWeek: List<Int>? = null,
    val dayOfMonth: Int? = null,
    /** MONTHLY by weekday: 1-4, or -1 for the last one. */
    val ordinal: Int? = null,
    val weekday: Int? = null,
    val month: Int? = null,
    val count: Int? = null,
    val period: Period? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
)

enum class TimeOfDay { MORNING, AFTERNOON, EVENING, ANYTIME }

data class Subtask(
    val id: String,
    val title: String,
    /** Only used on one-off tasks, whose progress carries over between days. */
    val done: Boolean = false,
)

data class Reminder(
    /** Local wall-clock time "HH:mm". */
    val localTime: String,
    val enabled: Boolean = true,
)

data class Task(
    val id: String,
    val name: String,
    val description: String = "",
    val icon: String = "check",
    /** Optional picture the person uploaded: a small (128px) data: URL. Shown instead of the icon. */
    val image: String? = null,
    val color: String = "teal",
    val timeOfDay: TimeOfDay = TimeOfDay.ANYTIME,
    /** Optional exact time "HH:mm". */
    val time: String? = null,
    val repeat: Repeat = Repeat(),
    val subtasks: List<Subtask> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
    val sortOrder: Long = 0,
    val createdAt: Long = 0,
    val archived: Boolean = false,
    val pausedUntil: LocalDate? = null,
    /** Made by the app from the football feed, never saved: "FPL_DEADLINE" or "MATCH". */
    val auto: String? = null,
    /** Auto tasks: a shorter label for the small calendar chips. */
    val shortName: String? = null,
)

enum class CompletionStatus { DONE, SKIPPED, PARTIAL }

data class Completion(
    /** "${taskId}_${date}" so the same tick on two devices is one record. */
    val id: String,
    val taskId: String,
    val date: LocalDate,
    val status: CompletionStatus,
    /** Subtask ids ticked that day (repeating tasks reset every day). */
    val subtasksDone: List<String> = emptyList(),
    val completedAt: Long = 0,
    val deviceId: String? = null,
)

data class Settings(
    /** ISO weekday the week starts on. */
    val firstDayOfWeek: Int = 1,
    /** 0-4: hour after midnight when "today" ends. */
    val dayEndsAt: Int = 0,
    /** Show FPL gameweek deadlines in Today and the calendar. */
    val fplDeadlines: Boolean = true,
    /** Show "Match Coverage" for every Premier League team's match. */
    val matchCoverage: Boolean = true,
)

fun completionId(taskId: String, date: LocalDate) = "${taskId}_$date"

/** Used as pausedUntil for "paused until I resume it". */
val PAUSED_FOREVER: LocalDate = LocalDate.of(9999, 12, 31)
