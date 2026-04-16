#!/usr/bin/env sh
set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
WRAPPER_PROPERTIES="$BASE_DIR/.mvn/wrapper/maven-wrapper.properties"

if [ ! -f "$WRAPPER_PROPERTIES" ]; then
  echo "Missing maven-wrapper.properties at $WRAPPER_PROPERTIES" >&2
  exit 1
fi

DIST_URL=$(grep '^distributionUrl=' "$WRAPPER_PROPERTIES" | cut -d'=' -f2-)
if [ -z "${DIST_URL:-}" ]; then
  echo "distributionUrl is missing in maven-wrapper.properties" >&2
  exit 1
fi

DIST_BASENAME=$(basename "$DIST_URL")
DIST_NAME=${DIST_BASENAME%-bin.zip}
M2_HOME_DIR=${MAVEN_USER_HOME:-"$HOME/.m2"}
CACHE_DIR="$M2_HOME_DIR/wrapper/dists/$DIST_NAME"
MAVEN_DIR="$CACHE_DIR/$DIST_NAME"
MAVEN_BIN="$MAVEN_DIR/bin/mvn"

if [ ! -x "$MAVEN_BIN" ]; then
  mkdir -p "$CACHE_DIR"
  TMP_ZIP="$CACHE_DIR/$DIST_BASENAME"

  if [ ! -f "$TMP_ZIP" ]; then
    if command -v curl >/dev/null 2>&1; then
      curl -fsSL "$DIST_URL" -o "$TMP_ZIP"
    elif command -v wget >/dev/null 2>&1; then
      wget -q "$DIST_URL" -O "$TMP_ZIP"
    else
      echo "Neither curl nor wget is available to download Maven." >&2
      exit 1
    fi
  fi

  unzip -q -o "$TMP_ZIP" -d "$CACHE_DIR"
fi

exec "$MAVEN_BIN" "$@"
