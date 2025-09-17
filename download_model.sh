#!/usr/bin/env bash
set -e
MODEL="${1:-Qwen3-4B-Q4_K_M.gguf}"
OUTDIR="$PWD/app/src/main/assets"
mkdir -p "$OUTDIR"
if [ -z "$HUGGINGFACE_TOKEN" ]; then
  echo "HUGGINGFACE_TOKEN not set. Place model manually or set env var and rerun."
  echo "Expected model filename: $MODEL"
  exit 1
fi
URL="https://huggingface.co/unsloth/Qwen3-4B-GGUF/resolve/main/$MODEL"
echo "Downloading $URL to $OUTDIR"
curl -L -H "Authorization: Bearer $HUGGINGFACE_TOKEN" -o "$OUTDIR/$MODEL" "$URL"
echo "Downloaded to $OUTDIR/$MODEL"
