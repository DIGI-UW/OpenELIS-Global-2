-- ============================================================================
-- INVENTORY MANAGEMENT DEMO DATA - Comprehensive Lab Inventory Dashboard
-- ============================================================================
-- This SQL file creates realistic demo data for the inventory management feature
-- including: catalog items (reagents, RDTs, cartridges), storage locations,
-- inventory lots with various statuses, transactions, usage records.
--
-- USAGE: Run this on a clean server after Liquibase migrations are complete
--        docker exec -i openelisglobal-database psql -U clinlims -d clinlims < src/test/resources/inventory-management-demo-data.sql
-- ============================================================================

BEGIN;

-- ============================================================================
-- SECTION 1: Storage Locations (Hierarchical Structure)
-- ============================================================================

INSERT INTO clinlims.inventory_storage_location (id, fhir_uuid, name, location_code, location_type, parent_location_id, description, temperature_min, temperature_max, is_active)
VALUES
    -- Top-level: Main Lab Room
    (1000, gen_random_uuid(), 'Main Laboratory', 'MAIN', 'ROOM', NULL,
     'Primary laboratory room for reagent and sample storage', NULL, NULL, true),

    -- Second level: Equipment in Main Lab
    (1001, gen_random_uuid(), 'Ultra-Low Freezer A1', 'MAIN-FRZ01', 'FREEZER', 1000,
     '-80°C ultra-low freezer for PCR reagents', -85.0, -75.0, true),

    (1002, gen_random_uuid(), 'Refrigerator A', 'MAIN-REFG01', 'REFRIGERATOR', 1000,
     '2-8°C refrigerator for test kits and blood products', 2.0, 8.0, true),

    (1003, gen_random_uuid(), 'Reagent Cabinet A', 'MAIN-CAB01', 'CABINET', 1000,
     'Room temperature storage for stable reagents', 15.0, 25.0, true),

    -- Third level: Shelves in Freezer
    (1004, gen_random_uuid(), 'Freezer A1 - Shelf A', 'MAIN-FRZ01-SHA', 'SHELF', 1001,
     'Top shelf - COVID/Respiratory reagents', -85.0, -75.0, true),

    (1005, gen_random_uuid(), 'Freezer A1 - Shelf B', 'MAIN-FRZ01-SHB', 'SHELF', 1001,
     'Middle shelf - HIV/Hepatitis reagents', -85.0, -75.0, true),

    (1006, gen_random_uuid(), 'Freezer A1 - Shelf C', 'MAIN-FRZ01-SHC', 'SHELF', 1001,
     'Bottom shelf - TB/Malaria reagents', -85.0, -75.0, true),

    -- Third level: Shelves in Refrigerator
    (1007, gen_random_uuid(), 'Refrigerator A - Shelf A', 'MAIN-REFG01-SHA', 'SHELF', 1002,
     'Top shelf - RDT kits', 2.0, 8.0, true),

    (1008, gen_random_uuid(), 'Refrigerator A - Shelf B', 'MAIN-REFG01-SHB', 'SHELF', 1002,
     'Middle shelf - Cartridges', 2.0, 8.0, true),

    -- Fourth level: Drawers within Shelves
    (1009, gen_random_uuid(), 'Freezer A1 Shelf A - Rack A', 'MAIN-FRZ01-SHA-RKA', 'DRAWER', 1004,
     'Left rack - COVID PCR master mix', -85.0, -75.0, true),

    (1010, gen_random_uuid(), 'Freezer A1 Shelf A - Rack B', 'MAIN-FRZ01-SHA-RKB', 'DRAWER', 1004,
     'Right rack - COVID primers/probes', -85.0, -75.0, true)
ON CONFLICT (id) DO NOTHING;

-- Update sequence for inventory_storage_location
SELECT setval('clinlims.inventory_storage_location_seq', (SELECT COALESCE(MAX(id), 0) FROM clinlims.inventory_storage_location), true);

-- ============================================================================
-- SECTION 2: Inventory Catalog Items (20 items covering common lab reagents)
-- ============================================================================

