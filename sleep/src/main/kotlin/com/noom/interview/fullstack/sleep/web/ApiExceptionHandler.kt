package com.noom.interview.fullstack.sleep.web

import com.noom.interview.fullstack.sleep.service.InvalidSleepIntervalException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(InvalidSleepIntervalException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleInvalidInterval(exception: InvalidSleepIntervalException) =
        ApiError(exception.message ?: "Invalid sleep interval")

    /**
     * Covers a malformed body, an unparseable instant, and a morning feeling
     * outside [BAD, OK, GOOD]. The cause is not echoed back: it carries parser
     * internals that are of no use to a client.
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleUnreadableBody(exception: HttpMessageNotReadableException) =
        ApiError("Malformed request body")

    @ExceptionHandler(MissingRequestHeaderException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleMissingHeader(exception: MissingRequestHeaderException) =
        ApiError("Missing required header: ${exception.headerName}")

    /** A constraint the service did not catch, e.g. a user id that does not exist. */
    @ExceptionHandler(DataIntegrityViolationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleDataIntegrityViolation(exception: DataIntegrityViolationException) =
        ApiError("Request violates a data constraint")
}
