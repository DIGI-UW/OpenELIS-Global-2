package org.openelisglobal.reportqueue.service;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.service.AnalysisServiceImpl;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.TableIdService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.reportqueue.dao.ReportQueueCriteria;
import org.openelisglobal.reportqueue.dao.ReportQueueQueryDAO;
import org.openelisglobal.reportqueue.dto.ReportQueuePageDTO;
import org.openelisglobal.reportqueue.dto.ReportQueuePreferenceDTO;
import org.openelisglobal.reportqueue.dto.ReportQueueRequest;
import org.openelisglobal.reportqueue.dto.ReportQueueRowDTO;
import org.openelisglobal.reportqueue.dto.ReportQueueRowDetailDTO;
import org.openelisglobal.reportqueue.service.ReportQueueService.PrintOutcome;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The Report Print Queue on one clinical order (FRS OGC-1031 r4, AC-2 to AC-6,
 * AC-16, AC-21, AC-23): which rows it lists, how printing creates versions and
 * reprints return the archived file, how released and corrected results move
 * the state, and what a restricted user sees.
 */
public class ReportQueueServiceTest extends BaseWebContextSensitiveTest {

    private static final long SAMPLE = 9301L;
    private static final String LAB_NO = "V1RPT0000009301";
    private static final String ADMIN = "9301";
    private static final String USER_WITHOUT_LAB_UNITS = "9302";

    @Autowired
    private ReportQueueService reportQueueService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private IStatusService statusService;

    @Autowired
    private ReportQueueQueryDAO queryDAO;

    @Autowired
    private UserRoleService userRoleService;

    private String originalBillingLabel;

