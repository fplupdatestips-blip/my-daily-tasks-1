package app.dailytasks.core

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

// What the calendar shows on each day. Same logic as web/src/core/calendar.ts,
// both tested against shared/calendar-cases.json.
//
// Built on TodayItems.forDay, with these changes so the month reads well:
// - A late task (a one-off not done yet, or an "N days after done" task)
//   is shown on its due date, on the day it was finished, and on today.
//   It is not repeated on every day in between, or on every future day.
// - Before today, "N days after done" tasks show only on days they were
//   ticked or skipped.
// - "N days after done" tasks are projected forward: after today they show
//   every N days from the next time they are due.

object CalendarItems {
    fun forDay(
        tasks: List<Task>,
        completions: List<Completion>,
        date: LocalDate,
        today: LocalDate,
        weekStart: Int,
    ): List<TodayItem> {
        val items = TodayItems.forDay(tasks, completions, date, today, weekStart)
        val markedThatDay = completions.filter { it.date == date }.map { it.taskId }.toSet()
        val out = items.filter { i ->
            when {
                date == today || i.task.id in markedThatDay -> true
                date < today && i.task.repeat.type == RepeatType.AFTER_DONE -> false
                else -> i.overdueDays == 0
            }
        }.toMutableList()
        if (date <= today) return out

        for (task in tasks) {
            if (task.archived || task.repeat.type != RepeatType.AFTER_DONE) continue
            if (out.any { it.task.id == task.id }) continue
            if (task.pausedUntil != null && date <= task.pausedUntil) continue
            if (task.repeat.endDate != null && date > task.repeat.endDate) continue
            val n = maxOf(1, task.repeat.interval ?: 1).toLong()
            val todayItem = TodayItems.forDay(listOf(task), completions, today, today, weekStart).firstOrNull()
            var next = today.plusDays(n)
            if (todayItem == null || todayItem.done) {
                var d = today.plusDays(1)
                while (d <= today.plusDays(n)) {
                    if (TodayItems.forDay(listOf(task), completions, d, today, weekStart).isNotEmpty()) {
                        next = d
                        break
                    }
                    d = d.plusDays(1)
                }
            }
            val diff = ChronoUnit.DAYS.between(next, date)
            if (diff >= 0 && diff % n == 0L) {
                out.add(TodayItem(task, done = false, skipped = false, late = null, overdueDays = 0, streak = 0, subtasksDone = emptyList()))
            }
        }
        return out
    }

    /** The weeks of a month as rows of 7 dates (null = outside the month). */
    fun monthGrid(month: YearMonth, weekStart: Int): List<List<LocalDate?>> {
        val first = month.atDay(1)
        val lead = (first.dayOfWeek.value - weekStart + 7) % 7
        val cells = MutableList<LocalDate?>(lead) { null }
        for (d in 1..month.lengthOfMonth()) cells.add(month.atDay(d))
        while (cells.size % 7 != 0) cells.add(null)
        return cells.chunked(7)
    }
}
