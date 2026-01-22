#!/bin/bash

# Add Sample Equipment Records for Testing Equipment Datatable
echo "🔧 Adding sample equipment records for testing equipment datatable..."

# Generate SQL for 21 equipment items (10 general + 11 bioanalytical analyzers)
cat > sample_equipment.sql << 'EOF'
-- Sample Equipment Records for Testing Equipment Datatable
BEGIN TRANSACTION;

-- Insert 21 equipment items (10 general + 11 bioanalytical analyzers)
INSERT INTO clinlims.inventory_item (
    id, fhir_uuid, name, description, item_type, category,
    manufacturer, catalog_number, model_number, serial_number, ahri_tag,
    project_name, storage_requirements, units, calibration_required,
    compatible_analyzers, equipment_condition, installation_date,
    last_service_date, last_maintenance_date, current_location,
    is_active, last_updated, version
) VALUES
-- PCR Equipment
(
    nextval('clinlims.inventory_item_seq'),
    gen_random_uuid(),
    'Applied Biosystems QuantStudio 3',
    'Real-Time PCR System for quantitative PCR applications',
    'CARTRIDGE',
    'PCR Equipment',
    'Applied Biosystems',
    'A28567',
    'QuantStudio-3',
    'QS3-2024-001',
    'AHRI-PCR-001',
    'Central Laboratory',
    'Room Temperature',
    'units',
    'Y',
    '96-well plates, 384-well plates',
    'functional',
    '2023-06-15'::timestamp,
    '2024-11-20'::timestamp,
    '2024-12-01'::timestamp,
    'PCR Laboratory - Room 201',
    'Y',
    NOW(),
    1
),
-- Microscopy Equipment
(
    nextval('clinlims.inventory_item_seq'),
    gen_random_uuid(),
    'Olympus BX43 Microscope',
    'Clinical microscope for pathology and cytology',
    'CARTRIDGE',
    'Microscopy',
    'Olympus',
    'BX43F',
    'BX43',
    'OLY-BX43-2023-045',
    'AHRI-MICRO-002',
    'Pathology Department',
    'Room Temperature',
    'units',
    'Y',
    'Brightfield, Fluorescence objectives',
    'functional',
    '2023-03-20'::timestamp,
    '2024-10-15'::timestamp,
    '2024-11-10'::timestamp,
    'Pathology Lab - Station 3',
    'Y',
    NOW(),
    1
),
-- Centrifuge
(
    nextval('clinlims.inventory_item_seq'),
    gen_random_uuid(),
    'Eppendorf Centrifuge 5810 R',
    'Refrigerated benchtop centrifuge with swing-bucket rotor',
    'CARTRIDGE',
    'Centrifugation',
    'Eppendorf',
    '5811000320',
    '5810R',
    'EPP-5810R-2024-012',
    'AHRI-CENT-003',
    'Sample Processing Lab',
    'Room Temperature',
    'units',
    'Y',
    'A-4-81 rotor, F-34-6-38 rotor',
    'functional',
    '2024-01-10'::timestamp,
    '2024-09-05'::timestamp,
    '2024-11-15'::timestamp,
    'Sample Prep - Bench 2',
    'Y',
    NOW(),
    1
),
-- Spectrophotometer
(
    nextval('clinlims.inventory_item_seq'),
    gen_random_uuid(),
    'Thermo NanoDrop One',
    'Microvolume UV-Vis spectrophotometer for nucleic acid quantification',
    'CARTRIDGE',
    'Spectrophotometry',
    'Thermo Fisher Scientific',
    'ND-ONE-W',
    'NanoDrop One',
    'ND-ONE-2024-007',
    'AHRI-SPEC-004',
    'Molecular Biology Lab',
    'Room Temperature',
    'units',
    'Y',
    'Pedestal, sample arm',
    'functional',
    '2023-11-25'::timestamp,
    '2024-08-30'::timestamp,
    '2024-10-22'::timestamp,
    'Molecular Lab - Hood 1',
    'Y',
    NOW(),
    1
),
-- Incubator
(
    nextval('clinlims.inventory_item_seq'),
    gen_random_uuid(),
    'Thermo Heratherm IMC18',
    'Microbiological incubator with natural convection',
    'CARTRIDGE',
    'Incubation',
    'Thermo Fisher Scientific',
    '51028112',
    'Heratherm IMC18',
    'TH-IMC18-2023-089',
    'AHRI-INCU-005',
    'Microbiology Department',
    'Room Temperature',
    'units',
    'Y',
    'Perforated shelves, timer',
    'functional',
    '2023-08-14'::timestamp,
    '2024-07-20'::timestamp,
    '2024-09-18'::timestamp,
    'Microbiology - Corner Unit',
    'Y',
    NOW(),
    1
),
-- Biosafety Cabinet
(
    nextval('clinlims.inventory_item_seq'),
    gen_random_uuid(),
    'Baker SterilGARD e3 Class II',
    'Biological safety cabinet Class II Type A2',
    'CARTRIDGE',
    'Safety Equipment',
    'Baker Company',
    'SG403A-HE',
    'SterilGARD e3',
    'BK-SG403-2022-156',
    'AHRI-SAFE-006',
    'Central Laboratory',
    'Room Temperature',
    'units',
    'Y',
    'HEPA filters, UV light',
    'functional',
    '2022-12-05'::timestamp,
    '2024-06-15'::timestamp,
    '2024-11-20'::timestamp,
    'Central Lab - BSC Station 1',
    'Y',
    NOW(),
    1
),
-- Gel Electrophoresis
(
    nextval('clinlims.inventory_item_seq'),
    gen_random_uuid(),
    'Bio-Rad Mini-PROTEAN Tetra',
    'Vertical electrophoresis system for protein gels',
    'CARTRIDGE',
    'Electrophoresis',
    'Bio-Rad',
    '1658004',
    'Mini-PROTEAN Tetra',
    'BR-MPT-2024-023',
    'AHRI-ELEC-007',
    'Protein Analysis Lab',
    'Room Temperature',
    'units',
    'N',
    'Mini gels, combs, plates',
    'functional',
    '2024-09-30'::timestamp,
    '2024-11-10'::timestamp,
    '2024-12-05'::timestamp,
    'Protein Lab - Electrophoresis Bay',
    'Y',
    NOW(),
    1
),
-- Autoclave
(
    nextval('clinlims.inventory_item_seq'),
    gen_random_uuid(),
    'Tuttnauer 3870EA',
    'Steam sterilizer autoclave for laboratory equipment',
    'CARTRIDGE',
    'Sterilization',
    'Tuttnauer',
    '3870EA',
    '3870EA',
    'TUT-3870EA-2023-067',
    'AHRI-AUTO-008',
    'Sterilization Unit',
    'Room Temperature',
    'units',
    'Y',
    'Baskets, trays, biological indicators',
    'functional',
    '2023-04-18'::timestamp,
    '2024-05-25'::timestamp,
    '2024-08-12'::timestamp,
    'Sterilization Room',
    'Y',
    NOW(),
    1
),
-- Balance
(
    nextval('clinlims.inventory_item_seq'),
    gen_random_uuid(),
    'Mettler Toledo XPE205',
    'Analytical balance with 0.01mg readability',
    'CARTRIDGE',
    'Weighing',
    'Mettler Toledo',
    '30100006',
    'XPE205',
    'MT-XPE205-2024-034',
    'AHRI-BAL-009',
    'Chemistry Lab',
    'Room Temperature',
    'units',
    'Y',
    'Draft shield, weighing pan',
    'functional',
    '2024-07-22'::timestamp,
    '2024-10-30'::timestamp,
    '2024-11-25'::timestamp,
    'Chemistry Lab - Balance Station',
    'Y',
    NOW(),
    1
),
-- Plate Reader
(
    nextval('clinlims.inventory_item_seq'),
    gen_random_uuid(),
    'BioTek ELx800',
    'Microplate reader for ELISA and colorimetric assays',
    'CARTRIDGE',
    'Plate Reading',
    'BioTek',
    'ELX800NB',
    'ELx800',
    'BT-ELx800-2023-098',
    'AHRI-READER-010',
    'Immunology Lab',
    'Room Temperature',
    'units',
    'Y',
    '96-well plates, 384-well plates',
    'functional',
    '2023-02-28'::timestamp,
    '2024-04-18'::timestamp,
    '2024-07-08'::timestamp,
    'Immunology Lab - Reader Bay',
    'Y',
    NOW(),
    1
),

