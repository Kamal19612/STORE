#!/usr/bin/env bash
# Charge les variables nécessaires au backend Java depuis supabase/.env du monorepo.
# Requiert bash (npm exécute package.json avec /bin/sh → utiliser : bash -c '. ./scripts/load-supabase-env.sh && …').
# Usage : source ce fichier pour que export soit visible dans le shell courant.
#
#   source STORE/scripts/load-supabase-env.sh
#   mvn -f STORE/pom.xml spring-boot:run
#
# Important : ne pas utiliser « return » hors fonction si ce fichier est sourcé depuis
# une autre fonction (ex. sucrestore.sh) — en bash cela quitterait la fonction appelante.
#
if [ -z "${BASH_VERSION:-}" ]; then
  echo "[load-supabase-env] Ce script nécessite bash (ex. : bash -c '. ./scripts/load-supabase-env.sh && mvn spring-boot:run')." >&2
  return 1 2>/dev/null || exit 1
fi
#
# Variables exportées :
#   SUPABASE_SERVICE_ROLE_KEY  ← SERVICE_ROLE_KEY (JWT service_role, pas ANON_KEY)
#   SUPABASE_URL               ← SUPABASE_PUBLIC_URL ou http://127.0.0.1:$KONG_HTTP_PORT
#   DB_PASSWORD                ← POSTGRES_PASSWORD

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")" && pwd)"
STORE_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REPO_ROOT="$(cd "$STORE_DIR/.." && pwd)"
ENV_FILE="${SUPABASE_ENV_FILE:-$REPO_ROOT/supabase/.env}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "[load-supabase-env] Fichier introuvable : $ENV_FILE" >&2
  echo "[load-supabase-env] Définissez SUPABASE_ENV_FILE ou placez supabase/.env à la racine du dépôt." >&2
else
# Lit une ligne KEY=value (une seule ligne, première occurrence). Préserve les '=' dans la valeur (JWT).
read_kv() {
  local key="$1"
  local line
  line="$(grep -m1 -E "^${key}=" "$ENV_FILE" 2>/dev/null || true)"
  if [[ -z "$line" ]]; then
    echo ""
    return
  fi
  echo "${line#*=}" | tr -d '\r'
}

trim() {
  local s="$1"
  s="${s#"${s%%[![:space:]]*}"}"
  s="${s%"${s##*[![:space:]]}"}"
  echo "$s"
}

SR="$(read_kv SERVICE_ROLE_KEY)"
SR="$(trim "$SR")"
if [[ -z "${SUPABASE_SERVICE_ROLE_KEY:-}" && -n "$SR" ]]; then
  export SUPABASE_SERVICE_ROLE_KEY="$SR"
fi

PG="$(read_kv POSTGRES_PASSWORD)"
PG="$(trim "$PG")"
if [[ -z "${DB_PASSWORD:-}" && -n "$PG" ]]; then
  export DB_PASSWORD="$PG"
fi

PUB="$(read_kv SUPABASE_PUBLIC_URL)"
PUB="$(trim "$PUB")"
PUB="${PUB%/}"

KONG_PORT="$(read_kv KONG_HTTP_PORT)"
KONG_PORT="$(trim "${KONG_PORT:-8000}")"

if [[ -n "$PUB" ]]; then
  if [[ -z "${SUPABASE_URL:-}" ]]; then
    export SUPABASE_URL="$PUB"
  fi
else
  if [[ -z "${SUPABASE_URL:-}" ]]; then
    export SUPABASE_URL="http://127.0.0.1:${KONG_PORT}"
  fi
fi

echo "[load-supabase-env] SUPABASE_URL=$SUPABASE_URL" >&2
if [[ -n "${SUPABASE_SERVICE_ROLE_KEY:-}" ]]; then
  echo "[load-supabase-env] SUPABASE_SERVICE_ROLE_KEY : OK (${#SUPABASE_SERVICE_ROLE_KEY} caractères)" >&2
else
  echo "[load-supabase-env] AVERTISSEMENT : SERVICE_ROLE_KEY absent dans $ENV_FILE — création livreur (Supabase Auth) échouera." >&2
fi
if [[ -n "${DB_PASSWORD:-}" ]]; then
  echo "[load-supabase-env] DB_PASSWORD : OK (${#DB_PASSWORD} caractères)" >&2
else
  echo "[load-supabase-env] AVERTISSEMENT : POSTGRES_PASSWORD absent — connexion Postgres peut échouer." >&2
fi
fi
