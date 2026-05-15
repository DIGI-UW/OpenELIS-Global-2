#!/usr/bin/env bash
# Rebuilds a pathology diagnostic PDF from PathologyDiagnosticReportServiceImpl (no DB),
# writes target/pathology-diagnostic-sanity.pdf, and sanity-checks extracted text.
# This matches the active preview/print backend path (same class as /rest/notebook/pathology/report/diagnostic-pdf).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
mvn -q test -Dtest=PathologyDiagnosticReportServiceImplPdfTest
PDF="$ROOT/target/pathology-diagnostic-sanity.pdf"
test -s "$PDF"
TEXT="$(mktemp)"
pdftotext "$PDF" "$TEXT"
if grep -q "This is a computer-generated document." "$TEXT"; then
  echo "FAIL: legacy footer disclaimer still present" >&2
  exit 1
fi
if grep -q "2026-05-13T21:00:00.000Z" "$TEXT"; then
  echo "FAIL: raw ISO timestamp still in PDF text" >&2
  exit 1
fi
grep -q "Dr. Verifier" "$TEXT" || { echo "FAIL: expected pathologist name missing" >&2; exit 1; }
grep -q "Procedure Date" "$TEXT" || { echo "FAIL: specimen header missing" >&2; exit 1; }
echo "OK — PDF text checks passed. Sample extract:"
head -n 25 "$TEXT"