INSERT INTO clinlims.inventory_item (id, fhir_uuid, name, item_type, category, manufacturer,
                                      units, low_stock_threshold, expiration_alert_days,
                                      stability_after_opening, storage_requirements,
                                      compatible_analyzers, tests_per_kit, is_active)
VALUES
    -- REAGENTS (PCR and Chemistry)
    (2000, gen_random_uuid(), 'COVID-19 PCR Master Mix', 'REAGENT', 'Molecular Diagnostics', 'ThermoFisher Scientific',
     'mL', 5, 30, 90, 'Store at -20°C. Protect from light. Thaw on ice.', NULL, NULL, 'Y'),

    (2001, gen_random_uuid(), 'HIV RNA Extraction Kit', 'REAGENT', 'Molecular Diagnostics', 'Qiagen',
     'extractions', 10, 30, 180, 'Store at 2-8°C. Do not freeze.', NULL, NULL, 'Y'),

    (2002, gen_random_uuid(), 'Hepatitis B PCR Reagent', 'REAGENT', 'Molecular Diagnostics', 'Roche Diagnostics',
     'mL', 3, 30, 60, 'Store at -80°C. Stable for 60 days at -20°C after opening.', NULL, NULL, 'Y'),

    (2003, gen_random_uuid(), 'TB MGIT Culture Medium', 'REAGENT', 'Microbiology', 'BD Diagnostics',
     'tubes', 20, 30, 30, 'Store at 2-8°C. Use within 30 days of opening.', NULL, NULL, 'Y'),

    (2004, gen_random_uuid(), 'Glucose Reagent Solution', 'REAGENT', 'Clinical Chemistry', 'Abbott Laboratories',
     'mL', 10, 30, 90, 'Store at 2-8°C. Stable for 90 days after opening.', NULL, NULL, 'Y'),

    (2005, gen_random_uuid(), 'Creatinine Reagent Kit', 'REAGENT', 'Clinical Chemistry', 'Siemens Healthineers',
     'mL', 8, 30, 60, 'Store at 2-8°C. Do not freeze.', NULL, NULL, 'Y'),

    -- RDTs (Rapid Diagnostic Tests)
    (2006, gen_random_uuid(), 'Malaria RDT (Pf/Pan)', 'RDT', 'Infectious Disease', 'SD Biosensor',
     'tests', 50, 60, NULL, 'Store at 2-30°C. Do not freeze.', NULL, 25, 'Y'),

    (2007, gen_random_uuid(), 'HIV Combo Test (Alere)', 'RDT', 'Infectious Disease', 'Abbott Rapid Diagnostics',
     'tests', 30, 60, NULL, 'Store at 2-30°C. Do not expose to moisture.', NULL, 20, 'Y'),

    (2008, gen_random_uuid(), 'COVID-19 Antigen RDT', 'RDT', 'Infectious Disease', 'Roche Diagnostics',
     'tests', 100, 90, NULL, 'Store at 2-30°C. Use within 24 months.', NULL, 25, 'Y'),

    (2009, gen_random_uuid(), 'Syphilis Rapid Test', 'RDT', 'Infectious Disease', 'SD Biosensor',
     'tests', 25, 60, NULL, 'Store at 2-30°C. Avoid direct sunlight.', NULL, 30, 'Y'),

    (2010, gen_random_uuid(), 'Hepatitis C Rapid Test', 'RDT', 'Infectious Disease', 'OraSure Technologies',
     'tests', 20, 60, NULL, 'Store at 2-30°C. Do not use if pouch is damaged.', NULL, 25, 'Y'),

    -- CARTRIDGES (Automated Analyzers)
    (2011, gen_random_uuid(), 'GeneXpert MTB/RIF Ultra Cartridge', 'CARTRIDGE', 'Molecular Diagnostics', 'Cepheid',
     'cartridges', 20, 60, NULL, 'Store at 2-28°C. Do not freeze.', 'GeneXpert System', NULL, 'Y'),

    (2012, gen_random_uuid(), 'GeneXpert HIV Viral Load', 'CARTRIDGE', 'Molecular Diagnostics', 'Cepheid',
     'cartridges', 15, 60, NULL, 'Store at 2-28°C.', 'GeneXpert System', NULL, 'Y'),

    (2013, gen_random_uuid(), 'Cobas HPV Test Cartridge', 'CARTRIDGE', 'Molecular Diagnostics', 'Roche Diagnostics',
     'cartridges', 10, 30, NULL, 'Store at 2-8°C. Equilibrate to room temp before use.', 'Cobas 4800, Cobas 6800/8800', NULL, 'Y'),

    (2014, gen_random_uuid(), 'Alinity HIV Combo Cartridge', 'CARTRIDGE', 'Immunoassay', 'Abbott Diagnostics',
     'cartridges', 12, 30, NULL, 'Store at 2-8°C.', 'Alinity i System', NULL, 'Y'),

    -- More specialized items
    (2015, gen_random_uuid(), 'Blood Culture Bottles (Aerobic)', 'REAGENT', 'Microbiology', 'BD Diagnostics',
     'bottles', 50, 90, 365, 'Store at 20-25°C. Do not refrigerate.', NULL, NULL, 'Y'),

    (2016, gen_random_uuid(), 'CD4 Count Reagent Kit', 'REAGENT', 'Flow Cytometry', 'BD Biosciences',
     'tests', 20, 30, 30, 'Store at 2-8°C. Use within 30 days after opening.', NULL, NULL, 'Y'),

    (2017, gen_random_uuid(), 'Hematology Control Material (3-Level)', 'REAGENT', 'Hematology', 'Sysmex',
     'mL', 10, 30, 90, 'Store at 2-8°C. Mix gently before use.', NULL, NULL, 'Y'),

    (2018, gen_random_uuid(), 'Urinalysis Reagent Strips', 'RDT', 'Clinical Chemistry', 'Siemens Healthineers',
     'strips', 100, 60, NULL, 'Store at 2-30°C. Keep bottle tightly closed.', NULL, 100, 'Y'),

    (2019, gen_random_uuid(), 'Pregnancy Test (hCG)', 'RDT', 'Clinical Chemistry', 'Quidel',
     'tests', 50, 60, NULL, 'Store at 2-30°C. Do not use if foil pouch is damaged.', NULL, 25, 'Y')
