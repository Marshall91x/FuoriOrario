package it.manu.fuoriorario.domain

import kotlinx.datetime.LocalDate

/** Colour of the plan pill in Quadro squadra. */
enum class PlanLevel { GOOD, MID, LOW }

/** GOOD from 75%, MID from 40%, LOW below or without a plan. */
fun planLevel(percent: Int?): PlanLevel = when {
    percent == null -> PlanLevel.LOW
    percent >= 75 -> PlanLevel.GOOD
    percent >= 40 -> PlanLevel.MID
    else -> PlanLevel.LOW
}

/** One player in Quadro squadra; percentages are null ("—") without a plan or attempts. */
data class OverviewRow(val plan: Int?, val attempted7: Int, val freeThrows30: Int?, val three30: Int?)

/** [week] is the current week's completamento, [sessions] the player's, any dates. */
fun overviewRow(week: Progress, sessions: List<ShotSession>, today: LocalDate): OverviewRow {
    val days30 = stats(Period.DAYS_30.filter(sessions, today))
    return OverviewRow(
        week.percent,
        stats(Period.DAYS_7.filter(sessions, today)).attempted,
        days30.freeThrows.percent,
        days30.three.percent
    )
}
