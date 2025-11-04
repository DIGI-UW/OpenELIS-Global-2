-- Test Data for Storage Management Integration Testing
-- Run this script to populate test storage hierarchy for testing P1 user story
-- Usage: psql -U clinlims -d clinlims -f storage-test-data.sql

-- Clean up existing test data (if any)
DELETE FROM sample_storage_movement WHERE sample_id IN (SELECT id FROM sample WHERE accession_number LIKE 'TEST-%');
DELETE FROM sample_storage_assignment WHERE sample_id IN (SELECT id FROM sample WHERE accession_number LIKE 'TEST-%');
DELETE FROM storage_position WHERE id BETWEEN 100 AND 10000;
DELETE FROM storage_rack WHERE id BETWEEN 30 AND 100;
DELETE FROM storage_shelf WHERE id BETWEEN 20 AND 100;
DELETE FROM storage_device WHERE id BETWEEN 10 AND 100;
DELETE FROM storage_room WHERE id BETWEEN 1 AND 100;

-- Insert Test Rooms
INSERT INTO storage_room (id, fhir_uuid, name, code, description, active, sys_user_id, last_updated) VALUES
(1, gen_random_uuid(), 'Main Laboratory', 'MAIN', 'Primary laboratory storage facility', true, 1, CURRENT_TIMESTAMP),
(2, gen_random_uuid(), 'Secondary Laboratory', 'SEC', 'Secondary storage area', true, 1, CURRENT_TIMESTAMP),
(3, gen_random_uuid(), 'Inactive Room', 'INACTIVE', 'Deactivated room for testing inactive validation', false, 1, CURRENT_TIMESTAMP);

-- Insert Test Devices
INSERT INTO storage_device (id, fhir_uuid, name, code, type, temperature_setting, capacity_limit, active, parent_room_id, sys_user_id, last_updated) VALUES
(10, gen_random_uuid(), 'Freezer Unit 1', 'FRZ01', 'freezer', -80.0, 500, true, 1, 1, CURRENT_TIMESTAMP),
(11, gen_random_uuid(), 'Refrigerator Unit 1', 'REF01', 'refrigerator', 4.0, 300, true, 1, 1, CURRENT_TIMESTAMP),
(12, gen_random_uuid(), 'Cabinet Unit 1', 'CAB01', 'cabinet', NULL, NULL, true, 2, 1, CURRENT_TIMESTAMP),
(13, gen_random_uuid(), 'Inactive Freezer', 'INACTIVE-FRZ', 'freezer', NULL, NULL, false, 3, 1, CURRENT_TIMESTAMP);

-- Insert Test Shelves
INSERT INTO storage_shelf (id, fhir_uuid, label, capacity_limit, active, parent_device_id, sys_user_id, last_updated) VALUES
(20, gen_random_uuid(), 'Shelf-A', 50, true, 10, 1, CURRENT_TIMESTAMP),
(21, gen_random_uuid(), 'Shelf-B', 50, true, 10, 1, CURRENT_TIMESTAMP),
(22, gen_random_uuid(), 'Shelf-1', NULL, true, 11, 1, CURRENT_TIMESTAMP),
(23, gen_random_uuid(), 'Shelf-1', NULL, true, 12, 1, CURRENT_TIMESTAMP);

-- Insert Test Racks
INSERT INTO storage_rack (id, fhir_uuid, label, rows, columns, position_schema_hint, active, parent_shelf_id, sys_user_id, last_updated) VALUES
(30, gen_random_uuid(), 'Rack R1', 8, 12, 'A1', true, 20, 1, CURRENT_TIMESTAMP),
(31, gen_random_uuid(), 'Rack R2', 10, 10, '1-1', true, 20, 1, CURRENT_TIMESTAMP),
(32, gen_random_uuid(), 'Rack R3', 0, 0, NULL, true, 21, 1, CURRENT_TIMESTAMP),
(33, gen_random_uuid(), 'Rack R1', 8, 12, NULL, true, 22, 1, CURRENT_TIMESTAMP);

