#!/usr/bin/env bash
#
# Deploys a previously built application JAR onto an EC2 host.
# The script expects the artefact to be available locally (for example in /tmp)
# and stages it under a versioned releases directory before restarting the app.
#
# Usage: remote-deploy.sh <release-version> <artifact-path>
# Environment variables:
#   DEPLOY_DIR             Base directory where releases/ and current/ are kept (default: /opt/fj6/app)
#   SERVICE_NAME           Optional systemd unit to restart instead of running the JAR manually
#   JAVA_OPTS              JVM options used when starting the standalone process (default: "-Xms256m -Xmx512m")
#   APP_PROCESS_PATTERN    Grep pattern that identifies the running Java process for manual restarts
#
set -euo pipefail

if [[ $# -lt 2 ]]; then
  echo "Usage: $0 <release-version> <artifact-path>" >&2
  exit 1
fi

RELEASE_VERSION="$1"
ARTIFACT_PATH="$2"

if [[ ! -f "$ARTIFACT_PATH" ]]; then
  echo "Artifact '$ARTIFACT_PATH' does not exist" >&2
  exit 1
fi

DEPLOY_DIR=${DEPLOY_DIR:-/opt/fj6/app}
RELEASES_DIR="$DEPLOY_DIR/releases"
CURRENT_LINK="$DEPLOY_DIR/current"
TARGET_DIR="$RELEASES_DIR/$RELEASE_VERSION"
JAR_NAME="application.jar"
JAVA_OPTS=${JAVA_OPTS:--Xms256m -Xmx512m}
APP_PROCESS_PATTERN=${APP_PROCESS_PATTERN:-application.jar}

mkdir -p "$TARGET_DIR"
install -m 644 "$ARTIFACT_PATH" "$TARGET_DIR/$JAR_NAME"

ln -sfn "$TARGET_DIR" "$CURRENT_LINK"

SUDO_BIN=""
if command -v sudo >/dev/null 2>&1; then
  SUDO_BIN="sudo"
fi

if [[ -n "${SERVICE_NAME:-}" && -x "$(command -v systemctl 2>/dev/null || true)" ]]; then
  echo "Restarting systemd service '$SERVICE_NAME'"
  ${SUDO_BIN:+$SUDO_BIN }systemctl restart "$SERVICE_NAME"
else
  echo "Restarting standalone Java process"
  if pgrep -f "$APP_PROCESS_PATTERN" >/dev/null 2>&1; then
    ${SUDO_BIN:+$SUDO_BIN }pkill -f "$APP_PROCESS_PATTERN" || true
    sleep 2
  fi
  nohup java $JAVA_OPTS -jar "$CURRENT_LINK/$JAR_NAME" > "$CURRENT_LINK/app.log" 2>&1 &
fi

echo "$RELEASE_VERSION" > "$CURRENT_LINK/VERSION"

echo "Deployment of version '$RELEASE_VERSION' completed"
