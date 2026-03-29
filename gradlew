#!/bin/sh
#
# Gradle wrapper script — bootstraps the Gradle version declared in
# gradle/wrapper/gradle-wrapper.properties via the Gradle Wrapper JAR.
# If the JAR is absent (fresh clone before a build), fall back to any
# system 'gradle' on PATH so local development still works.
#

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
WRAPPER_JAR="$SCRIPT_DIR/gradle/wrapper/gradle-wrapper.jar"

if [ -f "$WRAPPER_JAR" ]; then
  exec java $JAVA_OPTS \
       -classpath "$WRAPPER_JAR" \
       org.gradle.wrapper.GradleWrapperMain "$@"
elif command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
else
  echo "ERROR: gradle-wrapper.jar not found and no system 'gradle' on PATH." >&2
  echo "Run: gradle wrapper --gradle-version=8.11.1" >&2
  exit 1
fi
