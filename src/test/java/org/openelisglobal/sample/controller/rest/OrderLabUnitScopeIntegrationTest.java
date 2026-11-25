package org.openelisglobal.sample.controller.rest;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.sample.form.SampleEditForm;
import org.openelisglobal.sample.service.SampleLabUnitScopeService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Every order lookup reads with the lab-unit scope the home dashboard counts
 * with: an order whose analyses sit in another lab unit is not served to a user
 * assigned elsewhere, whichever endpoint they ask. Fixture {@code result.xml}:
 * order 12345 in test section 1 (TB), order 13333 in test section 2.
 *
 * <p>
 * The caller's lab-unit assignment needs a security principal, so
 * {@link UserService} and {@link UserRoleService} are stubbed on the scope
 * service; everything else runs for real.
 */
public class OrderLabUnitScopeIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String USER_ID = "1";
    private static final String TB_SECTION = "1";
    private static final String TB_LAB_NUMBER = "12345";
    private static final String OTHER_LAB_NUMBER = "13333";

    @Autowired
    private SampleLabUnitScopeService scopeService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private IStatusService statusService;

    @Autowired
    private SampleEditRestController sampleEditRestController;

    @Autowired
    private javax.sql.DataSource dataSource;

    private Object realUserService;
    private Object realUserRoleService;
    private UserService userService;
    private UserRoleService userRoleService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/result.xml");
        Object target = AopTestUtils.getUltimateTargetObject(scopeService);
        realUserService = ReflectionTestUtils.getField(target, "userService");
        realUserRoleService = ReflectionTestUtils.getField(target, "userRoleService");
        userService = mock(UserService.class);
        userRoleService = mock(UserRoleService.class);
        ReflectionTestUtils.setField(target, "userService", userService);
        ReflectionTestUtils.setField(target, "userRoleService", userRoleService);
    }

    @After
    public void restoreScopeService() {
        Object target = AopTestUtils.getUltimateTargetObject(scopeService);
        ReflectionTestUtils.setField(target, "userService", realUserService);
        ReflectionTestUtils.setField(target, "userRoleService", realUserRoleService);
        // result.xml replaces status_of_sample with its own rows; the next test
        // class in the JVM must still resolve every analysis status by name.
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        for (String name : new String[] { "Not Tested", "Test Canceled", "Technical Acceptance", "Technical Rejected",
                "Biologist Rejection", "Finalized" }) {
            jdbc.update("INSERT INTO clinlims.status_of_sample (id, description, code, status_type, name, is_active,"
                    + " lastupdated) SELECT (SELECT COALESCE(MAX(id), 0) + 1 FROM clinlims.status_of_sample), ?, '1',"
                    + " 'ANALYSIS', ?, 'Y', NOW() WHERE NOT EXISTS (SELECT 1 FROM clinlims.status_of_sample WHERE"
                    + " status_type = 'ANALYSIS' AND name = ?)", name, name, name);
        }
        statusService.refreshCache();
    }

    @Test
    public void aUserOfOneLabUnitSeesOnlyThatUnitsOrders() {
        userHoldsSections(TB_SECTION);

        assertTrue(scopeService.isVisible(USER_ID, sample(TB_LAB_NUMBER)));
        assertFalse(scopeService.isVisible(USER_ID, sample(OTHER_LAB_NUMBER)));
    }

    @Test
    public void aGlobalAdministratorAndAUserOfEveryUnitSeeTheWholeLab() {
        userHoldsEveryLabUnit();

        assertTrue(scopeService.restrictedSectionIds(USER_ID).isEmpty());
        assertTrue(scopeService.isVisible(USER_ID, sample(OTHER_LAB_NUMBER)));
    }

    @Test
    public void anOrderTheUserEnteredStaysVisibleOutsideTheirUnits() {
        userHoldsSections(TB_SECTION);
        Sample other = sample(OTHER_LAB_NUMBER);
        other.setSysUserId(USER_ID);

        assertTrue(scopeService.isVisible(USER_ID, other));
    }

    @Test
    public void theOrderLookupsAnswerNotFoundForAnOrderOutsideTheUsersUnits() throws Exception {
        userHoldsSections(TB_SECTION);

        mockMvc.perform(asUser(get("/rest/order/search").param("labNumber", OTHER_LAB_NUMBER)))
                .andExpect(status().isNotFound());
        mockMvc.perform(asUser(get("/rest/sample-management/search").param("accessionNumber", OTHER_LAB_NUMBER)))
                .andExpect(status().isNotFound());
        mockMvc.perform(asUser(get("/rest/patientByLabNumer").param("accessionNumber", OTHER_LAB_NUMBER)))
                .andExpect(status().isNotFound());
        assertTrue("Modify Order is told there is no such order", sampleEdit(OTHER_LAB_NUMBER).getNoSampleFound());
    }

    @Test
    public void theOrderLookupsServeAnOrderInsideTheUsersUnits() throws Exception {
        userHoldsSections(TB_SECTION);

        mockMvc.perform(asUser(get("/rest/order/search").param("labNumber", TB_LAB_NUMBER))).andExpect(status().isOk())
                .andExpect(jsonPath("$.labNumber").value(TB_LAB_NUMBER));
        mockMvc.perform(asUser(get("/rest/sample-management/search").param("accessionNumber", TB_LAB_NUMBER)))
                .andExpect(status().isOk());
        assertFalse("Modify Order loads the user's own order", sampleEdit(TB_LAB_NUMBER).getNoSampleFound());
    }

    @Test
    public void theOrdersDashboardIsScopedLikeTheHomeDashboardByDefault() throws Exception {
        userHoldsEveryLabUnit();

        mockMvc.perform(asUser(get("/rest/order/dashboard"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.orders[*].labNumber").value(hasItems(TB_LAB_NUMBER, OTHER_LAB_NUMBER)));

        userHoldsSections(TB_SECTION);

        mockMvc.perform(asUser(get("/rest/order/dashboard"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.orders[*].labNumber").value(hasItem(TB_LAB_NUMBER)))
                .andExpect(jsonPath("$.orders[*].labNumber").value(not(hasItem(OTHER_LAB_NUMBER))));
    }

    @Test
    public void aSiteCanSwitchTheOrdersDashboardBackToTheWholeLab() throws Exception {
        userHoldsSections(TB_SECTION);
        Object properties = ReflectionTestUtils.getField(
                (Object) AopTestUtils.getUltimateTargetObject(ConfigurationProperties.getInstance()),
                "finalProperties");
        ReflectionTestUtils.invokeMethod(properties, "setPropertyValue", "restrictRecentOrdersByTestSection", "false");
        try {
            mockMvc.perform(asUser(get("/rest/order/dashboard"))).andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCount").value(2));
        } finally {
            ReflectionTestUtils.invokeMethod(properties, "setPropertyValue", "restrictRecentOrdersByTestSection", "");
        }
    }

    /**
     * A vector order's analyses hang off its pools rather than a sample item; they
     * still decide which lab units read the order.
     */
    @Test
    public void aVectorOrderIsScopedByTheAnalysesOnItsPools() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update(
                "INSERT INTO clinlims.vector_pool (id, sample_id, active, lastupdated) VALUES (98701, 2, true, NOW())");
        jdbc.update("UPDATE clinlims.analysis SET sampitem_id = NULL, vector_pool_id = 98701, test_sect_id = 1"
                + " WHERE id = 2");
        try {
            userHoldsSections(TB_SECTION);
            assertTrue("the pool's analysis is in the user's unit",
                    scopeService.isVisible(USER_ID, sample(OTHER_LAB_NUMBER)));
            mockMvc.perform(asUser(get("/rest/order/dashboard"))).andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[*].labNumber").value(hasItem(OTHER_LAB_NUMBER)));
            userHoldsSections("2");
            assertFalse(scopeService.isVisible(USER_ID, sample(OTHER_LAB_NUMBER)));
            mockMvc.perform(asUser(get("/rest/order/dashboard"))).andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[*].labNumber").value(not(hasItem(OTHER_LAB_NUMBER))));
        } finally {
            jdbc.update("UPDATE clinlims.analysis SET sampitem_id = 602, vector_pool_id = NULL, test_sect_id = 2"
                    + " WHERE id = 2");
            jdbc.update("DELETE FROM clinlims.vector_pool WHERE id = 98701");
        }
    }

    @Test
    public void writesOnAnOrderOutsideTheUsersUnitsAreRefused() throws Exception {
        userHoldsSections(TB_SECTION);

        mockMvc.perform(asUser(post("/rest/sample-management/add-tests")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"sampleItemIds\":[\"602\"],\"testIds\":[\"2\"]}")).andExpect(status().isNotFound());
        mockMvc.perform(asUser(post("/rest/sample-management/cancel-test")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"analysisId\":\"2\",\"sampleItemId\":\"602\"}")).andExpect(status().isNotFound());
        mockMvc.perform(asUser(post("/rest/sample-management/aliquot")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"parentSampleItemId\":\"602\",\"quantityToTransfer\":1,\"numberOfAliquots\":1}"))
                .andExpect(status().isNotFound());
        assertTrue(scopeService.isSampleItemVisible(USER_ID, "601"));
        assertFalse(scopeService.isSampleItemVisible(USER_ID, "602"));
        assertFalse("an unknown item is not visible", scopeService.isSampleItemVisible(USER_ID, "999999"));
    }

    /**
     * The Aliquot screen names an order and the tubes to split. A tube of another
     * order, named under an unknown order or under the user's own one, is refused
     * and left as it was.
     */
    @Test
    public void anAliquotOfATubeOutsideTheNamedOrderIsRefused() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("UPDATE clinlims.sample_item SET external_id = 'SCOPE-TUBE-602' WHERE id = 602");
        userHoldsSections(TB_SECTION);

        for (String accessionNumber : new String[] { "NO-SUCH-ORDER-4757", TB_LAB_NUMBER }) {
            mockMvc.perform(asUser(post("/rest/Aliquot")).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accessionNumber\":\"" + accessionNumber + "\",\"sampleItems\":[{\"externalId\":"
                            + "\"SCOPE-TUBE-602\",\"aliquots\":[{\"externalId\":\"SCOPE-TUBE-602.1\",\"quantity\":1,"
                            + "\"analyses\":[\"2\"]}]}]}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.errors").exists())
                    .andExpect(jsonPath("$.success").doesNotExist());
        }
        assertEquals("the tube is not voided", Boolean.FALSE, jdbc.queryForObject(
                "SELECT COALESCE(voided, false) FROM clinlims.sample_item WHERE id = 602", Boolean.class));
        assertEquals("no aliquot is created", Integer.valueOf(0), jdbc.queryForObject(
                "SELECT COUNT(*) FROM clinlims.sample_item WHERE external_id = 'SCOPE-TUBE-602.1'", Integer.class));
    }

    /**
     * Modify Order's form is read through the controller: in this test context its
     * JSON rendering fails for every order alike, which is not what is under test.
     */
    private SampleEditForm sampleEdit(String accessionNumber) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, session());
        request.getSession().setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(new UsernamePasswordAuthenticationToken("scoped", "n/a", List.of())));
        return sampleEditRestController.showSampleEdit(request, accessionNumber, null);
    }

    private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder request) {
        return request.sessionAttr(IActionConstants.USER_SESSION_DATA, session()).with(user("scoped"));
    }

    private Sample sample(String accessionNumber) {
        return sampleService.getSampleByAccessionNumber(accessionNumber);
    }

    private void userHoldsEveryLabUnit() {
        when(userRoleService.userInRole(anyString(), anyString())).thenReturn(true);
    }

    private void userHoldsSections(String... sectionIds) {
        when(userRoleService.userInRole(anyString(), anyString())).thenReturn(false);
        List<IdValuePair> sections = Arrays.stream(sectionIds).map(id -> new IdValuePair(id, "section " + id))
                .collect(Collectors.toList());
        when(userService.getUserTestSections(USER_ID, null)).thenReturn(sections);
    }

    private UserSessionData session() {
        UserSessionData userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(Integer.parseInt(USER_ID));
        return userSessionData;
    }
}
