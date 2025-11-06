#!/bin/bash
# Quick script to reload storage fixtures for Cypress/manual testing
# Usage: ./load-storage-fixtures.sh

cd "$(dirname "$0")/../../.."
docker exec -i openelisglobal-database psql -U clinlims -d clinlims < src/test/resources/storage-test-data.sql
echo "✅ Storage fixtures loaded"




