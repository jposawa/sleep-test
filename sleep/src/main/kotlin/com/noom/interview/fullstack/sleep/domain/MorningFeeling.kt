package com.noom.interview.fullstack.sleep.domain

/**
 * How the user felt when they woke up.
 *
 * Persisted by name, not by ordinal: renaming a value costs a single UPDATE,
 * whereas reordering an ordinal enum would silently rewrite existing history.
 */
enum class MorningFeeling {
    BAD,
    OK,
    GOOD
}
