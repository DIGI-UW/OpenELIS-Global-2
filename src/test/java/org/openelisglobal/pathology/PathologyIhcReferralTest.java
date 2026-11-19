package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.exception.LocalizedValidationException;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.service.PathologySampleService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Referring a case to immunohistochemistry from the case save (FR-13.4): the
 * first referral creates the case there and orders its markers, later saves
 * order only markers the order does not already hold, and nothing is referred
 * outside the reading window.
 *
 * <p>
 * The order's own analysis is analysis 2, and tests 1 and 2 stand for two
 * markers.
 */
public class PathologyIhcReferralTest extends BaseWebContextSensitiveTest {

    private static final int CASE_ID = 1;
    private static final int FIXTURE_IHC_CASE_ID = 2;
    private static final int CASE_ANALYSIS_ID = 2;
    private static final String FIRST_MARKER = "1";
    private static final String SECOND_MARKER = "2";

    /** This class's own rows, outside the ids the fixture uses. */
    private static final int REFERRAL_SECTION_ID = 9511;
    private static final int HELD_MARKER_ANALYSIS_ID = 9512;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private PathologySampleService pathologySampleService;

    private StageSwitches switches;

    @Before
    public void init() throws Exception {
        removeOwnRows();
        executeDataSetWithStateManagement("testdata/program-case-release.xml");
        moveTheProgramSampleSequencePastTheFixture();
        authenticateAs("technician1");
        switches = StageSwitches.allOn();
        addImmunohistochemistrySection();
        storeAt("UNDER_REVIEW");
    }

    @After
    public void cleanUp() {
        removeOwnRows();
        switches.close();
    }

    @Test
    public void noTick_orAnUntick_changesNothing() {
        removeTheFixtureReferral();
        save(referral(false, List.of(FIRST_MARKER)));
        assertEquals("no tick creates no case", 0, referralCases().size());
        assertTrue("and orders nothing", markerRows().isEmpty());

        restoreTheFixtureReferral();
        save(referral(false, List.of()));
        assertEquals("an untick leaves the referral where it is", List.of(FIXTURE_IHC_CASE_ID), referralCases());
        assertTrue("and orders nothing", markerRows().isEmpty());
        assertTrue("neither save is audited as a referral", referralAudit().isEmpty());
    }

    @Test
    public void aReferral_createsTheIhcCase_ordersItsMarkers_andIsAudited() {
        removeTheFixtureReferral();

        save(referral(true, List.of(FIRST_MARKER, SECOND_MARKER)));

        List<Integer> created = referralCases();
        assertEquals("the case is created there", 1, created.size());
        assertEquals("marked referred", Boolean.TRUE,
                jdbcTemplate.queryForObject("SELECT reffered FROM clinlims.immunohistochemistry_sample WHERE id = ?",
                        Boolean.class, created.get(0)));
        assertEquals("each marker ordered once, under the case's own analysis",
                List.of(List.of(FIRST_MARKER, CASE_ANALYSIS_ID), List.of(SECOND_MARKER, CASE_ANALYSIS_ID)),
                markerRows());
        Map<String, Object> audit = referralAudit().get(0);
        assertEquals("audited with the new case", created.get(0), audit.get("ihcCaseId"));
        assertEquals("the markers ordered", List.of(FIRST_MARKER, SECOND_MARKER), audit.get("testIds"));
        assertEquals("and that it was created", true, audit.get("created"));

        removeOwnReferralRows();
        clearCaseAudit();
        PathologySampleForm noMarkers = referral(true, null);
        save(noMarkers);
        assertEquals("a referral without markers still creates the case", 1, referralCases().size());
        assertTrue("and orders nothing", markerRows().isEmpty());
        Map<String, Object> empty = referralAudit().get(0);
        assertEquals("audited as created", true, empty.get("created"));
        assertEquals("with no marker", List.of(), empty.get("testIds"));
    }

    @Test
    public void aReferralOrNewMarkersOutsideTheReading_areRefused() {
        removeTheFixtureReferral();
        storeAt("READY_PATHOLOGIST");
        assertRefusedWith("pathology.error.findingsLocked", referral(true, List.of(FIRST_MARKER)));
        assertEquals("no case is created", 0, referralCases().size());

        restoreTheFixtureReferral();
        storeAt("GROSSING");
        assertRefusedWith("pathology.error.findingsLocked", referral(true, List.of(SECOND_MARKER)));
        assertTrue("and no marker is ordered", markerRows().isEmpty());
    }

