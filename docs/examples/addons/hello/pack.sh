#!/usr/bin/env bash
# Builds an add-on package, optionally signed, with nothing but openssl (3.0+),
# sha256sum and zip (or python3).
#
#   ./pack.sh pack <out.zip>                 -> unsigned package (installs as "Unverified publisher")
#   ./pack.sh keygen <publisher>             -> <publisher>.pem (keep it secret) + the line a lab
#                                                adds to addons.trustedPublishers
#   ./pack.sh pack <out.zip> <publisher>     -> package signed with <publisher>.pem
#                                                (installs as "Verified publisher: <publisher>")
#
# The signature covers the sha256sum listing of every file in the package (sorted by
# name). It only says who published the package: changing any file after signing makes
# OpenELIS show "Signature does not match the contents".
set -euo pipefail
cd "$(dirname "$0")"

pubkey_raw_b64() { openssl pkey -in "$1" -pubout -outform DER | tail -c 32 | base64 -w0; }

case "${1:-}" in
keygen)
  pub="${2:?publisher name}"
  [ -e "$pub.pem" ] && { echo "$pub.pem exists" >&2; exit 1; }
  openssl genpkey -algorithm ed25519 -out "$pub.pem"
  chmod 600 "$pub.pem"
  echo "addons.trustedPublishers=$pub=$(pubkey_raw_b64 "$pub.pem")"
  ;;
pack)
  out="$(realpath -m "${2:?output zip}")"; pub="${3:-}"
  key=""; [ -n "$pub" ] && key="$(realpath "$pub.pem")"
  work="$(mktemp -d)"; trap 'rm -rf "$work"' EXIT
  mkdir "$work/pkg"; cp addon.json "$work/pkg/"; [ -d ui ] && cp -r ui "$work/pkg/"
  (
    cd "$work/pkg"
    if [ -n "$key" ]; then
      find . -type f | sed 's|^\./||' | LC_ALL=C sort | xargs -d '\n' sha256sum > ../listing.txt
      openssl pkeyutl -sign -rawin -inkey "$key" -in ../listing.txt -out ../sig.bin
      key_id="$(openssl pkey -in "$key" -pubout -outform DER | tail -c 32 | sha256sum | cut -c1-16)"
      printf '{"alg":"ed25519","publisher":"%s","keyId":"%s","signature":"%s"}\n' \
        "$pub" "$key_id" "$(base64 -w0 ../sig.bin)" > SIGNATURE.json
    fi
    rm -f "$out"
    if command -v zip >/dev/null; then
      zip -q -X -r "$out" ./*
    else
      python3 -m zipfile -c "$out" ./*
    fi
  )
  echo "$out"
  ;;
*)
  sed -n '2,14p' "$0"; exit 1
  ;;
esac
