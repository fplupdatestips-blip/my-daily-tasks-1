package app.dailytasks.core

import java.time.LocalDate

// Builds the list for a day. Same logic as web/src/core/today.ts.

data class TodayItem(
    val task: Task,
    val done: Boolean,
    val skipped: Boolean,
    val late: String?,
    val overdueDays: Int,
    val streak: Int,
    val subtasksDone: List<String>,
)

object TodayItems {
    fun marksByTask(completions: List<Completion>): Map<String, List<DayMark>> =
        completions.groupBy({ it.taskId }, { DayMark(it.date, it.status) })

    fun forDay(
        tasks: List<Task>,
        completions: List<Completion>,
        date: LocalDate,
        today: LocalDate,
        weekStart: Int,
    ): List<TodayItem> {
        val marks = marksByTask(completions)
        val byId = completions.associateBy { it.id }
        val items = tasks.mapNotNull { task ->
            val tm = marks[task.id] ?: emptyList()
            val info = RepeatEngine.dueInfo(task.repeat, date, tm, weekStart, task.archived, task.pausedUntil)
            if (!info.due) return@mapNotNull null
            val c = byId[completionId(task.id, date)]
            val oneOff = task.repeat.type == RepeatType.NONE
            TodayItem(
                task = task,
                done = c?.status == CompletionStatus.DONE,
                skipped = c?.status == CompletionStatus.SKIPPED,
                late = Describe.late(info.movedFrom, info.overdueDays, today),
                overdueDays = info.overdueDays,
                streak = RepeatEngine.streak(task.repeat, today, tm).current,
                subtasksDone = if (oneOff) task.subtasks.filter { it.done }.map { it.id } else c?.subtasksDone ?: emptyList(),
            )
        }
        // Late one-offs first in their group, then by exact time, then manual order.
        return items.sortedWith(
            compareBy<TodayItem>({ if (it.late != null) 0 else 1 }, { it.task.time ?: "99:99" }, { it.task.sortOrder }),
        )
    }
}
