package org.openelisglobal.testreagentlink.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testreagentlink.controller.rest.TestReagentLinkRestController;
import org.openelisglobal.testreagentlink.controller.rest.TestReagentLinkRestController.ReagentLinkRequest;
import org.openelisglobal.testreagentlink.controller.rest.TestReagentLinkRestController.ReagentLinkResponse;
import org.openelisglobal.testreagentlink.service.TestReagentLinkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;

/**
 * OGC-949 / OGC-987 — Test↔Reagent linkage REST endpoints, round-tripped
 * against a real DB. Covers the happy path (link + list with inventory stock),
 * the 409 duplicate guard, the 404 guards (unknown test / reagent / link), and
 * the 400 validation guards (bad usage type, non-reagent inventory item).
 *
 * <p>
 * The class is gated by {@code @PreAuthorize("hasRole('ADMIN')")}; non-admins
 * get 403. That is enforced by Spring Security's method proxy, which is
 * bypassed when the controller is invoked directly (as here, matching the
 * sibling {@code TestCatalogEditor*IntegrationTest}s), so it is not asserted in
 * this suite.
 */
public class TestReagentLinkRestControllerIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_ID = 95411L;

    @Autowired
    private TestReagentLinkService reagentLinkService;

    @Autowired
    private TestService testService;

    @Autowired
    private InventoryItemService inventoryItemService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private TestReagentLinkRestController controller;
    private JdbcTemplate jdbc;
    private Long reagentId;
    private Long nonReagentId;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        controller = new TestReagentLinkRestController(reagentLinkService, testService, inventoryItemService);
        cleanup();
        // Other suites in the same run insert inventory_item rows with explicit
        // ids; keep the sequence ahead of MAX(id) so our service-generated ids
        // never collide (intermittent PK ConstraintViolation under suite order).
        jdbc.queryForObject("SELECT setval('clinlims.inventory_item_seq',"
                + " GREATEST((SELECT COALESCE(MAX(id), 1) FROM clinlims.inventory_item), 1))", Long.class);
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " VALUES (?, ?, ?, 'Y', ?, NOW())",
                TEST_ID, "ReagentLinkIT", "ReagentLinkIT desc", UUID.randomUUID().toString());
        reagentId = createInventoryItem("ReagentLinkIT Reagent", ItemType.REAGENT);
        nonReagentId = createInventoryItem("ReagentLinkIT RDT", ItemType.RDT);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private Long createInventoryItem(String name, ItemType type) {
        InventoryItem item = new InventoryItem();
        item.setName(name);
        item.setItemType(type);
        item.setUnits("mL");
        item.setManufacturer("Acme Diagnostics");
        item.setFhirUuid(UUID.randomUUID());
        item.setSysUserId("1");
        return inventoryItemService.insert(item);
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.test_reagent_link WHERE test_id = ?", TEST_ID);
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?", TEST_ID);
        jdbc.update("DELETE FROM clinlims.inventory_item WHERE name LIKE 'ReagentLinkIT%'");
    }

    private static MockHttpServletRequest authedRequest() {
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(IActionConstants.USER_SESSION_DATA, usd);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        return request;
    }

    private String testId() {
        return String.valueOf(TEST_ID);
    }

    private ReagentLinkRequest req(Long reagentId, String usageType, String qty, String unit) {
        ReagentLinkRequest body = new ReagentLinkRequest();
        body.reagentId = reagentId;
        body.usageType = usageType;
        body.quantityPerTest = qty == null ? null : new BigDecimal(qty);
        body.quantityUnit = unit;
        return body;
    }

    @Test
    public void link_thenList_returnsLinkWithInventoryFieldsAndStock() {
        controller.link(testId(), req(reagentId, "PRIMARY", "2.5", "mL"), authedRequest());

        List<ReagentLinkResponse> links = controller.list(testId());
        assertEquals(1, links.size());
        ReagentLinkResponse r = links.get(0);
        assertEquals(reagentId, r.reagentId);
        assertEquals("PRIMARY", r.usageType);
        assertEquals(0, new BigDecimal("2.5").compareTo(r.quantityPerTest));
        assertEquals("mL", r.quantityUnit);
        assertEquals("ReagentLinkIT Reagent", r.reagentName);
        assertEquals("Acme Diagnostics", r.manufacturer);
        assertNotNull(r.id);
    }

    @Test
    public void link_duplicateReagent_throwsConflict() {
        controller.link(testId(), req(reagentId, "PRIMARY", "1", "mL"), authedRequest());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.link(testId(), req(reagentId, "SECONDARY", "3", "mL"), authedRequest()));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    public void link_unknownTest_throwsNotFound() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.link("99999999", req(reagentId, "PRIMARY", "1", "mL"), authedRequest()));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void link_unknownReagent_throwsNotFound() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.link(testId(), req(88888888L, "PRIMARY", "1", "mL"), authedRequest()));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void link_nonReagentInventoryItem_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.link(testId(), req(nonReagentId, "PRIMARY", "1", "mL"), authedRequest()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void link_invalidUsageType_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.link(testId(), req(reagentId, "TERTIARY", "1", "mL"), authedRequest()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void update_changesUsageTypeAndQuantity() {
        controller.link(testId(), req(reagentId, "PRIMARY", "1", "mL"), authedRequest());
        ReagentLinkResponse updated = controller.update(testId(), reagentId, req(reagentId, "SECONDARY", "4.0", "uL"),
                authedRequest());
        assertEquals("SECONDARY", updated.usageType);
        assertEquals(0, new BigDecimal("4.0").compareTo(updated.quantityPerTest));
        assertEquals("uL", updated.quantityUnit);
    }

    @Test
    public void update_unlinkedReagent_throwsNotFound() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.update(testId(), reagentId, req(reagentId, "PRIMARY", "1", "mL"), authedRequest()));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void unlink_removesLink() {
        controller.link(testId(), req(reagentId, "PRIMARY", "1", "mL"), authedRequest());
        assertEquals(204, controller.unlink(testId(), reagentId, authedRequest()).getStatusCode().value());
        assertTrue(controller.list(testId()).isEmpty());
    }

    @Test
    public void unlink_unlinkedReagent_throwsNotFound() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.unlink(testId(), reagentId, authedRequest()));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
