package org.openelisglobal.pathology;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.hibernate.ObjectNotFoundException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.program.controller.pathology.PathologySampleForm.PathologyReportForm;
import org.openelisglobal.program.service.InvalidClinicalAttachmentException;
import org.openelisglobal.program.service.PathologyReportConflictException;
import org.openelisglobal.program.service.PathologyReportService;
import org.openelisglobal.program.service.PathologyReportService.ReportFile;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.valueholder.pathology.PathologyReport.PathologyReportType;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.openelisglobal.program.valueholder.pathology.ReportView;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * A case's report versions are rows of their own: generated from the stored
 * case, uploaded from outside, withdrawn, and read back with a state worked out
 * from the case's other rows (design table R).
 */
public class PathologyReportServiceTest extends PathologyCaseTestSupport {

    /** pathologist1, so the actor differs from the signed-in technician. */
    private static final String ACTOR_ID = "1002";

    /**
     * The fixture's report row on case 1: on file from before versions, no file.
     */
    private static final int LEGACY_ROW_ID = 601;

    private static final int OTHER_CASE_ID = 2;

    @Autowired
    private PathologyReportService pathologyReportService;

    @Autowired
    private SystemUserService systemUserService;

    private StubReportRenderer renderer;

    private int nextEventId = 9660;

    @Before
    public void stubTheTemplate() {
        clearReportRows();
        renderer = StubReportRenderer.installIn(pathologyReportService);
    }

    @After
    public void restoreTheTemplate() {
        renderer.restore();
        clearReportRows();
        PathologyCaseRows.clearStageHistory(jdbcTemplate, OTHER_CASE_ID);
    }

    // R1
    @Test
    public void generating_isRefused_withoutEachFinding() {
        for (String column : List.of("gross_exam", "microscopy_exam")) {
            jdbcTemplate.update("UPDATE clinlims.pathology_sample SET " + column + " = '   ' WHERE id = ?", CASE_ID);
            assertRefusedWith("pathology.error.reportNeedsFindings", () -> generate());
            jdbcTemplate.update("UPDATE clinlims.pathology_sample SET " + column + " = 'Written' WHERE id = ?",
                    CASE_ID);
        }
        jdbcTemplate.update("UPDATE clinlims.pathology_conclusion SET value = '  ' WHERE pathology_sample_id = ?",
                CASE_ID);
        assertRefusedWith("pathology.error.reportNeedsFindings", () -> generate());

        assertTrue("a refused generate stores no row", filedRows(CASE_ID).isEmpty());
        assertTrue("and never reaches the template", renderer.amendedSeen.isEmpty());
    }

    // R2
    @Test
    public void generating_takesANarrativeAsTheConclusion() {
        ReportView narrativeOnly = generate();
        assertEquals("both descriptions and a narrative alone make a report", Integer.valueOf(1),
                narrativeOnly.version());

        jdbcTemplate.update("UPDATE clinlims.pathology_conclusion SET value = '' WHERE pathology_sample_id = ?",
                CASE_ID);
        jdbcTemplate.update("INSERT INTO clinlims.pathology_conclusion (id, pathology_sample_id, type, value,"
                + " last_updated) VALUES (9661, ?, 'DICTIONARY', '9102', now())", CASE_ID);
        assertEquals("and so does a coded conclusion beside a blank narrative", Integer.valueOf(2),
                generate().version());
    }

    // R3
    @Test
    public void generating_isNotGatedOnTheStage() {
        for (PathologyStatus stage : List.of(PathologyStatus.GROSSING, PathologyStatus.READY_PATHOLOGIST)) {
            storeAt(stage);
            assertEquals("a generate at " + stage + " stores a draft", "DRAFT", generate().type());
        }
    }

    // R4
    @Test
    public void theType_isDraftUntilTheCaseIsSignedOut_thenFinal() {
        storeAt(PathologyStatus.UNDER_REVIEW);
        assertEquals("under review", "DRAFT", generate().type());
        storeAt(PathologyStatus.COMPLETED);
        assertEquals("at Completed, never released", "DRAFT", generate().type());
        signOut(CASE_ID);
        assertEquals("signed out", "FINAL", generate().type());
        assertEquals("a first final is not asked for its corrected line", List.of(false, false, false),
                renderer.amendedSeen);
    }

