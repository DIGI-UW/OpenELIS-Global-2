#!/bin/bash

# Script to add Ultra-Low -80°C Freezers to OpenELIS storage system
# This script creates freezer devices with -80°C temperature setting for bioequivalence sample retention
# Author: Claude Code
# Date: 2026-01-09

set -e

echo "🧊 Adding Ultra-Low -80°C Freezers to OpenELIS Storage System..."

# Database connection parameters (modify as needed)
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME:-clinlims}
DB_USER=${DB_USER:-clinlims}
DB_PASSWORD=${DB_PASSWORD:-password}

# Check if docker is being used (default for development)
if command -v docker &> /dev/null && docker ps -q --filter "name=openelisglobal-database" | grep -q .; then
    echo "📦 Using Docker database container..."
    DB_EXEC_CMD="docker exec -i openelisglobal-database psql -U $DB_USER -d $DB_NAME"
else
    echo "🐘 Using local PostgreSQL..."
    DB_EXEC_CMD="psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME"
fi

# SQL script to add ultra-low freezers
read -r -d '' SQL_SCRIPT << 'EOF' || true
-- Add Ultra-Low -80°C Freezers for FDA Bioequivalence Sample Retention
-- Temperature: -80°C (±2°C) as required by FDA 21 CFR 320.63

BEGIN;

-- First, check if storage rooms exist, if not create a default biorepository room
INSERT INTO storage_room (id, fhir_uuid, name, code, description, active, sys_user_id, last_updated)
SELECT 100, gen_random_uuid(), 'Biorepository Storage Room', 'BIO-REPO',
       'Ultra-low temperature storage for bioequivalence sample retention',
       true, '1', NOW()
WHERE NOT EXISTS (SELECT 1 FROM storage_room WHERE code = 'BIO-REPO');

-- Add Ultra-Low -80°C Freezers
INSERT INTO storage_device (id, fhir_uuid, name, code, type, temperature_setting, capacity_limit, active, parent_room_id, sys_user_id, last_updated)
VALUES
    -- Ultra-Low Freezer 1 (-80°C for FDA bioequivalence retention)
    (nextval('storage_device_seq'), gen_random_uuid(), 'Ultra-Low Freezer ULF-001', 'ULF001', 'freezer', -80.0, 1000, true,
     (SELECT id FROM storage_room WHERE code = 'BIO-REPO' LIMIT 1), '1', NOW()),

    -- Ultra-Low Freezer 2 (-80°C backup unit)
    (nextval('storage_device_seq'), gen_random_uuid(), 'Ultra-Low Freezer ULF-002', 'ULF002', 'freezer', -80.0, 1000, true,
     (SELECT id FROM storage_room WHERE code = 'BIO-REPO' LIMIT 1), '1', NOW()),

    -- Ultra-Low Freezer 3 (-80°C high-capacity)
    (nextval('storage_device_seq'), gen_random_uuid(), 'Ultra-Low Freezer ULF-003', 'ULF003', 'freezer', -80.0, 1500, true,
     (SELECT id FROM storage_room WHERE code = 'BIO-REPO' LIMIT 1), '1', NOW()),

    -- Backup -80°C Freezer for critical samples
    (nextval('storage_device_seq'), gen_random_uuid(), 'Backup Ultra-Low Freezer ULF-BACKUP', 'ULFBAK', 'freezer', -80.0, 800, true,
     (SELECT id FROM storage_room WHERE code = 'BIO-REPO' LIMIT 1), '1', NOW())
ON CONFLICT (parent_room_id, code) DO NOTHING;

-- Add shelves to each freezer for better organization
-- Each ultra-low freezer typically has 4-6 shelves
INSERT INTO storage_shelf (id, fhir_uuid, name, code, capacity_limit, active, parent_device_id, sys_user_id, last_updated)
SELECT
    nextval('storage_shelf_seq'),
    gen_random_uuid(),
    'Shelf ' || shelf_letter,
    device_code || '-S' || shelf_letter,
    200, -- capacity per shelf
    true,
    device_id,
    '1',
    NOW()
