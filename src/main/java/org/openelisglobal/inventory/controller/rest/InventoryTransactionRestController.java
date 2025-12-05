package org.openelisglobal.inventory.controller.rest;

import java.util.List;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.inventory.service.InventoryTransactionService;
import org.openelisglobal.inventory.valueholder.InventoryTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/inventory-transactions")
public class InventoryTransactionRestController extends BaseRestController {

    @Autowired
    private InventoryTransactionService inventoryTransactionService;

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InventoryTransaction> getInventoryTransactionById(@PathVariable String id) {
        InventoryTransaction transaction = inventoryTransactionService.get(id);
        if (transaction == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(transaction);
    }

    @GetMapping(value = "/by-lot/{lotId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryTransaction>> getTransactionsByLot(@PathVariable String lotId) {
        List<InventoryTransaction> transactions = inventoryTransactionService.findByLotId(lotId);
        return ResponseEntity.ok(transactions);
    }
}
