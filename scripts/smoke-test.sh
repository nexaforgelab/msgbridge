#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-admin123}"
ADMIN_KEY="${ADMIN_KEY:-dev-admin-key}"
APP_ID="${APP_ID:-demo}"
APP_SECRET="${APP_SECRET:-demo-secret}"

need() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "missing required command: $1" >&2
    exit 1
  fi
}

need curl
need openssl
need awk
need sed

json_field() {
  sed -n "s/.*\"$1\":\"\\([^\"]*\\)\".*/\\1/p"
}

assert_contains() {
  local haystack="$1"
  local needle="$2"
  local label="$3"
  if ! printf '%s' "$haystack" | grep -F "$needle" >/dev/null; then
    echo "$label failed: expected to contain $needle" >&2
    echo "$haystack" >&2
    exit 1
  fi
  echo "$label: ok"
}

admin_json() {
  curl -sS "$BASE_URL$1" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    "${@:2}"
}

login_response=$(curl -sS -X POST "$BASE_URL/admin/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASSWORD\"}")
TOKEN=$(printf '%s' "$login_response" | json_field token)
if [ -z "$TOKEN" ]; then
  echo "admin login failed" >&2
  echo "$login_response" >&2
  exit 1
fi
echo "admin login: ok"

assert_contains "$(admin_json /admin/dashboard)" '"code":0' "dashboard"
assert_contains "$(admin_json /admin/users)" "\"username\":\"$ADMIN_USER\"" "users"
assert_contains "$(admin_json /admin/apps)" '"code":0' "apps"
assert_contains "$(admin_json /admin/channels/health)" '"code":0' "channel health"
assert_contains "$(admin_json /admin/templates)" '"code":0' "templates"
assert_contains "$(admin_json /admin/routes)" '"code":0' "routes"
assert_contains "$(admin_json /admin/audit-logs?size=5)" '"code":0' "audit logs"
assert_contains "$(admin_json /admin/system/status)" '"warnings"' "system status"

preview_body='{"sceneCode":"SMOKE","channelType":"WE_COM_ROBOT","msgType":"TEXT","contentTemplate":"hello ${name}","data":{"name":"MsgBridge"}}'
assert_contains "$(admin_json /admin/templates/preview -X POST -d "$preview_body")" 'hello MsgBridge' "template preview"

route_body='{"sceneCode":"SMOKE","routeKey":"none","data":{"amount":"1"}}'
assert_contains "$(admin_json /admin/routes/simulate -X POST -d "$route_body")" '"code":0' "route simulate"

request_id="smoke-$(date +%s)"
send_path="/api/v1/messages/send"
send_body="{\"requestId\":\"$request_id\",\"appId\":\"$APP_ID\",\"sceneCode\":\"SMOKE_NO_ROUTE\",\"priority\":\"NORMAL\",\"receiver\":{\"type\":\"ROUTE\",\"routeKey\":\"missing\"},\"message\":{\"type\":\"TEXT\",\"title\":\"Smoke\",\"content\":\"Smoke test\",\"data\":{\"source\":\"smoke\"}},\"options\":{\"deduplicate\":true,\"retry\":false,\"maxRetryCount\":0}}"
timestamp="$(date +%s)"
nonce="smoke-$timestamp"
canonical="POST
$send_path
$timestamp
$nonce
$send_body"
signature=$(printf '%s' "$canonical" | openssl dgst -sha256 -hmac "$APP_SECRET" | awk '{print $NF}')
send_response=$(curl -sS -X POST "$BASE_URL$send_path" \
  -H "Content-Type: application/json" \
  -H "X-MB-App-Id: $APP_ID" \
  -H "X-MB-Timestamp: $timestamp" \
  -H "X-MB-Nonce: $nonce" \
  -H "X-MB-Signature: $signature" \
  -d "$send_body")
assert_contains "$send_response" '"accepted"' "business send"
message_id=$(printf '%s' "$send_response" | json_field messageId)
if [ -z "$message_id" ]; then
  echo "business send failed: messageId missing" >&2
  echo "$send_response" >&2
  exit 1
fi

assert_contains "$(admin_json "/admin/messages?size=5&requestId=$request_id")" "$request_id" "message query"
csv_head=$(curl -sS "$BASE_URL/admin/messages/export?requestId=$request_id" -H "Authorization: Bearer $TOKEN" | sed -n '1p')
assert_contains "$csv_head" 'messageId,requestId,appId' "message export"
assert_contains "$(admin_json '/admin/messages/purge-preview?before=2026-01-01T00:00:00Z')" '"messages"' "purge preview"
bulk_body="{\"messageIds\":[\"$message_id\"]}"
assert_contains "$(admin_json /admin/messages/bulk-retry -X POST -d "$bulk_body")" '"succeeded":1' "bulk retry"
assert_contains "$(admin_json /admin/messages/bulk-terminate -X POST -d "$bulk_body")" '"succeeded":1' "bulk terminate"
assert_contains "$(curl -sS "$BASE_URL/admin/dashboard" -H "X-MB-Admin-Key: $ADMIN_KEY")" '"code":0' "admin key compatibility"

echo "smoke test finished"
