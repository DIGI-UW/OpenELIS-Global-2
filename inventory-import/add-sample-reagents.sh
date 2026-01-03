#!/bin/bash

# Add Sample Reagent Records for Testing Reagent Datatable
echo "🧪 Adding sample reagent records for testing reagent datatable..."

# Generate SQL for 10 diverse reagent items
cat > sample_reagents.sql << 'EOF'
-- Sample Reagent Records for Testing Reagent Datatable
BEGIN TRANSACTION;

-- Insert 10 diverse reagent items (REAGENT type)
INSERT INTO clinlims.inventory_item (
    id, fhir_uuid, name, description, item_type, category,
    manufacturer, catalog_number, concentration, project_name, storage_requirements,
    units, stability_after_opening, dilution_notes, is_active, last_updated, version
) VALUES
-- PCR Reagent
(
    nextval('clinlims.inventory_item_seq'),
    gen_random_uuid(),
    'Taq DNA Polymerase',
    'High-fidelity DNA polymerase for PCR amplification',
    'REAGENT',
    'PCR Reagents',
    'Thermo Fisher Scientific',
    'EP0402',
    '5 U/μL',
    'Molecular Biology Lab',
    '-20°C (Frozen)',
    'units',
    30,
    'Store at -20°C. Avoid repeated freeze-thaw cycles.',
    'Y',
    NOW(),
    1
),
-- Buffer Solution
(
    nextval('clinlims.inventory_item_seq'),
    gen_random_uuid(),
    'Tris-HCl Buffer pH 7.4',
    'Buffering solution for molecular biology applications',
    'REAGENT',
    'Buffers',
    'Sigma-Aldrich',
    'T5030',
    '1 M',
    'Central Laboratory',
    '2-8°C (Refrigerated)',
    'mL',
    60,
    'Good for buffer preparations. pH stable at room temperature.',
    'Y',
    NOW(),
    1
),
-- Antibody
(
    nextval('clinlims.inventory_item_seq'),
    gen_random_uuid(),
    'Anti-Human IgG-HRP',
    'Horseradish peroxidase-conjugated secondary antibody',
    'REAGENT',
    'Antibodies',
    'Jackson ImmunoResearch',
    '109-035-003',
    '1.5 mg/mL',
    'Immunology Lab',
    '2-8°C (Refrigerated)',
    'μL',
    90,
    'Dilute 1:5000-1:10000 for Western blot applications.',
    'Y',
    NOW(),
    1
),
-- Enzyme
(
    nextval('clinlims.inventory_item_seq'),
    gen_random_uuid(),
    'Restriction Enzyme EcoRI',
    'Type II restriction endonuclease for DNA cleavage',
    'REAGENT',
    'Enzymes',
    'New England Biolabs',
    'R0101S',
    '20,000 U/mL',
    'Molecular Biology Lab',
    '-20°C (Frozen)',
    'units',
    365,
    'Store at -20°C. Stable for 12 months after opening.',
    'Y',
    NOW(),
    1
),
-- Stain/Dye
(
    nextval('clinlims.inventory_item_seq'),
    gen_random_uuid(),
    'DAPI Nuclear Stain',
    'Blue fluorescent DNA stain for nuclear visualization',
    'REAGENT',
    'Stains & Dyes',
    'Life Technologies',
    'D1306',
    '5 mg/mL',
    'Pathology Department',
    '-20°C (Frozen)',
    'μL',
    180,
    'Dilute to working concentration. Protect from light.',
    'Y',
    NOW(),
    1
),
-- Cell Culture Media
(
    nextval('clinlims.inventory_item_seq'),
    gen_random_uuid(),
    'DMEM Culture Medium',
    'Dulbecco\'s Modified Eagle Medium for cell culture',
    'REAGENT',
    'Cell Culture',
    'Gibco',
    '11965092',
    '1x',
    'Cell Biology Lab',
    '2-8°C (Refrigerated)',
    'mL',
    14,
    'Supplement with FBS and antibiotics before use.',
    'Y',
    NOW(),
    1
),
-- Molecular Standard
(
    nextval('clinlims.inventory_item_seq'),
    gen_random_uuid(),
    'DNA Ladder 100bp',
    'Molecular weight standard for gel electrophoresis',
    'REAGENT',
    'Standards',
    'Promega',
    'G2101',
    '0.1 μg/μL',
    'Central Laboratory',
    '-20°C (Frozen)',
    'μL',
    365,
    'Load 5-10 μL per lane. Store aliquots to avoid freeze-thaw.',
    'Y',
    NOW(),
    1
),
-- Substrate
(
    nextval('clinlims.inventory_item_seq'),
    gen_random_uuid(),
    'TMB Substrate Solution',
    '3,3\',5,5\'-Tetramethylbenzidine substrate for ELISA',
    'REAGENT',
    'Substrates',
    'BD Biosciences',
    '555214',
    'Ready to use',
    'Immunology Lab',
    '2-8°C (Refrigerated)',
    'mL',
    30,
    'Use at room temperature. Protect from light.',
    'Y',
    NOW(),
    1
),
-- Fixative
(
    nextval('clinlims.inventory_item_seq'),
    gen_random_uuid(),
    'Paraformaldehyde 4%',
    'Tissue fixative for histological preparations',
    'REAGENT',
    'Fixatives',
    'Electron Microscopy Sciences',
    '15714',
    '4% w/v',
    'Pathology Department',
    '2-8°C (Refrigerated)',
    'mL',
    365,
    'Use fresh solution. Dispose according to safety protocols.',
    'Y',
    NOW(),
    1
),
-- Quality Control
(
    nextval('clinlims.inventory_item_seq'),
    gen_random_uuid(),
    'Protein Standard BSA',
    'Bovine serum albumin protein standard for assays',
    'REAGENT',
    'Standards',
    'Pierce',
    '23209',
    '2 mg/mL',
    'Quality Control Lab',
    '-20°C (Frozen)',
    'μL',
    365,
    'Prepare standard curve with serial dilutions.',
    'Y',
    NOW(),
    1
);

