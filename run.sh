#!/bin/sh
set -eu

cd "$(dirname "$0")"

JAVAC=""
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/javac" ]; then
    JAVAC="$JAVA_HOME/bin/javac"
else
    JAVAC="$(command -v javac || true)"
fi

if [ -z "$JAVAC" ]; then
    for JDK_DIR in "$HOME"/.local/share/jdks/* "$HOME"/.jdks/* \
            "$HOME"/.sdkman/candidates/java/* /usr/lib/jvm/*; do
        if [ -x "$JDK_DIR/bin/javac" ] && [ -x "$JDK_DIR/bin/java" ]; then
            JAVAC="$JDK_DIR/bin/javac"
            break
        fi
    done
fi

if [ -z "$JAVAC" ]; then
    echo "Error: javac not found." >&2
    echo "Install JDK 17 or newer" >&2
    echo "or point JAVA_HOME at an existing JDK directory." >&2
    exit 1
fi

JAVAC="$(readlink -f "$JAVAC" 2>/dev/null || printf '%s\n' "$JAVAC")"
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

if [ -t 0 ] && [ -t 1 ]; then
    TERMINAL_STATE="$(stty -g)"
    trap 'stty "$TERMINAL_STATE" 2>/dev/null || true' EXIT
    trap 'exit 129' HUP
    trap 'exit 130' INT
    trap 'exit 143' TERM
    stty -icanon -echo min 1 time 0
fi

"$JAVA" -cp build Main
