package it.manu.fuoriorario.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.minus

/** The Diario di tiro filter (PRD F3): every period ends today, in the team's time zone. */
enum class Period {
    DAYS_7,
    DAYS_30,

    /** 1 September of year N to 31 August of N+1. */
    SEASON;

    /** First day included: today counts as one of the 7 or 30 days. */
    fun start(today: LocalDate): LocalDate = when (this) {
        DAYS_7 -> today.minus(DatePeriod(days = 6))
        DAYS_30 -> today.minus(DatePeriod(days = 29))
        SEASON -> LocalDate(if (today.month >= Month.SEPTEMBER) today.year else today.year - 1, Month.SEPTEMBER, 1)
    }

    fun filter(sessions: List<ShotSession>, today: LocalDate): List<ShotSession> =
        (start(today)..today).let { range -> sessions.filter { it.date in range } }
}

/** Statistiche of a period: [attempted] counts free throws too. */
data class Stats(val fieldGoal: Shots, val three: Shots, val freeThrows: Shots) {
    val attempted get() = fieldGoal.attempted + freeThrows.attempted
}

fun stats(sessions: List<ShotSession>) = Stats(
    sessions.fold(Shots()) { sum, s -> sum + s.fieldGoal },
    sessions.fold(Shots()) { sum, s -> sum + s.three },
    sessions.fold(Shots()) { sum, s -> sum + s.freeThrows }
)
