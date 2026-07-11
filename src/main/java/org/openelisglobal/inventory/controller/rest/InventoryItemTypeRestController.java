package org.openelisglobal.inventory.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.exception.LocalizedValidationException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.inventory.service.InventoryItemTypeService;
import org.openelisglobal.inventory.valueholder.InventoryItemType;
import org.openelisglobal.localization.valueholder.Localization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OGC-658 Part A: admin CRUD for the inventory item type lookup table,
 * replacing the hardcoded {@code InventoryEnums.ItemType} enum.
 * {@code @PreAuthorize("hasRole('ADMIN')")} matches the permission already used
 * by analogous admin-managed reference lists (e.g.
 * {@code DictionaryRestController}, {@code SiteInformationRestController}) — no
 * new permission key.
 */
@RestController
@RequestMapping("/rest/inventory-item-types")
@PreAuthorize("hasRole('ADMIN')")
public class InventoryItemTypeRestController extends BaseRestController {

    @Autowired
    private InventoryItemTypeService inventoryItemTypeService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryItemTypeDTO>> getAll() {
        try {
            List<InventoryItemTypeDTO> dtos = inventoryItemTypeService.getAllOrderedBySortOrder().stream()
                    .map(InventoryItemTypeDTO::from).collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> create(@RequestBody CreateRequest request, HttpServletRequest httpRequest) {
        try {
            String sysUserId = org.openelisglobal.common.util.ControllerUtills.getSysUserId(httpRequest);
            InventoryItemType created = inventoryItemTypeService.create(request.getCode(), request.getName(),
                    request.getLocale(), request.getSortOrder(), request.getActive(), sysUserId);
            return ResponseEntity.status(HttpStatus.CREATED).body(InventoryItemTypeDTO.from(created));
        } catch (LocalizedValidationException e) {
            // OGC-658 Part A (C8): missing name / malformed / duplicate codes surface as a
            // clean, i18n-keyed 400 — see InventoryItemTypeServiceImpl.create() /
            // CodeGenerator.normalize().
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(e));
        } catch (LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(e.getMessage()));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody UpdateRequest request,
            HttpServletRequest httpRequest) {
        try {
            String sysUserId = org.openelisglobal.common.util.ControllerUtills.getSysUserId(httpRequest);
            InventoryItemType updated = inventoryItemTypeService.updateNameAndSortOrder(id, request.getLocale(),
                    request.getName(), request.getSortOrder(), sysUserId);
            return ResponseEntity.ok(InventoryItemTypeDTO.from(updated));
        } catch (LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(e.getMessage()));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping(value = "/{id}/deactivate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deactivate(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            String sysUserId = org.openelisglobal.common.util.ControllerUtills.getSysUserId(httpRequest);
            InventoryItemType deactivated = inventoryItemTypeService.deactivate(id, sysUserId);
            return ResponseEntity.ok(InventoryItemTypeDTO.from(deactivated));
        } catch (LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(e.getMessage()));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Map<String, Object> errorBody(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);
        return body;
    }

    private Map<String, Object> errorBody(LocalizedValidationException e) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", e.getMessage());
        body.put("errorCode", e.getErrorCode());
        body.put("params", e.getParams());
        return body;
    }

    @Setter
    @Getter
    public static class CreateRequest {
        private String code;
        private String name;
        private String locale;
        private Integer sortOrder;
        private Boolean active;
    }

    @Setter
    @Getter
    public static class UpdateRequest {
        private String name;
        private String locale;
        private Integer sortOrder;
    }

    @Setter
    @Getter
    public static class InventoryItemTypeDTO {
        private Long id;
        private String code;
        private String name;
        private Map<String, String> localized;
        private boolean active;
        private Integer sortOrder;
        private boolean seeded;

        static InventoryItemTypeDTO from(InventoryItemType type) {
            InventoryItemTypeDTO dto = new InventoryItemTypeDTO();
            dto.setId(type.getId());
            dto.setCode(type.getCode());
            Localization localization = type.getNameLocalization();
            dto.setLocalized(localization != null ? localization.getValuesAsMap() : new HashMap<>());
            dto.setName(localization != null ? localization.getLocalizedValue(Locale.ENGLISH) : type.getCode());
            dto.setActive(Boolean.TRUE.equals(type.getIsActive()));
            dto.setSortOrder(type.getSortOrder());
            dto.setSeeded(Boolean.TRUE.equals(type.getIsSeeded()));
            return dto;
        }
    }
}