ON CONFLICT (id) DO NOTHING;

-- Update sequence for inventory_item
SELECT setval('clinlims.inventory_item_seq', (SELECT COALESCE(MAX(id), 0) FROM clinlims.inventory_item), true);

-- ============================================================================
-- SECTION 3: Inventory Lots (Realistic Stock Scenarios)
-- ============================================================================

INSERT INTO clinlims.inventory_lot (id, fhir_uuid, inventory_item_id, lot_number, initial_quantity,
                                     current_quantity, expiration_date, receipt_date, storage_location_id,
                                     qc_status, status, barcode, date_opened, calculated_expiry_after_opening, version)
VALUES
    -- COVID-19 PCR Master Mix (Item 2000) - 3 lots showing FEFO
    (3000, gen_random_uuid(), 2000, 'COV-PCR-2024-001', 50.0, 42.5,
     CURRENT_DATE + INTERVAL '6 months', CURRENT_DATE - INTERVAL '2 months', 1009,
     'PASSED', 'IN_USE', 'COV-PCR-2024-001', CURRENT_DATE - INTERVAL '15 days', CURRENT_DATE + INTERVAL '75 days', 0),

    (3001, gen_random_uuid(), 2000, 'COV-PCR-2024-002', 50.0, 48.0,
     CURRENT_DATE + INTERVAL '9 months', CURRENT_DATE - INTERVAL '1 month', 1009,
     'PASSED', 'ACTIVE', 'COV-PCR-2024-002', NULL, NULL, 0),

    (3002, gen_random_uuid(), 2000, 'COV-PCR-2023-045', 50.0, 3.2,
     CURRENT_DATE + INTERVAL '25 days', CURRENT_DATE - INTERVAL '10 months', 1009,
     'PASSED', 'ACTIVE', 'COV-PCR-2023-045', CURRENT_DATE - INTERVAL '8 months', CURRENT_DATE - INTERVAL '5 months', 0),

    -- HIV RNA Extraction Kit (Item 2001) - 2 lots
    (3003, gen_random_uuid(), 2001, 'HIV-EXT-2024-078', 100.0, 87.0,
     CURRENT_DATE + INTERVAL '1 year', CURRENT_DATE - INTERVAL '3 months', 1005,
     'PASSED', 'IN_USE', 'HIV-EXT-2024-078', CURRENT_DATE - INTERVAL '1 month', CURRENT_DATE + INTERVAL '5 months', 0),

    (3004, gen_random_uuid(), 2001, 'HIV-EXT-2024-092', 100.0, 5.0,
     CURRENT_DATE + INTERVAL '8 months', CURRENT_DATE - INTERVAL '1 week', 1005,
     'PENDING', 'ACTIVE', 'HIV-EXT-2024-092', NULL, NULL, 0),

    -- Hepatitis B PCR (Item 2002) - QUARANTINED lot
    (3005, gen_random_uuid(), 2002, 'HBV-PCR-2024-034', 30.0, 28.0,
     CURRENT_DATE + INTERVAL '4 months', CURRENT_DATE - INTERVAL '2 months', 1005,
     'QUARANTINED', 'QUARANTINED', 'HBV-PCR-2024-034', CURRENT_DATE - INTERVAL '1 month', CURRENT_DATE + INTERVAL '1 month', 0),

    -- TB MGIT Culture Medium (Item 2003) - CONSUMED
    (3006, gen_random_uuid(), 2003, 'TB-MGIT-2024-012', 200.0, 0.0,
     CURRENT_DATE + INTERVAL '5 months', CURRENT_DATE - INTERVAL '4 months', 1002,
     'PASSED', 'CONSUMED', 'TB-MGIT-2024-012', CURRENT_DATE - INTERVAL '3 months', CURRENT_DATE - INTERVAL '2 months', 0),

    -- Glucose Reagent (Item 2004) - Healthy stock
    (3007, gen_random_uuid(), 2004, 'GLU-2024-156', 200.0, 165.0,
     CURRENT_DATE + INTERVAL '10 months', CURRENT_DATE - INTERVAL '2 months', 1002,
     'PASSED', 'IN_USE', 'GLU-2024-156', CURRENT_DATE - INTERVAL '1 month', CURRENT_DATE + INTERVAL '2 months', 0),

    (3008, gen_random_uuid(), 2004, 'GLU-2024-189', 200.0, 200.0,
     CURRENT_DATE + INTERVAL '1 year', CURRENT_DATE - INTERVAL '1 week', 1002,
     'PASSED', 'ACTIVE', 'GLU-2024-189', NULL, NULL, 0),

    -- Creatinine Reagent (Item 2005) - EXPIRED
    (3009, gen_random_uuid(), 2005, 'CREAT-2023-234', 150.0, 45.0,
     CURRENT_DATE - INTERVAL '10 days', CURRENT_DATE - INTERVAL '1 year', 1002,
     'PASSED', 'EXPIRED', 'CREAT-2023-234', CURRENT_DATE - INTERVAL '10 months', CURRENT_DATE - INTERVAL '8 months', 0),

    -- Malaria RDT (Item 2006) - Multiple lots showing FEFO
    (3010, gen_random_uuid(), 2006, 'MAL-RDT-2024-045', 500.0, 425.0,
     CURRENT_DATE + INTERVAL '3 months', CURRENT_DATE - INTERVAL '6 months', 1007,
     'PASSED', 'IN_USE', 'MAL-RDT-2024-045', NULL, NULL, 0),

    (3011, gen_random_uuid(), 2006, 'MAL-RDT-2024-067', 500.0, 475.0,
     CURRENT_DATE + INTERVAL '8 months', CURRENT_DATE - INTERVAL '2 months', 1007,
     'PASSED', 'ACTIVE', 'MAL-RDT-2024-067', NULL, NULL, 0),

    (3012, gen_random_uuid(), 2006, 'MAL-RDT-2024-089', 500.0, 500.0,
     CURRENT_DATE + INTERVAL '14 months', CURRENT_DATE - INTERVAL '1 week', 1007,
     'PASSED', 'ACTIVE', 'MAL-RDT-2024-089', NULL, NULL, 0),

    -- HIV Combo RDT (Item 2007) - LOW STOCK
    (3013, gen_random_uuid(), 2007, 'HIV-RDT-2024-123', 400.0, 18.0,
     CURRENT_DATE + INTERVAL '5 months', CURRENT_DATE - INTERVAL '4 months', 1007,
     'PASSED', 'IN_USE', 'HIV-RDT-2024-123', NULL, NULL, 0),

    -- COVID-19 Antigen RDT (Item 2008) - High demand
    (3014, gen_random_uuid(), 2008, 'COV-AG-2024-234', 2500.0, 1847.0,
     CURRENT_DATE + INTERVAL '6 months', CURRENT_DATE - INTERVAL '3 months', 1007,
     'PASSED', 'IN_USE', 'COV-AG-2024-234', NULL, NULL, 0),

    (3015, gen_random_uuid(), 2008, 'COV-AG-2024-256', 2500.0, 2500.0,
     CURRENT_DATE + INTERVAL '11 months', CURRENT_DATE - INTERVAL '2 weeks', 1007,
     'PASSED', 'ACTIVE', 'COV-AG-2024-256', NULL, NULL, 0),

    -- Syphilis RDT (Item 2009)
    (3016, gen_random_uuid(), 2009, 'SYPH-RDT-2024-078', 600.0, 487.0,
     CURRENT_DATE + INTERVAL '9 months', CURRENT_DATE - INTERVAL '2 months', 1007,
     'PASSED', 'IN_USE', 'SYPH-RDT-2024-078', NULL, NULL, 0),

    -- Hepatitis C RDT (Item 2010) - EXPIRING SOON
    (3017, gen_random_uuid(), 2010, 'HCV-RDT-2024-012', 500.0, 312.0,
     CURRENT_DATE + INTERVAL '22 days', CURRENT_DATE - INTERVAL '10 months', 1007,
     'PASSED', 'ACTIVE', 'HCV-RDT-2024-012', NULL, NULL, 0),

    -- GeneXpert MTB/RIF Ultra (Item 2011)
    (3018, gen_random_uuid(), 2011, 'GENX-TB-2024-045', 200.0, 156.0,
     CURRENT_DATE + INTERVAL '7 months', CURRENT_DATE - INTERVAL '3 months', 1008,
     'PASSED', 'IN_USE', 'GENX-TB-2024-045', NULL, NULL, 0),

    (3019, gen_random_uuid(), 2011, 'GENX-TB-2024-067', 200.0, 200.0,
     CURRENT_DATE + INTERVAL '10 months', CURRENT_DATE - INTERVAL '1 month', 1008,
     'PASSED', 'ACTIVE', 'GENX-TB-2024-067', NULL, NULL, 0),

    -- GeneXpert HIV VL (Item 2012) - LOW STOCK critical
    (3020, gen_random_uuid(), 2012, 'GENX-HIV-2024-089', 150.0, 12.0,
     CURRENT_DATE + INTERVAL '5 months', CURRENT_DATE - INTERVAL '6 months', 1008,
     'PASSED', 'IN_USE', 'GENX-HIV-2024-089', NULL, NULL, 0),

    -- Cobas HPV Cartridge (Item 2013)
    (3021, gen_random_uuid(), 2013, 'COBAS-HPV-2024-123', 100.0, 73.0,
     CURRENT_DATE + INTERVAL '8 months', CURRENT_DATE - INTERVAL '2 months', 1008,
     'PASSED', 'IN_USE', 'COBAS-HPV-2024-123', NULL, NULL, 0),

    -- Alinity HIV Combo (Item 2014) - PENDING QC
    (3022, gen_random_uuid(), 2014, 'ALIN-HIV-2024-234', 120.0, 120.0,
     CURRENT_DATE + INTERVAL '1 year', CURRENT_DATE - INTERVAL '3 days', 1008,
     'PENDING', 'ACTIVE', 'ALIN-HIV-2024-234', NULL, NULL, 0),

    -- Blood Culture Bottles (Item 2015)
    (3023, gen_random_uuid(), 2015, 'BC-AER-2024-567', 500.0, 234.0,
     CURRENT_DATE + INTERVAL '1 year', CURRENT_DATE - INTERVAL '4 months', 1003,
     'PASSED', 'IN_USE', 'BC-AER-2024-567', NULL, NULL, 0),

    (3024, gen_random_uuid(), 2015, 'BC-AER-2024-589', 500.0, 500.0,
     CURRENT_DATE + INTERVAL '16 months', CURRENT_DATE - INTERVAL '1 week', 1003,
     'PASSED', 'ACTIVE', 'BC-AER-2024-589', NULL, NULL, 0),

    -- CD4 Count Reagent (Item 2016)
    (3025, gen_random_uuid(), 2016, 'CD4-2024-345', 200.0, 145.0,
     CURRENT_DATE + INTERVAL '6 months', CURRENT_DATE - INTERVAL '2 months', 1002,
     'PASSED', 'IN_USE', 'CD4-2024-345', CURRENT_DATE - INTERVAL '1 month', CURRENT_DATE + INTERVAL '0 days', 0),

    -- Hematology Control (Item 2017)
    (3026, gen_random_uuid(), 2017, 'HEME-CTL-2024-456', 100.0, 67.0,
     CURRENT_DATE + INTERVAL '4 months', CURRENT_DATE - INTERVAL '3 months', 1002,
     'PASSED', 'IN_USE', 'HEME-CTL-2024-456', CURRENT_DATE - INTERVAL '2 months', CURRENT_DATE + INTERVAL '1 month', 0),

    -- Urinalysis Strips (Item 2018)
    (3027, gen_random_uuid(), 2018, 'URINE-2024-678', 1000.0, 623.0,
     CURRENT_DATE + INTERVAL '10 months', CURRENT_DATE - INTERVAL '2 months', 1003,
     'PASSED', 'IN_USE', 'URINE-2024-678', NULL, NULL, 0),

    -- Pregnancy Test (Item 2019)
    (3028, gen_random_uuid(), 2019, 'PREG-2024-789', 400.0, 287.0,
     CURRENT_DATE + INTERVAL '8 months', CURRENT_DATE - INTERVAL '3 months', 1003,
     'PASSED', 'IN_USE', 'PREG-2024-789', NULL, NULL, 0)
