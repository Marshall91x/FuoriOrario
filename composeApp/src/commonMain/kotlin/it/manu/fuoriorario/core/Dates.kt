package it.manu.fuoriorario.core

import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/** The team's time zone: dates are local to it, whatever the device says (ARCHITECTURE "Stack"). */
// ponytail: on web there's no zone database, so the browser's zone stands in (same for users in Italy);
// add the `@js-joda/timezone` npm package if web users abroad log the wrong day.
val ROME = runCatching { TimeZone.of("Europe/Rome") }.getOrElse { TimeZone.currentSystemDefault() }

fun today(): LocalDate = Clock.System.todayIn(ROME)
