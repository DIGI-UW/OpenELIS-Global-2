package org.openelisglobal.inventory.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.department.service.DepartmentIsolationService;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.service.InventoryLotService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
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

    @Autowired
    private DepartmentIsolationService departmentIsolationService;

    /**
     * Get all active reagents with their available lots. Returns a flat list where
     * each entry represents a reagent item with lot information.
     *
     * @param status Optional filter - "active" returns only active items (default)
     * @return List of ReagentDTO objects
     */
    @GetMapping(value = "/reagents", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ReagentDTO>> getReagents(
            @RequestParam(required = false, defaultValue = "active") String status,
            @RequestParam(required = false) List<Integer> departmentIds,
            @RequestParam(required = false, defaultValue = "true") boolean requireLots, HttpServletRequest request) {
        try {
            List<ReagentDTO> reagentDTOs = new ArrayList<>();

            // Get reagent items
            List<InventoryItem> reagentItems = inventoryItemService.getByItemType(ItemType.REAGENT);
            reagentItems = filterAccessible(reagentItems, request, departmentIds);
            reagentItems = filterActiveItems(reagentItems, status);

            for (InventoryItem item : reagentItems) {
                List<InventoryLot> lots = getNotebookSelectableLots(item.getId(), requireLots);
                if (shouldSkipForLots(status, requireLots, lots)) {
                    continue;
                }
                reagentDTOs.add(mapReagentDto(item, lots));
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
            @RequestParam(required = false, defaultValue = "active") String status,
            @RequestParam(required = false) List<Integer> departmentIds,
            @RequestParam(required = false, defaultValue = "true") boolean requireLots,
            @RequestParam(required = false) List<ItemType> itemTypes, HttpServletRequest request) {
        try {
            List<ItemType> resolvedTypes = resolveInstrumentItemTypes(itemTypes);
            List<InstrumentDTO> instrumentDTOs = new ArrayList<>();
            Set<Long> seenItemIds = new LinkedHashSet<>();

            for (ItemType itemType : resolvedTypes) {
                List<InventoryItem> items = inventoryItemService.getByItemType(itemType);
                items = filterAccessible(items, request, departmentIds);
                items = filterActiveItems(items, status);

                for (InventoryItem item : items) {
                    if (!seenItemIds.add(item.getId())) {
                        continue;
                    }
                    List<InventoryLot> lots = getNotebookSelectableLots(item.getId(), requireLots);
                    if (shouldSkipForLots(status, requireLots, lots)) {
                        continue;
                    }
                    instrumentDTOs.add(mapInstrumentDto(item, lots));
                }
            }

            instrumentDTOs.sort(Comparator.comparing(InstrumentDTO::getName, String.CASE_INSENSITIVE_ORDER));
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
        private String units;
        private String lotNumber;
        private String expirationDate;
        private Double currentQuantity;
        private String qcStatus;
        private String lotStatus;
        private List<String> selectionWarnings;
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
        private String itemType;
        private String compatibleAnalyzers;
        private Boolean calibrationRequired;
        private String serialNumber;
        private String expirationDate;
        private Double currentQuantity;
        private String qcStatus;
        private String lotStatus;
        private List<String> selectionWarnings;
        private Integer totalUnits;
    }

    private List<ItemType> resolveInstrumentItemTypes(List<ItemType> itemTypes) {
        if (itemTypes == null || itemTypes.isEmpty()) {
            return List.of(ItemType.EQUIPMENT);
        }
        return itemTypes;
    }

    private List<InventoryItem> filterActiveItems(List<InventoryItem> items, String status) {
        if (!"active".equalsIgnoreCase(status)) {
            return items;
        }
        return items.stream().filter(item -> "Y".equals(item.getIsActive())).toList();
    }

    /**
     * Notebook workflow selectors use department membership for visibility. Lot QC,
     * quantity, and status inform warnings but must not hide department reagents.
     */
    private boolean shouldSkipForLots(String status, boolean requireLots, List<InventoryLot> lots) {
        return false;
    }

    /**
     * Returns all lots for an inventory item in FEFO display order. Unlike
     * {@link InventoryLotService#getAvailableLotsByItemFEFO()}, this does not filter
     * by QC passed or available quantity — notebook UIs surface those as warnings.
     */
    private List<InventoryLot> getNotebookSelectableLots(Long itemId, boolean requireLots) {
        List<InventoryLot> allLots = inventoryLotService.getByInventoryItemId(itemId);
        if (allLots == null || allLots.isEmpty()) {
            return List.of();
        }

        return allLots.stream()
                .sorted(Comparator.comparing(InventoryLot::getExpirationDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(InventoryLot::getCalculatedExpiryAfterOpening,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    private List<String> buildLotSelectionWarnings(InventoryLot lot) {
        List<String> warnings = new ArrayList<>();
        if (lot == null) {
            return warnings;
        }
        if (lot.getQcStatus() == QCStatus.PENDING) {
            warnings.add("QC_PENDING");
        } else if (lot.getQcStatus() == QCStatus.FAILED) {
            warnings.add("QC_FAILED");
        } else if (lot.getQcStatus() == QCStatus.QUARANTINED) {
            warnings.add("QC_QUARANTINED");
        }
        if (lot.getCurrentQuantity() == null || lot.getCurrentQuantity() <= 0) {
            warnings.add("ZERO_QUANTITY");
        }
        if (lot.getStatus() != LotStatus.ACTIVE && lot.getStatus() != LotStatus.IN_USE) {
            warnings.add("INACTIVE_LOT");
        }
        return warnings;
    }

    private List<String> aggregateSelectionWarnings(List<InventoryLot> lots) {
        if (lots == null || lots.isEmpty()) {
            return List.of("NO_LOTS");
        }
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        for (InventoryLot lot : lots) {
            warnings.addAll(buildLotSelectionWarnings(lot));
        }
        return new ArrayList<>(warnings);
    }

    private ReagentDTO mapReagentDto(InventoryItem item, List<InventoryLot> lots) {
        ReagentDTO dto = new ReagentDTO();
        dto.setId(String.valueOf(item.getId()));
        dto.setItemId(item.getId());
        dto.setName(item.getName());
        dto.setDescription(item.getDescription());
        dto.setManufacturer(item.getManufacturer());
        dto.setCategory(item.getCategory());
        dto.setStorageRequirements(item.getStorageRequirements());
        dto.setUnits(item.getUnits());

        dto.setSelectionWarnings(aggregateSelectionWarnings(lots));
        if (lots != null && !lots.isEmpty()) {
            InventoryLot primaryLot = lots.get(0);
            dto.setLotId(primaryLot.getId());
            dto.setLotNumber(primaryLot.getLotNumber());
            dto.setExpirationDate(
                    primaryLot.getExpirationDate() != null ? primaryLot.getExpirationDate().toString() : null);
            dto.setQcStatus(primaryLot.getQcStatus() != null ? primaryLot.getQcStatus().name() : null);
            dto.setLotStatus(primaryLot.getStatus() != null ? primaryLot.getStatus().name() : null);
            double totalQuantity = lots.stream()
                    .mapToDouble(lot -> lot.getCurrentQuantity() != null ? lot.getCurrentQuantity() : 0.0).sum();
            dto.setCurrentQuantity(totalQuantity);
            dto.setTotalLots(lots.size());
        }
        return dto;
    }

    private InstrumentDTO mapInstrumentDto(InventoryItem item, List<InventoryLot> lots) {
        InstrumentDTO dto = new InstrumentDTO();
        dto.setId(String.valueOf(item.getId()));
        dto.setItemId(item.getId());
        dto.setName(item.getName());
        dto.setDescription(item.getDescription());
        dto.setManufacturer(item.getManufacturer());
        dto.setCategory(item.getCategory());
        dto.setItemType(item.getItemType() != null ? item.getItemType().name() : null);
        dto.setCompatibleAnalyzers(item.getCompatibleAnalyzers());
        dto.setCalibrationRequired("Y".equals(item.getCalibrationRequired()));

        dto.setSelectionWarnings(aggregateSelectionWarnings(lots));
        if (lots != null && !lots.isEmpty()) {
            InventoryLot primaryLot = lots.get(0);
            dto.setLotId(primaryLot.getId());
            dto.setSerialNumber(primaryLot.getLotNumber());
            dto.setExpirationDate(
                    primaryLot.getExpirationDate() != null ? primaryLot.getExpirationDate().toString() : null);
            dto.setQcStatus(primaryLot.getQcStatus() != null ? primaryLot.getQcStatus().name() : null);
            dto.setLotStatus(primaryLot.getStatus() != null ? primaryLot.getStatus().name() : null);
            double totalQuantity = lots.stream()
                    .mapToDouble(lot -> lot.getCurrentQuantity() != null ? lot.getCurrentQuantity() : 0.0).sum();
            dto.setCurrentQuantity(totalQuantity);
            dto.setTotalUnits(lots.size());
        }
        return dto;
    }

    private List<InventoryItem> filterAccessible(List<InventoryItem> items, HttpServletRequest request) {
        return items.stream().filter(item -> departmentIsolationService.canAccessInventoryItem(item, request)).toList();
    }

    private List<InventoryItem> filterAccessible(List<InventoryItem> items, HttpServletRequest request,
            List<Integer> departmentIds) {
        List<InventoryItem> accessibleItems = filterAccessible(items, request);
        if (departmentIds == null || departmentIds.isEmpty()) {
            return accessibleItems;
        }
        Set<Integer> requestedDepartmentIds = Set.copyOf(departmentIds);
        return accessibleItems.stream()
                .filter(item -> requestedDepartmentIds.stream()
                        .anyMatch(departmentId -> departmentIsolationService.inventoryBelongsToDepartment(item,
                                departmentId)))
                .toList();
    }
}
