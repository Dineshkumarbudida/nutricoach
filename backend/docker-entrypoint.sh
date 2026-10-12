#!/bin/sh
set -eu

if [ -n "${DATABASE_PASSWORD_FILE:-}" ]; then
  if [ ! -r "$DATABASE_PASSWORD_FILE" ]; then
    echo "Database password file is not readable: $DATABASE_PASSWORD_FILE" >&2
    exit 1
  fi

  DATABASE_PASSWORD=$(cat "$DATABASE_PASSWORD_FILE")
  if [ -z "$DATABASE_PASSWORD" ]; then
    echo "Database password file is empty: $DATABASE_PASSWORD_FILE" >&2
    exit 1
  fi
  export DATABASE_PASSWORD
fi

exec java -XX:MaxRAMPercentage=75 -jar /app/nutricoach-api.jar
