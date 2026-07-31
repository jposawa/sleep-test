package com.noom.interview.fullstack.sleep.domain

import java.time.Duration
import java.time.Instant

/**
 * A single sleep the user logged.
 *
 * Times are absolute instants. The interval is the only stored temporal fact:
 * total time in bed and the calendar date of the sleep are both derived from
 * it, so the two can never disagree.
 */
data class SleepLog(
    val id: Long,
    val userId: Long,
    val bedStart: Instant,
    val bedEnd: Instant,
    val morningFeeling: MorningFeeling
) {
    /** Total time in bed, derived from the interval. */
    val timeInBed: Duration
        get() = Duration.between(bedStart, bedEnd)
}
