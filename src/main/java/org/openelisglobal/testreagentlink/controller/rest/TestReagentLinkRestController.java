package org.openelisglobal.testreagentlink.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testreagentlink.service.TestReagentLinkService;
import org.openelisglobal.testreagentlink.valueholder.TestReagentLink;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * OGC-949 / OGC-987 (epic OGC-760) — CRUD endpoints for the Test↔Reagent
 * linkage that the v2 Reagents tab (OGC-762) consumes.
 *
 * <p>
 * A "reagent" is an {@code inventory_item} with {@code item_type = 'REAGENT'};
 * there is no standalone reagent table (see {@link TestReagentLink}). Current
 * stock is read from inventory ({@link InventoryItemService}).
 *
 * <p>
 * Base path {@code /rest/test-catalog/{testId}/reagents} stays within the
 * unified editor namespace established by
 * {@code TestCatalogEditorRestController} (research.md R10). Gated by
 * {@code ROLE_ADMIN} — non-admins get 403 — matching the editor controller and
 * the existing OE admin REST controllers; OpenELIS has no fine-grained
 * {@code admin.testCatalog.manage} authority.
 */
@RestController
@RequestMapping("/rest/test-catalog/{testId}/reagents")
@PreAuthorize("hasRole('ADMIN')")
public class TestReagentLinkRestController extends BaseRestController {

    private static final Set<String> USAGE_TYPES = Set.of("PRIMARY", "SECONDARY");

    private final TestReagentLinkService reagentLinkService;

    private final TestService testService;

    private final InventoryItemService inventoryItemService;

    public TestReagentLinkRestController(TestReagentLinkService reagentLinkService, TestService testService,
            InventoryItemService inventoryItemService) {
        this.reagentLinkService = reagentLinkService;
        this.testService = testService;
        this.inventoryItemService = inventoryItemService;
    }

    /** Request body for link create/update. */
    public static class ReagentLinkRequest {
        public Long reagentId;
        public String usageType;
        public BigDecimal quantityPerTest;
        public String quantityUnit;
    }

    /** A linked reagent enriched with inventory display fields + current stock. */
    public static class ReagentLinkResponse {
        public String id;
        public Long reagentId;
        public String reagentName;
        public String manufacturer;
        public String usageType;
        public BigDecimal quantityPerTest;
        public String quantityUnit;
        public Double currentStock;
        public Integer lowStockThreshold;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ReagentLinkResponse> list(@PathVariable String testId) {
        requireTest(testId);
        List<ReagentLinkResponse> out = new ArrayList<>();
        for (TestReagentLink link : reagentLinkService.getByTestId(testId)) {
            out.add(toResponse(link));
        }
        return out;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReagentLinkResponse> link(@PathVariable String testId, @RequestBody ReagentLinkRequest body,
            HttpServletRequest request) {
        requireTest(testId);
        requireReagent(body.reagentId);
        validateUsageType(body.usageType);
        if (reagentLinkService.getByTestIdAndReagentId(testId, body.reagentId) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reagent is already linked to this test");
        }

        TestReagentLink link = new TestReagentLink();
        link.setTestId(testId);
        link.setReagentId(body.reagentId);
        link.setUsageType(body.usageType);
        link.setQuantityPerTest(body.quantityPerTest);
        link.setQuantityUnit(body.quantityUnit);
        link.setSysUserId(getSysUserId(request));
        reagentLinkService.insert(link);

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(link));
    }

    @PutMapping(value = "/{reagentId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ReagentLinkResponse update(@PathVariable String testId, @PathVariable Long reagentId,
            @RequestBody ReagentLinkRequest body, HttpServletRequest request) {
        requireTest(testId);
        TestReagentLink link = reagentLinkService.getByTestIdAndReagentId(testId, reagentId);
        if (link == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reagent is not linked to this test");
        }
        if (body.usageType != null) {
            validateUsageType(body.usageType);
            link.setUsageType(body.usageType);
        }
        link.setQuantityPerTest(body.quantityPerTest);
        link.setQuantityUnit(body.quantityUnit);
        link.setSysUserId(getSysUserId(request));
        reagentLinkService.update(link);
        return toResponse(link);
    }

    @DeleteMapping(value = "/{reagentId}")
    public ResponseEntity<Void> unlink(@PathVariable String testId, @PathVariable Long reagentId,
            HttpServletRequest request) {
        requireTest(testId);
        TestReagentLink link = reagentLinkService.getByTestIdAndReagentId(testId, reagentId);
        if (link == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reagent is not linked to this test");
        }
        link.setSysUserId(getSysUserId(request));
        reagentLinkService.delete(link);
        return ResponseEntity.noContent().build();
    }

    private void requireTest(String testId) {
        // getTestById returns null on a missing test; get() would throw
        // ObjectNotFoundException (surfacing as 500 rather than the 404 we want).
        Test test = testService.getTestById(testId);
        if (test == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found: " + testId);
        }
    }

    private void requireReagent(Long reagentId) {
        if (reagentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reagentId is required");
        }
        InventoryItem item = findItem(reagentId);
        if (item == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reagent not found: " + reagentId);
        }
        if (item.getItemType() != ItemType.REAGENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Inventory item " + reagentId + " is not a reagent");
        }
    }

    /** Null-safe inventory_item lookup by id ({@code get()} throws on missing). */
    private InventoryItem findItem(Long itemId) {
        List<InventoryItem> matches = inventoryItemService.getAllMatching("id", itemId);
        return matches.isEmpty() ? null : matches.get(0);
    }

    private void validateUsageType(String usageType) {
        if (usageType == null || !USAGE_TYPES.contains(usageType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "usageType must be PRIMARY or SECONDARY");
        }
    }

    private ReagentLinkResponse toResponse(TestReagentLink link) {
        ReagentLinkResponse r = new ReagentLinkResponse();
        r.id = link.getId();
        r.reagentId = link.getReagentId();
        r.usageType = link.getUsageType();
        r.quantityPerTest = link.getQuantityPerTest();
        r.quantityUnit = link.getQuantityUnit();
        InventoryItem item = findItem(link.getReagentId());
        if (item != null) {
            r.reagentName = item.getName();
            r.manufacturer = item.getManufacturer();
            r.lowStockThreshold = item.getLowStockThreshold();
        }
        r.currentStock = inventoryItemService.getTotalCurrentStock(link.getReagentId());
        return r;
    }
}
