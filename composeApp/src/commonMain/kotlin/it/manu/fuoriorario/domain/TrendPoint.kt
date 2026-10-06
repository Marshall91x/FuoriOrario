package it.manu.fuoriorario.domain

import kotlinx.datetime.LocalDate

/** One session in the Andamento: percentages dal campo and liberi, null when that series has no attempts. */
data class TrendPoint(val date: LocalDate, val fieldGoal: Int?, val freeThrows: Int?)

private const val TREND_MAX = 14

/** Prototype `trendSvg`: [sessions] newest first, points oldest first, the last [TREND_MAX] with any shot. */
fun trend(sessions: List<ShotSession>): List<TrendPoint> = sessions.asReversed()
    .map { TrendPoint(it.date, it.fieldGoal.percent, it.freeThrows.percent) }
    .filter { it.fieldGoal != null || it.freeThrows != null }
    .takeLast(TREND_MAX)
