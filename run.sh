#!/bin/sh
# Build and run DevOpsSubmarine.
#
# Looks for a JDK in this order:
#   1. $JAVA_HOME/bin (if it contains javac)
#   2. javac on $PATH
# Then uses the matching java from the same JDK directory.
set -eu

cd "$(dirname "$0")"

JAVAC=""
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/javac" ]; then
    JAVAC="$JAVA_HOME/bin/javac"
else
    JAVAC="$(command -v javac || true)"
fi

if [ -z "$JAVAC" ]; then
    echo "Error: javac not found." >&2
    echo "Install a JDK (for example: apt install openjdk-21-jdk)" >&2
    echo "or point JAVA_HOME at an existing JDK directory." >&2
    exit 1
fi

JDK_BIN="$(dirname "$JAVAC")"
JAVA="$JDK_BIN/java"
if [ ! -x "$JAVA" ]; then
    echo "Error: found javac at $JAVAC but no matching java in $JDK_BIN." >&2
    echo "A full JDK is required (javac and java must come from the same installation)." >&2
    exit 1
fi

COMPILER_VERSION="$("$JAVAC" -version 2>&1 | sed -n 's/^javac \([0-9.]*\).*/\1/p')"
RUNTIME_VERSION="$("$JAVA" -version 2>&1 | head -n 1 | sed -n 's/.*version[[:space:]]*"\{0,1\}\([0-9.]*\).*/\1/p')"
echo "Using compiler: javac $COMPILER_VERSION ($JAVAC)"
echo "Using runtime:  java $RUNTIME_VERSION ($JAVA)"

rm -rf build
mkdir build

if ! "$JAVAC" -d build *.java; then
    echo "Error: compilation failed. See messages above." >&2
    exit 1
fi

"$JAVA" -cp build Main
