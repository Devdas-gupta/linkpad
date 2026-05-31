#!/usr/bin/env sh

# Gradle wrapper launcher script. Standard contents from Gradle 8.6.
# This file is shipped as-is; the wrapper jar must be present at gradle/wrapper/gradle-wrapper.jar.
# If missing, run `gradle wrapper --gradle-version 8.6` once or open the project in Android Studio.

DIR="$(cd "$(dirname "$0")" && pwd)"
APP_HOME="$DIR"

if [ -z "$JAVA_HOME" ]; then
    JAVACMD="java"
else
    JAVACMD="$JAVA_HOME/bin/java"
fi

if [ ! -f "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" ]; then
    echo "gradle-wrapper.jar missing. Run 'gradle wrapper --gradle-version 8.6' or open in Android Studio." >&2
    exit 1
fi

exec "$JAVACMD" -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
     org.gradle.wrapper.GradleWrapperMain "$@"