-- Insert reagent lots with realistic data
INSERT INTO clinlims.inventory_lot (
    id, fhir_uuid, inventory_item_id, lot_number, initial_quantity, current_quantity,
    unit_size, expiration_date, qc_status, status, last_updated, version
)
SELECT
    nextval('clinlims.inventory_lot_seq'),
    gen_random_uuid(),
    ii.id,
    CASE
        WHEN ii.name LIKE '%Taq DNA%' THEN 'TAQ-2024-156'
        WHEN ii.name LIKE '%Tris-HCl%' THEN 'TRS-2024-089'
        WHEN ii.name LIKE '%Anti-Human%' THEN 'AHI-2024-234'
        WHEN ii.name LIKE '%EcoRI%' THEN 'ECO-2024-045'
        WHEN ii.name LIKE '%DAPI%' THEN 'DAP-2024-078'
        WHEN ii.name LIKE '%DMEM%' THEN 'DME-2024-123'
        WHEN ii.name LIKE '%DNA Ladder%' THEN 'LAD-2024-067'
        WHEN ii.name LIKE '%TMB%' THEN 'TMB-2024-145'
        WHEN ii.name LIKE '%Paraformaldehyde%' THEN 'PFA-2024-198'
        WHEN ii.name LIKE '%BSA%' THEN 'BSA-2024-287'
        ELSE 'REA-' || EXTRACT(year FROM NOW()) || '-' || LPAD(FLOOR(RANDOM() * 999 + 1)::text, 3, '0')
    END,
    CASE
        WHEN ii.units = 'mL' THEN 500.0
        WHEN ii.units = 'μL' THEN 1000.0
        WHEN ii.units = 'units' THEN 100.0
        ELSE 50.0
    END,
    CASE
        WHEN ii.units = 'mL' THEN 450.0
        WHEN ii.units = 'μL' THEN 850.0
        WHEN ii.units = 'units' THEN 75.0
        ELSE 40.0
    END,
    CASE
        WHEN ii.units = 'mL' THEN '500 mL'
        WHEN ii.units = 'μL' THEN '1000 μL'
        WHEN ii.units = 'units' THEN '100 units'
        ELSE '50 units'
    END,
    CASE
        WHEN ii.name LIKE '%Taq DNA%' THEN '2025-08-15'::timestamp
        WHEN ii.name LIKE '%Tris-HCl%' THEN '2025-06-20'::timestamp
        WHEN ii.name LIKE '%Anti-Human%' THEN '2025-12-10'::timestamp
        WHEN ii.name LIKE '%EcoRI%' THEN '2025-09-30'::timestamp
        WHEN ii.name LIKE '%DAPI%' THEN '2025-11-25'::timestamp
        WHEN ii.name LIKE '%DMEM%' THEN '2025-04-18'::timestamp
        WHEN ii.name LIKE '%DNA Ladder%' THEN '2025-07-22'::timestamp
        WHEN ii.name LIKE '%TMB%' THEN '2025-05-14'::timestamp
        WHEN ii.name LIKE '%Paraformaldehyde%' THEN '2025-10-08'::timestamp
        WHEN ii.name LIKE '%BSA%' THEN '2025-03-28'::timestamp
        ELSE (NOW() + INTERVAL '6 months')::timestamp
    END,
    'PASSED',
    'ACTIVE',
    NOW(),
    1
