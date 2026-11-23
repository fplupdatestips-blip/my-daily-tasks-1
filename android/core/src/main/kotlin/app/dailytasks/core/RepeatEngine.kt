package app.dailytasks.core

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit

// The repeat-rule engine. Its TypeScript twin is web/src/core/repeat.ts and
// both are tested against shared/repeat-cases.json. Change them together.

data class DayMark(val date: LocalDate, val status: CompletionStatus)

data class DueInfo(
    val due: Boolean,
    /** One-off tasks shown after their due date: the original due date. */
    val movedFrom: LocalDate? = null,
    /** Days late (one-off or AFTER_DONE). 0 when on time. */
    val overdueDays: Int = 0,
)

data class Streak(val current: Int, val best: Int)

private val NOT_DUE = DueInfo(false)
private val CALENDAR_TYPES = setOf(RepeatType.DAILY, RepeatType.WEEKLY, RepeatType.MONTHLY, RepeatType.YEARLY)

object RepeatEngine {

    fun daysBetween(from: LocalDate, to: LocalDate): Int = ChronoUnit.DAYS.between(from, to).toInt()

    fun isoWeekday(d: LocalDate): Int = d.dayOfWeek.value

    fun startOfWeek(d: LocalDate, weekStart: Int): LocalDate {
        val back = (isoWeekday(d) - weekStart + 7) % 7
        return d.minusDays(back.toLong())
    }

    /** The calendar date "today" counts as, given when the user's day ends. */
    fun logicalToday(now: LocalDateTime = LocalDateTime.now(), dayEndsAtHour: Int = 0): LocalDate =
        now.minusHours(dayEndsAtHour.toLong()).toLocalDate()

    fun isCalendarRule(r: Repeat) = r.type in CALENDAR_TYPES

    /** Is the rule scheduled on this date, ignoring completions? Calendar rules only. */
    fun isScheduled(r: Repeat, date: LocalDate): Boolean {
        if (r.startDate != null && date < r.startDate) return false
        if (r.endDate != null && date > r.endDate) return false
        return when (r.type) {
            RepeatType.DAILY -> {
                val n = maxOf(1, r.interval ?: 1)
                if (n == 1) true else Math.floorMod(daysBetween(r.startDate ?: date, date), n) == 0
            }
            RepeatType.WEEKLY -> (r.daysOfWeek ?: emptyList()).contains(isoWeekday(date))
            RepeatType.MONTHLY -> {
                val ord = r.ordinal
                val wd = r.weekday
                if (ord != null && ord != 0 && wd != null && wd != 0) {
                    isNthWeekday(date, ord, wd)
                } else {
                    val len = YearMonth.from(date).lengthOfMonth()
                    date.dayOfMonth == minOf(r.dayOfMonth ?: 1, len)
                }
            }
            RepeatType.YEARLY -> {
                if (date.monthValue != (r.month ?: 1)) return false
                var want = r.dayOfMonth ?: 1
                if (want == 29 && date.monthValue == 2 && !date.isLeapYear) want = 28
                date.dayOfMonth == minOf(want, YearMonth.from(date).lengthOfMonth())
            }
            else -> false
        }
    }

    private fun isNthWeekday(date: LocalDate, ordinal: Int, weekday: Int): Boolean {
        if (isoWeekday(date) != weekday) return false
        if (ordinal == -1) return date.dayOfMonth + 7 > YearMonth.from(date).lengthOfMonth()
        return (date.dayOfMonth - 1) / 7 + 1 == ordinal
    }

    /**
     * Should the task appear on this day's list? A task completed on `date`
     * still appears (as done). `marks` are this task's completions.
     */
    fun dueInfo(
        repeat: Repeat,
        date: LocalDate,
        marks: List<DayMark>,
        weekStart: Int = 1,
        archived: Boolean = false,
        pausedUntil: LocalDate? = null,
    ): DueInfo {
        if (archived) return NOT_DUE
        if (pausedUntil != null && date <= pausedUntil) return NOT_DUE
        val r = repeat
        // No date yet: kept in the Someday list until it is given one.
        if (r.type == RepeatType.SOMEDAY) return NOT_DUE

        if (r.type == RepeatType.NONE) {
            val dueDate = r.dueDate ?: date
            if (r.fixed) return if (date == dueDate) DueInfo(true) else NOT_DUE
            if (date < dueDate) return NOT_DUE
            // Finished (done or skipped) on an earlier day: gone.
            if (marks.any { it.date < date && it.date >= dueDate && it.status != CompletionStatus.PARTIAL }) return NOT_DUE
            val late = daysBetween(dueDate, date)
            return if (late > 0) DueInfo(true, dueDate, late) else DueInfo(true)
        }

        if (r.startDate != null && date < r.startDate) return NOT_DUE
        if (r.endDate != null && date > r.endDate) return NOT_DUE

        if (r.type == RepeatType.QUOTA) {
            val count = maxOf(1, r.count ?: 1)
            val from = if (r.period == Period.MONTH) date.withDayOfMonth(1) else startOfWeek(date, weekStart)
            val doneBefore = marks.count { it.status == CompletionStatus.DONE && it.date >= from && it.date < date }
            return if (doneBefore < count) DueInfo(true) else NOT_DUE
        }

        if (r.type == RepeatType.AFTER_DONE) {
            val n = maxOf(1, r.interval ?: 1)
            val last = marks.filter { it.status == CompletionStatus.DONE && it.date < date }.maxOfOrNull { it.date }
            val nextDue = last?.plusDays(n.toLong()) ?: (r.startDate ?: date)
            if (date < nextDue) return NOT_DUE
            return DueInfo(true, overdueDays = daysBetween(nextDue, date))
        }

        return if (isScheduled(r, date)) DueInfo(true) else NOT_DUE
    }

    /**
     * Streaks count scheduled days only. Skipped days are neutral, and today
     * not done yet does not break the streak. Only calendar rules have streaks.
     */
    fun streak(repeat: Repeat, today: LocalDate, marks: List<DayMark>): Streak {
        if (!isCalendarRule(repeat)) return Streak(0, 0)
        val byDate = marks.associate { it.date to it.status }
        var start = repeat.startDate ?: today
        for (m in marks) if (m.date < start) start = m.date
        if (daysBetween(start, today) > 3660) start = today.minusDays(3660)

        var run = 0
        var best = 0
        var d = start
        while (!d.isAfter(today)) {
            if (isScheduled(repeat, d)) {
                when (byDate[d]) {
                    CompletionStatus.DONE -> {
                        run += 1
                        best = maxOf(best, run)
                    }
                    CompletionStatus.SKIPPED -> Unit
                    else -> if (d != today) run = 0
                }
            }
            d = d.plusDays(1)
        }
        return Streak(run, best)
    }
}