-- BIOANALYTICAL ANALYZERS --

-- 1. LC-MS/MS System
(
    nextval('clinlims.inventory_item_seq'),
    gen_random_uuid(),
    'LC-MS/MS',
    'Triple Quadrupole LC-MS/MS for bioanalytical testing and pharmacokinetic analysis',
    'CARTRIDGE',
    'Bioanalytical - LC-MS/MS',
    'Agilent Technologies',
    'G6470B',
    'LC-MS/MS 6470',
    'SN-2024-LCMS-001',
    'AHRI-LCMS-001',
    'Bioanalytical Laboratory',
    'Room Temperature, Power Supply 220V',
    'units',
    'Y',
    'Plasma, Serum, Urine, Whole Blood, API samples',
    'functional',
    '2023-11-15'::timestamp,
    '2024-11-01'::timestamp,
    '2024-12-10'::timestamp,
    'Bioanalytical Lab - Instrument Bay 1',
    'Y',
    NOW(),
    1
),

-- 2. HPLC System
(
    nextval('clinlims.inventory_item_seq'),
    gen_random_uuid(),
    'HPLC',
    'High Performance Liquid Chromatography for assay and purity testing',
    'CARTRIDGE',
    'Bioanalytical - HPLC',
    'Waters',
    'WAT054940',
    'ACQUITY HPLC',
    'SN-2024-HPLC-001',
    'AHRI-HPLC-001',
    'Bioanalytical Laboratory',
    'Room Temperature, Power Supply 220V',
    'units',
    'Y',
    'API, Tablet, Capsule, Suspension samples',
    'functional',
    '2023-09-20'::timestamp,
    '2024-10-15'::timestamp,
    '2024-11-28'::timestamp,
    'Bioanalytical Lab - Instrument Bay 2',
    'Y',
    NOW(),
    1
),

