package org.openelisglobal.inventory.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.inventory.valueholder.InventoryTransaction;
import org.springframework.beans.factory.annotation.Autowired;

public class InventoryTransactionServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private InventoryTransactionService inventoryTransactionService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/user-role.xml");
        executeDataSetWithStateManagement("testdata/inventory-test-data.xml");
    }

    @Test
    public void getAll_validCall_returnsNonNullList() {
        List<InventoryTransaction> transactions = inventoryTransactionService.getAll();
        assertNotNull(transactions);
    }

    @Test
    public void getAll_validCall_returnsTransactions() {
        List<InventoryTransaction> transactions = inventoryTransactionService.getAll();
        assertNotNull(transactions);
        assertTrue(transactions.size() >= 1);
    }

    @Test
    public void getByLotId_validId_returnsTransactions() {
        List<InventoryTransaction> transactions = inventoryTransactionService.getByLotId(1000L);
        assertNotNull(transactions);
        assertTrue(transactions.size() >= 1);
    }

    @Test
    public void getByLotId_invalidId_returnsEmpty() {
        List<InventoryTransaction> transactions = inventoryTransactionService.getByLotId(99999L);
        assertNotNull(transactions);
        assertTrue(transactions.isEmpty());
    }
}