    // R5
    @Test
    public void versions_countOverEveryRowOfTheCase() {
        assertEquals("the first row of a case is version 1", Integer.valueOf(1), generate().version());
        ReportView upload = upload(CASE_ID, png());
        generate();
        pathologyReportService.withdraw(CASE_ID, upload.id(), ACTOR_ID);
        upload(OTHER_CASE_ID, pdf());
        upload(OTHER_CASE_ID, pdf());

        assertEquals("after rows 1 to 3, one voided, a generate is version 4", Integer.valueOf(4),
                generate().version());
        assertEquals("and an upload takes the next, 5", Integer.valueOf(5), upload(CASE_ID, jpeg()).version());
        assertEquals("another case's rows count on their own", Integer.valueOf(3),
                upload(OTHER_CASE_ID, pdf()).version());
    }

    // R6, R21
    @Test
    public void states_areWorkedOut_newestFirst_withoutBytes() {
        storeAt(PathologyStatus.UNDER_REVIEW);
        generate();
        upload(CASE_ID, png());
        generate();
        signOut(CASE_ID);
        generate();
        pathologyReportService.voidFinalInForce(CASE_ID, ACTOR_ID);
        storeAt(PathologyStatus.UNDER_REVIEW);
        generate();

        List<ReportView> views = pathologyReportService.listFor(CASE_ID);
        assertEquals("newest first, each with its worked-out state; the legacy row without a file is not listed",
                List.of("5 DRAFT CURRENT", "4 FINAL VOIDED", "3 DRAFT SUPERSEDED", "2 EXTERNAL CURRENT",
                        "1 DRAFT SUPERSEDED"),
                views.stream().map(view -> view.version() + " " + view.type() + " " + view.state())
                        .collect(Collectors.toList()));
        String actorName = new TransactionTemplate(transactionManager)
                .execute(status -> systemUserService.get(ACTOR_ID).getDisplayName());
        assertTrue("each names who made it", views.stream().allMatch(view -> actorName.equals(view.generatedBy())));
        assertTrue("and when", views.stream().allMatch(view -> view.generatedAt() != null));
        assertTrue("a view never carries the file", Arrays.stream(ReportView.class.getRecordComponents())
                .noneMatch(component -> component.getType() == byte[].class));

        clearReportRows();
        signOut(CASE_ID);
        generate();
        generate();
        pathologyReportService.voidFinalInForce(CASE_ID, ACTOR_ID);
        assertEquals("a final older than a voided final is superseded, and nothing is current",
                List.of("2 FINAL VOIDED", "1 FINAL SUPERSEDED"),
                pathologyReportService.listFor(CASE_ID).stream()
                        .map(view -> view.version() + " " + view.type() + " " + view.state())
                        .collect(Collectors.toList()));
    }

    // R7
    @Test
    public void uploading_storesAnExternalVersion_forEachAllowedType() {
        Map<String, byte[]> files = Map.of("data:application/pdf", pdf(), "data:image/png", png(), "data:image/jpeg",
                jpeg());
        int version = 0;
        for (String type : List.of("data:application/pdf", "data:image/png", "data:image/jpeg")) {
            ReportView view = upload(CASE_ID, encoded(type, files.get(type)));
            version++;
            assertEquals(type + " is stored as an upload", "EXTERNAL", view.type());
            assertEquals("as the next version", Integer.valueOf(version), view.version());
            assertEquals("with its type", type, view.fileType());
            Map<String, Object> row = rowOf(view.id());
            assertArrayEquals("and its bytes", files.get(type), (byte[]) row.get("image"));
            assertEquals("by the actor", Integer.parseInt(ACTOR_ID), ((Number) row.get("generated_by")).intValue());
        }
    }

    // R8
    @Test
    public void uploading_isRefused_asTheAttachmentRuleRefuses() {
        byte[] text = "plain words, not a PDF".getBytes(StandardCharsets.UTF_8);
        byte[] tooLarge = new byte[11 * 1024 * 1024];
        System.arraycopy(pdf(), 0, tooLarge, 0, pdf().length);
        for (PathologyReportForm refused : List.of(encoded("data:application/pdf", text),
                encoded("data:application/pdf", tooLarge), new PathologyReportForm())) {
            assertThrows(InvalidClinicalAttachmentException.class,
                    () -> pathologyReportService.upload(CASE_ID, refused, ACTOR_ID));
        }
        assertTrue("a refused upload stores no row", filedRows(CASE_ID).isEmpty());
    }

    // R9
    @Test
    public void withdrawing_voidsAnUploadedRow_andKeepsIt() {
        ReportView upload = upload(CASE_ID, png());

        ReportView withdrawn = pathologyReportService.withdraw(CASE_ID, upload.id(), ACTOR_ID);

        assertEquals("the answer reads voided", ReportView.VOIDED, withdrawn.state());
        assertEquals("the row stays in the list, voided", List.of(upload.id() + " VOIDED"), pathologyReportService
                .listFor(CASE_ID).stream().map(view -> view.id() + " " + view.state()).collect(Collectors.toList()));
        assertEquals("with its file kept", Boolean.TRUE, rowOf(upload.id()).get("voided"));
        assertNotNullBytes(upload.id());
    }

