package com.noom.interview.fullstack.sleep.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalTime

class SleepAveragesTest {

    @Test
    fun `averages time in bed across the window`() {
        val averages = averagesOf(
            sleep("22:00", "06:00", MorningFeeling.GOOD), // 8h
            sleep("23:00", "06:00", MorningFeeling.GOOD)  // 7h
        )

        assertThat(averages.averageTimeInBed).isEqualTo(Duration.ofHours(7).plusMinutes(30))
    }

    @Test
    fun `averages bedtime and wake time as times of day`() {
        val averages = averagesOf(
            sleep("22:00", "06:00", MorningFeeling.OK),
            sleep("23:00", "07:00", MorningFeeling.OK)
        )

        assertThat(averages.averageBedStart).isEqualTo(LocalTime.of(22, 30))
        assertThat(averages.averageBedEnd).isEqualTo(LocalTime.of(6, 30))
    }

    @Test
    fun `counts every feeling, including those that never occurred`() {
        val averages = averagesOf(
            sleep("22:00", "06:00", MorningFeeling.GOOD),
            sleep("22:00", "06:00", MorningFeeling.GOOD),
            sleep("22:00", "06:00", MorningFeeling.BAD)
        )

        assertThat(averages.feelingFrequencies).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                MorningFeeling.GOOD to 2,
                MorningFeeling.BAD to 1,
                MorningFeeling.OK to 0
            )
        )
    }

    @Test
    fun `reports the window even when it holds no sleeps`() {
        val averages = averagesOf()

        assertThat(averages.from).isEqualTo(FROM)
        assertThat(averages.to).isEqualTo(TO)
        assertThat(averages.sleepCount).isZero
        assertThat(averages.averageTimeInBed).isNull()
        assertThat(averages.averageBedStart).isNull()
        assertThat(averages.feelingFrequencies.values).allMatch { it == 0 }
    }

    private fun averagesOf(vararg sleepLogs: SleepLog) =
        SleepAverages.of(FROM, TO, sleepLogs.toList())

    private fun sleep(bedStart: String, bedEnd: String, morningFeeling: MorningFeeling) = SleepLog(
        id = 1L,
        userId = 1L,
        bedStart = Instant.parse("2026-11-13T$bedStart:00Z"),
        bedEnd = Instant.parse("2026-11-14T$bedEnd:00Z"),
        morningFeeling = morningFeeling
    )

    private companion object {
        val FROM: Instant = Instant.parse("2026-10-15T00:00:00Z")
        val TO: Instant = Instant.parse("2026-11-14T00:00:00Z")
    }
}