-- 3. Dissolution Apparatus
(
    nextval('clinlims.inventory_item_seq'),
    gen_random_uuid(),
    'Dissolution Apparatus',
    'Dissolution testing apparatus for pharmaceutical dosage forms per USP <711>',
    'CARTRIDGE',
    'Bioanalytical - Dissolution',
    'Agilent Technologies',
    'VK-770',
    'USP Apparatus II',
    'SN-2024-DISS-001',
    'AHRI-DISS-001',
    'Pharmaceutical QC Laboratory',
    'Room Temperature, Temperature Control 37°C',
    'units',
    'Y',
    'Tablets, Capsules, Suspensions',
    'functional',
    '2024-02-28'::timestamp,
    '2024-10-22'::timestamp,
    '2024-11-30'::timestamp,
    'Pharma QC Lab - Dissolution Bay',
    'Y',
    NOW(),
    1
),

-- 4. Disintegration Tester
(
    nextval('clinlims.inventory_item_seq'),
    gen_random_uuid(),
    'Disintegration Tester',
    'Automated disintegration testing for tablets and capsules per USP <701>',
    'CARTRIDGE',
    'Bioanalytical - Disintegration',
    'Agilent Technologies',
    'VK-710',
    'Disintegration Tester',
    'SN-2024-DISINT-001',
    'AHRI-DISINT-001',
    'Pharmaceutical QC Laboratory',
    'Room Temperature, Temperature Control 37°C',
    'units',
    'N',
    'Tablets, Capsules, Pellets',
    'functional',
    '2024-01-15'::timestamp,
    '2024-09-30'::timestamp,
    '2024-11-20'::timestamp,
    'Pharma QC Lab - Physical Testing Station',
    'Y',
    NOW(),
    1
),

-- 5. Hardness Tester
(
    nextval('clinlims.inventory_item_seq'),
    gen_random_uuid(),
    'Hardness Tester',
    'Digital hardness tester for tablets and capsules measurement in Kiloponds',
    'CARTRIDGE',
    'Bioanalytical - Hardness',
    'Schleuniger',
    '2E',
    'Hardness Tester 2E',
    'SN-2024-HARD-001',
    'AHRI-HARD-001',
    'Pharmaceutical QC Laboratory',
    'Room Temperature',
    'units',
    'Y',
    'Tablets, Capsules, Compacts',
    'functional',
    '2023-08-10'::timestamp,
    '2024-08-25'::timestamp,
    '2024-10-15'::timestamp,
    'Pharma QC Lab - Physical Testing Station',
    'Y',
    NOW(),
    1
),

