package com.noom.interview.fullstack.sleep.web

/**
 * Binds the id of the user the request is acting for.
 *
 * Authentication is out of scope, so the caller states who it is through a
 * header. Resolving it in one place keeps that decision out of the controller,
 * which would otherwise repeat the header name on every endpoint.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class CurrentUser
