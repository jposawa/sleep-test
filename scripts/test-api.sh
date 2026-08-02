#!/usr/bin/env bash
#
# Exercises the sleep logger API end to end and fails if any step misbehaves.
#
# Requires the stack to be running and the database to be empty, so that the
# empty-state check is meaningful:
#
#   docker-compose down -v && docker-compose up --build -d
#   ./scripts/test-api.sh
#
# Override the defaults with environment variables:
#   BASE_URL=http://localhost:8080 USER_ID=1 ./scripts/test-api.sh

set -u

BASE_URL="${BASE_URL:-http://localhost:8080}"
USER_ID="${USER_ID:-1}"
SLEEP_LOGS="$BASE_URL/api/v1/sleep-logs"

failures=0

# Prints the response body and compares the status code with what was expected.
check() {
	local description="$1" expected="$2"
	shift 2

	local response status body
	response=$(curl -s -w $'\n%{http_code}' "$@")
	status="${response##*$'\n'}"
	body="${response%$'\n'*}"

	if [ "$status" = "$expected" ]; then
		printf '  ok   %-46s %s\n' "$description" "$status"
	else
		printf '  FAIL %-46s %s (expected %s)\n' "$description" "$status" "$expected"
		printf '       %s\n' "$body"
		failures=$((failures + 1))
	fi

	LAST_BODY="$body"
}

post_sleep() {
	check "$1" "$2" -X POST "$SLEEP_LOGS" \
		-H "X-User-Id: $USER_ID" \
		-H 'Content-Type: application/json' \
		-d "$3"
}

# An instant 'days_ago' days back at the given UTC clock time.
#
# A night spent in bed from 10:53 pm to 7:05 am at UTC-3 runs from 01:53 to
# 10:05 UTC, so in UTC both ends fall on the same calendar day.
#
# Every night below is anchored at least one day back, which is what keeps the
# sample data in the past whatever time the script runs: a clock time today may
# still be ahead of now, and the API refuses a sleep that has not ended. Both
# ends of a night must share the same offset, or the interval itself changes.
at_utc() {
	local days_ago="$1" clock="$2"
	if date -v-1d >/dev/null 2>&1; then
		date -u -v-"${days_ago}"d "+%Y-%m-%dT${clock}Z"   # BSD/macOS
	else
		date -u -d "${days_ago} days ago" "+%Y-%m-%dT${clock}Z"  # GNU
	fi
}

echo "Testing $SLEEP_LOGS as user $USER_ID"

if ! curl -s -o /dev/null --max-time 5 "$SLEEP_LOGS/last-night"; then
	echo
	echo "No API answering at $BASE_URL - start the stack first:"
	echo '  docker-compose up --build -d'
	exit 1
fi

echo
echo 'Requirement 1C - empty state'
check 'last night, before anything is logged' 404 \
	-H "X-User-Id: $USER_ID" "$SLEEP_LOGS/last-night"

if [ "$failures" -ne 0 ]; then
	echo
	echo 'The user already has sleep logs, so the empty state cannot be verified.'
	echo 'Start from a clean database:  docker-compose down -v && docker-compose up -d'
	exit 1
fi

echo
echo 'Requirement 1 - create a sleep log'
post_sleep 'last night, 10:53 pm to 7:05 am' 201 \
	"{\"bedStart\":\"$(at_utc 1 01:53:00)\",\"bedEnd\":\"$(at_utc 1 10:05:00)\",\"morningFeeling\":\"GOOD\"}"

echo
echo 'Requirement 1B - fetch last night'
check 'last night, once one exists' 200 \
	-H "X-User-Id: $USER_ID" "$SLEEP_LOGS/last-night"
echo "       $LAST_BODY"

echo
echo 'Requirement 3 - averages over a filled window'
post_sleep 'a second night, three days ago' 201 \
	"{\"bedStart\":\"$(at_utc 3 02:51:00)\",\"bedEnd\":\"$(at_utc 3 10:05:00)\",\"morningFeeling\":\"BAD\"}"
post_sleep 'a third night, six days ago' 201 \
	"{\"bedStart\":\"$(at_utc 6 01:40:00)\",\"bedEnd\":\"$(at_utc 6 09:50:00)\",\"morningFeeling\":\"OK\"}"
post_sleep 'a nap on the same day as the third night' 201 \
	"{\"bedStart\":\"$(at_utc 6 17:00:00)\",\"bedEnd\":\"$(at_utc 6 18:30:00)\",\"morningFeeling\":\"GOOD\"}"

check '30-day averages' 200 -H "X-User-Id: $USER_ID" "$SLEEP_LOGS/averages"
echo "       $LAST_BODY"

# Asserting the status alone would not notice a sleep silently missing from the
# window - which is exactly what happens when a logged instant is in the future.
if printf '%s' "$LAST_BODY" | grep -q '"sleepCount":4'; then
	printf '  ok   %-46s %s\n' 'every logged sleep counted in the window' 4
else
	printf '  FAIL %-46s %s\n' 'every logged sleep counted in the window' 'expected 4'
	failures=$((failures + 1))
fi

echo
echo 'Averages for a user with no sleeps stay a valid answer'
check 'averages, empty window' 200 -H 'X-User-Id: 4242' "$SLEEP_LOGS/averages"

echo
echo 'Rejected requests'
post_sleep 'a sleep that ends before it starts' 400 \
	"{\"bedStart\":\"$(at_utc 1 10:05:00)\",\"bedEnd\":\"$(at_utc 1 01:53:00)\",\"morningFeeling\":\"OK\"}"
post_sleep 'a sleep that has not finished yet' 400 \
	"{\"bedStart\":\"$(at_utc 1 01:53:00)\",\"bedEnd\":\"2099-01-01T00:00:00Z\",\"morningFeeling\":\"OK\"}"
post_sleep 'a feeling outside BAD, OK, GOOD' 400 \
	"{\"bedStart\":\"$(at_utc 1 01:53:00)\",\"bedEnd\":\"$(at_utc 1 10:05:00)\",\"morningFeeling\":\"GREAT\"}"
post_sleep 'a body that is not valid JSON' 400 '{'
check 'a request without the user header' 400 "$SLEEP_LOGS/last-night"

echo
if [ "$failures" -eq 0 ]; then
	echo 'All checks passed.'
else
	echo "$failures check(s) failed."
fi
exit "$failures"
