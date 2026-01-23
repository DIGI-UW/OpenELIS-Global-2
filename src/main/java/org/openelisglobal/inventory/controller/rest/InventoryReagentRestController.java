package org.openelisglobal.inventory.controller.rest;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.service.InventoryLotService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for reagent and instrument inventory endpoints. Provides
 * simplified API for frontend components like MNTDSampleProcessingPage that
 * need to select reagents and instruments for sample preparation.
 */
@RestController
@RequestMapping("/rest/inventory")
public class InventoryReagentRestController extends BaseRestController {

    @Autowired
    private InventoryItemService inventoryItemService;

    @Autowired
    private InventoryLotService inventoryLotService;

    /**
     * Get all active reagents with their available lots. Returns a flat list where
     * each entry represents a reagent item with lot information.
     *
     * @param status Optional filter - "active" returns only active items (default)
     * @return List of ReagentDTO objects
     */
    @GetMapping(value = "/reagents", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ReagentDTO>> getReagents(
            @RequestParam(required = false, defaultValue = "active") String status) {
        try {
            List<ReagentDTO> reagentDTOs = new ArrayList<>();

            // Get reagent items
            List<InventoryItem> reagentItems = inventoryItemService.getByItemType(ItemType.REAGENT);

            // Filter by active status if requested
            if ("active".equalsIgnoreCase(status)) {
                reagentItems = reagentItems.stream().filter(item -> "Y".equals(item.getIsActive())).toList();
            }

            // For each reagent item, get available lots and aggregate info
            for (InventoryItem item : reagentItems) {
                List<InventoryLot> lots = inventoryLotService.getAvailableLotsByItemFEFO(item.getId());

                ReagentDTO dto = new ReagentDTO();
                dto.setId(String.valueOf(item.getId()));
                dto.setItemId(item.getId());
                dto.setName(item.getName());
                dto.setDescription(item.getDescription());
                dto.setManufacturer(item.getManufacturer());
                dto.setCategory(item.getCategory());
                dto.setStorageRequirements(item.getStorageRequirements());

                if (!lots.isEmpty()) {
                    // Use the first lot (FEFO - earliest expiring) for display
                    InventoryLot primaryLot = lots.get(0);
                    dto.setLotId(primaryLot.getId());
                    dto.setLotNumber(primaryLot.getLotNumber());
                    dto.setExpirationDate(
                            primaryLot.getExpirationDate() != null ? primaryLot.getExpirationDate().toString() : null);
                    dto.setQcStatus(primaryLot.getQcStatus() != null ? primaryLot.getQcStatus().name() : null);

                    // Sum total quantity across all lots
                    double totalQuantity = lots.stream()
                            .mapToDouble(lot -> lot.getCurrentQuantity() != null ? lot.getCurrentQuantity() : 0.0)
                            .sum();
                    dto.setCurrentQuantity(totalQuantity);
                    dto.setTotalLots(lots.size());
                }

                reagentDTOs.add(dto);
            }

            return ResponseEntity.ok(reagentDTOs);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all active instruments (cartridges/analyzers) with their available lots.
     * Returns a flat list where each entry represents an instrument item with lot
     * information.
     *
     * @param status Optional filter - "active" returns only active items (default)
     * @return List of InstrumentDTO objects
     */
    @GetMapping(value = "/instruments", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InstrumentDTO>> getInstruments(
            @RequestParam(required = false, defaultValue = "active") String status) {
        try {
            List<InstrumentDTO> instrumentDTOs = new ArrayList<>();

            // Get cartridge items (instruments/analyzers)
            List<InventoryItem> cartridgeItems = inventoryItemService.getByItemType(ItemType.CARTRIDGE);

            // Filter by active status if requested
            if ("active".equalsIgnoreCase(status)) {
                cartridgeItems = cartridgeItems.stream().filter(item -> "Y".equals(item.getIsActive())).toList();
            }

            // For each cartridge item, get available lots and aggregate info
            for (InventoryItem item : cartridgeItems) {
                List<InventoryLot> lots = inventoryLotService.getAvailableLotsByItemFEFO(item.getId());

                InstrumentDTO dto = new InstrumentDTO();
                dto.setId(String.valueOf(item.getId()));
                dto.setItemId(item.getId());
                dto.setName(item.getName());
                dto.setDescription(item.getDescription());
                dto.setManufacturer(item.getManufacturer());
                dto.setCategory(item.getCategory());
                dto.setCompatibleAnalyzers(item.getCompatibleAnalyzers());
                dto.setCalibrationRequired("Y".equals(item.getCalibrationRequired()));

                if (!lots.isEmpty()) {
                    // Use the first lot for display (serial number)
                    InventoryLot primaryLot = lots.get(0);
                    dto.setLotId(primaryLot.getId());
                    dto.setSerialNumber(primaryLot.getLotNumber());
                    dto.setExpirationDate(
                            primaryLot.getExpirationDate() != null ? primaryLot.getExpirationDate().toString() : null);
                    dto.setQcStatus(primaryLot.getQcStatus() != null ? primaryLot.getQcStatus().name() : null);

                    // Sum total quantity across all lots
                    double totalQuantity = lots.stream()
                            .mapToDouble(lot -> lot.getCurrentQuantity() != null ? lot.getCurrentQuantity() : 0.0)
                            .sum();
                    dto.setCurrentQuantity(totalQuantity);
                    dto.setTotalUnits(lots.size());
                }

                instrumentDTOs.add(dto);
            }

            return ResponseEntity.ok(instrumentDTOs);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DTO for reagent information returned to frontend.
     */
    @Getter
    @Setter
    public static class ReagentDTO {
        private String id;
        private Long itemId;
        private Long lotId;
        private String name;
        private String description;
        private String manufacturer;
        private String category;
        private String storageRequirements;
        private String lotNumber;
        private String expirationDate;
        private Double currentQuantity;
        private String qcStatus;
        private Integer totalLots;
    }

    /**
     * DTO for instrument information returned to frontend.
     */
    @Getter
    @Setter
    public static class InstrumentDTO {
        private String id;
        private Long itemId;
        private Long lotId;
        private String name;
        private String description;
        private String manufacturer;
        private String category;
        private String compatibleAnalyzers;
        private Boolean calibrationRequired;
        private String serialNumber;
        private String expirationDate;
        private Double currentQuantity;
        private String qcStatus;
        private Integer totalUnits;
    }
}
