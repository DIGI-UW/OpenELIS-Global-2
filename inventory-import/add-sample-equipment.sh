#!/bin/bash

# Add Sample Equipment Records for Testing Equipment Datatable
echo "🔧 Adding sample equipment records for testing equipment datatable..."

# Generate SQL for 10 diverse equipment items
cat > sample_equipment.sql << 'EOF'
-- Sample Equipment Records for Testing Equipment Datatable
BEGIN TRANSACTION;

-- Insert 10 diverse equipment items (CARTRIDGE type for equipment)
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
);

-- Insert equipment lots with realistic data
INSERT INTO clinlims.inventory_lot (
    id, fhir_uuid, inventory_item_id, lot_number, initial_quantity, current_quantity,
    unit_size, expiration_date, qc_status, status, last_updated, version
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
    NOW(),
    1
FROM clinlims.inventory_item ii
WHERE ii.item_type = 'CARTRIDGE'
AND ii.project_name IN ('Central Laboratory', 'Pathology Department', 'Sample Processing Lab',
                        'Molecular Biology Lab', 'Microbiology Department', 'Protein Analysis Lab',
                        'Sterilization Unit', 'Chemistry Lab', 'Immunology Lab');

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
                        'Sterilization Unit', 'Chemistry Lab', 'Immunology Lab');

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
                      'Sterilization Unit', 'Chemistry Lab', 'Immunology Lab')
UNION ALL
SELECT
    'Equipment Lots' as type,
    COUNT(*) as count
FROM clinlims.inventory_lot il
INNER JOIN clinlims.inventory_item ii ON il.inventory_item_id = ii.id
WHERE ii.item_type = 'CARTRIDGE'
AND ii.project_name IN ('Central Laboratory', 'Pathology Department', 'Sample Processing Lab',
                        'Molecular Biology Lab', 'Microbiology Department', 'Protein Analysis Lab',
                        'Sterilization Unit', 'Chemistry Lab', 'Immunology Lab');
"

echo ""
echo "🎉 Sample Equipment Import Complete!"
echo "   ✅ 10 diverse equipment items added"
echo "   ✅ PCR, Microscope, Centrifuge, Spectrophotometer, Incubator"
echo "   ✅ Biosafety Cabinet, Electrophoresis, Autoclave, Balance, Plate Reader"
echo "   ✅ Each with proper lots and transaction history"
echo ""
echo "🔗 Test the equipment datatable at: http://localhost:8080/OpenELIS/inventory"
echo "🔍 Filter by Equipment type to see the populated equipment table"

# Cleanup
rm -f sample_equipment.sql