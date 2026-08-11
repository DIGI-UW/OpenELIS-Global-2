package org.openelisglobal.accreditation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.List;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.accreditation.dao.TestAccreditationDAO;
import org.openelisglobal.accreditation.dto.AccreditationSummary;
import org.openelisglobal.accreditation.dto.AccreditingBodyView;
import org.openelisglobal.accreditation.dto.TestAccreditationView;
import org.openelisglobal.accreditation.service.AccreditingBodyService;
import org.openelisglobal.accreditation.service.TestAccreditationService;
import org.openelisglobal.accreditation.valueholder.AccreditationStatus;
import org.openelisglobal.accreditation.valueholder.AccreditingBody;
import org.openelisglobal.accreditation.valueholder.LogoVisibilityMode;
import org.openelisglobal.image.service.ImageService;
import org.openelisglobal.image.valueholder.Image;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * OGC-686 [QA-D.1] — accreditation schema + service behaviour against a real DB
 * (no mocks).
 *
 * <p>
 * The load-bearing assertions are the ones the tickets get wrong or leave
 * unverified: that expiry is a <em>body-level</em> property driving a single
 * status chip (no per-row expiry, no "majority of enrolled tests" heuristic),
 * that the (test, body) pair is unique, that a body cannot be deleted out from
 * under live enrollments, that {@code code} is immutable, and that
 * {@code qa/013} registers both tables for audit (without which every write
 * here would throw).
 *
 * <p>
 * Permission behaviour (qa.view.qms vs qa.manage.accreditation) is not asserted
 * here: {@code @PreAuthorize} is bypassed under direct service invocation, the
 * same limitation the sibling QI-config and test-catalog ITs document. That is
 * a UAT step.
 */
