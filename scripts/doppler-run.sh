#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

if ! command -v doppler &>/dev/null; then
  echo "Error: doppler CLI is not installed. See https://docs.doppler.com/docs/install-cli"
  exit 1
fi

cd "$PROJECT_DIR"

exec doppler run --project dashboard-spring-oauth --config dev_personal -- "$PROJECT_DIR/mvnw" spring-boot:run
