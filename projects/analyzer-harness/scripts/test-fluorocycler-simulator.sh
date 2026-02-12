#!/bin/bash
# Test FluoroCyclerXT with simulator-generated data
#
# This script generates a test CSV file using the FluoroCycler XT template
# and places it in the watch directory for file-based import testing.
#
# Usage:
#   ./scripts/test-fluorocycler-simulator.sh [COUNT] [WATCH_DIR]
#
# Arguments:
#   COUNT     - Number of test samples to generate (default: 5)
#   WATCH_DIR - Directory to place generated file (default: /var/lib/openelis-global/analyzer-files/fluorocycler)

set -e

# Configuration
COUNT="${1:-5}"
WATCH_DIR="${2:-/var/lib/openelis-global/analyzer-files/fluorocycler}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
TIMESTAMP="$(date +%Y%m%d%H%M%S)"
OUTPUT_FILE="$WATCH_DIR/fluorocycler-sim-$TIMESTAMP.csv"

echo "================================================================"
echo "  FluoroCycler XT Simulator Test"
echo "================================================================"
echo "  Samples: $COUNT"
echo "  Watch Directory: $WATCH_DIR"
echo "  Output File: $(basename "$OUTPUT_FILE")"
echo "================================================================"
echo

# Ensure watch directory exists
if [ ! -d "$WATCH_DIR" ]; then
    echo "Creating watch directory: $WATCH_DIR"
    mkdir -p "$WATCH_DIR"
fi

# Navigate to simulator directory
cd "$SCRIPT_DIR/../tools/analyzer-mock-server"

# Generate test file
echo "Generating test file..."
python3 generate_file.py \
  --template hain_fluorocycler \
  --output "$OUTPUT_FILE" \
  --count "$COUNT"

if [ $? -eq 0 ]; then
    echo
    echo "================================================================"
    echo "  File Generated Successfully"
    echo "================================================================"
    echo "  Location: $OUTPUT_FILE"
    echo "  Size: $(wc -c < "$OUTPUT_FILE") bytes"
    echo "  Lines: $(wc -l < "$OUTPUT_FILE")"
    echo "================================================================"
    echo
    echo "Preview of generated file:"
    echo "----------------------------------------------------------------"
    head -n 10 "$OUTPUT_FILE"
    echo "----------------------------------------------------------------"
    echo
    echo "✓ Test file placed in watch directory"
    echo "  The FileImportWatchService will detect and process it"
    echo "  within ~60 seconds (polling interval)"
    echo
    echo "To monitor processing:"
    echo "  tail -f /var/log/openelis/openelis.log | grep -i fluorocycler"
    echo
else
    echo "✗ Failed to generate test file" >&2
    exit 1
fi
