package org.openelisglobal.inventory.controller.rest;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.inventory.service.InventoryTransactionService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.TransactionType;
import org.openelisglobal.inventory.valueholder.InventoryTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/inventory/transactions")
public class InventoryTransactionRestController extends BaseRestController {

    @Autowired
    private InventoryTransactionService transactionService;

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InventoryTransaction> getById(@PathVariable String id) {
        try {
            InventoryTransaction transaction = transactionService.get(Long.valueOf(id));
            if (transaction == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(transaction);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/lot/{lotId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryTransaction>> getByLotId(@PathVariable String lotId) {
        try {
            List<InventoryTransaction> transactions = transactionService.getByLotId(Long.valueOf(lotId));
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/type/{transactionType}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryTransaction>> getByType(@PathVariable TransactionType transactionType) {
        try {
            List<InventoryTransaction> transactions = transactionService.getByTransactionType(transactionType);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/date-range", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryTransaction>> getByDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Timestamp startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Timestamp endDate) {
        try {
            List<InventoryTransaction> transactions = transactionService.getByDateRange(startDate, endDate);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/reference", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryTransaction>> getByReference(@RequestParam String referenceId,
            @RequestParam String referenceType) {
        try {
            List<InventoryTransaction> transactions = transactionService.getByReference(Long.valueOf(referenceId),
                    referenceType);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