-- 6. Friability Tester
(
    nextval('clinlims.inventory_item_seq'),
    gen_random_uuid(),
    'Friability Tester',
    'Automated friability tester for measuring tablet abrasion resistance per USP <1216>',
    'CARTRIDGE',
    'Bioanalytical - Friability',
    'Erweka',
    'TA-3',
    'TA-3 Friability Tester',
    'SN-2024-FRIA-001',
    'AHRI-FRIA-001',
    'Pharmaceutical QC Laboratory',
    'Room Temperature',
    'units',
    'N',
    'Tablets (6.5 mm or larger)',
    'functional',
    '2024-03-05'::timestamp,
    '2024-09-12'::timestamp,
    '2024-11-08'::timestamp,
    'Pharma QC Lab - Physical Testing Station',
    'Y',
    NOW(),
    1
),

-- 7. Stability Chamber
(
    nextval('clinlims.inventory_item_seq'),
    gen_random_uuid(),
    'Stability Chamber',
    'Environmental chamber for stability studies per ICH and USP guidelines',
    'CARTRIDGE',
    'Bioanalytical - Environmental',
    'Binder',
    'MK-115',
    'MK 115 Climate Chamber',
    'SN-2024-STAB-001',
    'AHRI-STAB-001',
    'Stability Testing Laboratory',
    'Controlled Temperature 25°C, Humidity 60%',
    'units',
    'Y',
    'Pharmaceuticals, APIs, Formulations',
    'functional',
    '2023-05-20'::timestamp,
    '2024-06-30'::timestamp,
    '2024-11-10'::timestamp,
    'Stability Lab - Chamber Unit 1',
    'Y',
    NOW(),
    1
),

-- 8. UV-Vis Spectrophotometer
(
    nextval('clinlims.inventory_item_seq'),
    gen_random_uuid(),
    'UV-Vis Spectrophotometer',
    'Double-beam UV-Vis spectrophotometer for pharmaceutical assays and purity',
    'CARTRIDGE',
    'Bioanalytical - UV-Vis',
    'PerkinElmer',
    'N7820001',
    'Lambda 1050+',
    'SN-2024-UVVIS-001',
    'AHRI-UVVIS-001',
    'Analytical Chemistry Laboratory',
    'Room Temperature, Power Supply 220V',
    'units',
    'Y',
    'Solution, Powder, Tablet samples',
    'functional',
    '2023-12-01'::timestamp,
    '2024-09-05'::timestamp,
    '2024-11-15'::timestamp,
    'Analytical Lab - Instrument Station 2',
    'Y',
    NOW(),
    1
),

-- 9. FTIR Spectrometer
(
    nextval('clinlims.inventory_item_seq'),
    gen_random_uuid(),
    'FTIR',
    'Fourier Transform Infrared spectroscopy for identity testing and purity analysis',
    'CARTRIDGE',
    'Bioanalytical - FTIR',
    'Bruker',
    'D2-80400',
    'ALPHA II',
    'SN-2024-FTIR-001',
    'AHRI-FTIR-001',
    'Analytical Chemistry Laboratory',
    'Room Temperature, Power Supply 220V',
    'units',
    'Y',
    'Powders, Solids, Solutions, Reference standards',
    'functional',
    '2023-07-18'::timestamp,
    '2024-08-20'::timestamp,
    '2024-10-30'::timestamp,
    'Analytical Lab - Instrument Station 1',
    'Y',
    NOW(),
    1
),

-- 10. Laboratory Freezers (-20°C, -80°C)
(
    nextval('clinlims.inventory_item_seq'),
    gen_random_uuid(),
    'Freezers (-20°C, -80°C)',
    'Ultra-low temperature freezer for bioanalytical sample storage (-80°C)',
    'CARTRIDGE',
    'Bioanalytical - Storage',
    'Thermo Fisher Scientific',
    'ULT1390-3-A12',
    'Revco ULT3000-D80',
    'SN-2024-FREEZ-001',
    'AHRI-FREEZ-001',
    'Bioanalytical Laboratory',
    'Ultra-Low Temperature -80°C, Power Supply 220V',
    'units',
    'Y',
    'Plasma, Serum, Biological samples',
    'functional',
    '2023-10-25'::timestamp,
    '2024-11-05'::timestamp,
    '2024-12-01'::timestamp,
    'Bioanalytical Lab - Sample Storage Room',
    'Y',
    NOW(),
    1
),

