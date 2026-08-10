#!/usr/bin/env bash
#
# Smoke test for the Pullwise staging environment.
#
# Usage:
#   STAGING_URL=https://staging.pullwise.ai ./smoke-test-staging.sh
#   STAGING_URL=http://<ip> STAGING_TOKEN=<jwt> ./smoke-test-staging.sh
#
# Env vars:
#   STAGING_URL     Base URL of the staging environment (required)
#   STAGING_TOKEN   Optional JWT; enables the full WebSocket handshake check (expects 101).
#                   Without it, only verifies the WS endpoint answers (auth enforced).
#   SMOKE_WAIT      Max seconds to wait for readiness, default 180.

set -euo pipefail

STAGING_URL="${STAGING_URL:-}"
STAGING_TOKEN="${STAGING_TOKEN:-}"
SMOKE_WAIT="${SMOKE_WAIT:-180}"
POLL_INTERVAL=10

if [[ -z "$STAGING_URL" ]]; then
  echo "::error::STAGING_URL must be set (e.g. https://staging.pullwise.ai)" >&2
  exit 1
fi

PASS=0
FAIL=0

check() {
  local name="$1" result="$2"
  if [[ "$result" == "ok" ]]; then
    echo "[PASS] $name"
    PASS=$((PASS + 1))
  else
    echo "[FAIL] $name"
    FAIL=$((FAIL + 1))
  fi
}

wait_until() {
  local desc="$1" max=$((SMOKE_WAIT / POLL_INTERVAL)) elapsed=0
  while (( elapsed < max )); do
    if "$2" >/dev/null 2>&1; then
      return 0
    fi
    elapsed=$((elapsed + 1))
    (( elapsed < max )) && sleep "$POLL_INTERVAL"
  done
  return 1
}

echo ">>> Smoke test against: $STAGING_URL (readiness wait: ${SMOKE_WAIT}s max)"

frontend_ready() {
  [[ "$(curl -fsS --max-time 10 -o /dev/null -w '%{http_code}' "$STAGING_URL/")" == "200" ]]
}

health_up() {
  [[ "$(curl -fsS --max-time 15 "$STAGING_URL/actuator/health")" == *'"status":"UP"'* ]]
}

# 1. Frontend serves the SPA (wait for readiness)
if wait_until "frontend" frontend_ready; then
  check "frontend (GET / -> 200)" "ok"
else
  HTTP_CODE=$(curl -sS --max-time 10 -o /dev/null -w '%{http_code}' "$STAGING_URL/" 2>/dev/null || echo "000")
  check "frontend (GET / -> 200)" "unreachable (last code $HTTP_CODE)"
fi

# 2. Backend health via nginx proxy (aggregates postgres/redis/rabbitmq).
#    Note: /actuator/health returns HTTP 200 even when DOWN (mapped), so we
#    must inspect the JSON body.
if wait_until "backend health" health_up; then
  check "backend health (status UP)" "ok"
else
  HEALTH_BODY=$(curl -sS --max-time 15 "$STAGING_URL/api/actuator/health" 2>/dev/null || echo "unreachable")
  check "backend health (status UP)" "body: ${HEALTH_BODY:0:200}"
fi

# 3. WebSocket endpoint (native on /ws, proxied by nginx).
WS_KEY="$(printf '1234567890123456' | base64)"
WS_CODE=$(curl -sS --max-time 10 -o /dev/null -w '%{http_code}' \
  -H "Connection: Upgrade" \
  -H "Upgrade: websocket" \
  -H "Sec-WebSocket-Version: 13" \
  -H "Sec-WebSocket-Key: $WS_KEY" \
  "$STAGING_URL/ws?token=$STAGING_TOKEN" 2>/dev/null || echo "000")
if [[ -n "$STAGING_TOKEN" ]]; then
  check "websocket handshake (101)" "$([[ "$WS_CODE" == "101" ]] && echo ok || echo "got $WS_CODE")"
else
  # Without a token the interceptor rejects with 4xx; a 5xx/000 means the endpoint is down.
  if [[ "$WS_CODE" =~ ^(4[0-9][0-9])$ ]]; then
    check "websocket reachable + auth enforced (4xx)" "ok"
  else
    check "websocket reachable + auth enforced (4xx)" "got $WS_CODE"
  fi
fi

echo
echo ">>> Result: $PASS passed, $FAIL failed"
[[ "$FAIL" -eq 0 ]]