ON CONFLICT (id) DO NOTHING;

-- Update sequence for inventory_lot
SELECT setval('clinlims.inventory_lot_seq', (SELECT COALESCE(MAX(id), 0) FROM clinlims.inventory_lot), true);

-- ============================================================================
-- SECTION 4: Inventory Transactions (Audit Trail)
-- ============================================================================

INSERT INTO clinlims.inventory_transaction (id, lot_id, transaction_type, quantity_change,
                                             quantity_after, reference_type, reference_id,
                                             transaction_date, performed_by_user, notes)
VALUES
    -- COVID PCR Lot 3000
    (4000, 3000, 'RECEIPT', 50.0, 50.0, 'RECEIPT', NULL,
     CURRENT_DATE - INTERVAL '2 months', 1, 'Initial receipt - Invoice #INV-2024-123'),

    (4001, 3000, 'OPENING', 0.0, 50.0, 'MANUAL', NULL,
     CURRENT_DATE - INTERVAL '15 days', 1, 'Opened for COVID testing batch'),

    (4002, 3000, 'CONSUMPTION', -5.5, 44.5, 'TEST_RESULT', 12345,
     CURRENT_DATE - INTERVAL '14 days', 1, 'Used for 55 COVID PCR tests'),

    (4003, 3000, 'CONSUMPTION', -2.0, 42.5, 'TEST_RESULT', 12389,
     CURRENT_DATE - INTERVAL '7 days', 1, 'Used for 20 COVID PCR tests'),

    -- HIV Extraction Kit Lot 3003
    (4004, 3003, 'RECEIPT', 100.0, 100.0, 'RECEIPT', NULL,
     CURRENT_DATE - INTERVAL '3 months', 1, 'Quarterly HIV reagent order'),

    (4005, 3003, 'OPENING', 0.0, 100.0, 'MANUAL', NULL,
     CURRENT_DATE - INTERVAL '1 month', 1, 'Opened for HIV viral load batch'),

    (4006, 3003, 'CONSUMPTION', -13.0, 87.0, 'TEST_RESULT', 23456,
     CURRENT_DATE - INTERVAL '2 weeks', 1, 'Batch of 13 HIV VL extractions'),

    -- Malaria RDT Lot 3010
    (4007, 3010, 'RECEIPT', 500.0, 500.0, 'RECEIPT', NULL,
     CURRENT_DATE - INTERVAL '6 months', 1, 'Malaria RDT shipment'),

    (4008, 3010, 'CONSUMPTION', -75.0, 425.0, 'TEST_RESULT', 34567,
     CURRENT_DATE - INTERVAL '1 month', 1, 'Malaria season testing - 75 tests'),

    -- Glucose Reagent Lot 3007
    (4009, 3007, 'RECEIPT', 200.0, 200.0, 'RECEIPT', NULL,
     CURRENT_DATE - INTERVAL '2 months', 1, 'Monthly chemistry reagent order'),

    (4010, 3007, 'OPENING', 0.0, 200.0, 'MANUAL', NULL,
     CURRENT_DATE - INTERVAL '1 month', 1, 'Opened for routine chemistry panel'),

    (4011, 3007, 'CONSUMPTION', -30.0, 170.0, 'TEST_RESULT', 45678,
     CURRENT_DATE - INTERVAL '3 weeks', 1, 'Routine glucose testing'),

    (4012, 3007, 'ADJUSTMENT', -5.0, 165.0, 'ADJUSTMENT', NULL,
     CURRENT_DATE - INTERVAL '1 week', 1, 'Physical inventory count adjustment'),

    -- TB MGIT Lot 3006
    (4013, 3006, 'RECEIPT', 200.0, 200.0, 'RECEIPT', NULL,
     CURRENT_DATE - INTERVAL '4 months', 1, 'TB culture media order'),

    (4014, 3006, 'OPENING', 0.0, 200.0, 'MANUAL', NULL,
     CURRENT_DATE - INTERVAL '3 months', 1, 'Opened for TB culture setup'),

    (4015, 3006, 'CONSUMPTION', -120.0, 80.0, 'TEST_RESULT', 56789,
     CURRENT_DATE - INTERVAL '2 months', 1, 'High TB testing volume'),

    (4016, 3006, 'CONSUMPTION', -80.0, 0.0, 'TEST_RESULT', 67890,
     CURRENT_DATE - INTERVAL '1 month', 1, 'Final TB batch - lot consumed')
