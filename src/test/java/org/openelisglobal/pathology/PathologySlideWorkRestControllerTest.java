package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import jakarta.persistence.OptimisticLockException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.controller.pathology.PathologySampleForm.PathologySlideForm;
import org.openelisglobal.program.controller.pathology.PathologySlideWorkRestController;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.openelisglobal.program.valueholder.pathology.PathologySlide;
import org.openelisglobal.program.valueholder.pathology.StainStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The endpoints that record what staining found for a case's slides and how
 * their coverslip check went. That anonymous calls are refused is pinned by the
 * security slice test beside the controller.
 *
 * <p>
 * A recorded change answers with the slides as they now stand, a refusal
 * carries the message id and its values the screen translates, an unknown case
 * or one the user may not read is a plain not-found, and a request that lost a
 * version check, the case's or a slide's, is a conflict the screen can explain.
 */
public class PathologySlideWorkRestControllerTest extends PathologyControllerTestSupport {

    /** technician1, the fixture's own user. */
    private static final String TECHNICIAN_ID = "1001";

    @Autowired
    private PathologySampleService pathologySampleService;

    private static final String STAIN_STATUS = "/rest/pathology/caseView/" + CASE_ID + "/slides/stain-status";
    private static final String COVERSLIP_QC = "/rest/pathology/caseView/" + CASE_ID + "/slides/coverslip-qc";

    @Test
    public void aStainStatus_answers200WithTheSlidesAsTheyNowStand() throws Exception {
        storeAt(PathologyStatus.STAINING);

        MockHttpServletResponse response = postAsTechnician(STAIN_STATUS, stainBody("UNSATISFACTORY"));

        assertEquals("the status is recorded", HttpStatus.OK.value(), response.getStatus());
        Map<String, Object> slide = onlySlide(response);
        assertEquals("the answer names the slide", Integer.valueOf(LEGACY_SLIDE_ID), slide.get("id"));
        assertEquals("with its status as the enum's name", "UNSATISFACTORY", slide.get("stainStatus"));
        assertNull("and no coverslip check", slide.get("coverslipQc"));
        assertEquals("the row holds it", "UNSATISFACTORY", stainStatus());
    }

    @Test
    public void aCoverslipCheck_answers200WithTheSlidesAsTheyNowStand() throws Exception {
        storeAt(PathologyStatus.COVERSLIPPING);
        PathologyCaseRows.storeStainStatus(jdbcTemplate, LEGACY_SLIDE_ID, StainStatus.STAINED);

        MockHttpServletResponse response = postAsTechnician(COVERSLIP_QC, qcBody("PASS"));

        assertEquals("the check is recorded", HttpStatus.OK.value(), response.getStatus());
        Map<String, Object> slide = onlySlide(response);
        assertEquals("STAINED", slide.get("stainStatus"));
        assertEquals("PASS", slide.get("coverslipQc"));
        assertEquals("the row holds it", "PASS", coverslipQc());
    }

    /**
     * The answer lists the slides in the order the case view lists them, under
     * their block and a slide with no block last, whatever order the request named
     * them in.
     */
    @Test
    public void slidesNamedInReverse_comeBackInTheCasesOrder() throws Exception {
        int cutFromABlock = slideCutFromTheLegacyBlock();
        storeAt(PathologyStatus.STAINING);

        MockHttpServletResponse response = postAsTechnician(STAIN_STATUS,
                "{\"slideIds\":[" + LEGACY_SLIDE_ID + "," + cutFromABlock + "],\"status\":\"UNSATISFACTORY\"}");

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        assertEquals("the slide under a block first, the one with no block last",
                List.of(cutFromABlock, LEGACY_SLIDE_ID), answeredIds(response));
    }

    @Test
    public void anUnknownCase_answers404() throws Exception {
        MockHttpServletResponse response = postAsTechnician("/rest/pathology/caseView/424242/slides/stain-status",
                stainBody("STAINED"));

        assertEquals("a case id nothing carries is not found", HttpStatus.NOT_FOUND.value(), response.getStatus());
    }

    @Test
    public void aCaseOutsideTheUsersUnits_answers404() throws Throwable {
        storeAt(PathologyStatus.STAINING);

        asAUserOutsideTheCaseUnits(() -> assertEquals("a case the user may not read is not found",
                HttpStatus.NOT_FOUND.value(), postAsTechnician(STAIN_STATUS, stainBody("UNSATISFACTORY")).getStatus()));
        assertNull("and its slide is left as it was", stainStatus());
    }

    /** A slide with no stain is not marked stained, and the refusal names it. */
    @Test
    public void aRefusal_answers400WithTheMessageIdAndItsValues() throws Exception {
        storeAt(PathologyStatus.STAINING);

        MockHttpServletResponse response = postAsTechnician(STAIN_STATUS, stainBody("STAINED"));

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
        Map<String, Object> answer = bodyOf(response);
        assertEquals("the screen translates the refusal from its message id", "pathology.error.stainMissing",
                answer.get("messageKey"));
        assertEquals("naming the slide as the bench reads it", Map.of("designations", "S" + LEGACY_SLIDE_ID),
                answer.get("messageArgs"));
        assertFalse("the body carries no status, which the screen's request helper overwrites",
                answer.containsKey("status"));
        assertNull(stainStatus());
    }

