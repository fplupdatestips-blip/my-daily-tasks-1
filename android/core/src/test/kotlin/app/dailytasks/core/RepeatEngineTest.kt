package app.dailytasks.core

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import java.io.File
import java.time.LocalDate
import kotlin.test.assertEquals

/** Runs every case in shared/repeat-cases.json, the same file the web tests use. */
class RepeatEngineTest {
    private val root = JSONObject(File(System.getProperty("repeatCases")).readText())
    private val weekStart = root.getInt("weekStart")

    private fun date(s: String?) = s?.let { LocalDate.parse(it) }

    private fun JSONObject.optStr(k: String): String? = if (has(k) && !isNull(k)) getString(k) else null
    private fun JSONObject.optIntOrNull(k: String): Int? = if (has(k) && !isNull(k)) getInt(k) else null

    private fun repeat(o: JSONObject) = Repeat(
        type = RepeatType.valueOf(o.getString("type")),
        dueDate = date(o.optStr("dueDate")),
        interval = o.optIntOrNull("interval"),
        daysOfWeek = o.optJSONArray("daysOfWeek")?.let { a -> (0 until a.length()).map { a.getInt(it) } },
        dayOfMonth = o.optIntOrNull("dayOfMonth"),
        ordinal = o.optIntOrNull("ordinal"),
        weekday = o.optIntOrNull("weekday"),
        month = o.optIntOrNull("month"),
        count = o.optIntOrNull("count"),
        period = o.optStr("period")?.let { Period.valueOf(it) },
        startDate = date(o.optStr("startDate")),
        endDate = date(o.optStr("endDate")),
    )

    private fun marks(a: JSONArray?): List<DayMark> =
        if (a == null) emptyList() else (0 until a.length()).map {
            val m = a.getJSONObject(it)
            DayMark(LocalDate.parse(m.getString("date")), CompletionStatus.valueOf(m.getString("status")))
        }

    @Test
    fun knowsFriday() = assertEquals(5, RepeatEngine.isoWeekday(LocalDate.parse("2026-11-20")))

    @Test
    fun sharedDueCases() {
        val cases = root.getJSONArray("dueCases")
        var failures = 0
        for (i in 0 until cases.length()) {
            val c = cases.getJSONObject(i)
            val exp = c.getJSONObject("expect")
            val r = RepeatEngine.dueInfo(
                repeat(c.getJSONObject("repeat")),
                LocalDate.parse(c.getString("date")),
                marks(c.optJSONArray("completions")),
                weekStart,
                archived = c.optBoolean("archived", false),
                pausedUntil = date(c.optStr("pausedUntil")),
            )
            val ok = r.due == exp.getBoolean("due") &&
                (!r.due || (r.movedFrom == date(exp.optStr("movedFrom")) && r.overdueDays == (exp.optIntOrNull("overdueDays") ?: 0)))
            if (!ok) {
                failures++
                println("FAIL: ${c.getString("name")} -> $r, expected $exp")
            }
        }
        assertEquals(0, failures, "shared due cases failed")
        println("${cases.length()} shared due cases passed")
    }

    @Test
    fun sharedStreakCases() {
        val cases = root.getJSONArray("streakCases")
        for (i in 0 until cases.length()) {
            val c = cases.getJSONObject(i)
            val exp = c.getJSONObject("expect")
            val s = RepeatEngine.streak(repeat(c.getJSONObject("repeat")), LocalDate.parse(c.getString("today")), marks(c.optJSONArray("completions")))
            assertEquals(Streak(exp.getInt("current"), exp.getInt("best")), s, c.getString("name"))
        }
    }
}
