package app.dailytasks.core

import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// Plain-English labels. Same wording as web/src/core/describe.ts.
object Describe {
    private val locale = Locale.UK

    fun weekdayLong(iso: Int): String = java.time.DayOfWeek.of(iso).getDisplayName(TextStyle.FULL, locale)
    fun weekdayShort(iso: Int): String = java.time.DayOfWeek.of(iso).getDisplayName(TextStyle.SHORT, locale)
    fun monthLong(m: Int): String = java.time.Month.of(m).getDisplayName(TextStyle.FULL, locale)

    /** "Friday, 20 November" */
    fun longLabel(d: LocalDate) = "${weekdayLong(d.dayOfWeek.value)}, ${d.dayOfMonth} ${monthLong(d.monthValue)}"

    /** "Thu" if within the last week, else "12 Nov". */
    fun shortLabel(d: LocalDate, today: LocalDate): String {
        val diff = RepeatEngine.daysBetween(d, today)
        if (diff in 1..6) return weekdayShort(d.dayOfWeek.value)
        return "${d.dayOfMonth} ${monthLong(d.monthValue).take(3)}"
    }

    fun ordinalNum(n: Int): String {
        val s = if (n % 100 in 11..13) "th" else when (n % 10) { 1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th" }
        return "$n$s"
    }

    fun repeat(r: Repeat, short: Boolean = false): String = when (r.type) {
        RepeatType.SOMEDAY -> "No date yet"
        RepeatType.NONE -> if (short) "One-off" else r.dueDate?.let { "Once, on ${longLabel(it)}" } ?: "Once"
        RepeatType.DAILY -> if ((r.interval ?: 1) <= 1) "Every day" else "Every ${r.interval} days"
        RepeatType.WEEKLY -> {
            val d = (r.daysOfWeek ?: emptyList()).sorted()
            when {
                d == listOf(1, 2, 3, 4, 5) -> "Weekdays"
                d == listOf(6, 7) -> "Weekends"
                d.size == 7 -> "Every day"
                d.size == 1 -> if (short) "${weekdayLong(d[0])}s" else "Every ${weekdayLong(d[0])}"
                else -> "Every " + joinAnd(d.map { weekdayShort(it) })
            }
        }
        RepeatType.MONTHLY -> {
            val ord = r.ordinal
            val wd = r.weekday
            if (ord != null && ord != 0 && wd != null && wd != 0) {
                val o = if (ord == -1) "last" else listOf("", "1st", "2nd", "3rd", "4th")[ord]
                "Monthly, $o ${weekdayLong(wd)}"
            } else {
                "Monthly on the ${ordinalNum(r.dayOfMonth ?: 1)}"
            }
        }
        RepeatType.YEARLY -> "Every year on ${r.dayOfMonth ?: 1} ${monthLong(r.month ?: 1)}"
        RepeatType.QUOTA -> {
            val c = r.count ?: 1
            "$c ${if (c == 1) "time" else "times"} a ${if (r.period == Period.MONTH) "month" else "week"}"
        }
        RepeatType.AFTER_DONE -> {
            val n = r.interval ?: 1
            "$n ${if (n == 1) "day" else "days"} after last done"
        }
    }

    fun time(hhmm: String): String {
        val (h, m) = hhmm.split(":").map { it.toInt() }
        val ap = if (h >= 12) "PM" else "AM"
        val h12 = if (h % 12 == 0) 12 else h % 12
        return "$h12:${m.toString().padStart(2, '0')} $ap"
    }

    fun late(movedFrom: LocalDate?, overdueDays: Int, today: LocalDate): String? = when {
        movedFrom != null -> if (overdueDays >= 3) "$overdueDays days late" else "Moved from ${shortLabel(movedFrom, today)}"
        overdueDays > 0 -> "$overdueDays ${if (overdueDays == 1) "day" else "days"} overdue"
        else -> null
    }

    private fun joinAnd(xs: List<String>): String =
        if (xs.size <= 1) xs.joinToString("") else xs.dropLast(1).joinToString(", ") + " and " + xs.last()
}
