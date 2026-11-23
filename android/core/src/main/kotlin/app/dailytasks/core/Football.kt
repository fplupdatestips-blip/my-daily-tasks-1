package app.dailytasks.core

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Turns the football feed (football.json, built by scripts/football-feed.mjs)
// into read-only tasks: one per FPL gameweek deadline and one "Match
// Coverage" per match an English Premier League team plays. They are made
// fresh on every load and never saved, so a re-sync can't duplicate them;
// ticks are normal completions keyed by the stable ids below.
// Web twin: web/src/core/football.ts (tested with shared/football-cases.json).

data class FeedTeam(val id: String, val name: String, val short: String, val abbr: String)

data class FeedMatch(
    val id: String,
    val kickoff: Instant,
    /** false when only the date is fixed, not the kick-off time. */
    val timeValid: Boolean,
    val home: FeedTeam,
    val away: FeedTeam,
    val competition: String,
    val venue: String,
)

data class FeedDeadline(val gw: Int, val name: String, val deadline: Instant)

data class FootballFeed(val updatedAt: String, val deadlines: List<FeedDeadline>, val matches: List<FeedMatch>)

object Football {
    fun deadlineTaskId(gw: Int) = "fpl-gw-$gw"
    /** One Match Coverage task per local day. */
    fun matchDayTaskId(date: java.time.LocalDate) = "match-day-$date"

    private val HHMM = DateTimeFormatter.ofPattern("HH:mm")

    fun timeOfDayFor(time: String?): TimeOfDay {
        if (time == null) return TimeOfDay.ANYTIME
        val h = time.take(2).toInt()
        return if (h < 12) TimeOfDay.MORNING else if (h < 17) TimeOfDay.AFTERNOON else TimeOfDay.EVENING
    }

    fun tasks(feed: FootballFeed?, settings: Settings, zone: ZoneId = ZoneId.systemDefault()): List<Task> {
        if (feed == null) return emptyList()
        val out = mutableListOf<Task>()
        if (settings.fplDeadlines) {
            for (d in feed.deadlines) {
                val local = d.deadline.atZone(zone)
                val time = local.format(HHMM)
                out += Task(
                    id = deadlineTaskId(d.gw),
                    name = "FPL GW${d.gw} deadline",
                    shortName = "GW${d.gw} deadline",
                    description = "${d.name} deadline at $time. Transfers, captain and chips locked in by then.",
                    icon = "deadline",
                    color = "purple",
                    time = time,
                    timeOfDay = timeOfDayFor(time),
                    repeat = Repeat(type = RepeatType.NONE, dueDate = local.toLocalDate(), fixed = true),
                    auto = "FPL_DEADLINE",
                )
            }
        }
        if (settings.matchCoverage) {
            // One task per day, listing that day's matches in kick-off order.
            val byDay = linkedMapOf<java.time.LocalDate, MutableList<Pair<String?, String>>>()
            for (m in feed.matches.sortedBy { it.kickoff }) {
                val local = m.kickoff.atZone(zone)
                val time = if (m.timeValid) local.format(HHMM) else null
                byDay.getOrPut(local.toLocalDate()) { mutableListOf() } += time to "${time ?: "TBC"}  ${m.home.short} v ${m.away.short} · ${m.competition}"
            }
            for ((date, list) in byDay) {
                val first = list.firstNotNullOfOrNull { it.first }
                out += Task(
                    id = matchDayTaskId(date),
                    name = "Match Coverage",
                    shortName = "${list.size} ${if (list.size == 1) "match" else "matches"}",
                    description = "${list.size} ${if (list.size == 1) "match" else "matches"}:\n" + list.joinToString("\n") { it.second },
                    icon = "football",
                    color = "green",
                    time = first,
                    timeOfDay = timeOfDayFor(first),
                    repeat = Repeat(type = RepeatType.NONE, dueDate = date, fixed = true),
                    auto = "MATCH",
                )
            }
        }
        return out
    }
}
