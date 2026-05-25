#!/usr/bin/env bash
# Installation locale STORE (Sucre) : .env + Supabase Docker + clés.
# Usage : ./scripts/store-local-setup.sh [--env-only] [--no-start]
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
chmod +x "$ROOT/scripts"/*.sh "$ROOT/scripts"/*.py 2>/dev/null || true
ENV_ONLY=false
NO_START=false
while (($#)); do
  case "${1:-}" in
    --env-only) ENV_ONLY=true ;;
    --no-start) NO_START=true ;;
    -h|--help)
      echo "Usage: store-local-setup.sh [--env-only] [--no-start]"
      exit 0
      ;;
    *) echo "Option inconnue: $1" >&2; exit 1 ;;
  esac
  shift
done

mkdir -p "$ROOT/supabase" "$ROOT/logs"

if [[ ! -f "$ROOT/supabase/.env" ]]; then
  cp "$ROOT/supabase/.env.example" "$ROOT/supabase/.env"
  echo "[sucre-store] Créé supabase/.env"
fi

if "$ENV_ONLY"; then
  echo "[sucre-store] Fin (--env-only)."
  exit 0
fi

command -v supabase &>/dev/null || { echo "[sucre-store] Installe la CLI Supabase." >&2; exit 1; }

if "$NO_START"; then
  echo "[sucre-store] --no-start"
elif ! (cd "$ROOT/supabase" && supabase status &>/dev/null); then
  (cd "$ROOT/supabase" && supabase start)
else
  echo "[sucre-store] Supabase déjà actif."
fi

bash "$ROOT/scripts/supabase-refresh-local-keys.sh"
bash "$ROOT/scripts/ensure-sucre-store-database.sh"

echo ""
echo "[sucre-store] Prêt — Postgres 5434, API 54321, Spring ~8082"
