package app.dailytasks.ui.screens

import app.dailytasks.core.Period
import app.dailytasks.core.Repeat
import app.dailytasks.core.RepeatType
import app.dailytasks.core.Task
import app.dailytasks.core.TimeOfDay

// Starter templates, same list as web/src/screens/templates.ts.
object Templates {
    val list = listOf(
        Task(id = "", name = "Drink water", description = "8 glasses through the day", icon = "water", color = "blue", timeOfDay = TimeOfDay.ANYTIME, repeat = Repeat(RepeatType.DAILY, interval = 1)),
        Task(id = "", name = "Take medicine", description = "With breakfast", icon = "pill", color = "rose", timeOfDay = TimeOfDay.MORNING, repeat = Repeat(RepeatType.DAILY, interval = 1)),
        Task(id = "", name = "Go for a walk", description = "20 minutes outside", icon = "walk", color = "green", timeOfDay = TimeOfDay.AFTERNOON, repeat = Repeat(RepeatType.DAILY, interval = 1)),
        Task(id = "", name = "Read 10 pages", icon = "book", color = "purple", timeOfDay = TimeOfDay.EVENING, repeat = Repeat(RepeatType.DAILY, interval = 1)),
        Task(id = "", name = "Water plants", icon = "leaf", color = "green", timeOfDay = TimeOfDay.EVENING, repeat = Repeat(RepeatType.DAILY, interval = 3)),
        Task(id = "", name = "Exercise", icon = "dumbbell", color = "amber", timeOfDay = TimeOfDay.MORNING, repeat = Repeat(RepeatType.QUOTA, count = 3, period = Period.WEEK)),
    )
}
