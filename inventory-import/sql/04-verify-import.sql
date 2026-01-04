-- MNTD Inventory Import Verification
-- Run this AFTER import to verify data integrity

BEGIN TRANSACTION;

-- Section 1: Import Statistics
-- ============================================================================

\echo '========== IMPORT STATISTICS =========='

SELECT
    COUNT(*) as total_items,
    COUNT(DISTINCT project_name) as projects,
    COUNT(DISTINCT item_type) as item_types
FROM inventory_item
WHERE project_name = 'MNTD Research';

SELECT
    COUNT(*) as total_lots,
    COUNT(DISTINCT status) as lot_statuses,
    COUNT(DISTINCT qc_status) as qc_statuses,
    COUNT(DISTINCT CASE WHEN expiration_date < NOW() THEN 1 END) as expired_lots
FROM inventory_lot
WHERE inventory_item_id IN (
    SELECT id FROM inventory_item WHERE project_name = 'MNTD Research'
);

-- Section 2: Data Quality Checks
-- ============================================================================

\echo '========== DATA QUALITY CHECKS =========='

-- Check for items without units
\echo 'Items without units (should be 0):'
SELECT COUNT(*) as items_missing_units FROM inventory_item
WHERE project_name = 'MNTD Research' AND (units IS NULL OR units = '');

-- Check for lots without lot numbers
\echo 'Lots without lot numbers (should be 0):'
SELECT COUNT(*) as lots_missing_lot_number FROM inventory_lot
WHERE inventory_item_id IN (SELECT id FROM inventory_item WHERE project_name = 'MNTD Research')
AND (lot_number IS NULL OR lot_number = '');

-- Check for negative quantities
\echo 'Lots with negative quantities (should be 0):'
SELECT COUNT(*) as negative_quantities FROM inventory_lot
WHERE inventory_item_id IN (SELECT id FROM inventory_item WHERE project_name = 'MNTD Research')
AND (initial_quantity < 0 OR current_quantity < 0);

-- Check for lots with NULL expiration dates
\echo 'Lots without expiration dates (check if intentional):'
SELECT COUNT(*) as null_expiration_dates FROM inventory_lot
WHERE inventory_item_id IN (SELECT id FROM inventory_item WHERE project_name = 'MNTD Research')
AND expiration_date IS NULL;

-- Section 3: Status Distribution
-- ============================================================================

\echo '========== LOT STATUS DISTRIBUTION =========='

SELECT
    status,
    COUNT(*) as count,
    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM inventory_lot
        WHERE inventory_item_id IN (SELECT id FROM inventory_item WHERE project_name = 'MNTD Research'))::numeric, 2) as percentage
FROM inventory_lot
WHERE inventory_item_id IN (SELECT id FROM inventory_item WHERE project_name = 'MNTD Research')
GROUP BY status
ORDER BY count DESC;

-- Section 4: QC Status Distribution
-- ============================================================================

\echo '========== QC STATUS DISTRIBUTION =========='

SELECT
    qc_status,
    COUNT(*) as count,
    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM inventory_lot
        WHERE inventory_item_id IN (SELECT id FROM inventory_item WHERE project_name = 'MNTD Research'))::numeric, 2) as percentage
FROM inventory_lot
WHERE inventory_item_id IN (SELECT id FROM inventory_item WHERE project_name = 'MNTD Research')
GROUP BY qc_status
ORDER BY count DESC;

-- Section 5: Expiration Analysis
-- ============================================================================

\echo '========== EXPIRATION ANALYSIS =========='

-- Lots expiring in next 30 days
\echo 'Lots expiring in next 30 days:'
SELECT COUNT(*) as expiring_soon FROM inventory_lot
WHERE inventory_item_id IN (SELECT id FROM inventory_item WHERE project_name = 'MNTD Research')
AND expiration_date BETWEEN NOW() AND NOW() + INTERVAL '30 days'
AND status != 'EXPIRED';

-- Expired lots
\echo 'Expired lots:'
SELECT COUNT(*) as expired FROM inventory_lot
WHERE inventory_item_id IN (SELECT id FROM inventory_item WHERE project_name = 'MNTD Research')
AND expiration_date < NOW();

-- Section 6: Item Type Distribution
-- ============================================================================

\echo '========== ITEM TYPE DISTRIBUTION =========='

