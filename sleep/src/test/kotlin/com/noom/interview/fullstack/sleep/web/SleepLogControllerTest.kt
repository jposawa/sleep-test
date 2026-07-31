package com.noom.interview.fullstack.sleep.web

import com.noom.interview.fullstack.sleep.domain.MorningFeeling
import com.noom.interview.fullstack.sleep.domain.SleepAverages
import com.noom.interview.fullstack.sleep.domain.SleepLog
import com.noom.interview.fullstack.sleep.service.SleepLogService
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Duration
import java.time.Instant
import java.time.LocalTime

/** Covers the web layer only: the service is mocked. */
@WebMvcTest(SleepLogController::class)
class SleepLogControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var service: SleepLogService

    @Test
    fun `creates a sleep log and reports the derived time in bed`() {
        given(service.create(USER_ID, BED_START, BED_END, MorningFeeling.GOOD)).willReturn(sleepLog())

        mockMvc.perform(
            post(SLEEP_LOGS)
                .header(USER_HEADER, USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(BED_START, BED_END, "GOOD"))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.totalMinutesInBed").value(492))
            .andExpect(jsonPath("$.morningFeeling").value("GOOD"))
    }

    @Test
    fun `rejects an interval that ends before it starts without calling the service`() {
        mockMvc.perform(
            post(SLEEP_LOGS)
                .header(USER_HEADER, USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(BED_END, BED_START, "OK"))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("bedEnd must be after bedStart"))

        verifyNoInteractions(service)
    }

    @Test
    fun `rejects a morning feeling the domain does not know`() {
        mockMvc.perform(
            post(SLEEP_LOGS)
                .header(USER_HEADER, USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(BED_START, BED_END, "GREAT"))
        )
            .andExpect(status().isBadRequest)

        verifyNoInteractions(service)
    }

    @Test
    fun `rejects a request that does not say who it is`() {
        mockMvc.perform(get("$SLEEP_LOGS/last-night"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Missing required header: $USER_HEADER"))

        verifyNoInteractions(service)
    }

    @Test
    fun `rejects a user id that is not a number`() {
        mockMvc.perform(get("$SLEEP_LOGS/last-night").header(USER_HEADER, "not-a-number"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("$USER_HEADER must be a numeric user id"))

        verifyNoInteractions(service)
    }

    @Test
    fun `returns last night when the user has one`() {
        given(service.findLastNight(USER_ID)).willReturn(sleepLog())

        mockMvc.perform(get("$SLEEP_LOGS/last-night").header(USER_HEADER, USER_ID))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.bedStart").value("2026-11-14T01:53:00Z"))
    }

    @Test
    fun `answers 404 when the user has never logged a sleep`() {
        given(service.findLastNight(USER_ID)).willReturn(null)

        mockMvc.perform(get("$SLEEP_LOGS/last-night").header(USER_HEADER, USER_ID))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `reports averages, including the feelings that never occurred`() {
        given(service.findLast30DayAverages(USER_ID)).willReturn(averages())

        mockMvc.perform(get("$SLEEP_LOGS/averages").header(USER_HEADER, USER_ID))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sleepCount").value(1))
            .andExpect(jsonPath("$.averageMinutesInBed").value(492))
            .andExpect(jsonPath("$.averageBedStartUtc").value("01:53:00"))
            .andExpect(jsonPath("$.feelingFrequencies.OK").value(0))
    }

    @Test
    fun `reports an empty window as a valid answer rather than an error`() {
        given(service.findLast30DayAverages(USER_ID)).willReturn(emptyAverages())

        mockMvc.perform(get("$SLEEP_LOGS/averages").header(USER_HEADER, USER_ID))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sleepCount").value(0))
            .andExpect(jsonPath("$.averageMinutesInBed").doesNotExist())

        verify(service, never()).findLastNight(USER_ID)
    }

    private fun body(bedStart: Instant, bedEnd: Instant, morningFeeling: String) =
        """{"bedStart":"$bedStart","bedEnd":"$bedEnd","morningFeeling":"$morningFeeling"}"""

    private fun sleepLog() = SleepLog(
        id = 1L,
        userId = USER_ID,
        bedStart = BED_START,
        bedEnd = BED_END,
        morningFeeling = MorningFeeling.GOOD
    )

    private fun averages() = SleepAverages(
        from = WINDOW_START,
        to = BED_END,
        sleepCount = 1,
        averageTimeInBed = Duration.ofMinutes(492),
        averageBedStart = LocalTime.of(1, 53),
        averageBedEnd = LocalTime.of(10, 5),
        feelingFrequencies = mapOf(
            MorningFeeling.BAD to 0,
            MorningFeeling.OK to 0,
            MorningFeeling.GOOD to 1
        )
    )

    private fun emptyAverages() = SleepAverages(
        from = WINDOW_START,
        to = BED_END,
        sleepCount = 0,
        averageTimeInBed = null,
        averageBedStart = null,
        averageBedEnd = null,
        feelingFrequencies = MorningFeeling.values().associateWith { 0 }
    )

    private companion object {
        const val SLEEP_LOGS = "/api/v1/sleep-logs"
        const val USER_HEADER = "X-User-Id"
        const val USER_ID = 1L
        val BED_START: Instant = Instant.parse("2026-11-14T01:53:00Z")
        val BED_END: Instant = Instant.parse("2026-11-14T10:05:00Z")
        val WINDOW_START: Instant = Instant.parse("2026-10-15T10:05:00Z")
    }
}
