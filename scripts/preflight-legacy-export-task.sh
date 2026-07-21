#!/usr/bin/env bash
set -euo pipefail

# Release gate for the destructive removal of tb_export_task.
#
# Usage:
#   preflight-legacy-export-task.sh <sqlite-db> <backup-dir> [--allow-destructive]
#
# MySQL and PostgreSQL deployments must run their vendor-native backup command
# before this gate and apply the same count/authorization decision. This script
# is intentionally SQLite-only so a missing client cannot silently skip the gate.

if [[ $# -lt 2 || $# -gt 3 ]]; then
  echo "usage: $0 <sqlite-db> <backup-dir> [--allow-destructive]" >&2
  exit 64
fi

db_path="$1"
backup_dir="$2"
authorization="${3:-}"

if [[ ! -f "$db_path" ]]; then
  echo "SQLite database does not exist: $db_path" >&2
  exit 66
fi
if ! command -v sqlite3 >/dev/null 2>&1; then
  echo "sqlite3 is required for the legacy export-task preflight" >&2
  exit 69
fi

mkdir -p "$backup_dir"
timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
backup_path="$backup_dir/tb-export-task-before-removal-$timestamp.sqlite"

# The backup is taken before reading the count or allowing a destructive flag.
sqlite3 "$db_path" ".backup '$backup_path'"
printf 'backup=%s\n' "$backup_path"

row_count="$(sqlite3 "$db_path" \
  "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='tb_export_task';")"
if [[ "$row_count" == "1" ]]; then
  row_count="$(sqlite3 "$db_path" 'SELECT COUNT(*) FROM tb_export_task;')"
else
  row_count=0
fi
printf 'tb_export_task_rows=%s\n' "$row_count"

if [[ "$row_count" != "0" && "$authorization" != "--allow-destructive" ]]; then
  echo "tb_export_task is non-empty; backup exists but --allow-destructive is required" >&2
  exit 2
fi

echo "legacy export-task preflight passed"
