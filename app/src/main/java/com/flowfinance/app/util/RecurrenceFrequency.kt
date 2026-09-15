package com.flowfinance.app.util

import java.time.LocalDate

enum class RecurrenceFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

// Always anchored to the rule's original start date to avoid day-of-month drift
// (e.g. Jan 31 -> Feb 28 -> Mar 28 instead of Mar 31 if chained month-by-month).
fun RecurrenceFrequency.occurrenceDate(anchor: LocalDate, occurrenceIndex: Long): LocalDate {
    return when (this) {
        RecurrenceFrequency.DAILY -> anchor.plusDays(occurrenceIndex)
        RecurrenceFrequency.WEEKLY -> anchor.plusWeeks(occurrenceIndex)
        RecurrenceFrequency.MONTHLY -> anchor.plusMonths(occurrenceIndex)
        RecurrenceFrequency.YEARLY -> anchor.plusYears(occurrenceIndex)
    }
}
