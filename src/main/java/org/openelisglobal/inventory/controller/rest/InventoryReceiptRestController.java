package org.openelisglobal.inventory.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import javax.validation.Valid;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.inventory.service.InventoryReceiptService;
import org.openelisglobal.inventory.valueholder.InventoryReceipt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/inventory-receipts")
public class InventoryReceiptRestController extends BaseRestController {

    @Autowired
    private InventoryReceiptService inventoryReceiptService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryReceipt>> getAllInventoryReceipts() {
        List<InventoryReceipt> receipts = inventoryReceiptService.getAllInventoryReceipts();
        return ResponseEntity.ok(receipts);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InventoryReceipt> getInventoryReceiptById(@PathVariable String id) {
        InventoryReceipt receipt = inventoryReceiptService.getInventoryReceiptById(id);
        if (receipt == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(receipt);
    }

    @GetMapping(value = "/by-item/{itemId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryReceipt>> getReceiptsByItem(@PathVariable String itemId) {
        List<InventoryReceipt> receipts = inventoryReceiptService.findByInventoryItemId(itemId);
        return ResponseEntity.ok(receipts);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InventoryReceipt> createInventoryReceipt(HttpServletRequest httpRequest,
            @Valid @RequestBody InventoryReceipt receipt) {
        receipt.setSysUserId(getSysUserId(httpRequest));
        String id = inventoryReceiptService.insert(receipt);
        InventoryReceipt created = inventoryReceiptService.getInventoryReceiptById(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
