package org.openelisglobal.common.rest.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.reportqueue.dto.ReportQueueRequest;
import org.openelisglobal.reportqueue.service.ReportQueueService;
import org.openelisglobal.reportqueue.service.ReportQueueServiceImpl;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * OGC-431 (ruling of 22 Oct 2026, AC-23): the home dashboard's Unprinted
 * results tile counts what it opens, the Report Print Queue's Unprinted orders
 * for the user, whenever they were released.
 */
public class UnprintedResultsTileTest extends BaseWebContextSensitiveTest {

    private static final long SAMPLE = 9301L;
    private static final String ADMIN = "9301";
    private static final String USER_WITHOUT_LAB_UNITS = "9302";
    private static final String CHEMISTRY_USER = "9303";
    private static final String CHEMISTRY = "9301";

    @Autowired
    private IStatusService statusService;

    @Autowired
    private ReportQueueService reportQueueService;

    @Autowired
    private UserService userService;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private RoleService roleService;

    private String originalBillingLabel;

    @Before
    public void loadOrder() throws Exception {
        executeDataSetWithStateManagement("testdata/patient-report-v1.xml");
        originalBillingLabel = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.BILLING_REFERENCE_NUMBER_LABEL);
        ConfigurationProperties.getInstance().setPropertyValue(Property.BILLING_REFERENCE_NUMBER_LABEL, "9309");
        removePrintHistory();
        ensureSystemRole(Constants.ROLE_GLOBAL_ADMIN);
        jdbcTemplate.update("INSERT INTO clinlims.system_user_role (system_user_id, role_id)"
                + " SELECT 9301, id FROM clinlims.system_role WHERE name = ?", Constants.ROLE_GLOBAL_ADMIN);
        ensureSiteInformation(ReportQueueServiceImpl.INSTALL_DATE_SITE_INFO, "2026-01-01 00:00:00");
        jdbcTemplate.update("UPDATE clinlims.site_information SET value = '2026-01-01 00:00:00' WHERE name = ?",
                ReportQueueServiceImpl.INSTALL_DATE_SITE_INFO);
        statusService.refreshCache();
        jdbcTemplate.update("UPDATE clinlims.analysis SET released_date = now() - interval '10 days',"
                + " lastupdated = now() WHERE id IN (9301, 9305)");
    }

    @After
    public void restore() {
        ConfigurationProperties.getInstance().setPropertyValue(Property.BILLING_REFERENCE_NUMBER_LABEL,
                originalBillingLabel);
        jdbcTemplate.update("DELETE FROM clinlims.system_user_role WHERE system_user_id IN (9301, 9303)");
        removeLabUnits(CHEMISTRY_USER);
        RequestContextHolder.resetRequestAttributes();
        removePrintHistory();
    }

    private void removeLabUnits(String user) {
        Integer userId = Integer.valueOf(user);
        List<Long> maps = jdbcTemplate.queryForList(
                "SELECT lab_unit_role_map_id FROM clinlims.lab_unit_roles WHERE system_user_id = ?", Long.class,
                userId);
        jdbcTemplate.update("DELETE FROM clinlims.lab_unit_roles WHERE system_user_id = ?", userId);
        for (Long map : maps) {
            jdbcTemplate.update("DELETE FROM clinlims.lab_roles WHERE lab_unit_role_map_id = ?", map);
            jdbcTemplate.update("DELETE FROM clinlims.lab_unit_role_map WHERE lab_unit_role_map_id = ?", map);
        }
        jdbcTemplate.update("DELETE FROM clinlims.user_lab_unit_roles WHERE system_user_id = ?", userId);
    }

    private void removePrintHistory() {
        jdbcTemplate.update("DELETE FROM clinlims.document_track WHERE row_id = 9301"
                + " AND table_id = (SELECT id FROM clinlims.reference_tables WHERE name = 'SAMPLE')");
        jdbcTemplate.update("DELETE FROM clinlims.report_print_event WHERE report_version_id IN"
                + " (SELECT id FROM clinlims.report_version WHERE sample_id = ?)", SAMPLE);
        jdbcTemplate.update("DELETE FROM clinlims.report_version_item WHERE report_version_id IN"
                + " (SELECT id FROM clinlims.report_version WHERE sample_id = ?)", SAMPLE);
        jdbcTemplate.update("DELETE FROM clinlims.report_version WHERE sample_id = ?", SAMPLE);
    }

    @Test
    public void theTileCountsEveryUnprintedOrderTheQueueOpensWith() {
        long queue = unprintedInQueue(ADMIN);

        assertEquals("an order released ten days ago still counts, once for its two results", queue, tile(ADMIN));
        assertEquals(queue, reportQueueService.unprintedCount(ADMIN));

        reportQueueService.print(SAMPLE, ADMIN).orElseThrow();

        assertEquals("a printed order leaves the tile and the queue together", queue - 1, tile(ADMIN));
        assertEquals(queue - 1, unprintedInQueue(ADMIN));
    }

    @Test
    public void theTileStaysWithinTheUsersLabUnits() {
        signIn(CHEMISTRY_USER);
        ensureSystemRole(Constants.ROLE_RESULTS);
        userService.saveUserLabUnitRoles(systemUserService.get(CHEMISTRY_USER),
                Map.of(CHEMISTRY, Set.of(roleService.getRoleByName(Constants.ROLE_RESULTS).getId())), "1");

        long chemistry = unprintedInQueue(CHEMISTRY_USER);
        assertTrue("the order's own lab unit shows it", chemistry >= 1);
        assertEquals("a user of the order's lab unit counts what the queue lists", chemistry, tile(CHEMISTRY_USER));

        signIn(USER_WITHOUT_LAB_UNITS);
        assertEquals(0, unprintedInQueue(USER_WITHOUT_LAB_UNITS));
        assertEquals("a user without lab units sees no order in the tile either", 0, tile(USER_WITHOUT_LAB_UNITS));
    }

    private void signIn(String user) {
        UserDetails details = User.withUsername("v1-" + user).password("N/A").authorities("ROLE_RESULTS").build();
        SecurityContext context = new SecurityContextImpl();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(details, "N/A", details.getAuthorities()));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private long unprintedInQueue(String user) {
        ReportQueueRequest request = new ReportQueueRequest();
        request.setPrintStatus("UNPRINTED");
        return reportQueueService.list(request, user).getTotal();
    }

    private long tile(String user) {
        PatientDashBoardProvider provider = new PatientDashBoardProvider();
        provider.reportQueueService = reportQueueService;
        return provider.unprintedResultsTile(user);
    }
}