    // R10
    @Test
    public void withdrawing_isRefusedForAGeneratedRow_andConflictsOnAVoidedOne() {
        ReportView draft = generate();
        signOut(CASE_ID);
        ReportView finalReport = generate();
        for (ReportView generated : List.of(draft, finalReport)) {
            assertRefusedWith("pathology.error.reportNotWithdrawable",
                    () -> pathologyReportService.withdraw(CASE_ID, generated.id(), ACTOR_ID));
            assertEquals("and the row is left as it was", Boolean.FALSE, rowOf(generated.id()).get("voided"));
        }
        ReportView upload = upload(CASE_ID, pdf());
        pathologyReportService.withdraw(CASE_ID, upload.id(), ACTOR_ID);

        assertThrows("a second withdrawal of the same upload is a conflict", PathologyReportConflictException.class,
                () -> pathologyReportService.withdraw(CASE_ID, upload.id(), ACTOR_ID));
    }

    // R11
    @Test
    public void theFile_isServedWithItsType_alsoWhenVoided() {
        ReportView image = upload(CASE_ID, png());
        ReportView generated = generate();

        ReportFile png = pathologyReportService.fileOf(CASE_ID, image.id());
        assertArrayEquals("the stored bytes", png(), png.bytes());
        assertEquals("served as the stored type without its data: prefix", "image/png", png.mediaType());
        assertEquals("named by lab number and version", accessionNumber + "-v1.png", png.fileName());
        ReportFile pdf = pathologyReportService.fileOf(CASE_ID, generated.id());
        assertEquals("application/pdf", pdf.mediaType());
        assertEquals(accessionNumber + "-v2.pdf", pdf.fileName());

        pathologyReportService.withdraw(CASE_ID, image.id(), ACTOR_ID);
        assertArrayEquals("a voided row still opens", png(),
                pathologyReportService.fileOf(CASE_ID, image.id()).bytes());
    }

    // R12
    @Test
    public void aRowOfAnotherCase_noRow_orAnOddStoredType_isNotFound() {
        ReportView otherCases = upload(OTHER_CASE_ID, pdf());
        jdbcTemplate.update("INSERT INTO clinlims.pathology_report (id, pathology_sample_id, image, file_type,"
                + " report_type, version_number, voided, last_updated) VALUES (9662, ?, ?, 'data:application/msword',"
                + " 'EXTERNAL', 7, false, now())", CASE_ID, "old document".getBytes(StandardCharsets.UTF_8));

        for (int reportId : List.of(otherCases.id(), 999999, 9662, LEGACY_ROW_ID)) {
            assertThrows("row " + reportId + " is not found as a file", ObjectNotFoundException.class,
                    () -> pathologyReportService.fileOf(CASE_ID, reportId));
            assertThrows("nor withdrawn", ObjectNotFoundException.class,
                    () -> pathologyReportService.withdraw(CASE_ID, reportId, ACTOR_ID));
        }
        assertEquals("the other case's row is left as it was", Boolean.FALSE, rowOf(otherCases.id()).get("voided"));
    }

    // R14
    @Test
    public void twoGeneratesAtOnce_takeDifferentNumbers() {
        Outcome outcome = holdOpenWhile(() -> generate(), () -> generate());

        assertNull("the first generate is stored", outcome.held());
        assertNull("the second waits for the first, then is stored too", outcome.other());
        assertEquals("with different numbers", List.of(2, 1), versionsOf(CASE_ID));

        // The unique index is the last guard: a number taken while the generate was
        // rendering answers as a conflict, never as a duplicate.
        renderer.during = () -> jdbcTemplate.update("INSERT INTO clinlims.pathology_report (id, pathology_sample_id,"
                + " image, file_type, report_type, version_number, voided, last_updated) VALUES (9663, ?, ?,"
                + " 'data:application/pdf', 'DRAFT', 3, false, now())", CASE_ID, StubReportRenderer.BODY);
        assertThrows(PathologyReportConflictException.class, () -> generate());
        assertEquals("no number is stored twice", Set.copyOf(versionsOf(CASE_ID)).size(), versionsOf(CASE_ID).size());
    }