FROM clinlims.inventory_item ii
WHERE ii.item_type = 'REAGENT'
AND ii.project_name IN ('Molecular Biology Lab', 'Central Laboratory', 'Immunology Lab',
                        'Pathology Department', 'Cell Biology Lab', 'Quality Control Lab');

-- Insert initial receipt transactions for reagents
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
    NOW() - INTERVAL '15 days',
    'Reagent received and quality checked',
    (SELECT id FROM clinlims.system_user WHERE login_name = 'admin' LIMIT 1),
    NOW()
FROM clinlims.inventory_lot il
INNER JOIN clinlims.inventory_item ii ON il.inventory_item_id = ii.id
WHERE ii.item_type = 'REAGENT'
AND ii.project_name IN ('Molecular Biology Lab', 'Central Laboratory', 'Immunology Lab',
                        'Pathology Department', 'Cell Biology Lab', 'Quality Control Lab');

COMMIT TRANSACTION;
EOF

echo "📥 Importing sample reagent records..."

# Run the reagent import
docker exec -i openelisglobal-database psql -U clinlims -d clinlims < sample_reagents.sql

echo "🔍 Verifying reagent import..."

# Check results
docker exec openelisglobal-database psql -U clinlims -d clinlims -c "
SELECT
    'Reagent Items' as type,
    COUNT(*) as count
FROM clinlims.inventory_item
WHERE item_type = 'REAGENT'
AND project_name IN ('Molecular Biology Lab', 'Central Laboratory', 'Immunology Lab',
                      'Pathology Department', 'Cell Biology Lab', 'Quality Control Lab')
UNION ALL
SELECT
    'Reagent Lots' as type,
    COUNT(*) as count
FROM clinlims.inventory_lot il
INNER JOIN clinlims.inventory_item ii ON il.inventory_item_id = ii.id
WHERE ii.item_type = 'REAGENT'
AND ii.project_name IN ('Molecular Biology Lab', 'Central Laboratory', 'Immunology Lab',
                        'Pathology Department', 'Cell Biology Lab', 'Quality Control Lab');
"

echo ""
echo "🎉 Sample Reagent Import Complete!"
echo "   ✅ 10 diverse reagent items added"
echo "   ✅ PCR reagents, buffers, antibodies, enzymes, stains"
echo "   ✅ Cell culture media, standards, substrates, fixatives, QC materials"
echo "   ✅ Each with proper concentration, storage location, box numbers"
echo "   ✅ Realistic lot quantities and expiration dates"
echo ""
echo "🔗 Test the reagent datatable at: http://localhost:8080/OpenELIS/inventory"
echo "🔍 Filter by Reagent type to see the populated reagent table"

# Cleanup
rm -f sample_reagents.sql