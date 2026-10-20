#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"
JAR='gradle/wrapper/gradle-wrapper.jar'
if [[ ! -s "$JAR" ]]; then
  echo 'Downloading official Gradle 8.7 wrapper jar...'
  curl --fail --location --show-error --silent 'https://raw.githubusercontent.com/gradle/gradle/v8.7.0/gradle/wrapper/gradle-wrapper.jar' -o "$JAR"
fi
if ! unzip -t "$JAR" >/dev/null 2>&1; then
  echo 'Gradle wrapper jar is invalid; remove it and try again.' >&2
  exit 1
fi
echo 'Ready. Open this folder in Android Studio.'
