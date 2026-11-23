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
    fun matchTaskId(matchId: String) = "match-$matchId"

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
            for (m in feed.matches) {
                val local = m.kickoff.atZone(zone)
                val time = if (m.timeValid) local.format(HHMM) else null
                val bits = listOf(m.competition, if (time != null) "kick-off $time" else "kick-off time to be confirmed", m.venue).filter { it.isNotEmpty() }
                out += Task(
                    id = matchTaskId(m.id),
                    name = "Match Coverage: ${m.home.short} v ${m.away.short}",
                    description = "${m.home.name} v ${m.away.name} · ${bits.joinToString(" · ")}",
                    icon = "football",
                    color = "green",
                    time = time,
                    timeOfDay = timeOfDayFor(time),
                    repeat = Repeat(type = RepeatType.NONE, dueDate = local.toLocalDate(), fixed = true),
                    auto = "MATCH",
                )
            }
        }
        return out
    }
}
