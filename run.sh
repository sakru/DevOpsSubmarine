#!/bin/sh
set -e

cd "$(dirname "$0")"

rm -rf build
mkdir build
javac -d build *.java
java -cp build Main
