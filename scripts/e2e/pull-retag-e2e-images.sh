#!/usr/bin/env bash
# Pull GHCR-cached images and retag to local compose image names (e2e image map artifact).
#
# Usage: E2E_IMAGE_MAP_FILE=/path/to/e2e-image-map[-base].txt ./pull-retag-e2e-images.sh

set -euo pipefail

MAP_FILE="${E2E_IMAGE_MAP_FILE:?Set E2E_IMAGE_MAP_FILE to the image map txt path}"

xargs -r -I{} -P4 bash -lc '
  IFS="|" read -r source_image ghcr_image <<< "$1"
  [ -z "$source_image" ] && exit 0
  docker pull "$ghcr_image" >/dev/null
  docker tag "$ghcr_image" "$source_image"
' _ {} < "$MAP_FILE"
