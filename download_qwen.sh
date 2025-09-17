#!/usr/bin/env bash
set -e
MODEL="${1:-Qwen3-4B-Q4_K_M.gguf}"
OUT="app/src/main/assets/$MODEL"
if [ -z "${HUGGINGFACE_TOKEN:-}" ]; then
  echo "Set HUGGINGFACE_TOKEN env var. Exiting."
  exit 1
fi
URL="https://huggingface.co/unsloth/Qwen3-4B-GGUF/resolve/main/$MODEL"
echo "Downloading $URL -> $OUT"
curl -L -H "Authorization: Bearer $HUGGINGFACE_TOKEN" -o "$OUT" "$URL"
echo "Done."