    // R17, and R9's verb
    @Test
    public void generatingAndUploading_areAudited_onTheReportRow() {
        ReportView generated = generate();
        ReportView uploaded = upload(CASE_ID, png());
        pathologyReportService.withdraw(CASE_ID, uploaded.id(), ACTOR_ID);

        assertEquals("a generate names its version and type",
                List.of(Map.of("verb", "PATHOLOGY_REPORT_GENERATED", "version", 1, "type", "DRAFT")),
                payloads(generated.id()));
        assertEquals("an upload its version and type, then its withdrawal its version",
                List.of(Map.of("verb", "PATHOLOGY_REPORT_UPLOADED", "version", 2, "fileType", "data:image/png"),
                        Map.of("verb", "PATHOLOGY_REPORT_WITHDRAWN", "version", 2)),
                payloads(uploaded.id()));
        assertTrue("each by the actor", entries(generated.id(), uploaded.id()).stream()
                .allMatch(history -> ACTOR_ID.equals(history.getSysUserId())));
    }

    // R8, R23
    @Test
    public void anUpload_needsAFile_andCannotNameItsOwnVersionTypeFlagOrAuthor() {
        PathologyReportForm noFile = new PathologyReportForm();
        noFile.setFileType("data:application/pdf");
        noFile.setBase64Image("   ");
        for (PathologyReportForm empty : List.of(new PathologyReportForm(), noFile)) {
            assertThrows("a body with no file is refused", InvalidClinicalAttachmentException.class,
                    () -> pathologyReportService.upload(CASE_ID, empty, ACTOR_ID));
        }
        assertTrue("and stores nothing", filedRows(CASE_ID).isEmpty());

        PathologyReportForm naming = encoded("data:application/pdf", pdf());
        naming.setId(LEGACY_ROW_ID);
        naming.setPathologySampleId(OTHER_CASE_ID);
        naming.setVersionNumber(42);
        naming.setReportType(PathologyReportType.FINAL);
        naming.setVoided(true);
        naming.setGeneratedAt(Timestamp.valueOf("2020-01-01 00:00:00"));
        naming.setGeneratedBy(
                new TransactionTemplate(transactionManager).execute(status -> systemUserService.get("1003")));
        ReportView stored = pathologyReportService.upload(CASE_ID, naming, ACTOR_ID);

        assertNotEquals("a new row is built", Integer.valueOf(LEGACY_ROW_ID), stored.id());
        Map<String, Object> row = rowOf(stored.id());
        assertEquals("on the case of the address", CASE_ID, ((Number) row.get("pathology_sample_id")).intValue());
        assertEquals("as an upload", "EXTERNAL", row.get("report_type"));
        assertEquals("with the next version", 1, ((Number) row.get("version_number")).intValue());
        assertEquals("not voided", Boolean.FALSE, row.get("voided"));
        assertEquals("by the actor", Integer.parseInt(ACTOR_ID), ((Number) row.get("generated_by")).intValue());
        assertFalse("dated now",
                ((Timestamp) row.get("generated_at")).before(Timestamp.valueOf("2021-01-01 00:00:00")));
        assertNull("and the legacy row is left without a file", rowOf(LEGACY_ROW_ID).get("image"));
    }

    // R22
    @Test
    public void aGenerateStillRendering_holdsAReopen_andItsRowIsVoided() throws Exception {
        signOut(CASE_ID);
        renderer.release = new CountDownLatch(1);
        ExecutorService pool = new DelegatingSecurityContextExecutorService(Executors.newFixedThreadPool(2));
        try {
            Future<ReportView> generating = pool.submit(() -> generate());
            assertTrue("setup: the generate is rendering", renderer.rendering.await(30, TimeUnit.SECONDS));
            Future<Optional<Integer>> reopening = pool.submit(() -> {
                pathologySampleService.reopen(CASE_ID, PathologyStatus.COMPLETED, "Amended diagnosis", ACTOR_ID);
                return voidedByTheReopen();
            });
            try {
                reopening.get(3, TimeUnit.SECONDS);
                fail("the reopen went ahead while the generate was still rendering");
            } catch (TimeoutException expected) {
                // Waiting on the case's row the generate holds.
            }
            renderer.release.countDown();
            ReportView issued = generating.get(30, TimeUnit.SECONDS);

            assertEquals("setup: the generate issued a final", "FINAL", issued.type());
            assertEquals("the reopen voids the final the generate stored", Optional.of(issued.id()),
                    reopening.get(30, TimeUnit.SECONDS));
            assertEquals("so no final is in force on the reopened case", Integer.valueOf(0),
                    jdbcTemplate.queryForObject(
                            "SELECT count(*) FROM clinlims.pathology_report WHERE"
                                    + " pathology_sample_id = ? AND report_type = 'FINAL' AND NOT voided",
                            Integer.class, CASE_ID));
        } finally {
            renderer.release.countDown();
            pool.shutdownNow();
        }
    }

