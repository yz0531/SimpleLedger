#!/bin/sh

# Gradle startup script for POSIX systems.

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P) || exit 1
APP_BASE_NAME=${0##*/}
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ -n "$JAVA_HOME" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
    if [ ! -x "$JAVACMD" ]; then
        echo "ERROR: JAVA_HOME points to an invalid directory: $JAVA_HOME" >&2
        exit 1
    fi
else
    JAVACMD=java
    if ! command -v java >/dev/null 2>&1; then
        echo "ERROR: JAVA_HOME is not set and no 'java' command could be found." >&2
        exit 1
    fi
fi

if [ ! -r "$CLASSPATH" ]; then
    echo "ERROR: Missing $CLASSPATH. Regenerate the Gradle wrapper before using this script." >&2
    exit 1
fi

exec "$JAVACMD" "-Xmx64m" "-Xms64m" $JAVA_OPTS $GRADLE_OPTS \
    "-Dorg.gradle.appname=$APP_BASE_NAME" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain "$@"