    @Test
    public void markersAlreadyOrdered_areNotOrderedAgain_andNewOnesAre() {
        holdFirstMarker();

        save(referral(true, List.of(FIRST_MARKER)));
        assertEquals("a marker the order holds is not ordered again", List.of(List.of(FIRST_MARKER, CASE_ANALYSIS_ID)),
                markerRows());
        assertTrue("and nothing is audited", referralAudit().isEmpty());

        save(referral(true, List.of(FIRST_MARKER, SECOND_MARKER)));
        assertEquals("only the new marker is ordered, under the case's own analysis, not the earlier marker",
                List.of(List.of(FIRST_MARKER, CASE_ANALYSIS_ID), List.of(SECOND_MARKER, CASE_ANALYSIS_ID)),
                markerRows());
        Map<String, Object> audit = referralAudit().get(0);
        assertEquals("audited on the existing case", FIXTURE_IHC_CASE_ID, audit.get("ihcCaseId"));
        assertEquals("naming the new marker only", List.of(SECOND_MARKER), audit.get("testIds"));
        assertEquals("not created", false, audit.get("created"));

        clearCaseAudit();
        storeAt("GROSSING");
        save(referral(true, List.of()));
        assertEquals("a referred case saved on the bench with no marker is taken and orders nothing", 2,
                markerRows().size());
        assertTrue("nor audits", referralAudit().isEmpty());
    }

    @Test
    public void theCaseRead_listsTheMarkersAlreadyReferred() {
        save(referral(true, List.of(SECOND_MARKER)));
        assertEquals("after a referral with one marker the read lists that test", List.of(SECOND_MARKER),
                pathologySampleService.ihcReferredTestIds(pathologySampleService.get(CASE_ID)));

        removeOwnReferralRows();
        assertEquals("an order that holds no referral lists no marker", List.of(),
                pathologySampleService.ihcReferredTestIds(pathologySampleService.get(CASE_ID)));
    }

    // helpers

    private void save(PathologySampleForm form) {
        pathologySampleService.updateWithFormValues(CASE_ID, form);
    }

    private PathologySampleForm referral(boolean ticked, List<String> testIds) {
        PathologySampleForm form = new PathologySampleForm();
        form.setSystemUserId("1001");
        form.setBlocks(new ArrayList<>());
        form.setSlides(new ArrayList<>());
        form.setReports(new ArrayList<>());
        form.setReferToImmunoHistoChemistry(ticked);
        form.setImmunoHistoChemistryTestIds(testIds);
        return form;
    }

    private void assertRefusedWith(String errorCode, PathologySampleForm form) {
        LocalizedValidationException refusal = org.junit.Assert.assertThrows(LocalizedValidationException.class,
                () -> save(form));
        assertEquals(errorCode, refusal.getErrorCode());
    }

    private void storeAt(String stage) {
        jdbcTemplate.update("UPDATE clinlims.pathology_sample SET status = ? WHERE id = ?", stage, CASE_ID);
    }

    private List<Integer> referralCases() {
        return jdbcTemplate.queryForList(
                "SELECT id FROM clinlims.immunohistochemistry_sample WHERE pathology_sample_id = ? ORDER BY id",
                Integer.class, CASE_ID);
    }

    /**
     * Each marker the order holds as a referral, by test: its test and its parent.
     */
    private List<List<Object>> markerRows() {
        return jdbcTemplate
                .queryForList("SELECT test_id, parent_analysis_id FROM clinlims.analysis WHERE test_sect_id = ?"
                        + " ORDER BY test_id", REFERRAL_SECTION_ID)
                .stream().map(row -> List.<Object>of(String.valueOf(((Number) row.get("test_id")).intValue()),
                        ((Number) row.get("parent_analysis_id")).intValue()))
                .collect(Collectors.toList());
    }

    private void removeTheFixtureReferral() {
        jdbcTemplate.update("DELETE FROM clinlims.immunohistochemistry_sample WHERE id = ?", FIXTURE_IHC_CASE_ID);
    }

    private void restoreTheFixtureReferral() {
        jdbcTemplate.update("DELETE FROM clinlims.immunohistochemistry_sample WHERE pathology_sample_id = ?", CASE_ID);
        jdbcTemplate.update("INSERT INTO clinlims.immunohistochemistry_sample (id, last_updated, program_id,"
                + " sample_id, technician_id, pathologist_id, status, pathology_sample_id)"
                + " VALUES (?, now(), 1, 1, 1001, 1002, 'IN_PROGRESS', ?)", FIXTURE_IHC_CASE_ID, CASE_ID);
    }