    // helpers

    /** The report the reopen's own entry names as voided. */
    private Optional<Integer> voidedByTheReopen() {
        return caseAudit().stream().map(this::payloadOf)
                .filter(payload -> PathologySampleService.PATHOLOGY_CASE_REOPENED.equals(payload.get("verb")))
                .map(payload -> (Integer) payload.get("voidedReportId")).filter(id -> id != null).findFirst();
    }

    private ReportView generate() {
        return pathologyReportService.generate(CASE_ID, ACTOR_ID, StubReportRenderer.PATHS);
    }

    private ReportView upload(int caseId, byte[] bytes) {
        return upload(caseId, encoded(typeOf(bytes), bytes));
    }

    private ReportView upload(int caseId, PathologyReportForm form) {
        return pathologyReportService.upload(caseId, form, ACTOR_ID);
    }

    /**
     * Signed out as the record reads it: at Completed, entered forward. The case
     * save's own release is another task's subject.
     */
    private void signOut(int caseId) {
        PathologyCaseRows.storeAt(jdbcTemplate, caseId, PathologyStatus.COMPLETED);
        jdbcTemplate.update("INSERT INTO clinlims.pathology_stage_event (id, pathology_sample_id, stage, entered_at,"
                + " entered_by, direction, last_updated) VALUES (?, ?, 'COMPLETED', now(), 1001, 'FORWARD', now())",
                nextEventId++, caseId);
    }

    static PathologyReportForm encoded(String type, byte[] bytes) {
        PathologyReportForm form = new PathologyReportForm();
        form.setBase64Image(type + ";base64," + Base64.getEncoder().encodeToString(bytes));
        return form;
    }

    private static String typeOf(byte[] bytes) {
        if (bytes[0] == (byte) 137) {
            return "data:image/png";
        }
        return bytes[0] == (byte) 255 ? "data:image/jpeg" : "data:application/pdf";
    }

    static byte[] pdf() {
        return "%PDF-1.4 an outside report".getBytes(StandardCharsets.US_ASCII);
    }

    static byte[] png() {
        return new byte[] { (byte) 137, 80, 78, 71, 13, 10, 26, 10, 1, 2, 3 };
    }

    static byte[] jpeg() {
        return new byte[] { (byte) 255, (byte) 216, (byte) 255, 4, 5, 6 };
    }

    private Map<String, Object> rowOf(int reportId) {
        return jdbcTemplate.queryForMap("SELECT * FROM clinlims.pathology_report WHERE id = ?", reportId);
    }

    private void assertNotNullBytes(int reportId) {
        assertTrue("the row keeps its file", rowOf(reportId).get("image") != null);
    }

    private List<Map<String, Object>> filedRows(int caseId) {
        return jdbcTemplate.queryForList(
                "SELECT id FROM clinlims.pathology_report WHERE pathology_sample_id = ? AND image IS NOT NULL", caseId);
    }

    private List<Integer> versionsOf(int caseId) {
        return jdbcTemplate.queryForList(
                "SELECT version_number FROM clinlims.pathology_report WHERE"
                        + " pathology_sample_id = ? AND version_number IS NOT NULL ORDER BY version_number DESC",
                Integer.class, caseId);
    }

    private List<History> entries(Integer... reportIds) {
        String referenceTable = referenceTablesService.getReferenceTableByName("PATHOLOGY_REPORT").getId();
        List<History> entries = new ArrayList<>();
        for (Integer reportId : reportIds) {
            entries.addAll(historyService.getHistoryByRefIdAndRefTableId(String.valueOf(reportId), referenceTable));
        }
        entries.sort((first, second) -> Long.compare(Long.parseLong(first.getId()), Long.parseLong(second.getId())));
        return entries;
    }

    private List<Map<String, Object>> payloads(int reportId) {
        return entries(reportId).stream().map(this::payloadOf).collect(Collectors.toList());
    }

    /**
     * Every row this class wrote, and their audit entries; the fixture's row stays.
     */
    private void clearReportRows() {
        jdbcTemplate.update("DELETE FROM clinlims.history WHERE reference_table ="
                + " (SELECT id FROM clinlims.reference_tables WHERE name = 'PATHOLOGY_REPORT')");
        jdbcTemplate.update("DELETE FROM clinlims.pathology_report WHERE pathology_sample_id IN (?, ?) AND id <> ?",
                CASE_ID, OTHER_CASE_ID, LEGACY_ROW_ID);
        jdbcTemplate.update("DELETE FROM clinlims.pathology_conclusion WHERE id = 9661");
    }
}
