#!/bin/bash
# Quick script to reload storage fixtures for Cypress/manual testing
# Usage: ./load-storage-fixtures.sh
# 
# This script is a convenience wrapper that calls the unified fixture loader.
# For direct usage, use: src/test/resources/load-test-fixtures.sh

cd "$(dirname "$0")/../../.."
bash src/test/resources/load-test-fixtures.sh




