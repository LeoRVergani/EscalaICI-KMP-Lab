#!/usr/bin/env sh
# Roda dentro de `firebase emulators:exec` (cwd = firebase/). Semeia o
# Emulator e executa o teste de integracao Kotlin real contra ele.
set -eu
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)

sh "$SCRIPT_DIR/seed-firebase-emulator.sh"

cd "$REPO_ROOT"
export ESCALAICI_FIREBASE_EMULATOR=true
./gradlew :composeApp:testDebugUnitTest --tests "*FirebaseIntegrationTest*" --console=plain --rerun