-- 11. Water Purification System
(
    nextval('clinlims.inventory_item_seq'),
    gen_random_uuid(),
    'Millipore Water Purification',
    'Laboratory water purification system for HPLC and analytical reagent preparation',
    'CARTRIDGE',
    'Bioanalytical - Utilities',
    'Millipore Sigma',
    'MQI70001',
    'Milli-Q IQ 7000',
    'SN-2024-WATER-001',
    'AHRI-WATER-001',
    'Bioanalytical Laboratory',
    'Room Temperature, Ambient Pressure',
    'units',
    'Y',
    'HPLC solvents, Reagent preparation',
    'functional',
    '2023-06-30'::timestamp,
    '2024-10-10'::timestamp,
    '2024-11-25'::timestamp,
    'Bioanalytical Lab - Utilities Room',
    'Y',
    NOW(),
    1
);

-- Insert equipment lots with realistic data and unified storage location
INSERT INTO clinlims.inventory_lot (
    id, fhir_uuid, inventory_item_id, lot_number, initial_quantity, current_quantity,
    unit_size, expiration_date, qc_status, status,
    location_id, location_type, storage_path,
    last_updated, version
)
SELECT
    nextval('clinlims.inventory_lot_seq'),
    gen_random_uuid(),
    ii.id,
    CASE
        WHEN ii.name LIKE '%QuantStudio%' THEN 'QS-2024-001'
        WHEN ii.name LIKE '%Olympus%' THEN 'OLY-2023-045'
        WHEN ii.name LIKE '%Eppendorf%' THEN 'EPP-2024-012'
        WHEN ii.name LIKE '%NanoDrop%' THEN 'ND-2024-007'
        WHEN ii.name LIKE '%Heratherm%' THEN 'TH-2023-089'
        WHEN ii.name LIKE '%SterilGARD%' THEN 'BK-2022-156'
        WHEN ii.name LIKE '%Mini-PROTEAN%' THEN 'BR-2024-023'
        WHEN ii.name LIKE '%Tuttnauer%' THEN 'TUT-2023-067'
        WHEN ii.name LIKE '%Mettler%' THEN 'MT-2024-034'
        WHEN ii.name LIKE '%BioTek%' THEN 'BT-2023-098'
        ELSE 'GEN-' || EXTRACT(year FROM NOW()) || '-' || LPAD(FLOOR(RANDOM() * 999 + 1)::text, 3, '0')
    END,
    1.0,
    1.0,
    '1 unit',
    CASE
        WHEN ii.name LIKE '%QuantStudio%' THEN '2029-06-15'::timestamp
        WHEN ii.name LIKE '%Olympus%' THEN '2028-03-20'::timestamp
        WHEN ii.name LIKE '%Eppendorf%' THEN '2030-01-10'::timestamp
        WHEN ii.name LIKE '%NanoDrop%' THEN '2027-11-25'::timestamp
        WHEN ii.name LIKE '%Heratherm%' THEN '2029-08-14'::timestamp
        WHEN ii.name LIKE '%SterilGARD%' THEN '2026-12-05'::timestamp
        WHEN ii.name LIKE '%Mini-PROTEAN%' THEN '2028-09-30'::timestamp
        WHEN ii.name LIKE '%Tuttnauer%' THEN '2031-04-18'::timestamp
        WHEN ii.name LIKE '%Mettler%' THEN '2027-07-22'::timestamp
        WHEN ii.name LIKE '%BioTek%' THEN '2028-02-28'::timestamp
        ELSE (NOW() + INTERVAL '3 years')::timestamp
    END,
    'PASSED',
    'ACTIVE',
    -- All equipment stored at Main Laboratory > Ultra-Low Freezer 1 > Shelf 1 > Rack 1 > Plate ULF1-S1-R1-003
    10002,
    'box',
    'Main Laboratory > Ultra-Low Freezer 1 > Shelf 1 > Rack 1 > Plate ULF1-S1-R1-003',
    NOW(),
    1