    /** A value the endpoint does not know is a request that names none. */
    @Test
    public void anUnknownStatus_answers400() throws Exception {
        storeAt(PathologyStatus.STAINING);

        MockHttpServletResponse response = postAsTechnician(STAIN_STATUS, stainBody("DONE"));

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
        assertEquals("pathology.error.slideSelectionRequired", bodyOf(response).get("messageKey"));
    }

    /**
     * A request that loses a version check, usually the case's, which every mark
     * raises, is a conflict the screen can explain, not a server error.
     */
    @Test
    public void aRequestThatLosesTheVersionCheck_answers409() throws Exception {
        storeAt(PathologyStatus.STAINING);
        Object controller = AopTestUtils
                .getUltimateTargetObject(securityContext.getBean(PathologySlideWorkRestController.class));
        PathologySampleService real = (PathologySampleService) ReflectionTestUtils.getField(controller,
                "pathologySampleService");
        PathologySampleService losing = Mockito.mock(PathologySampleService.class, AdditionalAnswers.delegatesTo(real));
        doThrow(new OptimisticLockException("version")).when(losing).recordStainStatus(any(), any(), any(), any());
        ReflectionTestUtils.setField(controller, "pathologySampleService", losing);
        try {
            MockHttpServletResponse response = postAsTechnician(STAIN_STATUS, stainBody("UNSATISFACTORY"));

            assertEquals("the request that lost the race is a conflict", HttpStatus.CONFLICT.value(),
                    response.getStatus());
            assertEquals("and the answer says what kind", "version", bodyOf(response).get("conflict"));
        } finally {
            ReflectionTestUtils.setField(controller, "pathologySampleService", real);
        }
        assertNull("the stored row is untouched", stainStatus());
    }

    /**
     * A lock the database would not grant, a deadlock it broke or a wait it gave up
     * on, is the same race seen from the other side, so it is the same conflict and
     * never a server error.
     */
    @Test
    public void aRequestThatLosesALock_answers409() throws Exception {
        storeAt(PathologyStatus.STAINING);
        Object controller = AopTestUtils
                .getUltimateTargetObject(securityContext.getBean(PathologySlideWorkRestController.class));
        PathologySampleService real = (PathologySampleService) ReflectionTestUtils.getField(controller,
                "pathologySampleService");
        PathologySampleService losing = Mockito.mock(PathologySampleService.class, AdditionalAnswers.delegatesTo(real));
        doThrow(new CannotAcquireLockException("deadlock", new SQLException("deadlock detected", "40P01"))).when(losing)
                .recordStainStatus(any(), any(), any(), any());
        ReflectionTestUtils.setField(controller, "pathologySampleService", losing);
        try {
            MockHttpServletResponse response = postAsTechnician(STAIN_STATUS, stainBody("UNSATISFACTORY"));

            assertEquals("a broken deadlock is a conflict", HttpStatus.CONFLICT.value(), response.getStatus());
            assertEquals("of the version kind", "version", bodyOf(response).get("conflict"));
        } finally {
            ReflectionTestUtils.setField(controller, "pathologySampleService", real);
        }
    }

    // helpers

    private static String stainBody(String status) {
        return "{\"slideIds\":[" + LEGACY_SLIDE_ID + "],\"status\":\"" + status + "\"}";
    }

    private static String qcBody(String result) {
        return "{\"slideIds\":[" + LEGACY_SLIDE_ID + "],\"result\":\"" + result + "\"}";
    }

    /**
     * Embeds the fixture's block and cuts one slide from it through the service,
     * the way the case view would, and answers the new slide's id.
     */
    private int slideCutFromTheLegacyBlock() {
        storeAt(PathologyStatus.EMBEDDING);
        pathologySampleService.embedBlock(LEGACY_BLOCK_ID, TECHNICIAN_ID);
        PathologySampleForm form = new PathologySampleForm();
        form.setGrossExam(pathologySampleService.get(CASE_ID).getGrossExam());
        form.setSystemUserId(TECHNICIAN_ID);
        form.setBlocks(new ArrayList<>());
        form.setReports(new ArrayList<>());
        PathologySlideForm slide = new PathologySlideForm();
        slide.setBlockId(LEGACY_BLOCK_ID);
        form.setSlides(List.of(slide));
        pathologySampleService.updateWithFormValues(CASE_ID, form);
        return pathologySampleService.getSlides(CASE_ID).stream().map(PathologySlide::getId)
                .filter(id -> id != LEGACY_SLIDE_ID).findFirst()
                .orElseThrow(() -> new AssertionError("setup: no slide was cut from the block"));
    }

    @SuppressWarnings("unchecked")
    private List<Integer> answeredIds(MockHttpServletResponse response) throws Exception {
        List<Map<String, Object>> slides = (List<Map<String, Object>>) bodyOf(response).get("slides");
        return slides.stream().map(slide -> (Integer) slide.get("id")).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> onlySlide(MockHttpServletResponse response) throws Exception {
        List<Map<String, Object>> slides = (List<Map<String, Object>>) bodyOf(response).get("slides");
        assertEquals("the answer carries the one slide named", 1, slides.size());
        return slides.get(0);
    }

    private String stainStatus() {
        return jdbcTemplate.queryForObject("SELECT stain_status FROM clinlims.pathology_slide WHERE id = ?",
                String.class, LEGACY_SLIDE_ID);
    }

    private String coverslipQc() {
        return jdbcTemplate.queryForObject("SELECT coverslip_qc FROM clinlims.pathology_slide WHERE id = ?",
                String.class, LEGACY_SLIDE_ID);
    }
}
