#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

if ! command -v doppler &>/dev/null; then
  echo "Error: doppler CLI is not installed. See https://docs.doppler.com/docs/install-cli"
  exit 1
fi

doppler secrets download --project dashboard-spring-data --config dev_personal --format env --no-file > "$PROJECT_DIR/.env.local"
echo "Secrets written to .env.local"
