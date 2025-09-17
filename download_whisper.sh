#!/usr/bin/env bash
set -e
MODEL="${1:-ggml-base.en-q5_1.bin}"
OUT="app/src/main/assets/$MODEL"
URL="https://huggingface.co/ggerganov/whisper.cpp/resolve/main/$MODEL"
echo "Downloading $URL -> $OUT"
curl -L -o "$OUT" "$URL"
echo "Done."