    @Before
    public void loadOrder() throws Exception {
        executeDataSetWithStateManagement("testdata/patient-report-v1.xml");
        originalBillingLabel = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.BILLING_REFERENCE_NUMBER_LABEL);
        ConfigurationProperties.getInstance().setPropertyValue(Property.BILLING_REFERENCE_NUMBER_LABEL, "9309");
        jdbcTemplate.update("DELETE FROM clinlims.report_print_event WHERE report_version_id IN"
                + " (SELECT id FROM clinlims.report_version WHERE sample_id = ?)", SAMPLE);
        jdbcTemplate.update("DELETE FROM clinlims.report_version_item WHERE report_version_id IN"
                + " (SELECT id FROM clinlims.report_version WHERE sample_id = ?)", SAMPLE);
        jdbcTemplate.update("DELETE FROM clinlims.report_version WHERE sample_id = ?", SAMPLE);
        jdbcTemplate.update("DELETE FROM clinlims.report_queue_preference");
        ensureSystemRole(Constants.ROLE_GLOBAL_ADMIN);
        jdbcTemplate.update("INSERT INTO clinlims.system_user_role (system_user_id, role_id)"
                + " SELECT 9301, id FROM clinlims.system_role WHERE name = ?", Constants.ROLE_GLOBAL_ADMIN);
        ensureSiteInformation(ReportQueueServiceImpl.INSTALL_DATE_SITE_INFO, "2026-01-01 00:00:00");
        jdbcTemplate.update("UPDATE clinlims.site_information SET value = '2026-01-01 00:00:00' WHERE name = ?",
                ReportQueueServiceImpl.INSTALL_DATE_SITE_INFO);
    }

    @After
    public void restoreBillingLabel() {
        ConfigurationProperties.getInstance().setPropertyValue(Property.BILLING_REFERENCE_NUMBER_LABEL,
                originalBillingLabel);
    }

    @After
    public void revokeAdminRole() {
        jdbcTemplate.update("DELETE FROM clinlims.system_user_role WHERE system_user_id = 9301");
    }

    /**
     * Tests here are not transactional (the base class turns transactions off), so
     * every print's history row and version is removed again: a leftover
     * document_track row dated today would make the order count as reported for the
     * report tests that share this fixture.
     */
    @After
    public void removePrintHistory() {
        jdbcTemplate.update("DELETE FROM clinlims.document_track WHERE row_id = 9301"
                + " AND table_id = (SELECT id FROM clinlims.reference_tables WHERE name = 'SAMPLE')");
        jdbcTemplate.update("DELETE FROM clinlims.report_print_event WHERE report_version_id IN"
                + " (SELECT id FROM clinlims.report_version WHERE sample_id = 9301)");
        jdbcTemplate.update("DELETE FROM clinlims.report_version_item WHERE report_version_id IN"
                + " (SELECT id FROM clinlims.report_version WHERE sample_id = 9301)");
        jdbcTemplate.update("DELETE FROM clinlims.report_version WHERE sample_id = 9301");
    }

    @Test
    public void aReleasedOrderListsAsPartialAndUnprintedBeforeAnyPrint() {
        ReportQueueCriteria unscoped = new ReportQueueCriteria();
        unscoped.setFinalizedStatusId(statusService.getStatusID(AnalysisStatus.Finalized));
        unscoped.setExcludedStatusIds(List.of(statusService.getStatusID(AnalysisStatus.Canceled),
                statusService.getStatusID(AnalysisStatus.SampleRejected)));
        unscoped.setOrganizationRequesterTypeId(TableIdService.getInstance().ORGANIZATION_REQUESTER_TYPE_ID);
        unscoped.setProviderRequesterTypeId(TableIdService.getInstance().PROVIDER_REQUESTER_TYPE_ID);
        assertTrue("the state query sees the order", queryDAO.findRow(SAMPLE, unscoped).isPresent());
        assertTrue("the admin user has global scope", userRoleService.userInRole(ADMIN, Constants.ROLE_GLOBAL_ADMIN));

        ReportQueueRowDTO row = row(list("all"));

        assertEquals("PARTIAL", row.getState());
        assertEquals("rejected sample's analysis is not counted", 2, row.getReleasedCount());
        assertEquals(3, row.getTotalCount());
        assertEquals("UNPRINTED", row.getPrintStatus());
        assertNull(row.getVersion());
        assertEquals("Kila, Mary", row.getPatientName());
        assertFalse("6.3 is above normal, not critical", row.isCritical());
        assertTrue(list("all").getToPrint() >= 1);
        assertFalse(analysisService.patientReportHasBeenDone(analysisService.get("9301")));
    }

    @Test
    public void printingCreatesVersionOneArchivedWithItsHashAndTheRowTurnsPrinted() throws Exception {
        PrintOutcome print = reportQueueService.print(SAMPLE, ADMIN).orElseThrow();

        assertEquals(1, print.getVersion());
        assertFalse(print.isReprint());
        assertTrue(print.getPdf().length > 1000);
        Map<String, Object> version = jdbcTemplate.queryForMap(
                "SELECT version, amended, sha256_hash, length(pdf_content) AS size, released_count, total_count,"
                        + " legacy FROM clinlims.report_version WHERE sample_id = ?",
                SAMPLE);
        assertEquals(1, ((Number) version.get("version")).intValue());
        assertEquals(Boolean.FALSE, version.get("amended"));
        assertEquals(64, ((String) version.get("sha256_hash")).length());
        assertEquals(ReportQueueServiceImpl.sha256Hex(print.getPdf()), version.get("sha256_hash"));
        assertEquals(print.getPdf().length, ((Number) version.get("size")).intValue());
        assertEquals(2, ((Number) version.get("released_count")).intValue());
        assertEquals(3, ((Number) version.get("total_count")).intValue());
        assertEquals(Boolean.FALSE, version.get("legacy"));
        assertEquals(Integer.valueOf(2),
                jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM clinlims.report_version_item i"
                                + " JOIN clinlims.report_version v ON v.id = i.report_version_id WHERE v.sample_id = ?",
                        Integer.class, SAMPLE));
        assertEquals("6.3", jdbcTemplate.queryForObject(
                "SELECT printed_value FROM clinlims.report_version_item" + " WHERE analysis_id = 9301", String.class));

        ReportQueueRowDTO row = row(list("all"));
        assertEquals("PRINTED", row.getPrintStatus());
        assertEquals(Integer.valueOf(1), row.getVersion());
        assertEquals("PARTIAL", row.getState());
        assertTrue("the home dashboard reads the same definition",
                analysisService.patientReportHasBeenDone(analysisService.get("9301")));
        assertEquals("printed rows leave the To print tile", 0, list("all").getToPrint());
    }

    @Test
    public void printingAgainIsAReprintOfTheArchivedBytesWithoutANewVersion() {
        PrintOutcome first = reportQueueService.print(SAMPLE, ADMIN).orElseThrow();
        PrintOutcome again = reportQueueService.print(SAMPLE, ADMIN).orElseThrow();

        assertTrue(again.isReprint());
        assertEquals(1, again.getVersion());
        assertArrayEquals("a reprint returns the archived file byte for byte", first.getPdf(), again.getPdf());
        assertEquals(Integer.valueOf(1), jdbcTemplate.queryForObject(
                "SELECT count(*) FROM clinlims.report_version WHERE sample_id = ?", Integer.class, SAMPLE));
        assertEquals(Integer.valueOf(1),
                jdbcTemplate.queryForObject("SELECT count(*) FROM clinlims.report_print_event e JOIN"
                        + " clinlims.report_version v ON v.id = e.report_version_id WHERE v.sample_id = ? AND e.reprint",
                        Integer.class, SAMPLE));
        ReportQueueRowDetailDTO detail = reportQueueService.detail(SAMPLE, ADMIN).orElseThrow();
        assertEquals(1, detail.getVersions().size());
        assertEquals(1, detail.getVersions().get(0).getReprints().size());
        assertTrue(detail.getVersions().get(0).isArchived());
    }

    @Test
    public void releasingMoreResultsReturnsTheOrderAsFinalUnprintedAndPrintsANonAmendedVersionTwo() {
        reportQueueService.print(SAMPLE, ADMIN).orElseThrow();
        release("9302");

        ReportQueueRowDTO row = row(list("all"));
        assertEquals("FINAL", row.getState());
        assertEquals("UNPRINTED", row.getPrintStatus());
        assertEquals(3, row.getReleasedCount());

        PrintOutcome second = reportQueueService.print(SAMPLE, ADMIN).orElseThrow();
        assertEquals(2, second.getVersion());
        assertFalse(second.isReprint());
        Map<String, Object> version = jdbcTemplate.queryForMap(
                "SELECT amended, change_summary FROM clinlims.report_version WHERE sample_id = ? AND version = 2",
                SAMPLE);
        assertEquals("completing a partial report is not an amendment", Boolean.FALSE, version.get("amended"));
        assertTrue(String.valueOf(version.get("change_summary")),
                String.valueOf(version.get("change_summary")).contains("Added: Urea"));
        row = row(list("all"));
        assertEquals("FINAL", row.getState());
        assertEquals("PRINTED", row.getPrintStatus());
        assertEquals(Integer.valueOf(2), row.getVersion());
    }

    @Test
    public void changingAPrintedResultReturnsTheOrderAsAmendedAndItStaysAmended() {
        reportQueueService.print(SAMPLE, ADMIN).orElseThrow();
        jdbcTemplate.update("UPDATE clinlims.result SET value = '7.1', lastupdated = now() WHERE id = 9301");

        ReportQueueRowDTO row = row(list("all"));
        assertEquals("AMENDED", row.getState());
        assertEquals("UNPRINTED", row.getPrintStatus());
        assertEquals(1, list("all").getAmendedToPrint());

        PrintOutcome amended = reportQueueService.print(SAMPLE, ADMIN).orElseThrow();
        assertEquals(2, amended.getVersion());
        Map<String, Object> version = jdbcTemplate.queryForMap(
                "SELECT amended, change_summary FROM clinlims.report_version WHERE sample_id = ? AND version = 2",
                SAMPLE);
        assertEquals(Boolean.TRUE, version.get("amended"));
        String summary = String.valueOf(version.get("change_summary"));
        assertTrue(summary, summary.contains("Glucose") && summary.contains("6.3") && summary.contains("7.1"));

        row = row(list("all"));
        assertEquals("amended stays once reached", "AMENDED", row.getState());
        assertEquals("PRINTED", row.getPrintStatus());

        release("9302");
        row = row(list("all"));
        assertEquals("a later release keeps the state Amended", "AMENDED", row.getState());
        assertEquals("UNPRINTED", row.getPrintStatus());
        assertEquals(0, list("all").getAmendedToPrint() - 1);
    }

    /**
     * OGC-1529: a printed result corrected on Results waits for validation before
     * the order is due again. A print in that window reprints the version on file,
     * and the version printed after validation names the old and the new value
     * instead of removing the result and adding it back.
     */
    @Test
    public void aCorrectionAwaitingValidationKeepsTheOrderPrintedUntilItIsValidated() {
        PrintOutcome first = reportQueueService.print(SAMPLE, ADMIN).orElseThrow();
        correctAwaitingValidation("9301", "7.1");

        ReportQueueRowDTO row = row(list("all"));
        assertEquals("PRINTED", row.getPrintStatus());
        assertTrue(row.isAwaitingValidation());
        assertEquals("not Amended before validation", "PARTIAL", row.getState());
        assertEquals(0, list("all").getAmendedToPrint());
        assertEquals(0, list("all").getToPrint());
        assertTrue("the home dashboard reads the same definition",
                analysisService.patientReportHasBeenDone(analysisService.get("9305")));

        PrintOutcome waiting = reportQueueService.print(SAMPLE, ADMIN).orElseThrow();
        assertTrue("a print before validation is a reprint", waiting.isReprint());
        assertEquals(1, waiting.getVersion());
        assertArrayEquals(first.getPdf(), waiting.getPdf());
        assertEquals(Integer.valueOf(1), jdbcTemplate.queryForObject(
                "SELECT count(*) FROM clinlims.report_version WHERE sample_id = ?", Integer.class, SAMPLE));

        release("9301");
        row = row(list("all"));
        assertEquals("AMENDED", row.getState());
        assertEquals("UNPRINTED", row.getPrintStatus());
        assertFalse(row.isAwaitingValidation());
        assertEquals(1, list("all").getAmendedToPrint());

        PrintOutcome corrected = reportQueueService.print(SAMPLE, ADMIN).orElseThrow();
        assertEquals(2, corrected.getVersion());
        assertFalse(corrected.isReprint());
        Map<String, Object> version = jdbcTemplate.queryForMap(
                "SELECT amended, change_summary FROM clinlims.report_version WHERE sample_id = ? AND version = 2",
                SAMPLE);
        assertEquals(Boolean.TRUE, version.get("amended"));
        String summary = String.valueOf(version.get("change_summary"));
        assertTrue(summary, summary.contains("Glucose") && summary.contains("6.3") && summary.contains("7.1"));
        assertFalse(summary, summary.contains("Removed") || summary.contains("Added"));
    }

    /**
     * A result released while a printed one awaits validation waits with it, so the
     * next version carries both changes and never drops the corrected result.
     */
    @Test
    public void aNewReleaseWaitsForACorrectionAwaitingValidation() {
        reportQueueService.print(SAMPLE, ADMIN).orElseThrow();
        correctAwaitingValidation("9301", "7.1");
        release("9302");

        ReportQueueRowDTO row = row(list("all"));
        assertEquals("PRINTED", row.getPrintStatus());
        assertEquals("the corrected result is not released again yet", "PARTIAL", row.getState());
        assertTrue(reportQueueService.print(SAMPLE, ADMIN).orElseThrow().isReprint());

        release("9301");
        assertEquals("UNPRINTED", row(list("all")).getPrintStatus());
        assertEquals(2, reportQueueService.print(SAMPLE, ADMIN).orElseThrow().getVersion());
        String summary = jdbcTemplate.queryForObject(
                "SELECT change_summary FROM clinlims.report_version WHERE sample_id = ? AND version = 2", String.class,
                SAMPLE);
        assertTrue(summary, summary.contains("6.3") && summary.contains("7.1") && summary.contains("Added: Urea"));
        assertFalse(summary, summary.contains("Removed"));
    }

    /**
     * A printed result that is cancelled is still removed from the next version,
     * whatever else is pending: only a correction awaiting validation holds.
     */
    @Test
    public void aCancelledPrintedResultIsStillRemoved() {
        reportQueueService.print(SAMPLE, ADMIN).orElseThrow();
        jdbcTemplate.update("UPDATE clinlims.analysis SET status_id = CAST(? AS numeric) WHERE id = 9305",
                statusService.getStatusID(AnalysisStatus.Canceled));

        ReportQueueRowDTO row = row(list("all"));
        assertEquals("AMENDED", row.getState());
        assertEquals("UNPRINTED", row.getPrintStatus());
        assertEquals(2, reportQueueService.print(SAMPLE, ADMIN).orElseThrow().getVersion());
        String summary = jdbcTemplate.queryForObject(
                "SELECT change_summary FROM clinlims.report_version WHERE sample_id = ? AND version = 2", String.class,
                SAMPLE);
        assertTrue(summary, summary.contains("Removed"));
    }

    @Test
    public void theTimeWindowHidesOnlyPrintedRows() {
        assertNotNull("an unprinted row released on 2026-10-01 shows under Last 24 hours", find(list("24h")));

        reportQueueService.print(SAMPLE, ADMIN).orElseThrow();
        assertNull("a printed row that old leaves Last 24 hours", find(list("24h")));
        assertNotNull("and still shows under All time", find(list("all")));
    }

    @Test
    public void aLegacyPrintCountsAsPrintedUntilSomethingIsReleasedOrCorrected() {
        insertLegacyVersion("2026-10-02 09:00:00");
        ReportQueueRowDTO row = row(list("all"));
        assertEquals("PRINTED", row.getPrintStatus());
        assertEquals(Integer.valueOf(1), row.getVersion());
        assertEquals("PARTIAL", row.getState());
        ReportQueueRowDetailDTO detail = reportQueueService.detail(SAMPLE, ADMIN).orElseThrow();
        assertTrue(detail.getVersions().get(0).isLegacy());
        assertFalse(detail.getVersions().get(0).isArchived());

        jdbcTemplate.update("UPDATE clinlims.analysis SET corrected = true WHERE id = 9301");
        assertEquals("a correction after a legacy print is an amendment", "AMENDED", row(list("all")).getState());

        jdbcTemplate.update("UPDATE clinlims.analysis SET corrected = false WHERE id = 9301");
        correctAwaitingValidation("9305", "81");
        row = row(list("all"));
        assertEquals("a correction awaiting validation is not an amendment yet", "PARTIAL", row.getState());
        assertEquals("PRINTED", row.getPrintStatus());
        release("9305");
        jdbcTemplate.update("UPDATE clinlims.analysis SET corrected = false WHERE id = 9305");
        jdbcTemplate.update(
                "UPDATE clinlims.report_version SET issued_at = '2026-09-30 09:00:00+00' WHERE sample_id = ?", SAMPLE);
        row = row(list("all"));
        assertEquals("released after the legacy print", "UNPRINTED", row.getPrintStatus());
        assertEquals("PARTIAL", row.getState());
    }

    @Test
    public void aLabNumberLookupShowsAnOrderWithNothingReleasedWithoutAPrintButton() {
        String technicalAcceptance = statusService.getStatusID(AnalysisStatus.TechnicalAcceptance);
        jdbcTemplate.update("UPDATE clinlims.analysis SET status_id = CAST(? AS numeric) WHERE id IN (9301, 9305)",
                technicalAcceptance);

        ReportQueueRequest request = new ReportQueueRequest();
        request.setLabNo(LAB_NO);
        ReportQueuePageDTO page = reportQueueService.list(request, ADMIN);
        assertEquals(1, page.getRows().size());
        assertTrue(page.getRows().get(0).isNothingReleased());
        assertEquals(LAB_NO, page.getRows().get(0).getLabNumber());

        try {
            reportQueueService.print(SAMPLE, ADMIN);
            throw new AssertionError("an order with nothing released must not print");
        } catch (ReportQueueService.NothingReleasedException expected) {
            assertEquals(LAB_NO, expected.getMessage());
        }
    }

    /**
     * Every released result goes back for correction after version 1 was printed:
     * the order leaves the queue, but its lookup row says a correction awaits
     * validation, keeps the version that is on file, and a print reprints it
     * (OGC-1529) instead of claiming nothing was ever released.
     */
    @Test
    public void aPrintedOrderWhoseResultsGoBackKeepsItsVersionInTheLookupAndTheDetail() {
        jdbcTemplate.update("INSERT INTO clinlims.person (id, first_name, last_name, lastupdated)"
                + " VALUES (99301, 'Ruth', 'Nambi', now())");
        jdbcTemplate.update("INSERT INTO clinlims.sample_requester (id, sample_id, requester_id, requester_type_id,"
                + " lastupdated) VALUES (99301, 9301, 99301, (SELECT id FROM clinlims.requester_type"
                + " WHERE requester_type = 'provider'), now())");
        reportQueueService.print(SAMPLE, ADMIN).orElseThrow();
        String technicalAcceptance = statusService.getStatusID(AnalysisStatus.TechnicalAcceptance);
        jdbcTemplate.update("UPDATE clinlims.analysis SET status_id = CAST(? AS numeric) WHERE id IN (9301, 9305)",
                technicalAcceptance);

        ReportQueueRequest request = new ReportQueueRequest();
        request.setLabNo(LAB_NO);
        ReportQueueRowDTO row = reportQueueService.list(request, ADMIN).getRows().get(0);
        assertFalse(row.isNothingReleased());
        assertTrue(row.isAwaitingValidation());
        assertEquals("PARTIAL", row.getState());
        assertEquals(0, row.getReleasedCount());
        assertEquals(3, row.getTotalCount());
        assertEquals(Integer.valueOf(1), row.getVersion());
        assertEquals("PRINTED", row.getPrintStatus());
        assertEquals("Nambi, Ruth", row.getRequester());
        PrintOutcome reprint = reportQueueService.print(SAMPLE, ADMIN).orElseThrow();
        assertTrue(reprint.isReprint());
        assertEquals(1, reprint.getVersion());

        ReportQueueRowDetailDTO detail = reportQueueService.detail(SAMPLE, ADMIN).orElseThrow();
        assertEquals(1, detail.getVersions().size());
        assertEquals(1, detail.getVersions().get(0).getVersion());
        assertEquals("Nambi, Ruth", detail.getRequester());
    }

    /**
     * A batch print (the Patient Status Report range) gives an order with nothing
     * released its preliminary page instead of dropping it, and archives nothing.
     */
    @Test
    public void aBatchPrintsThePreliminaryReportForAnOrderWithNothingReleased() {
        String technicalAcceptance = statusService.getStatusID(AnalysisStatus.TechnicalAcceptance);
        jdbcTemplate.update("UPDATE clinlims.analysis SET status_id = CAST(? AS numeric) WHERE id IN (9301, 9305)",
                technicalAcceptance);

        byte[] pdf = reportQueueService.printPatientReports(List.of(String.valueOf(SAMPLE)), ADMIN);

        assertEquals("%PDF-", new String(pdf, 0, 5));
        assertEquals(Integer.valueOf(0), jdbcTemplate.queryForObject(
                "SELECT count(*) FROM clinlims.report_version WHERE sample_id = ?", Integer.class, SAMPLE));
    }

    /**
     * Two clerks print the same changed order at the same moment: one issues the
     * new version and the other reprints it, nobody gets an error.
     */
    @Test
    public void twoPrintsAtOnceIssueOneVersionAndOneReprint() throws Exception {
        reportQueueService.print(SAMPLE, ADMIN).orElseThrow();
        jdbcTemplate.update("UPDATE clinlims.result SET value = '7.1', lastupdated = now() WHERE id = 9301");

        ExecutorService clerks = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<PrintOutcome>> prints = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            prints.add(clerks.submit(() -> {
                ready.countDown();
                go.await();
                return reportQueueService.print(SAMPLE, ADMIN).orElseThrow();
            }));
        }
        ready.await();
        go.countDown();
        List<PrintOutcome> outcomes = new ArrayList<>();
        for (Future<PrintOutcome> print : prints) {
            outcomes.add(print.get(60, TimeUnit.SECONDS));
        }
        clerks.shutdown();

        assertEquals(2, outcomes.stream().filter(outcome -> outcome.getVersion() == 2).count());
        assertEquals(1, outcomes.stream().filter(PrintOutcome::isReprint).count());
        assertEquals(Integer.valueOf(2), jdbcTemplate.queryForObject(
                "SELECT count(*) FROM clinlims.report_version WHERE sample_id = ?", Integer.class, SAMPLE));
        assertEquals(Integer.valueOf(2),
                jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM clinlims.report_print_event e JOIN clinlims.report_version v"
                                + " ON v.id = e.report_version_id WHERE v.sample_id = ? AND v.version = 2",
                        Integer.class, SAMPLE));
    }

    @Test
    public void aUserWithoutLabUnitsSeesNoRowsAndNoCounts() {
        ReportQueuePageDTO page = list("all", USER_WITHOUT_LAB_UNITS);
        assertEquals(0, page.getRows().size());
        assertEquals(0, page.getTotal());
        assertEquals(0, page.getToPrint());
    }

    /**
     * BR-9: the scope that hides a row also closes its detail, its print and its
     * archived file, whose ids are sequential and easy to guess.
     */
    @Test
    public void aUserWithoutLabUnitsCannotOpenPrintOrDownloadAnOrder() {
        reportQueueService.print(SAMPLE, ADMIN).orElseThrow();
        long versionId = jdbcTemplate.queryForObject("SELECT id FROM clinlims.report_version WHERE sample_id = ?",
                Long.class, SAMPLE);
        assertTrue(reportQueueService.version(versionId, ADMIN).isPresent());
        assertTrue(reportQueueService.detail(SAMPLE, ADMIN).isPresent());

        assertTrue(reportQueueService.detail(SAMPLE, USER_WITHOUT_LAB_UNITS).isEmpty());
        assertTrue(reportQueueService.print(SAMPLE, USER_WITHOUT_LAB_UNITS).isEmpty());
        assertTrue(reportQueueService.version(versionId, USER_WITHOUT_LAB_UNITS).isEmpty());
        ReportQueueRequest lookup = new ReportQueueRequest();
        lookup.setLabNo(LAB_NO);
        assertEquals("a lab number lookup does not reach outside the user's units either", 0,
                reportQueueService.list(lookup, USER_WITHOUT_LAB_UNITS).getRows().size());
        assertEquals("the refused print issued nothing", Integer.valueOf(1), jdbcTemplate.queryForObject(
                "SELECT count(*) FROM clinlims.report_version WHERE sample_id = ?", Integer.class, SAMPLE));
    }

    @Test
    public void anOrderIsInScopeOnlyThroughOneOfItsSections() {
        ReportQueueCriteria criteria = new ReportQueueCriteria();
        criteria.setSectionIds(null);
        assertTrue("no scope means the whole lab", queryDAO.isInScope(SAMPLE, criteria));
        criteria.setSectionIds(List.of("9301"));
        assertTrue("the order's own section", queryDAO.isInScope(SAMPLE, criteria));
        criteria.setSectionIds(List.of("9302"));
        assertFalse("another section", queryDAO.isInScope(SAMPLE, criteria));
        criteria.setSectionIds(List.of());
        assertFalse("no sections at all", queryDAO.isInScope(SAMPLE, criteria));
    }

    /**
     * The Critical tag comes from the same BB / EE rule as the report, now from one
     * query per page instead of a report snapshot per row (NFR-1).
     */
    @Test
    public void aReleasedResultBeyondItsCriticalBoundMarksTheRowCritical() {
        assertFalse("6.3 is only above normal", row(list("all")).isCritical());
        jdbcTemplate.update("INSERT INTO clinlims.test_result_component (id, test_id, code, label, display_order,"
                + " allow_multiple_readings, is_active, lastupdated, is_primary, show_on_report)"
                + " VALUES ('cba2e26b-537a-417d-ab03-b375e4246daf', 9301, 'GLU', 'Glucose', 1, false, 'Y', now(),"
                + " true, true)");
        jdbcTemplate.update("INSERT INTO clinlims.test_result (id, test_id, tst_rslt_type, component_id, lastupdated)"
                + " VALUES (99301, 9301, 'N', 'cba2e26b-537a-417d-ab03-b375e4246daf', now())");
        jdbcTemplate.update("UPDATE clinlims.result SET test_result_id = 99301 WHERE id = 9301");
        assertFalse("a result answering a component (UUID id) is read like any other", row(list("all")).isCritical());
        jdbcTemplate.update("INSERT INTO clinlims.result_limits (id, test_id, test_result_type_id, min_age, max_age,"
                + " low_normal, high_normal, low_valid, high_valid, low_reporting_range, high_reporting_range,"
                + " low_critical, high_critical, always_validate, lastupdated) VALUES (99301, 9301, 4, 0, 'Infinity',"
                + " 3.5, 5.5, '-Infinity', 'Infinity', '-Infinity', 'Infinity', 2.0, 6.0, false, NOW())");

        assertTrue("6.3 is beyond the critical bound of 6.0", row(list("all")).isCritical());
    }

    @After
    public void removeCriticalLimit() {
        jdbcTemplate.update("DELETE FROM clinlims.result_limits WHERE id = 99301");
        jdbcTemplate.update("UPDATE clinlims.result SET test_result_id = NULL WHERE test_result_id = 99301");
        jdbcTemplate.update("DELETE FROM clinlims.test_result WHERE id = 99301");
        jdbcTemplate
                .update("DELETE FROM clinlims.test_result_component WHERE id = 'cba2e26b-537a-417d-ab03-b375e4246daf'");
    }

    /**
     * The order's provider requester is stored as the person's id, and the user who
     * released a result is only recorded in the analysis's audit history: the queue
     * reads both the way the patient report does.
     */
    @Test
    public void theRequesterIsThePersonOnTheOrderAndTheValidatorComesFromTheReleaseHistory() {
        jdbcTemplate.update("INSERT INTO clinlims.person (id, first_name, last_name, lastupdated)"
                + " VALUES (99301, 'Ruth', 'Nambi', now())");
        jdbcTemplate.update("INSERT INTO clinlims.sample_requester (id, sample_id, requester_id, requester_type_id,"
                + " lastupdated) VALUES (99301, 9301, 99301, (SELECT id FROM clinlims.requester_type"
                + " WHERE requester_type = 'provider'), now())");
        history("9301", "9303", "2026-10-01 10:00:00");
        history("9301", "9301", "2026-10-01 10:15:00.400");
        history("9301", "9303", "2026-10-01 13:00:00");

        ReportQueueRowDTO row = row(list("all"));
        ReportQueueRowDetailDTO detail = reportQueueService.detail(SAMPLE, ADMIN).orElseThrow();

        assertEquals("Nambi, Ruth", row.getRequester());
        assertEquals("Nambi, Ruth", detail.getRequester());
        assertEquals("Sarah Chen", detail.getTests().stream().filter(test -> test.getAnalysisId() == 9301L).findFirst()
                .orElseThrow().getValidatedBy());
    }

    @After
    public void removeRequesterAndHistory() {
        jdbcTemplate.update("DELETE FROM clinlims.history WHERE reference_id = 9301 AND reference_table = ?",
                Integer.valueOf(AnalysisServiceImpl.getTableReferenceId()));
        jdbcTemplate.update("DELETE FROM clinlims.sample_requester WHERE id = 99301");
        jdbcTemplate.update("DELETE FROM clinlims.person WHERE id = 99301");
    }

    private void history(String analysisId, String userId, String at) {
        jdbcTemplate.update(
                "INSERT INTO clinlims.history (id, sys_user_id, reference_id, reference_table, timestamp, activity)"
                        + " VALUES (nextval('clinlims.history_seq'), ?, ?, ?, ?, 'U')",
                Integer.valueOf(userId), Integer.valueOf(analysisId),
                Integer.valueOf(AnalysisServiceImpl.getTableReferenceId()), Timestamp.valueOf(at));
    }

    @Test
    public void preferencesPersistTheWindowAndPageSizePerUser() {
        ReportQueuePreferenceDTO saved = reportQueueService.savePreferences(ADMIN,
                new ReportQueuePreferenceDTO("30d", 50));
        assertEquals("30d", saved.getTimeWindow());
        assertEquals(Integer.valueOf(50), saved.getPageSize());
        assertEquals("30d", reportQueueService.preferences(ADMIN).getTimeWindow());

        ReportQueuePageDTO page = reportQueueService.list(new ReportQueueRequest(), ADMIN);
        assertEquals("30d", page.getTimeWindow());
        assertEquals(50, page.getPageSize());

        ReportQueuePreferenceDTO refused = reportQueueService.savePreferences(ADMIN,
                new ReportQueuePreferenceDTO("yesterday", 7));
        assertEquals("unknown values keep the saved ones", "30d", refused.getTimeWindow());
        assertEquals(Integer.valueOf(50), refused.getPageSize());
    }

    private ReportQueuePageDTO list(String window) {
        return list(window, ADMIN);
    }

    private ReportQueuePageDTO list(String window, String user) {
        ReportQueueRequest request = new ReportQueueRequest();
        request.setWindow(window);
        request.setPageSize(100);
        return reportQueueService.list(request, user);
    }

    private static ReportQueueRowDTO find(ReportQueuePageDTO page) {
        for (ReportQueueRowDTO row : page.getRows()) {
            if (LAB_NO.equals(row.getLabNumber())) {
                return row;
            }
        }
        return null;
    }

    private static ReportQueueRowDTO row(ReportQueuePageDTO page) {
        ReportQueueRowDTO row = find(page);
        assertNotNull("the order must be listed: " + page.getRows().size() + " rows", row);
        return row;
    }

    private void release(String analysisId) {
        jdbcTemplate.update(
                "UPDATE clinlims.analysis SET status_id = CAST(? AS numeric), released_date = now(),"
                        + " lastupdated = now() WHERE id = CAST(? AS numeric)",
                statusService.getStatusID(AnalysisStatus.Finalized), analysisId);
    }

    /**
     * What a correction saved on Results leaves behind: the new value, the analysis
     * back at technical acceptance, and the corrected flag.
     */
    private void correctAwaitingValidation(String analysisId, String value) {
        jdbcTemplate.update(
                "UPDATE clinlims.result SET value = ?, lastupdated = now() WHERE analysis_id = CAST(? AS" + " numeric)",
                value, analysisId);
        jdbcTemplate.update(
                "UPDATE clinlims.analysis SET status_id = CAST(? AS numeric), corrected = true, lastupdated = now()"
                        + " WHERE id = CAST(? AS numeric)",
                statusService.getStatusID(AnalysisStatus.TechnicalAcceptance), analysisId);
    }

    private void insertLegacyVersion(String issuedAt) {
        jdbcTemplate.update(
                "INSERT INTO clinlims.report_version (sample_id, report_type, version, amended,"
                        + " amended_count, reissue, released_count, total_count, issued_at, legacy, archive_failed)"
                        + " VALUES (?, 'PATIENT', 1, false, 0, false, 0, 0, CAST(? AS timestamp), true, false)",
                SAMPLE, issuedAt);
    }

    static Optional<String> none(List<String> values) {
        return values.isEmpty() ? Optional.empty() : Optional.of(values.get(0));
    }
}