ON CONFLICT (id) DO NOTHING;

-- Update sequence for inventory_transaction
SELECT setval('clinlims.inventory_transaction_seq', (SELECT COALESCE(MAX(id), 0) FROM clinlims.inventory_transaction), true);

-- ============================================================================
-- SECTION 5: Inventory Usage Records (Test Result Traceability)
-- ============================================================================

INSERT INTO clinlims.inventory_usage (id, lot_id, inventory_item_id, test_result_id, analysis_id,
                                       quantity_used, usage_date, performed_by_user)
VALUES
    -- COVID PCR tests using Lot 3000
    (5000, 3000, 2000, 12345, NULL, 5.5,
     CURRENT_DATE - INTERVAL '14 days', 1),

    (5001, 3000, 2000, 12389, NULL, 2.0,
     CURRENT_DATE - INTERVAL '7 days', 1),

    -- HIV VL tests using Lot 3003
    (5002, 3003, 2001, 23456, NULL, 13.0,
     CURRENT_DATE - INTERVAL '2 weeks', 1),

    -- Malaria RDT using Lot 3010
    (5003, 3010, 2006, 34567, NULL, 75.0,
     CURRENT_DATE - INTERVAL '1 month', 1),

    -- Glucose tests using Lot 3007
    (5004, 3007, 2004, 45678, NULL, 30.0,
     CURRENT_DATE - INTERVAL '3 weeks', 1),

    -- TB culture using Lot 3006
    (5005, 3006, 2003, 56789, NULL, 120.0,
     CURRENT_DATE - INTERVAL '2 months', 1),

    (5006, 3006, 2003, 67890, NULL, 80.0,
     CURRENT_DATE - INTERVAL '1 month', 1)
