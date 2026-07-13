#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
DIST_DIR="$PROJECT_ROOT/composeApp/build/dist/wasmJs/productionExecutable"
OUTPUT_DIR="$PROJECT_ROOT/cloudflare-dist"
CLOUDFLARE_SOURCE_DIR="$PROJECT_ROOT/web/cloudflare"

cd "$PROJECT_ROOT"

if [ ! -f settings.gradle.kts ] || [ ! -d composeApp ] || [ ! -x ./gradlew ]; then
  echo "Raiz do projeto ou Gradle Wrapper inválido." >&2
  exit 1
fi

./gradlew :composeApp:wasmJsTest
./gradlew :composeApp:composeCompatibilityBrowserDistribution

if [ ! -d "$DIST_DIR" ]; then
  echo "Distribuição Web não encontrada: $DIST_DIR" >&2
  exit 1
fi

rm -rf "$OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"
cp -R "$DIST_DIR"/. "$OUTPUT_DIR"/

for cloudflare_file in _headers _redirects; do
  if [ -f "$CLOUDFLARE_SOURCE_DIR/$cloudflare_file" ]; then
    cp "$CLOUDFLARE_SOURCE_DIR/$cloudflare_file" "$OUTPUT_DIR/$cloudflare_file"
  fi
done

if [ ! -f "$OUTPUT_DIR/index.html" ]; then
  echo "cloudflare-dist/index.html não foi gerado." >&2
  exit 1
fi
if [ ! -f "$OUTPUT_DIR/composeApp.js" ]; then
  echo "cloudflare-dist/composeApp.js não foi gerado." >&2
  exit 1
fi
if ! find "$OUTPUT_DIR" -type f -name '*.wasm' -print -quit | grep -q .; then
  echo "Nenhum arquivo Wasm foi gerado em cloudflare-dist." >&2
  exit 1
fi

echo "Distribuição WEB_WASM pronta em: $OUTPUT_DIR"
find "$OUTPUT_DIR" -type f -print
