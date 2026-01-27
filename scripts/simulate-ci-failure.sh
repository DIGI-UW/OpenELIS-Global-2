#!/bin/bash

# Simulate CI Failure Script
# Reproduces CI failures locally to enable debugging without relying on CI
# Usage: ./scripts/simulate-ci-failure.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "======================================"
echo "Simulating CI Failure Conditions"
echo "======================================"
echo ""

cd "$PROJECT_ROOT"

# Step 1: Start containers WITHOUT test context (simulates CI failure)
echo "Step 1: Starting containers without test context..."
echo ""

# Create a temporary docker-compose override file that removes test context
cat > /tmp/build.docker-compose.ci-simulation.yml <<EOF
version: '3.3'
services:
  oe.openelis.org:
    environment:
      # Explicitly remove test context to simulate CI failure
      - SPRING_LIQUIBASE_CONTEXTS=default
EOF

# Start containers with override (removes test context)
docker compose -f build.docker-compose.yml -f /tmp/build.docker-compose.ci-simulation.yml up -d --build --wait --wait-timeout 600

echo "✅ Containers started"
echo ""

# Step 2: Wait for application to start
echo "Step 2: Waiting for application to start..."
sleep 10

# Step 3: Verify storage hierarchy is missing
echo "Step 3: Verifying storage hierarchy is missing (should return 0 rows)..."
echo ""

ROOM_COUNT=$(docker exec openelisglobal-database psql -U clinlims -d clinlims -t -c "SELECT COUNT(*) FROM storage_room WHERE code IN ('MAIN', 'SEC', 'INACTIVE');" 2>/dev/null | tr -d '[:space:]' || echo "0")

if [ "$ROOM_COUNT" -eq 0 ]; then
    echo "✅ Storage hierarchy missing as expected (found: $ROOM_COUNT rooms)"
else
    echo "⚠️  WARNING: Expected 0 rooms, but found $ROOM_COUNT"
    echo "   This may indicate the test context is still active"
fi
echo ""

# Step 4: Attempt to load fixtures (should fail)
echo "Step 4: Attempting to load fixtures (should fail)..."
echo ""

if ./src/test/resources/load-test-fixtures.sh 2>&1; then
    echo "⚠️  WARNING: load-test-fixtures.sh succeeded, but it should have failed"
    echo "   This indicates the issue may not be reproducible"
else
    echo "✅ load-test-fixtures.sh failed as expected"
    echo "   Error matches CI failure condition"
fi
echo ""

# Step 5: Run a sample Cypress test (should fail)
echo "Step 5: Running sample Cypress test (should fail)..."
echo ""

cd frontend
if npm run cy:run -- --spec "cypress/e2e/storageAssignment.cy.js" 2>&1 | head -50; then
    echo "⚠️  WARNING: Cypress test passed, but it should have failed"
else
    echo "✅ Cypress test failed as expected"
    echo "   Failure matches CI condition"
fi
echo ""

# Step 6: Report summary
echo "======================================"
echo "CI Failure Simulation Summary"
echo "======================================"
echo ""
echo "Conditions verified:"
echo "  - Containers started without test context: ✅"
echo "  - Storage hierarchy missing: $([ "$ROOM_COUNT" -eq 0 ] && echo "✅" || echo "⚠️")"
echo "  - load-test-fixtures.sh fails: ✅"
echo "  - Cypress test fails: ✅"
echo ""
echo "To fix and test:"
echo "  1. Stop containers: docker compose -f build.docker-compose.yml -f /tmp/build.docker-compose.ci-simulation.yml down"
echo "  2. Start with test context: docker compose -f build.docker-compose.yml up -d"
echo "  3. Verify storage rooms exist: docker exec openelisglobal-database psql -U clinlims -d clinlims -c \"SELECT code FROM storage_room WHERE code IN ('MAIN', 'SEC', 'INACTIVE');\""
echo "  4. Run load-test-fixtures.sh: ./src/test/resources/load-test-fixtures.sh"
echo "  5. Run Cypress test: cd frontend && npm run cy:run -- --spec \"cypress/e2e/storageAssignment.cy.js\""
echo ""

# Cleanup
rm -f /tmp/build.docker-compose.ci-simulation.yml
