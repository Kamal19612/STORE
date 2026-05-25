#!/usr/bin/env bash
# Garantit que STORE n'utilise que la base Postgres du stack Supabase Sucre_Store (port 5434).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SUPABASE_DIR="$ROOT/supabase"
CONFIG="$SUPABASE_DIR/config.toml"
EXPECTED_DB_PORT="${SUCRE_STORE_DB_PORT:-5434}"
EXPECTED_API_PORT="${SUCRE_STORE_API_PORT:-54321}"
OTHER_DB_PORTS=(5534 5634 6434)

read_toml_port() {
  local section="$1"
  local key="$2"
  local default="$3"
  if [[ ! -f "$CONFIG" ]]; then
    echo "$default"
    return
  fi
  python3 - "$CONFIG" "$section" "$key" "$default" <<'PY'
import sys
path, section, key, default = sys.argv[1:5]
text = open(path, encoding="utf-8").read()
in_section = False
for line in text.splitlines():
    s = line.strip()
    if s.startswith("[") and s.endswith("]"):
        in_section = s == f"[{section}]"
        continue
    if in_section and s.startswith(f"{key}") and "=" in s:
        val = s.split("=", 1)[1].strip().strip('"').strip("'")
        if val.isdigit():
            print(val)
            sys.exit(0)
print(default)
PY
}

if [[ -f "$CONFIG" ]]; then
  EXPECTED_DB_PORT="$(read_toml_port db port "$EXPECTED_DB_PORT")"
  EXPECTED_API_PORT="$(read_toml_port api port "$EXPECTED_API_PORT")"
fi

stop_other_supabase_stacks() {
  local dir name
  for dir in "$ROOT/../STORE-ALL/supabase" "$ROOT/../SPIRIT_S/supabase" "$ROOT/../SPIRIT-STORE/supabase"; do
    [[ -d "$dir" ]] || continue
    if (cd "$dir" && supabase status &>/dev/null); then
      name="$(basename "$(dirname "$dir")")"
      echo "[sucre-store-db] Arrêt Supabase de $name …" >&2
      (cd "$dir" && supabase stop) || true
    fi
  done
  if docker ps -q --filter 'name=_SPIRIT-STORE' 2>/dev/null | grep -q .; then
    docker stop $(docker ps -q --filter 'name=_SPIRIT-STORE') 2>/dev/null || true
  fi
}

assert_jdbc_not_other_project() {
  local url="${DB_JDBC_URL:-}"
  [[ -z "$url" ]] && return 0
  local p
  for p in "${OTHER_DB_PORTS[@]}"; do
    if [[ "$url" == *":${p}/"* ]] || [[ "$url" == *":${p} "* ]]; then
      echo "[sucre-store-db] ERREUR : DB_JDBC_URL utilise le port $p (autre projet). Attendu : $EXPECTED_DB_PORT." >&2
      exit 1
    fi
  done
}

ensure_supabase() {
  command -v supabase &>/dev/null || { echo "[sucre-store-db] CLI supabase absente." >&2; exit 1; }
  if ! (cd "$SUPABASE_DIR" && supabase status &>/dev/null); then
    echo "[sucre-store-db] Démarrage Supabase Sucre_Store (Postgres $EXPECTED_DB_PORT)…" >&2
    (cd "$SUPABASE_DIR" && supabase start)
  fi
}

export_db_env_from_local_dotenv() {
  local env_file="$SUPABASE_DIR/.env"
  [[ -f "$env_file" ]] || return 0
  local line key val
  for key in DB_JDBC_URL DB_HOST DB_PORT DB_NAME POSTGRES_PASSWORD; do
    line="$(grep -m1 -E "^${key}=" "$env_file" 2>/dev/null || true)"
    [[ -n "$line" ]] || continue
    val="${line#*=}"
    val="$(echo "$val" | tr -d '\r' | sed -e 's/^["'\'']//' -e 's/["'\'']$//')"
    case "$key" in
      DB_JDBC_URL) [[ -z "${DB_JDBC_URL:-}" ]] && export DB_JDBC_URL="$val" ;;
      DB_HOST) [[ -z "${DB_HOST:-}" ]] && export DB_HOST="$val" ;;
      DB_PORT) [[ -z "${DB_PORT:-}" ]] && export DB_PORT="$val" ;;
      DB_NAME) [[ -z "${DB_NAME:-}" ]] && export DB_NAME="$val" ;;
      POSTGRES_PASSWORD) [[ -z "${DB_PASSWORD:-}" ]] && export DB_PASSWORD="$val" ;;
    esac
  done
}

finalize_db_exports() {
  export DB_HOST="${DB_HOST:-127.0.0.1}"
  export DB_PORT="${DB_PORT:-$EXPECTED_DB_PORT}"
  export DB_NAME="${DB_NAME:-postgres}"
  if [[ -z "${DB_JDBC_URL:-}" ]]; then
    export DB_JDBC_URL="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}"
  fi
  if [[ "${DB_PORT}" != "$EXPECTED_DB_PORT" ]]; then
    echo "[sucre-store-db] ERREUR : DB_PORT=$DB_PORT — attendu $EXPECTED_DB_PORT." >&2
    exit 1
  fi
  assert_jdbc_not_other_project
  echo "[sucre-store-db] Base unique : $DB_JDBC_URL (API : http://127.0.0.1:$EXPECTED_API_PORT)" >&2
}

stop_other_supabase_stacks
ensure_supabase
export_db_env_from_local_dotenv
finalize_db_exports