public class AccreditationServiceIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String TEST_GLUCOSE = "9101";
    private static final String TEST_SODIUM = "9102";
    private static final String USER = "1";

    @Autowired
    private AccreditingBodyService accreditingBodyService;

    @Autowired
    private TestAccreditationService testAccreditationService;

    @Autowired
    private TestAccreditationDAO testAccreditationDAO;

    @Autowired
    private ImageService imageService;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        executeDataSetWithStateManagement("testdata/accreditation.xml");
        clean();
    }

    @After
    public void tearDown() {
        clean();
    }

    private void clean() {
        jdbc.update("DELETE FROM clinlims.test_accreditation");
        jdbc.update("DELETE FROM clinlims.accrediting_body");
        jdbc.update("DELETE FROM clinlims.image WHERE description = 'accreditation-logo-test'");
    }

    // ---- bodies: create + validation ----

    @Test
    public void createBody_persistsAndDerivesStatus() {
        Long id = accreditingBodyService.createBody(body("SANAS", "SANAS General", LocalDate.now().plusYears(2)), USER)
                .getId();

        List<AccreditingBodyView> views = accreditingBodyService.getBodyViews();
        assertEquals(1, views.size());
        AccreditingBodyView view = views.get(0);
        assertEquals(id, view.id);
        assertEquals("SANAS", view.code);
        assertEquals("SANAS General", view.name);
        assertEquals(AccreditationStatus.ACTIVE.name(), view.status);
        assertEquals(LogoVisibilityMode.ANY_ACCREDITED_TEST.name(), view.logoVisibilityMode);
        // Threshold is stored even in ANY mode so toggling the mode never loses it.
        assertEquals(Short.valueOf((short) 80), view.thresholdPct);
        assertEquals(0L, view.enrolledTestCount);
        assertNull(view.logoImageId);
    }

    @Test
    public void createBody_lowercaseCodeIsNormalized() {
        accreditingBodyService.createBody(body("iso15189", "ISO 15189", LocalDate.now().plusYears(1)), USER);
        assertEquals("ISO15189", accreditingBodyService.getBodyViews().get(0).code);
    }

    @Test
    public void createBody_duplicateCodeRejected() {
        accreditingBodyService.createBody(body("SANAS", "First", LocalDate.now().plusYears(1)), USER);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> accreditingBodyService
                .createBody(body("sanas", "Second by different case", LocalDate.now().plusYears(1)), USER));
        assertTrue(e.getMessage().contains("already exists"));
    }

    @Test
    public void createBody_malformedCodeRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> accreditingBodyService.createBody(body("A", "Too short", LocalDate.now().plusYears(1)), USER));
        assertThrows(IllegalArgumentException.class, () -> accreditingBodyService
                .createBody(body("has space", "Bad chars", LocalDate.now().plusYears(1)), USER));
    }

    @Test
    public void createBody_requiresNameAndExpiry() {
        assertThrows(IllegalArgumentException.class,
                () -> accreditingBodyService.createBody(body("SANAS", "  ", LocalDate.now().plusYears(1)), USER));
        assertThrows(IllegalArgumentException.class,
                () -> accreditingBodyService.createBody(body("SANAS", "No expiry", null), USER));
    }

    @Test
    public void createBody_thresholdOutOfRangeRejected() {
        AccreditingBody b = body("SANAS", "Bad threshold", LocalDate.now().plusYears(1));
        b.setLogoVisibilityMode(LogoVisibilityMode.PERCENTAGE);
        b.setThresholdPct((short) 101);
        assertThrows(IllegalArgumentException.class, () -> accreditingBodyService.createBody(b, USER));
    }

    // ---- bodies: update ----

    @Test
    public void updateBody_changesEditableFieldsButNotCode() {
        Long id = accreditingBodyService.createBody(body("SANAS", "Original", LocalDate.now().plusYears(2)), USER)
                .getId();

        AccreditingBody edit = body(null, "Renamed", LocalDate.now().plusYears(3));
        edit.setLogoVisibilityMode(LogoVisibilityMode.PERCENTAGE);
        edit.setThresholdPct((short) 50);
        edit.setDisplayOrder((short) 20);
        accreditingBodyService.updateBody(id, edit, USER);

        AccreditingBodyView view = accreditingBodyService.getBodyViews().get(0);
        assertEquals("SANAS", view.code);
        assertEquals("Renamed", view.name);
        assertEquals(LogoVisibilityMode.PERCENTAGE.name(), view.logoVisibilityMode);
        assertEquals(Short.valueOf((short) 50), view.thresholdPct);
        assertEquals(Short.valueOf((short) 20), view.displayOrder);
    }

    @Test
    public void updateBody_changingCodeRejected() {
        Long id = accreditingBodyService.createBody(body("SANAS", "Original", LocalDate.now().plusYears(2)), USER)
                .getId();
        AccreditingBody edit = body("OTHER", "Original", LocalDate.now().plusYears(2));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> accreditingBodyService.updateBody(id, edit, USER));
        assertTrue(e.getMessage().contains("cannot be changed"));
    }

    @Test
    public void updateBody_unknownIdRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> accreditingBodyService.updateBody(-1L, body(null, "Nobody", LocalDate.now().plusYears(1)), USER));
    }

    // ---- status: the whole point of putting expiry on the body ----

    @Test
    public void status_reflectsExpiryWindowAndActiveFlag() {
        accreditingBodyService.createBody(body("FUTURE", "Comfortably in date", LocalDate.now().plusYears(1)), USER);
        accreditingBodyService.createBody(body("SOON", "Inside the 60-day window", LocalDate.now().plusDays(30)), USER);
        accreditingBodyService.createBody(body("GONE", "Lapsed", LocalDate.now().minusDays(1)), USER);
        AccreditingBody inactive = body("OFF", "Deactivated", LocalDate.now().plusYears(1));
        inactive.setActive(false);
        accreditingBodyService.createBody(inactive, USER);

        assertEquals(AccreditationStatus.ACTIVE.name(), statusOf("FUTURE"));
        assertEquals(AccreditationStatus.EXPIRING.name(), statusOf("SOON"));
        assertEquals(AccreditationStatus.EXPIRED.name(), statusOf("GONE"));
        // Inactive wins over an in-date expiry: a deactivated body is out entirely.
        assertEquals(AccreditationStatus.INACTIVE.name(), statusOf("OFF"));
    }

    @Test
    public void status_boundaryOfExpiringWindowIsInclusive() {
        LocalDate today = LocalDate.now();
        assertEquals(AccreditationStatus.EXPIRING,
                AccreditationStatus.of(true, today.plusDays(AccreditationStatus.EXPIRING_WINDOW_DAYS), today));
        assertEquals(AccreditationStatus.ACTIVE,
                AccreditationStatus.of(true, today.plusDays(AccreditationStatus.EXPIRING_WINDOW_DAYS + 1), today));
        // Expiring today is still valid for reporting, not yet expired.
        assertEquals(AccreditationStatus.EXPIRING, AccreditationStatus.of(true, today, today));
    }

    // ---- enrollment ----

    @Test
    public void enroll_persistsMembershipAndCounts() {
        Long bodyId = accreditingBodyService
                .createBody(body("SANAS", "SANAS General", LocalDate.now().plusYears(2)), USER).getId();

        testAccreditationService.enroll(TEST_GLUCOSE, bodyId, LocalDate.now(), USER);
        testAccreditationService.enroll(TEST_SODIUM, bodyId, null, USER);

        AccreditingBodyView view = accreditingBodyService.getBodyViews().get(0);
        assertEquals(2L, view.enrolledTestCount);

        List<TestAccreditationView> rows = testAccreditationService.getEnrollmentViews(bodyId, null);
        assertEquals(2, rows.size());
        // Enrollment rows carry the BODY's expiry and status — they have none of their
        // own.
        for (TestAccreditationView row : rows) {
            assertEquals("SANAS", row.bodyCode);
            assertEquals(view.expiresOn, row.bodyExpiresOn);
            assertEquals(AccreditationStatus.ACTIVE.name(), row.status);
            assertNotNull(row.testName);
        }
    }

    @Test
    public void enroll_duplicatePairRejected() {
        Long bodyId = accreditingBodyService.createBody(body("SANAS", "S", LocalDate.now().plusYears(1)), USER).getId();
        testAccreditationService.enroll(TEST_GLUCOSE, bodyId, null, USER);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> testAccreditationService.enroll(TEST_GLUCOSE, bodyId, null, USER));
        assertTrue(e.getMessage().contains("already accredited"));
        assertEquals(1L, testAccreditationDAO.countByBody(bodyId));
    }

    @Test
    public void enroll_sameTestUnderTwoBodiesAllowed() {
        Long a = accreditingBodyService.createBody(body("ISO15189", "ISO", LocalDate.now().plusYears(2)), USER).getId();
        Long b = accreditingBodyService.createBody(body("SANAS", "SANAS", LocalDate.now().plusYears(2)), USER).getId();

        testAccreditationService.enroll(TEST_GLUCOSE, a, null, USER);
        testAccreditationService.enroll(TEST_GLUCOSE, b, null, USER);

        assertEquals(2, testAccreditationService.getEnrollmentViews(null, TEST_GLUCOSE).size());
    }

    @Test
    public void enroll_unknownTestOrBodyRejected() {
        Long bodyId = accreditingBodyService.createBody(body("SANAS", "S", LocalDate.now().plusYears(1)), USER).getId();
        assertThrows(IllegalArgumentException.class,
                () -> testAccreditationService.enroll("99999999", bodyId, null, USER));
        assertThrows(IllegalArgumentException.class,
                () -> testAccreditationService.enroll(TEST_GLUCOSE, -1L, null, USER));
    }

    @Test
    public void unenroll_removesRowAndFreesTheBody() {
        Long bodyId = accreditingBodyService.createBody(body("SANAS", "S", LocalDate.now().plusYears(1)), USER).getId();
        testAccreditationService.enroll(TEST_GLUCOSE, bodyId, null, USER);
        Long rowId = testAccreditationService.getEnrollmentViews(bodyId, null).get(0).id;

        testAccreditationService.unenroll(rowId, USER);

        assertEquals(0L, testAccreditationDAO.countByBody(bodyId));
        assertTrue(accreditingBodyService.getBodyViews().get(0).enrolledTestCount == 0);
    }

    // ---- delete guard (FR-6) ----

    @Test
    public void deleteBody_blockedWhileEnrolled_thenAllowed() {
        Long bodyId = accreditingBodyService.createBody(body("SANAS", "S", LocalDate.now().plusYears(1)), USER).getId();
        testAccreditationService.enroll(TEST_GLUCOSE, bodyId, null, USER);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> accreditingBodyService.deleteBody(bodyId, USER));
        assertTrue(e.getMessage().contains("Cannot delete"));
        assertTrue("the count belongs in the message so the UI can explain itself", e.getMessage().contains("1"));

        Long rowId = testAccreditationService.getEnrollmentViews(bodyId, null).get(0).id;
        testAccreditationService.unenroll(rowId, USER);
        accreditingBodyService.deleteBody(bodyId, USER);

        assertTrue(accreditingBodyService.getBodyViews().isEmpty());
    }

    // ---- logo image lifecycle: no orphaned image rows ----

    @Test
    public void logoImage_cleanedUpOnReplaceRemoveAndBodyDelete() {
        Long bodyId = accreditingBodyService.createBody(body("SANAS", "S", LocalDate.now().plusYears(1)), USER).getId();

        String first = saveImage();
        accreditingBodyService.setLogo(bodyId, first, USER);

        // Replace: the previous image must not be left dangling.
        String second = saveImage();
        accreditingBodyService.setLogo(bodyId, second, USER);
        assertEquals(0, imageRowCount(first));
        assertEquals(1, imageRowCount(second));

        // Remove (set to null): the current image goes too.
        accreditingBodyService.setLogo(bodyId, null, USER);
        assertEquals(0, imageRowCount(second));

        // Delete body: its logo image is removed with it.
        String third = saveImage();
        accreditingBodyService.setLogo(bodyId, third, USER);
        accreditingBodyService.deleteBody(bodyId, USER);
        assertEquals(0, imageRowCount(third));
    }

    private String saveImage() {
        Image image = new Image();
        image.setImage(new byte[] { 1, 2, 3 });
        image.setDescription("accreditation-logo-test");
        return imageService.save(image).getId();
    }

    private int imageRowCount(String imageId) {
        return jdbc.queryForObject("SELECT count(*) FROM clinlims.image WHERE id = ?", Integer.class,
                Long.valueOf(imageId));
    }

    // ---- summary (page banner + QA Overview Q5) ----

    @Test
    public void summary_countsActiveAndReportsWorstStatus() {
        accreditingBodyService.createBody(body("ISO15189", "ISO 15189", LocalDate.now().plusYears(2)), USER);
        accreditingBodyService.createBody(body("SANAS", "SANAS General", LocalDate.now().plusDays(10)), USER);
        AccreditingBody retired = body("OLD", "Regional legacy", LocalDate.now().plusYears(1));
        retired.setActive(false);
        accreditingBodyService.createBody(retired, USER);

        AccreditationSummary summary = accreditingBodyService.getSummary();
        assertEquals(3, summary.totalBodies);
        // The three counts are mutually exclusive and exclude the inactive body, so
        // "active" never silently includes something expiring or expired.
        assertEquals(1, summary.activeBodies);
        assertEquals(1, summary.expiringBodies);
        assertEquals(0, summary.expiredBodies);
        assertEquals(List.of("ISO 15189", "SANAS General"), summary.inForceBodyNames);
        assertEquals(AccreditationStatus.EXPIRING.name(), summary.worstStatus);
    }

    @Test
    public void summary_expiredBodyIsNotCountedAsActive() {
        accreditingBodyService.createBody(body("GONE", "Lapsed", LocalDate.now().minusDays(5)), USER);

        AccreditationSummary summary = accreditingBodyService.getSummary();
        assertEquals(1, summary.totalBodies);
        assertEquals(0, summary.activeBodies);
        assertEquals(1, summary.expiredBodies);
        // An expired body cannot be claimed, so it is not named as in force.
        assertTrue(summary.inForceBodyNames.isEmpty());
    }

    @Test
    public void summary_expiredBeatsExpiringAsWorstStatus() {
        accreditingBodyService.createBody(body("SOON", "Expiring", LocalDate.now().plusDays(10)), USER);
        accreditingBodyService.createBody(body("GONE", "Lapsed", LocalDate.now().minusDays(5)), USER);
        assertEquals(AccreditationStatus.EXPIRED.name(), accreditingBodyService.getSummary().worstStatus);
    }

    @Test
    public void summary_noBodiesConfigured_isInertNotAnError() {
        AccreditationSummary summary = accreditingBodyService.getSummary();
        assertEquals(0, summary.totalBodies);
        assertNull("no bodies means no status to report, not a red flag", summary.worstStatus);
        assertTrue(summary.inForceBodyNames.isEmpty());
    }

    // ---- audit registration (what qa/013's reference_tables rows are for) ----

    /**
     * Both tables must be registered in {@code reference_tables} with
     * {@code keep_history='Y'}, because every write here goes through the audited
     * base-class methods and {@code AuditTrailServiceImpl} <em>throws</em> (rolling
     * the write back) when the row is missing. So this asserts the registration
     * directly, and the fact that every other test in this class can write at all
     * is the second, indirect proof.
     *
     * <p>
     * Audit <em>emission</em> is deliberately not asserted:
     * {@code clinlims.history} stays empty for every entity under this harness, not
     * just these two — the same limitation the sibling
     * {@code QiConfigServiceIntegrationTest} documents. Verifying that a history
     * row appears per create/edit/delete is a UAT step against the deployed app.
     */
    @Test
    public void auditRegistration_isInPlaceForBothTables() {
        Long bodyId = accreditingBodyService.createBody(body("SANAS", "S", LocalDate.now().plusYears(1)), USER).getId();
        accreditingBodyService.updateBody(bodyId, body(null, "Renamed", LocalDate.now().plusYears(1)), USER);
        testAccreditationService.enroll(TEST_GLUCOSE, bodyId, null, USER);

        assertEquals("Y", keepHistoryFlag("accrediting_body"));
        assertEquals("Y", keepHistoryFlag("test_accreditation"));
    }

    // ---- helpers ----

    private String keepHistoryFlag(String tableName) {
        List<String> flags = jdbc.queryForList(
                "SELECT keep_history FROM clinlims.reference_tables WHERE LOWER(name) = ?", String.class, tableName);
        assertEquals("expected exactly one reference_tables row for " + tableName, 1, flags.size());
        return flags.get(0);
    }

    private String statusOf(String code) {
        return accreditingBodyService.getBodyViews().stream().filter(v -> code.equals(v.code)).findFirst()
                .orElseThrow(() -> new AssertionError("no body with code " + code)).status;
    }

    private AccreditingBody body(String code, String name, LocalDate expiresOn) {
        AccreditingBody b = new AccreditingBody();
        b.setCode(code);
        b.setName(name);
        b.setExpiresOn(expiresOn);
        return b;
    }
}