    /**
     * The three program case tables share one sequence and one identifier space
     * (table per class), and the fixture writes ids 1 to 3 without moving it. Left
     * behind, a new immunohistochemistry case takes id 1, which the pathology case
     * being saved already holds in the session.
     */
    private void moveTheProgramSampleSequencePastTheFixture() {
        for (String table : List.of("pathology_sample", "immunohistochemistry_sample", "cytology_sample")) {
            resyncSequence("clinlims.program_sample_seq", "clinlims." + table);
        }
    }

    private void addImmunohistochemistrySection() {
        jdbcTemplate.update("INSERT INTO clinlims.test_section (id, name, description, org_id, is_external,"
                + " lastupdated, sort_order, name_localization_id, display_key, domain)"
                + " SELECT ?, 'Immunohistochemistry', 'Referral section', org_id, is_external, now(), sort_order,"
                + " name_localization_id, 'ReferralSectionKey', domain FROM clinlims.test_section WHERE id = 2",
                REFERRAL_SECTION_ID);
    }

    /** The first marker on the order already, as an earlier referral wrote it. */
    private void holdFirstMarker() {
        jdbcTemplate.update(
                "INSERT INTO clinlims.analysis (id, sampitem_id, test_sect_id, test_id, revision,"
                        + " status_id, analysis_type, lastupdated, started_date, type_of_sample_name,"
                        + " fhir_uuid, parent_analysis_id, referred_out, corrected, result_calculated, reflex_trigger,"
                        + " method_id) SELECT ?, sampitem_id, ?, ?, revision, status_id, analysis_type, now(),"
                        + " started_date, type_of_sample_name, gen_random_uuid(), id, false, corrected,"
                        + " result_calculated, reflex_trigger, method_id FROM clinlims.analysis WHERE id = ?",
                HELD_MARKER_ANALYSIS_ID, REFERRAL_SECTION_ID, Integer.parseInt(FIRST_MARKER), CASE_ANALYSIS_ID);
    }

    private List<Map<String, Object>> referralAudit() {
        return jdbcTemplate
                .queryForList("SELECT changes FROM clinlims.history WHERE reference_id = ? AND reference_table ="
                        + " (SELECT id FROM clinlims.reference_tables WHERE name = 'PATHOLOGY_SAMPLE') ORDER BY id",
                        CASE_ID)
                .stream().map(row -> textOf(row.get("changes"))).filter(changes -> changes.startsWith("{"))
                .map(this::parse)
                .filter(payload -> PathologySampleService.PATHOLOGY_IHC_REFERRED.equals(payload.get("verb")))
                .collect(Collectors.toList());
    }

    private static String textOf(Object changes) {
        return changes instanceof byte[] bytes ? new String(bytes, StandardCharsets.UTF_8) : String.valueOf(changes);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parse(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void clearCaseAudit() {
        jdbcTemplate.update("DELETE FROM clinlims.history WHERE reference_id = ? AND reference_table ="
                + " (SELECT id FROM clinlims.reference_tables WHERE name = 'PATHOLOGY_SAMPLE')", CASE_ID);
    }

    /**
     * The markers and cases a referral wrote, so a second referral starts afresh.
     */
    private void removeOwnReferralRows() {
        String markers = "SELECT id FROM clinlims.analysis WHERE test_sect_id = " + REFERRAL_SECTION_ID;
        jdbcTemplate.update("DELETE FROM clinlims.result WHERE analysis_id IN (" + markers + ")");
        jdbcTemplate.update("DELETE FROM clinlims.note WHERE reference_table ="
                + " (SELECT id FROM clinlims.reference_tables WHERE name = 'ANALYSIS') AND reference_id IN (" + markers
                + ")");
        jdbcTemplate.update("DELETE FROM clinlims.analysis WHERE test_sect_id = ?", REFERRAL_SECTION_ID);
        jdbcTemplate.update("DELETE FROM clinlims.immunohistochemistry_sample WHERE pathology_sample_id = ?", CASE_ID);
    }

    /**
     * The markers, the referral section, and the case's stage entries and audit
     * entries, which outlive a fixture reload.
     */
    private void removeOwnRows() {
        removeOwnReferralRows();
        jdbcTemplate.update("DELETE FROM clinlims.test_section WHERE id = ?", REFERRAL_SECTION_ID);
        jdbcTemplate.update("DELETE FROM clinlims.pathology_stage_event WHERE pathology_sample_id = ?", CASE_ID);
        clearCaseAudit();
    }
}
