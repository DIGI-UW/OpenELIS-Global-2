#!/usr/bin/env bash
# Extract production bundle (e.g. 0504.zip) and optionally align local dev trees.
# Snapshot layout seen: 0504/config (Bahmni default-config), 0504/lab (legacy OpenELIS WAR), 0504/dist (Bahmni clinical assets).
set -euo pipefail

WORKTREE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ZIP="${1:-${HOME}/Downloads/0504.zip}"
DEST="${WORKTREE}/.dev-mirror"
SNAP="${DEST}/0504"

usage() {
  cat <<'EOF'
Usage:
  sync-prod-snapshot.sh [path/to/0504.zip]

  Environment (optional):
    ORBIT_CONFIG   If set, rsync 0504/config/openmrs i18n → this Bahmni config dir (dry-run unless APPLY=1).
                   Example: ORBIT_CONFIG=$HOME/orbit-stpeter/default-config-stpeter

  Flags (second arg or env):
    APPLY_BAHMNI=1     With ORBIT_CONFIG set, copy openmrs i18n from snapshot into that tree.
    APPLY_OE_PROPS=1   Backup and copy legacy SystemConfiguration.properties from 0504/lab into
                       volume/properties/SystemConfiguration.properties (OpenELIS Global dev compose).

Without flags: only extracts the zip and prints paths.
EOF
}

[[ "${1:-}" == "-h" || "${1:-}" == "--help" ]] && { usage; exit 0; }

if [[ ! -f "${ZIP}" ]]; then
  echo "Missing zip: ${ZIP}" >&2
  exit 1
fi

mkdir -p "${DEST}"
echo "Extracting ${ZIP} → ${DEST} ..."
unzip -q -o "${ZIP}" -d "${DEST}"

if [[ ! -d "${SNAP}" ]]; then
  echo "Expected folder ${SNAP} not found after unzip (check zip top-level name)." >&2
  exit 1
fi

echo "OK: snapshot at ${SNAP}"

echo ""
echo "Contents (high level):"
echo "  Bahmni-style config:     ${SNAP}/config/"
echo "  Legacy OpenELIS (WAR):   ${SNAP}/lab/"
echo "  Bahmni clinical dist:    ${SNAP}/dist/"
echo ""

if [[ -n "${ORBIT_CONFIG:-}" && "${APPLY_BAHMNI:-0}" == "1" ]]; then
  src="${SNAP}/config/openmrs/i18n"
  dst="${ORBIT_CONFIG}/openmrs/i18n"
  if [[ ! -d "$src" ]]; then
    echo "No i18n in snapshot: $src" >&2
  else
    mkdir -p "$dst"
    echo "Applying i18n → ${dst} (rsync) ..."
    rsync -a --delete-delay "${src}/" "${dst}/"
  fi
fi

if [[ "${APPLY_OE_PROPS:-0}" == "1" ]]; then
  prop="${SNAP}/lab/WEB-INF/classes/SystemConfiguration.properties"
  target="${WORKTREE}/volume/properties/SystemConfiguration.properties"
  if [[ ! -f "$prop" ]]; then
    echo "No legacy properties in snapshot: $prop" >&2
  else
    mkdir -p "$(dirname "$target")"
    if [[ -f "$target" ]]; then
      cp -a "${target}" "${target}.bak.$(date +%Y%m%d%H%M%S)"
      echo "Backed up existing ${target}"
    fi
    cp -a "$prop" "$target"
    echo "Copied production SystemConfiguration.properties → ${target}"
    echo "Restart dev OpenELIS (oe.openelis.org) to pick this up."
  fi
fi

echo ""
echo "Next steps:"
echo "  - OpenELIS Global 2 (this worktree): compare ${SNAP}/lab/WEB-INF/classes/*.properties with"
echo "    volume/properties/ — many keys differ from legacy; use APPLY_OE_PROPS only if intentional."
echo "  - Bahmni (St Peter): set ORBIT_CONFIG and APPLY_BAHMNI=1 to copy i18n only; full default-config"
echo "    mirroring is best done as a git merge from a branch that tracks production."
echo "  - Reference legacy WAR tree for diffs: ${SNAP}/lab/"