SELECT
    item_type,
    COUNT(*) as count
FROM inventory_item
WHERE project_name = 'MNTD Research'
GROUP BY item_type
ORDER BY count DESC;

-- Section 7: Sample Data - Items
-- ============================================================================

\echo '========== SAMPLE IMPORTED ITEMS (First 10) =========='

SELECT
    id,
    name,
    item_type,
    category,
    units,
    is_active,
    created_date
FROM inventory_item
WHERE project_name = 'MNTD Research'
ORDER BY created_date DESC
LIMIT 10;

-- Section 8: Sample Data - Lots
-- ============================================================================

\echo '========== SAMPLE IMPORTED LOTS (First 10) =========='

SELECT
    il.id,
    ii.name as item_name,
    il.lot_number,
    il.initial_quantity,
    il.current_quantity,
    il.expiration_date,
    il.status,
    il.qc_status,
    il.storage_path
FROM inventory_lot il
JOIN inventory_item ii ON il.inventory_item_id = ii.id
WHERE ii.project_name = 'MNTD Research'
ORDER BY il.created_date DESC
LIMIT 10;

-- Section 9: Transaction Audit Trail
-- ============================================================================

\echo '========== IMPORT TRANSACTION COUNTS =========='

SELECT
    COUNT(*) as total_transactions,
    COUNT(DISTINCT CASE WHEN transaction_type = 'RECEIPT' THEN 1 END) as receipt_transactions,
    COUNT(DISTINCT CASE WHEN transaction_type = 'CONSUMPTION' THEN 1 END) as consumption_transactions
FROM inventory_transaction
WHERE lot_id IN (
    SELECT id FROM inventory_lot
    WHERE inventory_item_id IN (SELECT id FROM inventory_item WHERE project_name = 'MNTD Research')
);

-- Section 10: Storage Location Verification
-- ============================================================================

\echo '========== STORAGE LOCATION ASSIGNMENT =========='

SELECT
    COALESCE(location_type, 'UNASSIGNED') as location_type,
    COUNT(*) as count,
    COUNT(DISTINCT location_id) as unique_locations
FROM inventory_lot
WHERE inventory_item_id IN (SELECT id FROM inventory_item WHERE project_name = 'MNTD Research')
GROUP BY location_type
ORDER BY count DESC;

-- Lots without location assignment
\echo 'Lots without location assignment (should review):'
SELECT COUNT(*) as unlocated_lots FROM inventory_lot
WHERE inventory_item_id IN (SELECT id FROM inventory_item WHERE project_name = 'MNTD Research')
AND location_id IS NULL;

-- Section 11: Integrity Checks
-- ============================================================================

\echo '========== FOREIGN KEY INTEGRITY =========='

-- Check for orphaned lots (FK constraint should prevent this)
SELECT COUNT(*) as orphaned_lots FROM inventory_lot il
WHERE inventory_item_id IN (SELECT id FROM inventory_item WHERE project_name = 'MNTD Research')
AND il.inventory_item_id NOT IN (SELECT id FROM inventory_item);

-- Section 12: Final Status Summary
-- ============================================================================

\echo '========== FINAL IMPORT STATUS =========='

WITH stats AS (
    SELECT
        (SELECT COUNT(*) FROM inventory_item WHERE project_name = 'MNTD Research') as items,
        (SELECT COUNT(*) FROM inventory_lot WHERE inventory_item_id IN (SELECT id FROM inventory_item WHERE project_name = 'MNTD Research')) as lots,
        (SELECT COUNT(*) FROM inventory_transaction WHERE lot_id IN (SELECT id FROM inventory_lot WHERE inventory_item_id IN (SELECT id FROM inventory_item WHERE project_name = 'MNTD Research'))) as transactions,
        (SELECT COUNT(*) FROM inventory_lot WHERE inventory_item_id IN (SELECT id FROM inventory_item WHERE project_name = 'MNTD Research') AND expiration_date < NOW()) as expired
)
SELECT
    items,
    lots,
    transactions,
    expired,
    CASE
        WHEN items > 0 AND lots > 0 THEN 'IMPORT SUCCESSFUL'
        ELSE 'IMPORT INCOMPLETE - CHECK LOGS'
    END as status
FROM stats;

COMMIT;

-- End of verification script
\echo '========== VERIFICATION COMPLETE =========='
