-- ============================================================================
-- FREEZER MONITORING DEMO DATA - Comprehensive Cold Storage Dashboard
-- ============================================================================
-- This SQL file creates realistic demo data for the freezer monitoring feature
-- including: 5 devices, threshold profiles, temperature readings, alerts,
-- corrective actions, and historical data for trending/reporting.
--
-- USAGE: Run this on a clean server after Liquibase migrations are complete
--        psql -U postgres -d clinlims -f freezer-monitoring-demo-data.sql
-- ============================================================================

BEGIN;

-- ============================================================================
-- SECTION 1: Storage Rooms and Devices (Parent Entities)
-- ============================================================================

-- Insert storage rooms (if they don't exist)
INSERT INTO clinlims.storage_room (id, fhir_uuid, code, name, description, active, sys_user_id, last_updated)
VALUES
    (1000, '550e8400-e29b-41d4-a716-446655440100', 'CSR-LABA', 'Laboratory A Cold Storage',
     'Main laboratory cold storage facility - Building A', true, '1', CURRENT_TIMESTAMP),
    (1001, '550e8400-e29b-41d4-a716-446655440101', 'CSR-LABB', 'Laboratory B Cold Storage',
     'Secondary laboratory cold storage - Building B', true, '1', CURRENT_TIMESTAMP),
    (1002, '550e8400-e29b-41d4-a716-446655440102', 'CSR-BB', 'Blood Bank Cold Storage',
     'Blood bank dedicated cold storage', true, '1', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Update sequence for storage_room
SELECT setval('clinlims.storage_room_seq', (SELECT MAX(id)::bigint FROM clinlims.storage_room), true);

-- Insert storage devices (5 devices)
INSERT INTO clinlims.storage_device (id, fhir_uuid, name, code, type, parent_room_id, active, sys_user_id, last_updated)
VALUES
    (2000, '650e8400-e29b-41d4-a716-446655440200', 'Ultra-Low Freezer A1', 'ULF-A1', 'freezer', 1000, true, '1', CURRENT_TIMESTAMP),
    (2001, '650e8400-e29b-41d4-a716-446655440201', 'Ultra-Low Freezer A2', 'ULF-A2', 'freezer', 1000, true, '1', CURRENT_TIMESTAMP),
    (2002, '650e8400-e29b-41d4-a716-446655440202', 'Standard Freezer B1', 'STD-B1', 'freezer', 1001, true, '1', CURRENT_TIMESTAMP),
    (2003, '650e8400-e29b-41d4-a716-446655440203', 'Refrigerator B2', 'REF-B2', 'refrigerator', 1001, true, '1', CURRENT_TIMESTAMP),
    (2004, '650e8400-e29b-41d4-a716-446655440204', 'Blood Bank Refrigerator', 'BB-REF-1', 'refrigerator', 1002, true, '1', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Update sequence for storage_device
SELECT setval('clinlims.storage_device_seq', (SELECT MAX(id)::bigint FROM clinlims.storage_device), true);

-- ============================================================================
-- SECTION 2: Freezer Devices (5 Devices with Varied Configurations)
-- ============================================================================

INSERT INTO clinlims.freezer (id, name, storage_device_id, protocol, host, port, serial_port,
                               baud_rate, data_bits, stop_bits, parity, slave_id,
                               temperature_register, humidity_register, temperature_scale,
                               temperature_offset, humidity_scale, humidity_offset,
                               target_temperature, polling_interval_seconds, active, last_updated)
VALUES
    -- Device 1: Ultra-Low Freezer (TCP, -80°C, with humidity monitoring)
    (3000, 'Ultra-Low Freezer A1', 2000, 'TCP', '192.168.10.101', 502, NULL,
     NULL, NULL, NULL, NULL, 1,
     40001, 40002, 0.1, 0.0, 0.1, 0.0,
     -80.0, 60, true, CURRENT_TIMESTAMP),

    -- Device 2: Ultra-Low Freezer (TCP, -80°C, backup unit)
    (3001, 'Ultra-Low Freezer A2', 2001, 'TCP', '192.168.10.102', 502, NULL,
     NULL, NULL, NULL, NULL, 2,
     40001, 40002, 0.1, 0.0, 0.1, 0.0,
     -80.0, 60, true, CURRENT_TIMESTAMP),

    -- Device 3: Standard Freezer (RTU Serial, -20°C)
    (3002, 'Standard Freezer B1', 2002, 'RTU', NULL, NULL, '/dev/ttyUSB0',
     9600, 8, 1, 'NONE', 3,
     0, 1, 0.1, 0.0, 0.1, 0.0,
     -20.0, 120, true, CURRENT_TIMESTAMP),

    -- Device 4: Refrigerator (TCP, 2-8°C, with humidity)
    (3003, 'Refrigerator B2', 2003, 'TCP', '192.168.10.104', 502, NULL,
     NULL, NULL, NULL, NULL, 4,
     40001, 40002, 0.1, 0.0, 0.1, 0.0,
     4.0, 90, true, CURRENT_TIMESTAMP),

    -- Device 5: Blood Bank Refrigerator (TCP, 2-6°C, critical storage)
    (3004, 'Blood Bank Refrigerator', 2004, 'TCP', '192.168.10.105', 502, NULL,
     NULL, NULL, NULL, NULL, 5,
     40001, 40002, 0.1, 0.0, 0.1, 0.0,
     4.0, 60, true, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Update sequence for freezer
SELECT setval('clinlims.freezer_seq', (SELECT MAX(id)::bigint FROM clinlims.freezer), true);

-- ============================================================================
-- SECTION 3: Threshold Profiles (Reusable Temperature/Humidity Rules)
-- ============================================================================

INSERT INTO clinlims.threshold_profile (id, name, description, warning_min, warning_max,
                                         critical_min, critical_max, min_excursion_minutes,
                                         max_duration_minutes, humidity_warning_min,
                                         humidity_warning_max, humidity_critical_min,
                                         humidity_critical_max, created_by, created_at, updated_at)
VALUES
    -- Profile 1: Ultra-Low Freezer (-80°C)
    (4000, 'Ultra-Low Freezer Standard',
     'Standard thresholds for -80°C ultra-low freezers',
     -85.0, -75.0,  -- Warning range: -85 to -75°C
     -90.0, -70.0,  -- Critical range: -90 to -70°C
     5, 15,         -- Min 5 min excursion, escalate after 15 min
     NULL, NULL, NULL, NULL,  -- No humidity thresholds
     1, CURRENT_TIMESTAMP, NULL),

    -- Profile 2: Standard Freezer (-20°C)
    (4001, 'Standard Freezer',
     'Standard thresholds for -20°C freezers',
     -25.0, -15.0,  -- Warning range: -25 to -15°C
     -30.0, -10.0,  -- Critical range: -30 to -10°C
     10, 30,        -- Min 10 min excursion, escalate after 30 min
     NULL, NULL, NULL, NULL,
     1, CURRENT_TIMESTAMP, NULL),

    -- Profile 3: Refrigerator (2-8°C) with humidity
    (4002, 'Standard Refrigerator 2-8°C',
     'Standard thresholds for 2-8°C refrigerators with humidity monitoring',
     1.0, 9.0,      -- Warning range: 1 to 9°C
     -1.0, 12.0,    -- Critical range: -1 to 12°C
     5, 20,         -- Min 5 min excursion, escalate after 20 min
     20.0, 80.0,    -- Humidity warning: 20-80%
     10.0, 90.0,    -- Humidity critical: 10-90%
     1, CURRENT_TIMESTAMP, NULL),

    -- Profile 4: Blood Bank Refrigerator (2-6°C, strict)
    (4003, 'Blood Bank Refrigerator',
     'Strict thresholds for blood bank refrigerators',
     1.5, 6.5,      -- Warning range: 1.5 to 6.5°C (tight tolerance)
     0.0, 8.0,      -- Critical range: 0 to 8°C
     3, 10,         -- Min 3 min excursion, escalate after 10 min
     NULL, NULL, NULL, NULL,
     1, CURRENT_TIMESTAMP, NULL),

    -- Profile 5: Ultra-Low Freezer (Summer - relaxed)
    (4004, 'Ultra-Low Freezer Summer',
     'Relaxed thresholds for ultra-low freezers during high ambient temperature (summer)',
     -83.0, -72.0,  -- Warning range: -83 to -72°C (slightly relaxed)
     -88.0, -68.0,  -- Critical range: -88 to -68°C
     8, 20,         -- Min 8 min excursion, escalate after 20 min
     NULL, NULL, NULL, NULL,
     1, CURRENT_TIMESTAMP, NULL)
ON CONFLICT (id) DO NOTHING;

-- Update sequence for threshold_profile
SELECT setval('clinlims.threshold_profile_seq', (SELECT MAX(id)::bigint FROM clinlims.threshold_profile), true);

-- ============================================================================
-- SECTION 4: Freezer-Threshold Profile Assignments (Time-Based Rules)
-- ============================================================================

INSERT INTO clinlims.freezer_threshold_profile (id, freezer_id, threshold_profile_id,
                                                 effective_start, effective_end, is_default)
VALUES
    -- Ultra-Low Freezer A1: Default profile (year-round)
    (5000, 3000, 4000, '2024-01-01 00:00:00+00', NULL, true),

    -- Ultra-Low Freezer A1: Summer profile (June-August)
    (5001, 3000, 4004, '2024-06-01 00:00:00+00', '2024-08-31 23:59:59+00', false),

    -- Ultra-Low Freezer A2: Default profile
    (5002, 3001, 4000, '2024-01-01 00:00:00+00', NULL, true),

    -- Standard Freezer B1: Standard freezer profile
    (5003, 3002, 4001, '2024-01-01 00:00:00+00', NULL, true),

    -- Refrigerator B2: Standard refrigerator profile
    (5004, 3003, 4002, '2024-01-01 00:00:00+00', NULL, true),

    -- Blood Bank Refrigerator: Strict blood bank profile
    (5005, 3004, 4003, '2024-01-01 00:00:00+00', NULL, true)
ON CONFLICT (id) DO NOTHING;

-- Update sequence for freezer_threshold_profile
SELECT setval('clinlims.freezer_threshold_profile_seq', (SELECT MAX(id)::bigint FROM clinlims.freezer_threshold_profile), true);

-- ============================================================================
-- SECTION 5: Temperature Readings (Historical Data for 7 Days)
-- ============================================================================
-- Generates realistic temperature and humidity readings for the past 7 days
-- Creates trends, anomalies, and excursion events for dashboard visualization
-- ============================================================================

-- Helper function to generate temperature readings with realistic variance
DO $$
DECLARE
    v_freezer_id BIGINT;
    v_target_temp DECIMAL;
    v_temp_variance DECIMAL := 2.0;  -- Random variance range
    v_current_temp DECIMAL;
    v_humidity DECIMAL;
    v_status VARCHAR(16);
    v_timestamp TIMESTAMP WITH TIME ZONE;
    v_day_offset INTEGER;
    v_hour INTEGER;
    v_minute INTEGER;
    v_reading_id BIGINT := 6000;
BEGIN
    -- Generate readings for each freezer for the past 7 days (every 5 minutes)

    -- Device 1: Ultra-Low Freezer A1 (-80°C) - Mostly NORMAL with one WARNING event
    v_freezer_id := 3000;
    v_target_temp := -80.0;
    FOR v_day_offset IN 0..6 LOOP
        FOR v_hour IN 0..23 LOOP
            FOR v_minute IN 0..59 BY 5 LOOP
                v_timestamp := CURRENT_TIMESTAMP - INTERVAL '1 day' * v_day_offset
                               - INTERVAL '1 hour' * v_hour
                               - INTERVAL '1 minute' * v_minute;

                -- Normal operation most of the time
                IF (v_day_offset = 2 AND v_hour BETWEEN 14 AND 15) THEN
                    -- Warning event on day 2 (2 hours)
                    v_current_temp := v_target_temp + (6.0 + random() * 2.0);  -- -74 to -72°C
                    v_status := 'WARNING';
                ELSIF (v_day_offset = 5 AND v_hour = 9 AND v_minute BETWEEN 0 AND 20) THEN
                    -- Brief critical event on day 5 (20 minutes)
                    v_current_temp := v_target_temp + (12.0 + random() * 3.0);  -- -68 to -65°C
                    v_status := 'CRITICAL';
                ELSE
                    -- Normal operation with small variance
                    v_current_temp := v_target_temp + (random() - 0.5) * 3.0;  -- ±1.5°C variance
                    v_status := 'NORMAL';
                END IF;

                v_humidity := 30.0 + random() * 20.0;  -- 30-50% humidity

                INSERT INTO clinlims.freezer_reading
                    (id, freezer_id, recorded_at, temperature_celsius, humidity_percentage, status, transmission_ok, last_updated)
                VALUES
                    (v_reading_id, v_freezer_id, v_timestamp, v_current_temp, v_humidity, v_status, true, v_timestamp);

                v_reading_id := v_reading_id + 1;
            END LOOP;
        END LOOP;
    END LOOP;

    -- Device 2: Ultra-Low Freezer A2 (-80°C) - NORMAL operation (backup unit)
    v_freezer_id := 3001;
    v_target_temp := -80.0;
    FOR v_day_offset IN 0..6 LOOP
        FOR v_hour IN 0..23 LOOP
            FOR v_minute IN 0..59 BY 5 LOOP
                v_timestamp := CURRENT_TIMESTAMP - INTERVAL '1 day' * v_day_offset
                               - INTERVAL '1 hour' * v_hour
                               - INTERVAL '1 minute' * v_minute;

                -- Very stable operation
                v_current_temp := v_target_temp + (random() - 0.5) * 2.0;  -- ±1.0°C variance
                v_humidity := 35.0 + random() * 10.0;  -- 35-45% humidity
                v_status := 'NORMAL';

                INSERT INTO clinlims.freezer_reading
                    (id, freezer_id, recorded_at, temperature_celsius, humidity_percentage, status, transmission_ok, last_updated)
                VALUES
                    (v_reading_id, v_freezer_id, v_timestamp, v_current_temp, v_humidity, v_status, true, v_timestamp);

                v_reading_id := v_reading_id + 1;
            END LOOP;
        END LOOP;
    END LOOP;

    -- Device 3: Standard Freezer B1 (-20°C) - One WARNING event
    v_freezer_id := 3002;
    v_target_temp := -20.0;
    FOR v_day_offset IN 0..6 LOOP
        FOR v_hour IN 0..23 LOOP
            FOR v_minute IN 0..59 BY 10 LOOP  -- Every 10 minutes (slower polling)
                v_timestamp := CURRENT_TIMESTAMP - INTERVAL '1 day' * v_day_offset
                               - INTERVAL '1 hour' * v_hour
                               - INTERVAL '1 minute' * v_minute;

                IF (v_day_offset = 3 AND v_hour BETWEEN 10 AND 11) THEN
                    -- Warning event on day 3
                    v_current_temp := v_target_temp + (6.0 + random() * 2.0);  -- -14 to -12°C
                    v_status := 'WARNING';
                ELSE
                    v_current_temp := v_target_temp + (random() - 0.5) * 4.0;  -- ±2.0°C variance
                    v_status := 'NORMAL';
                END IF;

                v_humidity := 40.0 + random() * 15.0;  -- 40-55% humidity

                INSERT INTO clinlims.freezer_reading
                    (id, freezer_id, recorded_at, temperature_celsius, humidity_percentage, status, transmission_ok, last_updated)
                VALUES
                    (v_reading_id, v_freezer_id, v_timestamp, v_current_temp, v_humidity, v_status, true, v_timestamp);

                v_reading_id := v_reading_id + 1;
            END LOOP;
        END LOOP;
    END LOOP;

    -- Device 4: Refrigerator B2 (4°C) - NORMAL with minor fluctuations
    v_freezer_id := 3003;
    v_target_temp := 4.0;
    FOR v_day_offset IN 0..6 LOOP
        FOR v_hour IN 0..23 LOOP
            FOR v_minute IN 0..59 BY 10 LOOP
                v_timestamp := CURRENT_TIMESTAMP - INTERVAL '1 day' * v_day_offset
                               - INTERVAL '1 hour' * v_hour
                               - INTERVAL '1 minute' * v_minute;

                -- Very stable refrigerator
                v_current_temp := v_target_temp + (random() - 0.5) * 2.0;  -- ±1.0°C variance
                v_humidity := 50.0 + random() * 15.0;  -- 50-65% humidity
                v_status := 'NORMAL';

                INSERT INTO clinlims.freezer_reading
                    (id, freezer_id, recorded_at, temperature_celsius, humidity_percentage, status, transmission_ok, last_updated)
                VALUES
                    (v_reading_id, v_freezer_id, v_timestamp, v_current_temp, v_humidity, v_status, true, v_timestamp);

                v_reading_id := v_reading_id + 1;
            END LOOP;
        END LOOP;
    END LOOP;

    -- Device 5: Blood Bank Refrigerator (4°C) - One CRITICAL event (door left open)
    v_freezer_id := 3004;
    v_target_temp := 4.0;
    FOR v_day_offset IN 0..6 LOOP
        FOR v_hour IN 0..23 LOOP
            FOR v_minute IN 0..59 BY 5 LOOP
                v_timestamp := CURRENT_TIMESTAMP - INTERVAL '1 day' * v_day_offset
                               - INTERVAL '1 hour' * v_hour
                               - INTERVAL '1 minute' * v_minute;

                IF (v_day_offset = 1 AND v_hour = 16 AND v_minute BETWEEN 0 AND 30) THEN
                    -- Critical event on day 1 (door left open for 30 minutes)
                    v_current_temp := 4.0 + (4.0 + random() * 2.0);  -- 8-10°C
                    v_status := 'CRITICAL';
                ELSIF (v_day_offset = 4 AND v_hour BETWEEN 8 AND 9) THEN
                    -- Warning event on day 4
                    v_current_temp := v_target_temp + (3.0 + random() * 1.0);  -- 7-8°C
                    v_status := 'WARNING';
                ELSE
                    v_current_temp := v_target_temp + (random() - 0.5) * 1.5;  -- ±0.75°C variance
                    v_status := 'NORMAL';
                END IF;

                v_humidity := 45.0 + random() * 20.0;  -- 45-65% humidity

                INSERT INTO clinlims.freezer_reading
                    (id, freezer_id, recorded_at, temperature_celsius, humidity_percentage, status, transmission_ok, last_updated)
                VALUES
                    (v_reading_id, v_freezer_id, v_timestamp, v_current_temp, v_humidity, v_status, true, v_timestamp);

                v_reading_id := v_reading_id + 1;
            END LOOP;
        END LOOP;
    END LOOP;

    RAISE NOTICE 'Generated % freezer readings for 5 devices over 7 days', v_reading_id - 6000;
END $$;

-- Update sequence for freezer_reading
SELECT setval('clinlims.freezer_reading_seq', (SELECT MAX(id)::bigint FROM clinlims.freezer_reading), true);

-- ============================================================================
-- SECTION 6: Alerts (Temperature Excursions and Equipment Failures)
-- ============================================================================

INSERT INTO clinlims.alert (id, alert_type, alert_entity_type, alert_entity_id, severity,
                             status, start_time, end_time, message, context_data,
                             acknowledged_at, acknowledged_by, resolved_at, resolved_by,
                             resolution_notes, duplicate_count, last_duplicate_time)
VALUES
    -- Alert 1: Ultra-Low Freezer A1 - WARNING (20 hours ago, resolved with maintenance action 8000)
    (7000, 'FREEZER_TEMPERATURE', 'Freezer', 3000, 'WARNING', 'RESOLVED',
     CURRENT_TIMESTAMP - INTERVAL '20 hours',
     CURRENT_TIMESTAMP - INTERVAL '18 hours',
     'Temperature exceeds warning threshold: -73.5°C (threshold: -75.0°C)',
     '{"temperature": -73.5, "threshold": -75.0, "thresholdType": "WARNING_MAX", "humidity": 42.3, "deviceName": "Ultra-Low Freezer A1", "duration_minutes": 120}',
     CURRENT_TIMESTAMP - INTERVAL '19 hours 45 minutes', 1,
     CURRENT_TIMESTAMP - INTERVAL '18 hours', 1,
     'Quarterly maintenance performed. Temperature returned to normal range after filter replacement.',
     0, NULL),

    -- Alert 2: Ultra-Low Freezer A1 - Equipment Failure (15 hours ago, resolved with repair action 8001)
    (7001, 'EQUIPMENT_FAILURE', 'Freezer', 3000, 'CRITICAL', 'RESOLVED',
     CURRENT_TIMESTAMP - INTERVAL '15 hours',
     CURRENT_TIMESTAMP - INTERVAL '14 hours',
     'Compressor failure detected on Ultra-Low Freezer A1',
     '{"deviceName": "Ultra-Low Freezer A1", "failureType": "COMPRESSOR", "errorCode": "E03"}',
     CURRENT_TIMESTAMP - INTERVAL '14 hours 55 minutes', 1,
     CURRENT_TIMESTAMP - INTERVAL '14 hours', 1,
     'Emergency compressor replacement completed. System tested and operational.',
     0, NULL),

    -- Alert 3: Ultra-Low Freezer A1 - CRITICAL Temperature (15 hours ago, resolved with temp adjustment action 8002)
    (7002, 'FREEZER_TEMPERATURE', 'Freezer', 3000, 'CRITICAL', 'RESOLVED',
     CURRENT_TIMESTAMP - INTERVAL '15 hours',
     CURRENT_TIMESTAMP - INTERVAL '13 hours 30 minutes',
     'Temperature critically high: -66.2°C (critical threshold: -70.0°C)',
     '{"temperature": -66.2, "threshold": -70.0, "thresholdType": "CRITICAL_MAX", "humidity": 38.7, "deviceName": "Ultra-Low Freezer A1", "duration_minutes": 90}',
     CURRENT_TIMESTAMP - INTERVAL '14 hours 30 minutes', 1,
     CURRENT_TIMESTAMP - INTERVAL '13 hours 30 minutes', 1,
     'Temperature recalibrated after compressor repair. Now stable at -80°C.',
     0, NULL),

    -- Alert 4: Blood Bank Refrigerator - CRITICAL (10 hours ago, resolved with sample relocation action 8003)
    (7003, 'FREEZER_TEMPERATURE', 'Freezer', 3004, 'CRITICAL', 'RESOLVED',
     CURRENT_TIMESTAMP - INTERVAL '10 hours',
     CURRENT_TIMESTAMP - INTERVAL '9 hours 30 minutes',
     'Temperature critically high: 9.2°C (critical threshold: 8.0°C)',
     '{"temperature": 9.2, "threshold": 8.0, "thresholdType": "CRITICAL_MAX", "humidity": 62.5, "deviceName": "Blood Bank Refrigerator", "duration_minutes": 30}',
     CURRENT_TIMESTAMP - INTERVAL '9 hours 55 minutes', 1,
     CURRENT_TIMESTAMP - INTERVAL '9 hours 30 minutes', 1,
     'Door left open incident. Samples relocated during door alarm installation. Temperature restored.',
     0, NULL),

    -- Alert 5: Standard Freezer B1 - WARNING (8 hours ago, being addressed by calibration action 8004)
    (7004, 'FREEZER_TEMPERATURE', 'Freezer', 3002, 'WARNING', 'ACKNOWLEDGED',
     CURRENT_TIMESTAMP - INTERVAL '8 hours',
     NULL,
     'Temperature above warning threshold: -13.8°C (threshold: -15.0°C)',
     '{"temperature": -13.8, "threshold": -15.0, "thresholdType": "WARNING_MAX", "humidity": 48.5, "deviceName": "Standard Freezer B1", "duration_minutes": 90}',
     CURRENT_TIMESTAMP - INTERVAL '8 hours', 1,
     NULL, NULL, NULL,
     0, NULL),

    -- Alert 6: Ultra-Low Freezer A2 - WARNING for Low Filter Stock (3 hours ago, pending item reorder action 8006)
    (7005, 'EQUIPMENT_FAILURE', 'Freezer', 3001, 'WARNING', 'OPEN',
     CURRENT_TIMESTAMP - INTERVAL '3 hours',
     NULL,
     'Preventive maintenance due: HEPA filter stock running low',
     '{"deviceName": "Ultra-Low Freezer A2", "maintenanceType": "FILTER_REPLACEMENT", "stockLevel": "LOW"}',
     NULL, NULL, NULL, NULL, NULL,
     0, NULL)
ON CONFLICT (id) DO NOTHING;

-- Update sequence for alert
SELECT setval('clinlims.alert_seq', (SELECT MAX(id)::bigint FROM clinlims.alert), true);

-- ============================================================================
-- SECTION 7: Corrective Actions (Maintenance and Repairs)
-- ============================================================================

INSERT INTO clinlims.corrective_action (id, freezer_id, action_type, description, status,
                                         created_at, created_by, updated_at, updated_by,
                                         completed_at, completion_notes, is_edited)
VALUES
    -- Action 1: RESOLVED Alert 7000 - Maintenance on Ultra-Low Freezer A1 (20 hours ago)
    (8000, 3000, 'MAINTENANCE',
     'ALERT 7000: Quarterly preventive maintenance triggered by temperature warning (-73.5°C). Filter replacement, seal inspection, temperature calibration verification required.',
     'COMPLETED',
     CURRENT_TIMESTAMP - INTERVAL '20 hours', 1,
     CURRENT_TIMESTAMP - INTERVAL '18 hours', 1,
     CURRENT_TIMESTAMP - INTERVAL '18 hours',
     'Maintenance completed successfully. Replaced air filters, inspected door seals (no issues), verified temperature calibration. Temperature returned to normal range (-80.2°C). All systems operating within specifications. Next maintenance due in 3 months.',
     false),

    -- Action 2: RESOLVED Alert 7001 - Equipment repair on Ultra-Low Freezer A1 (15 hours ago)
    (8001, 3000, 'EQUIPMENT_REPAIR',
     'ALERT 7001: URGENT - Compressor failure detected (Error Code E03). Temperature rising to -66.2°C. Samples at critical risk. Immediate emergency repair required.',
     'COMPLETED',
     CURRENT_TIMESTAMP - INTERVAL '15 hours', 1,
     CURRENT_TIMESTAMP - INTERVAL '14 hours', 1,
     CURRENT_TIMESTAMP - INTERVAL '14 hours',
     'Emergency compressor replacement completed within 1 hour. Old compressor showed bearing failure. New compressor installed and tested. Refrigerant refilled to spec levels. Temperature stabilized at -80°C. All samples remain safe - no exposure above -66°C.',
     false),

    -- Action 3: RESOLVED Alert 7002 - Temperature adjustment on Ultra-Low Freezer A1 (14 hours ago)
    (8002, 3000, 'TEMPERATURE_ADJUSTMENT',
     'ALERT 7002: Recalibrate temperature sensor and adjust setpoint to -80°C after compressor replacement. Critical temperature reading of -66.2°C requires verification.',
     'COMPLETED',
     CURRENT_TIMESTAMP - INTERVAL '14 hours', 1,
     CURRENT_TIMESTAMP - INTERVAL '13 hours 30 minutes', 1,
     CURRENT_TIMESTAMP - INTERVAL '13 hours 30 minutes',
     'Temperature sensor recalibrated using certified reference thermometer. Sensor accuracy verified ±0.5°C. Setpoint confirmed at -80°C. Post-repair monitoring shows stable operation at -80.1°C. System returned to normal operations.',
     false),

    -- Action 4: RESOLVED Alert 7003 - Sample relocation for Blood Bank Refrigerator (10 hours ago)
    (8003, 3004, 'SAMPLE_RELOCATION',
     'ALERT 7003: Emergency sample relocation required. Door left open - temperature reached 9.2°C (CRITICAL). Move blood samples to backup unit during door alarm installation.',
     'COMPLETED',
     CURRENT_TIMESTAMP - INTERVAL '10 hours', 1,
     CURRENT_TIMESTAMP - INTERVAL '9 hours 30 minutes', 1,
     CURRENT_TIMESTAMP - INTERVAL '9 hours 30 minutes',
     'All 24 blood samples (Units B-1001 through B-1024) temporarily moved to backup refrigerator BB-REF-2. Door alarm sensor installed on main unit. Staff training completed on proper door closure procedures. All samples returned to primary unit after temperature stabilized at 4.1°C. No sample degradation detected.',
     false),

    -- Action 5: IN PROGRESS - Addressing Alert 7004 - Calibration on Standard Freezer B1 (8 hours ago)
    (8004, 3002, 'CALIBRATION',
     'ALERT 7004: Temperature warning at -13.8°C (threshold -15°C). Annual calibration overdue. Perform temperature sensor validation and adjustment to resolve warning.',
     'IN_PROGRESS',
     CURRENT_TIMESTAMP - INTERVAL '8 hours', 1,
     CURRENT_TIMESTAMP - INTERVAL '8 hours', 1,
     NULL, NULL, false),

    -- Action 6: PENDING - Routine maintenance on Refrigerator B2 (5 hours ago)
    (8005, 3003, 'MAINTENANCE',
     'Monthly preventive maintenance: clean condenser coils, check door seals, verify temperature accuracy. No alerts - routine scheduled maintenance.',
     'PENDING',
     CURRENT_TIMESTAMP - INTERVAL '5 hours', 1,
     NULL, NULL, NULL, NULL, false),

    -- Action 7: PENDING - Addressing Alert 7005 - Item reorder for Ultra-Low Freezer A2 (3 hours ago)
    (8006, 3001, 'ITEM_REORDER',
     'ALERT 7005: Order replacement HEPA filters for Ultra-Low Freezer A2. Stock running low - preventive maintenance due within 2 weeks. Avoid equipment downtime.',
     'PENDING',
     CURRENT_TIMESTAMP - INTERVAL '3 hours', 1,
     NULL, NULL, NULL, NULL, false),

    -- Action 8: CANCELLED - False alarm on Standard Freezer B1 (12 hours ago)
    (8007, 3002, 'EQUIPMENT_REPAIR',
     'Investigate intermittent Modbus RTU communication errors on Standard Freezer B1. Possible wiring issue.',
     'CANCELLED',
     CURRENT_TIMESTAMP - INTERVAL '12 hours', 1,
     CURRENT_TIMESTAMP - INTERVAL '9 hours', 1,
     NULL, NULL, false),

    -- Action 9: RETRACTED - Duplicate entry on Ultra-Low Freezer A1 (21 hours ago)
    (8008, 3000, 'MAINTENANCE',
     'Duplicate maintenance entry - please disregard. Refers to same work as Action 8000.',
     'RETRACTED',
     CURRENT_TIMESTAMP - INTERVAL '21 hours', 1,
     CURRENT_TIMESTAMP - INTERVAL '20 hours 55 minutes', 1,
     NULL, NULL, false)
ON CONFLICT (id) DO NOTHING;

-- Update the retracted action with retraction details
UPDATE clinlims.corrective_action
SET retracted_at = CURRENT_TIMESTAMP - INTERVAL '20 hours 55 minutes',
    retraction_reason = 'Duplicate entry created by mistake. This action refers to the same maintenance work as Action 8000. Retracted to avoid confusion and duplicate record keeping.'
WHERE id = 8008;

-- Update sequence for corrective_action
SELECT setval('clinlims.corrective_action_seq', (SELECT MAX(id)::bigint FROM clinlims.corrective_action), true);

-- ============================================================================
-- SECTION 8: Summary Statistics (for verification)
-- ============================================================================

DO $$
DECLARE
    v_freezer_count INTEGER;
    v_profile_count INTEGER;
    v_reading_count INTEGER;
    v_alert_count INTEGER;
    v_action_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_freezer_count FROM clinlims.freezer WHERE id >= 3000;
    SELECT COUNT(*) INTO v_profile_count FROM clinlims.threshold_profile WHERE id >= 4000;
    SELECT COUNT(*) INTO v_reading_count FROM clinlims.freezer_reading WHERE id >= 6000;
    SELECT COUNT(*) INTO v_alert_count FROM clinlims.alert WHERE id >= 7000;
    SELECT COUNT(*) INTO v_action_count FROM clinlims.corrective_action WHERE id >= 8000;

    RAISE NOTICE '============================================================';
    RAISE NOTICE 'FREEZER MONITORING DEMO DATA - SUMMARY';
    RAISE NOTICE '============================================================';
    RAISE NOTICE 'Freezer Devices:         %', v_freezer_count;
    RAISE NOTICE 'Threshold Profiles:      %', v_profile_count;
    RAISE NOTICE 'Temperature Readings:    %', v_reading_count;
    RAISE NOTICE 'Alerts:                  %', v_alert_count;
    RAISE NOTICE 'Corrective Actions:      %', v_action_count;
    RAISE NOTICE '============================================================';
    RAISE NOTICE 'Data loaded successfully!';
    RAISE NOTICE 'Time range: 7 days of historical data';
    RAISE NOTICE '============================================================';
END $$;

COMMIT;

-- ============================================================================
-- END OF DEMO DATA SCRIPT
-- ============================================================================
