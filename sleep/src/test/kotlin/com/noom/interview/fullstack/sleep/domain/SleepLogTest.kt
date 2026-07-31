package com.noom.interview.fullstack.sleep.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class SleepLogTest {

    @Test
    fun `derives time in bed from the interval`() {
        val sleepLog = sleepLog(
            bedStart = Instant.parse("2026-11-14T01:53:00Z"),
            bedEnd = Instant.parse("2026-11-14T10:05:00Z")
        )

        assertThat(sleepLog.timeInBed).isEqualTo(Duration.ofHours(8).plusMinutes(12))
    }

    @Test
    fun `spans midnight without special handling`() {
        // Both ends are full instants, so the duration is a plain subtraction
        // even though the sleep crosses a calendar day.
        val sleepLog = sleepLog(
            bedStart = Instant.parse("2026-11-13T23:40:00Z"),
            bedEnd = Instant.parse("2026-11-14T06:10:00Z")
        )

        assertThat(sleepLog.timeInBed).isEqualTo(Duration.ofHours(6).plusMinutes(30))
    }

    private fun sleepLog(bedStart: Instant, bedEnd: Instant) = SleepLog(
        id = 1L,
        userId = 1L,
        bedStart = bedStart,
        bedEnd = bedEnd,
        morningFeeling = MorningFeeling.GOOD
    )
}