ON CONFLICT (id) DO NOTHING;

-- Update sequence for inventory_usage
SELECT setval('clinlims.inventory_usage_seq', (SELECT COALESCE(MAX(id), 0) FROM clinlims.inventory_usage), true);

-- ============================================================================
-- SECTION 6: Summary Statistics
-- ============================================================================

DO $$
DECLARE
    v_location_count INTEGER;
    v_item_count INTEGER;
    v_lot_count INTEGER;
    v_transaction_count INTEGER;
    v_usage_count INTEGER;
    v_low_stock_count INTEGER;
    v_expiring_count INTEGER;
    v_expired_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_location_count FROM clinlims.inventory_storage_location WHERE id >= 1000;
    SELECT COUNT(*) INTO v_item_count FROM clinlims.inventory_item WHERE id >= 2000;
    SELECT COUNT(*) INTO v_lot_count FROM clinlims.inventory_lot WHERE id >= 3000;
    SELECT COUNT(*) INTO v_transaction_count FROM clinlims.inventory_transaction WHERE id >= 4000;
    SELECT COUNT(*) INTO v_usage_count FROM clinlims.inventory_usage WHERE id >= 5000;

    -- Calculate dashboard metrics
    SELECT COUNT(DISTINCT il.id) INTO v_low_stock_count
    FROM clinlims.inventory_lot il
    JOIN clinlims.inventory_item ii ON il.inventory_item_id = ii.id
    WHERE il.current_quantity < ii.low_stock_threshold
      AND il.status IN ('ACTIVE', 'IN_USE')
      AND il.id >= 3000;

    SELECT COUNT(*) INTO v_expiring_count
    FROM clinlims.inventory_lot
    WHERE expiration_date BETWEEN CURRENT_DATE AND CURRENT_DATE + INTERVAL '30 days'
      AND status IN ('ACTIVE', 'IN_USE')
      AND id >= 3000;

    SELECT COUNT(*) INTO v_expired_count
    FROM clinlims.inventory_lot
    WHERE (expiration_date < CURRENT_DATE
           OR (calculated_expiry_after_opening IS NOT NULL
               AND calculated_expiry_after_opening < CURRENT_DATE))
      AND status NOT IN ('CONSUMED', 'DISPOSED')
      AND id >= 3000;

    RAISE NOTICE '============================================================';
    RAISE NOTICE 'INVENTORY MANAGEMENT DEMO DATA - SUMMARY';
    RAISE NOTICE '============================================================';
    RAISE NOTICE 'Storage Locations:       %', v_location_count;
    RAISE NOTICE 'Catalog Items:           %', v_item_count;
    RAISE NOTICE 'Inventory Lots:          %', v_lot_count;
    RAISE NOTICE 'Transactions:            %', v_transaction_count;
    RAISE NOTICE 'Usage Records:           %', v_usage_count;
    RAISE NOTICE '------------------------------------------------------------';
    RAISE NOTICE 'DASHBOARD METRICS:';
    RAISE NOTICE 'Low Stock Alerts:        %', v_low_stock_count;
    RAISE NOTICE 'Expiring Soon (30 days): %', v_expiring_count;
    RAISE NOTICE 'Expired Lots:            %', v_expired_count;
    RAISE NOTICE '============================================================';
    RAISE NOTICE 'Data loaded successfully!';
    RAISE NOTICE '============================================================';
END $$;

COMMIT;

-- ============================================================================
-- END OF DEMO DATA SCRIPT
-- ============================================================================
