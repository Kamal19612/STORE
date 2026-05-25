#!/usr/bin/env bash
# Charge supabase/.env du projet STORE uniquement (pas de repli monorepo parent).
# Usage : source STORE/scripts/load-supabase-env.sh && mvn spring-boot:run
if [ -z "${BASH_VERSION:-}" ]; then
  echo "[load-supabase-env] Nécessite bash." >&2
  return 1 2>/dev/null || exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")" && pwd)"
STORE_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
LOCAL_ENV="$STORE_DIR/supabase/.env"

if [[ -n "${SUPABASE_ENV_FILE:-}" ]]; then
  ENV_FILE="$SUPABASE_ENV_FILE"
else
  ENV_FILE="$LOCAL_ENV"
fi

if [[ ! -f "$ENV_FILE" ]]; then
  echo "[load-supabase-env] Fichier introuvable : $ENV_FILE" >&2
  echo "[load-supabase-env] Lancez : ./scripts/store-local-setup.sh --env-only" >&2
  return 1 2>/dev/null || exit 1
fi

# shellcheck source=/dev/null
source "$SCRIPT_DIR/ensure-sucre-store-database.sh"

read_kv() {
  local key="$1"
  local line
  line="$(grep -m1 -E "^${key}=" "$ENV_FILE" 2>/dev/null || true)"
  [[ -z "$line" ]] && echo "" && return
  echo "${line#*=}" | tr -d '\r'
}

trim() {
  local s="$1"
  s="${s#"${s%%[![:space:]]*}"}"
  s="${s%"${s##*[![:space:]]}"}"
  s="${s#\"}"
  s="${s%\"}"
  echo "$s"
}

SR="$(trim "$(read_kv SERVICE_ROLE_KEY)")"
if [[ -z "${SUPABASE_SERVICE_ROLE_KEY:-}" && -n "$SR" ]]; then
  export SUPABASE_SERVICE_ROLE_KEY="$SR"
fi

PG="$(trim "$(read_kv POSTGRES_PASSWORD)")"
if [[ -z "${DB_PASSWORD:-}" && -n "$PG" ]]; then
  export DB_PASSWORD="$PG"
fi

PUB="$(trim "$(read_kv SUPABASE_PUBLIC_URL)")"
PUB="${PUB%/}"
KONG_PORT="$(trim "$(read_kv KONG_HTTP_PORT)")"
[[ -z "$KONG_PORT" ]] && KONG_PORT="54321"

if [[ -n "$PUB" ]]; then
  [[ -z "${SUPABASE_URL:-}" ]] && export SUPABASE_URL="$PUB"
else
  [[ -z "${SUPABASE_URL:-}" ]] && export SUPABASE_URL="http://127.0.0.1:${KONG_PORT}"
fi

echo "[load-supabase-env] Fichier : $ENV_FILE" >&2
echo "[load-supabase-env] SUPABASE_URL=$SUPABASE_URL" >&2
echo "[load-supabase-env] DB_JDBC_URL=${DB_JDBC_URL:-non défini}" >&2
