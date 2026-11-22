package app.dailytasks.core

import org.json.JSONObject
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.assertEquals

/** Runs shared/calendar-cases.json, the same file the web tests use. */
class CalendarItemsTest {
    private val root = JSONObject(File(System.getProperty("calendarCases")).readText())

    @Test
    fun days() {
        val today = LocalDate.parse(root.getString("today"))
        val weekStart = root.getInt("weekStart")
        val ta = root.getJSONArray("tasks")
        val tasks = (0 until ta.length()).map { i ->
            val o = ta.getJSONObject(i)
            val r = o.getJSONObject("repeat")
            Task(
                id = o.getString("id"),
                name = o.getString("id"),
                sortOrder = i.toLong(),
                repeat = Repeat(
                    type = RepeatType.valueOf(r.getString("type")),
                    dueDate = if (r.has("dueDate")) LocalDate.parse(r.getString("dueDate")) else null,
                    interval = if (r.has("interval")) r.getInt("interval") else null,
                    daysOfWeek = r.optJSONArray("daysOfWeek")?.let { a -> (0 until a.length()).map { a.getInt(it) } },
                ),
            )
        }
        val ca = root.getJSONArray("completions")
        val completions = (0 until ca.length()).map { i ->
            val o = ca.getJSONObject(i)
            val d = LocalDate.parse(o.getString("date"))
            Completion(completionId(o.getString("taskId"), d), o.getString("taskId"), d, CompletionStatus.valueOf(o.getString("status")))
        }
        val days = root.getJSONArray("days")
        for (i in 0 until days.length()) {
            val o = days.getJSONObject(i)
            val date = LocalDate.parse(o.getString("date"))
            val ex = o.getJSONArray("expect").let { a -> (0 until a.length()).map { a.getString(it) } }.sorted()
            val got = CalendarItems.forDay(tasks, completions, date, today, weekStart).map { it.task.id }.sorted()
            assertEquals(ex, got, "day $date")
        }
    }

    @Test
    fun monthGrid() {
        val g = CalendarItems.monthGrid(YearMonth.of(2026, 11), 1)
        assertEquals(listOf(null, null, null, null, null, null, LocalDate.of(2026, 11, 1)), g[0])
        assertEquals(6, g.size)
        assertEquals(LocalDate.of(2026, 11, 30), g[5][0])
    }
}