FROM clinlims.inventory_item ii
WHERE ii.item_type = 'CARTRIDGE'
AND ii.project_name IN ('Central Laboratory', 'Pathology Department', 'Sample Processing Lab',
                        'Molecular Biology Lab', 'Microbiology Department', 'Protein Analysis Lab',
                        'Sterilization Unit', 'Chemistry Lab', 'Immunology Lab',
                        'Bioanalytical Laboratory', 'Pharmaceutical QC Laboratory',
                        'Analytical Chemistry Laboratory', 'Stability Testing Laboratory');

-- Insert initial receipt transactions for equipment
INSERT INTO clinlims.inventory_transaction (
    id, lot_id, transaction_type, quantity_change, quantity_after,
    transaction_date, notes, performed_by_user, last_updated
)
SELECT
    nextval('clinlims.inventory_transaction_seq'),
    il.id,
    'RECEIPT',
    il.initial_quantity,
    il.current_quantity,
    NOW() - INTERVAL '30 days',
    'Equipment received and commissioned',
    (SELECT id FROM clinlims.system_user WHERE login_name = 'admin' LIMIT 1),
    NOW()
FROM clinlims.inventory_lot il
INNER JOIN clinlims.inventory_item ii ON il.inventory_item_id = ii.id
WHERE ii.item_type = 'CARTRIDGE'
AND ii.project_name IN ('Central Laboratory', 'Pathology Department', 'Sample Processing Lab',
                        'Molecular Biology Lab', 'Microbiology Department', 'Protein Analysis Lab',
                        'Sterilization Unit', 'Chemistry Lab', 'Immunology Lab',
                        'Bioanalytical Laboratory', 'Pharmaceutical QC Laboratory',
                        'Analytical Chemistry Laboratory', 'Stability Testing Laboratory');

COMMIT TRANSACTION;
EOF

echo "📥 Importing sample equipment records..."

# Run the equipment import
docker exec -i openelisglobal-database psql -U clinlims -d clinlims < sample_equipment.sql

echo "🔍 Verifying equipment import..."

# Check results
docker exec openelisglobal-database psql -U clinlims -d clinlims -c "
SELECT
    'Equipment Items' as type,
    COUNT(*) as count
FROM clinlims.inventory_item
WHERE item_type = 'CARTRIDGE'
AND project_name IN ('Central Laboratory', 'Pathology Department', 'Sample Processing Lab',
                      'Molecular Biology Lab', 'Microbiology Department', 'Protein Analysis Lab',
                      'Sterilization Unit', 'Chemistry Lab', 'Immunology Lab',
                      'Bioanalytical Laboratory', 'Pharmaceutical QC Laboratory',
                      'Analytical Chemistry Laboratory', 'Stability Testing Laboratory')
UNION ALL
SELECT
    'Equipment Lots' as type,
    COUNT(*) as count
FROM clinlims.inventory_lot il
INNER JOIN clinlims.inventory_item ii ON il.inventory_item_id = ii.id
WHERE ii.item_type = 'CARTRIDGE'
AND ii.project_name IN ('Central Laboratory', 'Pathology Department', 'Sample Processing Lab',
                        'Molecular Biology Lab', 'Microbiology Department', 'Protein Analysis Lab',
                        'Sterilization Unit', 'Chemistry Lab', 'Immunology Lab',
                        'Bioanalytical Laboratory', 'Pharmaceutical QC Laboratory',
                        'Analytical Chemistry Laboratory', 'Stability Testing Laboratory');
"

echo ""
echo "🎉 Sample Equipment Import Complete!"
echo "   ✅ 21 equipment items added total"
echo "   ✅ 10 General Lab Equipment:"
echo "      PCR, Microscope, Centrifuge, Spectrophotometer, Incubator,"
echo "      Biosafety Cabinet, Electrophoresis, Autoclave, Balance, Plate Reader"
echo "   ✅ 11 Bioanalytical Analyzers:"
echo "      LC-MS/MS, HPLC, Dissolution Apparatus, Disintegration Tester,"
echo "      Hardness Tester, Friability Tester, Stability Chamber,"
echo "      UV-Vis Spectrophotometer, FTIR Spectrometer, Freezers, Water Purification"
echo "   ✅ Each with proper lots and transaction history"
echo ""
echo "🔗 Test the equipment datatable at: http://localhost:8080/OpenELIS/inventory"
echo "🔍 Filter by 'Bioanalytical' category to see the bioanalytical analyzers"
echo "🔍 These analyzers can now be selected in Stage 2 Test Assignment"

# Cleanup
rm -f sample_equipment.sql