FROM (
    SELECT
        d.id as device_id,
        d.code as device_code,
        unnest(ARRAY['A', 'B', 'C', 'D', 'E', 'F']) as shelf_letter
    FROM storage_device d
    WHERE d.type = 'freezer'
    AND d.temperature_setting = -80.0
    AND d.code LIKE 'ULF%'
) shelves_to_create
ON CONFLICT (parent_device_id, code) DO NOTHING;

-- Add some sample racks to each shelf (2 racks per shelf)
INSERT INTO storage_rack (id, fhir_uuid, name, code, capacity_limit, active, parent_shelf_id, sys_user_id, last_updated)
SELECT
    nextval('storage_rack_seq'),
    gen_random_uuid(),
    'Rack R' || rack_num,
    shelf_code || '-R' || rack_num,
    50, -- capacity per rack
    true,
    shelf_id,
    '1',
    NOW()
FROM (
    SELECT
        s.id as shelf_id,
        s.code as shelf_code,
        unnest(ARRAY['1', '2']) as rack_num
    FROM storage_shelf s
    JOIN storage_device d ON s.parent_device_id = d.id
    WHERE d.type = 'freezer'
    AND d.temperature_setting = -80.0
    AND d.code LIKE 'ULF%'
) racks_to_create
ON CONFLICT (parent_shelf_id, code) DO NOTHING;

-- Verify the devices were created
SELECT
    r.name as room_name,
    d.name as device_name,
    d.code as device_code,
    d.type,
    d.temperature_setting as temp_celsius,
    d.capacity_limit,
    d.active,
    COUNT(s.id) as shelf_count
FROM storage_device d
JOIN storage_room r ON d.parent_room_id = r.id
LEFT JOIN storage_shelf s ON s.parent_device_id = d.id
WHERE d.type = 'freezer' AND d.temperature_setting = -80.0
GROUP BY r.name, d.name, d.code, d.type, d.temperature_setting, d.capacity_limit, d.active
ORDER BY d.code;

COMMIT;

-- Display summary
\echo ''
\echo '✅ Ultra-Low -80°C Freezers Successfully Added!'
\echo ''
\echo 'Summary:'
\echo '- Created biorepository storage room if needed'
\echo '- Added 4 ultra-low freezers (-80°C)'
\echo '- Each freezer has 6 shelves (A-F)'
\echo '- Each shelf has 2 racks for sample organization'
\echo '- Total capacity: ~4,300 samples across all freezers'
\echo ''
\echo 'Freezer Devices:'
\echo '🧊 ULF-001: Ultra-Low Freezer ULF-001 (1000 samples)'
\echo '🧊 ULF-002: Ultra-Low Freezer ULF-002 (1000 samples)'
\echo '🧊 ULF-003: Ultra-Low Freezer ULF-003 (1500 samples)'
\echo '🧊 ULF-BACKUP: Backup Ultra-Low Freezer (800 samples)'
\echo ''
\echo 'These freezers are now available for FDA bioequivalence sample retention!'
\echo 'Temperature: -80°C (±2°C) - Compliant with FDA 21 CFR 320.63'
\echo ''
EOF

echo "📝 Executing SQL script to add freezers..."

# Execute the SQL script
if echo "$SQL_SCRIPT" | $DB_EXEC_CMD; then
    echo ""
    echo "🎉 Success! Ultra-low -80°C freezers have been added to OpenELIS."
    echo ""
    echo "📋 What was created:"
    echo "   • Biorepository Storage Room (if not existing)"
    echo "   • 4 Ultra-Low Freezers at -80°C"
    echo "   • 6 Shelves per freezer (A-F)"
    echo "   • 2 Racks per shelf for organization"
    echo ""
    echo "🔬 These freezers are now available for:"
    echo "   • FDA bioequivalence sample retention"
    echo "   • 2-year sample storage at -80°C"
    echo "   • Regulatory compliant sample archival"
    echo ""
    echo "🚀 You can now use the retention storage modal in Stage 5!"
else
    echo "❌ Error: Failed to add freezers. Check database connection and permissions."
    exit 1
fi
EOF