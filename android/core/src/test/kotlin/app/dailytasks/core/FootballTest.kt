package app.dailytasks.core

import org.json.JSONObject
import org.junit.Test
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals

/** Runs shared/football-cases.json, the same file the web tests use. */
class FootballTest {
    private val root = JSONObject(File(System.getProperty("footballCases")).readText())

    private fun team(o: JSONObject) = FeedTeam(o.getString("id"), o.getString("name"), o.getString("short"), o.getString("abbr"))

    private val feed: FootballFeed = root.getJSONObject("feed").let { f ->
        val d = f.getJSONArray("deadlines")
        val m = f.getJSONArray("matches")
        FootballFeed(
            f.getString("updatedAt"),
            (0 until d.length()).map { i -> d.getJSONObject(i).let { FeedDeadline(it.getInt("gw"), it.getString("name"), Instant.parse(it.getString("deadline"))) } },
            (0 until m.length()).map { i ->
                m.getJSONObject(i).let {
                    FeedMatch(it.getString("id"), Instant.parse(it.getString("kickoff")), it.getBoolean("timeValid"), team(it.getJSONObject("home")), team(it.getJSONObject("away")), it.getString("competition"), it.getString("venue"))
                }
            },
        )
    }

    @Test
    fun zones() {
        val zones = root.getJSONArray("zones")
        for (z in 0 until zones.length()) {
            val zo = zones.getJSONObject(z)
            val ex = zo.getJSONArray("expect").let { a ->
                (0 until a.length()).map { i ->
                    val o = a.getJSONObject(i)
                    listOf(o.getString("id"), o.getString("name"), o.getString("date"), if (o.isNull("time")) null else o.getString("time"), o.getString("timeOfDay"), o.getString("icon"))
                }
            }
            val got = Football.tasks(feed, Settings(), ZoneId.of(zo.getString("zone"))).map {
                listOf(it.id, it.name, it.repeat.dueDate.toString(), it.time, it.timeOfDay.name, it.icon)
            }
            assertEquals(ex, got, zo.getString("zone"))
        }
    }

    @Test
    fun listsTheDaysMatches() {
        val c = root.getJSONObject("matchDayDescription")
        val t = Football.tasks(feed, Settings(), ZoneId.of(c.getString("zone"))).first { it.id == c.getString("id") }
        assertEquals(c.getString("description"), t.description)
    }

    @Test
    fun settingsTurnEachKindOff() {
        val utc = ZoneId.of("UTC")
        assert(Football.tasks(feed, Settings(fplDeadlines = false), utc).all { it.auto == "MATCH" })
        assert(Football.tasks(feed, Settings(matchCoverage = false), utc).all { it.auto == "FPL_DEADLINE" })
        assertEquals(emptyList(), Football.tasks(null, Settings(), utc))
    }

    @Test
    fun matchesAreNotMovedToTheNextDay() {
        val tasks = Football.tasks(feed, Settings(), ZoneId.of("Europe/London"))
        val today = LocalDate.parse("2026-11-25")
        assertEquals(listOf("match-day-2026-11-24"), TodayItems.forDay(tasks, emptyList(), LocalDate.parse("2026-11-24"), today, 1).map { it.task.id })
        assertEquals(emptyList(), TodayItems.forDay(tasks, emptyList(), today, today, 1).map { it.task.id })
    }
}
