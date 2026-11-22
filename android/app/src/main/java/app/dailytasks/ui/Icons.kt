package app.dailytasks.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

// The same stroke icons as web/src/ui/icons.tsx, drawn from identical path
// strings so both apps look alike.

private val PATHS = mapOf(
    "water" to "M12 3s6 6.5 6 11a6 6 0 0 1-12 0c0-4.5 6-11 6-11z",
    "book" to "M4 5a2 2 0 0 1 2-2h13v16H6a2 2 0 0 0-2 2zM4 21V5",
    "leaf" to "M5 19c0-8 5-14 15-14 0 10-6 15-14 15M5 19 13 11",
    "bill" to "M6 3h12v18l-3-2-3 2-3-2-3 2zM9 8h6M9 12h6",
    "clean" to "M12 3v4M12 17v4M3 12h4M17 12h4M6 6l2.5 2.5M15.5 15.5 18 18M6 18l2.5-2.5M15.5 8.5 18 6",
    "pen" to "M4 20h4L19 9l-4-4L4 16z",
    "pill" to "M10.5 20.5a5 5 0 0 1-7-7l6-6a5 5 0 0 1 7 7zM8.5 8.5l7 7",
    "walk" to "M13 4a1.5 1.5 0 1 0 0 .01M10 21l2-6 3 3v3M9 12l2-4 3 2 3 1M11 8l-3 2-1 3",
    "dumbbell" to "M6 7v10M18 7v10M3 10v4M21 10v4M6 12h12",
    "bed" to "M3 18V8M3 14h18v4M21 14v-2a3 3 0 0 0-3-3h-7v5M7 11.5a1.5 1.5 0 1 0 0 .01",
    "heart" to "M12 20s-7-4.5-7-10a4 4 0 0 1 7-2.5A4 4 0 0 1 19 10c0 5.5-7 10-7 10z",
    "sun" to "M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8zM12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4",
    "moon" to "M20 14.5A8 8 0 0 1 9.5 4 8 8 0 1 0 20 14.5z",
    "cart" to "M3 4h2l2.5 11h10L20 7H6.5M9 19.5a1 1 0 1 0 0 .01M17 19.5a1 1 0 1 0 0 .01",
    "paw" to "M12 13c-3 0-5 3-5 5s2 2 5 2 5 0 5-2-2-5-5-5zM6.5 10a1.5 2 0 1 0 0 .01M17.5 10a1.5 2 0 1 0 0 .01M9.5 6a1.5 2 0 1 0 0 .01M14.5 6a1.5 2 0 1 0 0 .01",
    "phone" to "M8 3h8a1 1 0 0 1 1 1v16a1 1 0 0 1-1 1H8a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1zM11 18h2",
    "star" to "M12 3l2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1-4.4-4.3 6.1-.9z",
    "check" to "m5 12 5 5 9-10",
    "plus" to "M12 5v14M5 12h14",
    "close" to "M6 6l12 12M18 6 6 18",
    "back" to "M19 12H5M11 6l-6 6 6 6",
    "chevronDown" to "m6 9 6 6 6-6",
    "chevronUp" to "m6 15 6-6 6 6",
    "chevronLeft" to "m15 6-6 6 6 6",
    "chevronRight" to "m9 6 6 6-6 6",
    "settings" to "M12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6zM19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z",
    "cloud" to "M17.5 19H7a5 5 0 1 1 1.4-9.8A6 6 0 0 1 20 11a4 4 0 0 1-2.5 8zM9.5 14l2 2 3.5-3.5",
    "cloudSync" to "M17.5 19H7a5 5 0 1 1 1.4-9.8A6 6 0 0 1 20 11a4 4 0 0 1-2.5 8zM12 11v5M9.5 13.5 12 16l2.5-2.5",
    "cloudOff" to "M3 3l18 18M17.5 19H7a5 5 0 0 1-1.6-9.7M10 6.2A6 6 0 0 1 20 11a4 4 0 0 1 .3 6.6",
    "today" to "M3 5h18v16H3zM3 10h18M8 3v4M16 3v4M9 15l2 2 4-4",
    "calendar" to "M3 5h18v16H3zM3 10h18M8 3v4M16 3v4M7.5 14h.01M12 14h.01M16.5 14h.01M7.5 17.5h.01M12 17.5h.01",
    "list" to "M9 6h11M9 12h11M9 18h11M4.5 6h.01M4.5 12h.01M4.5 18h.01",
    "chart" to "M5 20V11M12 20V5M19 20v-6",
    "flame" to "M12 3c1 4 5 5.5 5 10a5 5 0 0 1-10 0c0-2 1-3.5 2-4.5.3 1.6 1.2 2.5 2 2.5-1-3 0-6 1-8z",
    "subtasks" to "M9 6h11M9 12h11M9 18h11M3 6l1.5 1.5L7 5",
    "trash" to "M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13",
    "archive" to "M3 4h18v4H3zM5 8v12h14V8M10 12h4",
    "pause" to "M9 5v14M15 5v14",
    "play" to "M7 4l12 8-12 8z",
    "skip" to "M5 5l8 7-8 7zM17 5v14",
    "edit" to "M4 20h4L19 9l-4-4L4 16z",
    "bell" to "M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9M10.3 21a1.9 1.9 0 0 0 3.4 0",
    "logout" to "M15 4h4v16h-4M10 8l-4 4 4 4M6 12h11",
)

val TASK_ICONS = listOf("check", "water", "book", "leaf", "bill", "clean", "pen", "pill", "walk", "dumbbell", "bed", "heart", "sun", "moon", "cart", "paw", "phone", "star")

private val cache = HashMap<Pair<String, Float>, ImageVector>()

/** Tinted at the call site with Icon(tint = ...). */
fun appIcon(name: String, strokeWidth: Float = 1.9f): ImageVector = cache.getOrPut(name to strokeWidth) {
    val d = PATHS[name] ?: PATHS.getValue("check")
    ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .addPath(
            pathData = PathParser().parsePathString(d).toNodes(),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        .build()
}
