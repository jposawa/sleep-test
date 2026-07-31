package com.noom.interview.fullstack.sleep.repository

import com.noom.interview.fullstack.sleep.domain.MorningFeeling
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import java.sql.ResultSet
import java.time.Instant
import java.time.OffsetDateTime

/** Pure mapping logic, exercised without a database. */
class SleepLogRowMapperTest {

    @Test
    fun `maps every column onto the domain`() {
        val sleepLog = SleepLogRowMapper.mapRow(rowWithFeeling("GOOD"), 0)

        assertThat(sleepLog.id).isEqualTo(7L)
        assertThat(sleepLog.userId).isEqualTo(3L)
        assertThat(sleepLog.morningFeeling).isEqualTo(MorningFeeling.GOOD)
    }

    @Test
    fun `converts the offset the row carries into an absolute instant`() {
        val sleepLog = SleepLogRowMapper.mapRow(rowWithFeeling("OK"), 0)

        // 10:53 pm at UTC-3 is 01:53 the next day in UTC.
        assertThat(sleepLog.bedStart).isEqualTo(Instant.parse("2026-11-14T01:53:00Z"))
        assertThat(sleepLog.bedEnd).isEqualTo(Instant.parse("2026-11-14T10:05:00Z"))
    }

    @Test
    fun `refuses a feeling the domain does not know`() {
        assertThatThrownBy { SleepLogRowMapper.mapRow(rowWithFeeling("GREAT"), 0) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    private fun rowWithFeeling(morningFeeling: String): ResultSet {
        val resultSet = mock(ResultSet::class.java)
        given(resultSet.getLong("id")).willReturn(7L)
        given(resultSet.getLong("user_id")).willReturn(3L)
        given(resultSet.getObject("bed_start", OffsetDateTime::class.java))
            .willReturn(OffsetDateTime.parse("2026-11-13T22:53:00-03:00"))
        given(resultSet.getObject("bed_end", OffsetDateTime::class.java))
            .willReturn(OffsetDateTime.parse("2026-11-14T07:05:00-03:00"))
        given(resultSet.getString("morning_feeling")).willReturn(morningFeeling)
        return resultSet
    }
}
