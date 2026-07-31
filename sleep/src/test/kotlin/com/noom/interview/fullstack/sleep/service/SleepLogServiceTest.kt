package com.noom.interview.fullstack.sleep.service

import com.noom.interview.fullstack.sleep.domain.MorningFeeling
import com.noom.interview.fullstack.sleep.domain.SleepLog
import com.noom.interview.fullstack.sleep.repository.SleepLogRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import java.time.Instant

class SleepLogServiceTest {

    private val repository = mock(SleepLogRepository::class.java)
    private val service = SleepLogService(repository)

    @Test
    fun `stores a sleep whose interval is ordered`() {
        val stored = sleepLog(BED_START, BED_END)
        given(repository.create(USER_ID, BED_START, BED_END, MorningFeeling.GOOD)).willReturn(stored)

        val created = service.create(USER_ID, BED_START, BED_END, MorningFeeling.GOOD)

        assertThat(created).isEqualTo(stored)
    }

    @Test
    fun `rejects a sleep that ends before it starts`() {
        assertThatThrownBy { service.create(USER_ID, BED_END, BED_START, MorningFeeling.OK) }
            .isInstanceOf(InvalidSleepIntervalException::class.java)
            .hasMessage("bedEnd must be after bedStart")
    }

    @Test
    fun `rejects a sleep of zero length`() {
        assertThatThrownBy { service.create(USER_ID, BED_START, BED_START, MorningFeeling.OK) }
            .isInstanceOf(InvalidSleepIntervalException::class.java)
    }

    @Test
    fun `does not reach the repository when the interval is invalid`() {
        runCatching { service.create(USER_ID, BED_END, BED_START, MorningFeeling.OK) }

        verifyNoInteractions(repository)
    }

    @Test
    fun `reports no last night when the user has never logged a sleep`() {
        given(repository.findLatestByUserId(USER_ID)).willReturn(null)

        assertThat(service.findLastNight(USER_ID)).isNull()
    }

    @Test
    fun `returns the latest sleep as last night`() {
        val latest = sleepLog(BED_START, BED_END)
        given(repository.findLatestByUserId(USER_ID)).willReturn(latest)

        assertThat(service.findLastNight(USER_ID)).isEqualTo(latest)
    }

    private fun sleepLog(bedStart: Instant, bedEnd: Instant) = SleepLog(
        id = 1L,
        userId = USER_ID,
        bedStart = bedStart,
        bedEnd = bedEnd,
        morningFeeling = MorningFeeling.GOOD
    )

    private companion object {
        const val USER_ID = 1L
        val BED_START: Instant = Instant.parse("2026-11-14T01:53:00Z")
        val BED_END: Instant = Instant.parse("2026-11-14T10:05:00Z")
    }
}
