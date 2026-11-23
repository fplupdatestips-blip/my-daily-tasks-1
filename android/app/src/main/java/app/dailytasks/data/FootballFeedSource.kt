package app.dailytasks.data

import android.content.Context
import app.dailytasks.core.FeedDeadline
import app.dailytasks.core.FeedMatch
import app.dailytasks.core.FeedTeam
import app.dailytasks.core.FootballFeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

/**
 * football.json (FPL deadlines and every Premier League team's matches) is
 * built by the Build workflow and published with the web app. The phone
 * downloads it and keeps a copy for offline starts.
 */
class FootballFeedSource(context: Context) {
    private val file = File(context.filesDir, "football.json")

    fun cached(): FootballFeed? = runCatching { parse(file.readText()) }.getOrNull()

    /** Downloads a fresh copy; null when offline or the file is unusable. */
    suspend fun download(): FootballFeed? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL(FEED_URL).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 20_000
            conn.useCaches = false
            try {
                if (conn.responseCode != 200) return@runCatching null
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val feed = parse(text)
                file.writeText(text)
                feed
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    companion object {
        const val FEED_URL = "https://daily-tasks-19f9e.web.app/football.json"

        private fun team(o: JSONObject?) = FeedTeam(
            o?.optString("id").orEmpty(), o?.optString("name", "?") ?: "?", o?.optString("short", "?") ?: "?", o?.optString("abbr").orEmpty(),
        )

        fun parse(text: String): FootballFeed {
            val f = JSONObject(text)
            val d = f.optJSONArray("deadlines")
            val m = f.optJSONArray("matches")
            val deadlines = (0 until (d?.length() ?: 0)).mapNotNull { i ->
                val o = d!!.getJSONObject(i)
                runCatching { FeedDeadline(o.getInt("gw"), o.optString("name"), Instant.parse(o.getString("deadline"))) }.getOrNull()
            }
            val matches = (0 until (m?.length() ?: 0)).mapNotNull { i ->
                val o = m!!.getJSONObject(i)
                runCatching {
                    FeedMatch(
                        id = o.getString("id"),
                        kickoff = Instant.parse(o.getString("kickoff")),
                        timeValid = o.optBoolean("timeValid", true),
                        home = team(o.optJSONObject("home")),
                        away = team(o.optJSONObject("away")),
                        competition = o.optString("competition"),
                        venue = o.optString("venue"),
                    )
                }.getOrNull()
            }
            return FootballFeed(f.optString("updatedAt"), deadlines, matches)
        }
    }
}