-- Insert Test Positions
INSERT INTO storage_position (id, fhir_uuid, coordinate, row_index, column_index, occupied, parent_rack_id, sys_user_id, last_updated) VALUES
-- Rack R1 (8x12 grid) - First row
(100, gen_random_uuid(), 'A1', 1, 1, false, 30, 1, CURRENT_TIMESTAMP),
(101, gen_random_uuid(), 'A2', 1, 2, false, 30, 1, CURRENT_TIMESTAMP),
(102, gen_random_uuid(), 'A3', 1, 3, true, 30, 1, CURRENT_TIMESTAMP), -- Occupied for testing
(103, gen_random_uuid(), 'A4', 1, 4, false, 30, 1, CURRENT_TIMESTAMP),
(104, gen_random_uuid(), 'A5', 1, 5, false, 30, 1, CURRENT_TIMESTAMP),
(105, gen_random_uuid(), 'A6', 1, 6, false, 30, 1, CURRENT_TIMESTAMP),
(106, gen_random_uuid(), 'A7', 1, 7, false, 30, 1, CURRENT_TIMESTAMP),
(107, gen_random_uuid(), 'A8', 1, 8, false, 30, 1, CURRENT_TIMESTAMP),

-- Rack R2 - First position
(200, gen_random_uuid(), '1-1', 1, 1, false, 31, 1, CURRENT_TIMESTAMP),

-- Rack R3 (no grid) - flexible positions
(110, gen_random_uuid(), 'RED-01', NULL, NULL, false, 32, 1, CURRENT_TIMESTAMP),
(111, gen_random_uuid(), 'RED-02', NULL, NULL, false, 32, 1, CURRENT_TIMESTAMP),
(112, gen_random_uuid(), 'RED-01', NULL, NULL, false, 32, 1, CURRENT_TIMESTAMP), -- Duplicate coordinate (allowed)

-- Position in inactive location
(120, gen_random_uuid(), 'X1', NULL, NULL, false, 33, 1, CURRENT_TIMESTAMP);

-- Add more positions to Rack R2 for capacity testing (80 occupied out of 100 = 80%)
INSERT INTO storage_position (id, fhir_uuid, coordinate, row_index, column_index, occupied, parent_rack_id, sys_user_id, last_updated)
SELECT 
    200 + (row_num - 1) * 10 + col_num,
    gen_random_uuid(),
    row_num || '-' || col_num,
    row_num,
    col_num,
    CASE WHEN ((row_num - 1) * 10 + col_num) <= 80 THEN true ELSE false END,
    31,
    1,
    CURRENT_TIMESTAMP
FROM generate_series(1, 10) AS row_num
CROSS JOIN generate_series(2, 10) AS col_num
WHERE ((row_num - 1) * 10 + col_num) <= 99;

-- Update sequences to avoid conflicts with test data
SELECT setval('storage_room_seq', 1000, false);
SELECT setval('storage_device_seq', 1000, false);
SELECT setval('storage_shelf_seq', 1000, false);
SELECT setval('storage_rack_seq', 1000, false);
SELECT setval('storage_position_seq', 10000, false);
SELECT setval('sample_storage_assignment_seq', 10000, false);
SELECT setval('sample_storage_movement_seq', 10000, false);

-- Verification queries
\echo 'Test Data Summary:'
SELECT 'Rooms' AS entity, COUNT(*) AS count FROM storage_room
UNION ALL
SELECT 'Devices', COUNT(*) FROM storage_device
UNION ALL
SELECT 'Shelves', COUNT(*) FROM storage_shelf
UNION ALL
SELECT 'Racks', COUNT(*) FROM storage_rack
UNION ALL
SELECT 'Positions', COUNT(*) FROM storage_position;

\echo ''
\echo 'Sample Hierarchy:'
SELECT 
    r.code AS room_code,
    d.code AS device_code,
    s.label AS shelf_label,
    k.label AS rack_label,
    COUNT(p.id) AS position_count,
    SUM(CASE WHEN p.occupied THEN 1 ELSE 0 END) AS occupied_count
FROM storage_room r
LEFT JOIN storage_device d ON d.parent_room_id = r.id
LEFT JOIN storage_shelf s ON s.parent_device_id = d.id
LEFT JOIN storage_rack k ON k.parent_shelf_id = s.id
LEFT JOIN storage_position p ON p.parent_rack_id = k.id
GROUP BY r.code, d.code, s.label, k.label
ORDER BY r.code, d.code, s.label, k.label;

