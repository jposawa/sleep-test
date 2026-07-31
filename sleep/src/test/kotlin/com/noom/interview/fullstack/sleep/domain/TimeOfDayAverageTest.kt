package com.noom.interview.fullstack.sleep.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalTime

class TimeOfDayAverageTest {

    @Test
    fun `averages times that sit within the same day`() {
        val average = TimeOfDayAverage.of(listOf(at("22:00"), at("23:00")))

        assertThat(average).isEqualTo(LocalTime.of(22, 30))
    }

    @Test
    fun `averages across midnight instead of landing at noon`() {
        // The arithmetic mean of 23:50 and 00:10 is 12:00, which is the whole
        // reason this cannot be a plain average.
        val average = TimeOfDayAverage.of(listOf(at("23:50"), at("00:10")))

        assertThat(average).isEqualTo(LocalTime.MIDNIGHT)
    }

    @Test
    fun `is unaffected by the day each time falls on`() {
        val sameClockTimeDaysApart = listOf(
            Instant.parse("2026-11-01T23:00:00Z"),
            Instant.parse("2026-11-30T23:00:00Z")
        )

        assertThat(TimeOfDayAverage.of(sameClockTimeDaysApart)).isEqualTo(LocalTime.of(23, 0))
    }

    @Test
    fun `returns the time itself for a single entry`() {
        assertThat(TimeOfDayAverage.of(listOf(at("07:05")))).isEqualTo(LocalTime.of(7, 5))
    }

    @Test
    fun `has no average for an empty input`() {
        assertThat(TimeOfDayAverage.of(emptyList())).isNull()
    }

    @Test
    fun `has no average when the times cancel each other out`() {
        // Diametrically opposed on the clock: no direction is more central.
        assertThat(TimeOfDayAverage.of(listOf(at("00:00"), at("12:00")))).isNull()
    }

    @Test
    fun `shifting every time shifts the average by the same amount`() {
        // Rotation equivariance is what lets a client convert the UTC answer to
        // its own offset rather than the API taking a timezone.
        val times = listOf(at("22:40"), at("23:51"), at("00:20"))
        val shifted = times.map { it.plusSeconds(3 * 3600) }

        val average = TimeOfDayAverage.of(times)!!
        assertThat(TimeOfDayAverage.of(shifted)).isEqualTo(average.plusHours(3))
    }

    private fun at(time: String): Instant = Instant.parse("2026-11-14T$time:00Z")
}
