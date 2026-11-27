package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import jakarta.persistence.OptimisticLockException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.openelisglobal.program.controller.pathology.PathologyConsultationRestController;
import org.openelisglobal.program.dao.PathologyConsultationDAO;
import org.openelisglobal.program.service.PathologyConsultationService;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * The endpoints that ask for a second opinion, record its outcome and withdraw
 * it (FR-11). That anonymous calls are refused, and that the user's lab units
 * are checked before anything is written, is pinned by the security slice test
 * beside the controller; the rules themselves by the service tests.
 *
 * <p>
 * Each answers the second opinion's id and status only, since the screen reads
 * the case again; a refusal carries the message id and its values; a second
 * opinion no longer awaited is a consultation conflict naming where it stands,
 * and a lost race a version conflict. The signed-in user is technician1 (1001).
 */
public class PathologyConsultationRestControllerTest extends PathologyControllerTestSupport {

    private static final String BASE = "/rest/pathology/caseView/" + CASE_ID + "/consultations";

    private static final String ASK_CONSULTED = "{\"consultedUserId\":\"1003\",\"reasonId\":\"9401\","
            + "\"preTreatment\":true}";

    private PathologyConsultationFixture fixture;

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class ConsultationConfig {

        /**
         * Built against the parent's own services, as the other pathology endpoints
         * are, so what a call leaves behind is a real row and a real audit entry.
         */
        @Bean
        PathologyConsultationRestController pathologyConsultationRestController(ApplicationContext context) {
            return context.getParent().getAutowireCapableBeanFactory()
                    .createBean(PathologyConsultationRestController.class);
        }

        /** The rule the application's chain applies to /rest/**, as a status. */
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .httpBasic(Customizer.withDefaults());
            return http.build();
        }
    }

    @Before
    public void mountTheConsultationEndpoints() {
        securityContext.close();
        securityContext = new AnnotationConfigWebApplicationContext();
        securityContext.setParent(webApplicationContext);
        securityContext.setServletContext(webApplicationContext.getServletContext());
        securityContext.register(ConsultationConfig.class);
        securityContext.refresh();
        mockMvc = MockMvcBuilders.webAppContextSetup(securityContext).apply(springSecurity()).build();

        fixture = PathologyConsultationFixture.seed(jdbcTemplate);
        storeAt(PathologyStatus.UNDER_REVIEW);
    }

    @After
    public void removeThePeopleAndReasons() {
        fixture.close();
    }

    @Test
    public void aRequest_answersItsIdAndStatus() throws Exception {
        MockHttpServletResponse response = postAsTechnician(BASE, ASK_CONSULTED);

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        Map<String, Object> answer = bodyOf(response);
        assertEquals("the id and the status, nothing more", 2, answer.size());
        assertEquals("REQUESTED", answer.get("status"));
        Map<String, Object> row = row(((Number) answer.get("id")).intValue());
        assertEquals("on this case", CASE_ID, ((Number) row.get("pathology_sample_id")).intValue());
        assertEquals("asked by the signed-in user", 1001, ((Number) row.get("requested_by")).intValue());
        assertEquals("of the colleague posted", 1003, ((Number) row.get("consulted_user_id")).intValue());
        assertEquals("with the reason posted", 9401, ((Number) row.get("reason_dictionary_id")).intValue());
    }

    @Test
    public void anAnswer_andAWithdrawal_answerTheNewStatus() throws Exception {
        int toAnswer = insertRow(1001, "REQUESTED");
        int toWithdraw = insertRow(1003, "REQUESTED");

        MockHttpServletResponse answered = postAsTechnician(answerOf(toAnswer),
                "{\"outcome\":\"DISAGREE\",\"note\":\"Reactive\"}");
        MockHttpServletResponse withdrawn = postAsTechnician(withdrawalOf(toWithdraw), "{}");

        assertEquals(HttpStatus.OK.value(), answered.getStatus());
        assertEquals(Map.of("id", toAnswer, "status", "ANSWERED"), bodyOf(answered));
        assertEquals("DISAGREE", row(toAnswer).get("outcome"));
        assertEquals(HttpStatus.OK.value(), withdrawn.getStatus());
        assertEquals(Map.of("id", toWithdraw, "status", "WITHDRAWN"), bodyOf(withdrawn));
        assertEquals("WITHDRAWN", row(toWithdraw).get("status"));
    }

    @Test
    public void aRefusal_isAnsweredWithItsMessageKeyAndArguments() throws Exception {
        int awaited = insertRow(1003, "REQUESTED");

        MockHttpServletResponse notYours = postAsTechnician(answerOf(awaited), "{\"outcome\":\"AGREE\"}");
        MockHttpServletResponse alreadyOpen = postAsTechnician(BASE, ASK_CONSULTED);

        assertEquals(HttpStatus.BAD_REQUEST.value(), notYours.getStatus());
        assertEquals("pathology.error.consultationNotYours", bodyOf(notYours).get("messageKey"));
        assertEquals(Map.of("name", PathologyConsultationFixture.CONSULTED_NAME), bodyOf(notYours).get("messageArgs"));
        assertEquals(HttpStatus.BAD_REQUEST.value(), alreadyOpen.getStatus());
        assertEquals("pathology.error.consultationAlreadyOpen", bodyOf(alreadyOpen).get("messageKey"));
        assertEquals(Map.of("name", PathologyConsultationFixture.CONSULTED_NAME),
                bodyOf(alreadyOpen).get("messageArgs"));
        assertEquals("REQUESTED", row(awaited).get("status"));
    }

    @Test
    public void aRowNoLongerAwaited_is409_namingItsStatus() throws Exception {
        int answered = insertRow(1001, "ANSWERED");

        for (MockHttpServletResponse response : List.of(postAsTechnician(answerOf(answered), "{\"outcome\":\"AGREE\"}"),
                postAsTechnician(withdrawalOf(answered), null))) {
            assertEquals(HttpStatus.CONFLICT.value(), response.getStatus());
            Map<String, Object> body = bodyOf(response);
            assertEquals("consultation", body.get("conflict"));
            assertEquals("ANSWERED", body.get("status"));
            assertTrue("with a sentence for the log", body.get("error") instanceof String);
        }
        assertEquals("AGREE", row(answered).get("outcome"));
    }

    @Test
    public void aLostRace_byVersionOrByTheAwaitedIndex_is409Version() throws Exception {
        for (RuntimeException lost : List.of(new OptimisticLockException("version"),
                new DataIntegrityViolationException(
                        "duplicate key value violates unique constraint" + " \"pathology_consultation_awaited_uk\""))) {
            MockHttpServletResponse response = withALosingService(
                    losing -> doThrow(lost).when(losing).request(any(), any(), any()),
                    () -> postAsTechnician(BASE, ASK_CONSULTED));
            assertEquals(HttpStatus.CONFLICT.value(), response.getStatus());
            assertEquals("version", bodyOf(response).get("conflict"));
        }
        assertEquals("nothing stored", 0, consultationCount());
    }

    @Test
    public void aReasonOfAnotherList_is400WithASentence_andNothingStored() throws Exception {
        MockHttpServletResponse response = postAsTechnician(BASE, "{\"consultedUserId\":\"1003\",\"reasonId\":\""
                + PathologyFindingsDictionary.ADENOCARCINOMA + "\",\"preTreatment\":true}");

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
        assertTrue("with a sentence for the log", bodyOf(response).get("error") instanceof String);
        assertEquals("nothing stored", 0, consultationCount());
    }

    /**
     * The real race: the awaited row is stored, the service reads the case as if it
     * were not, and the unique index refuses the second insert.
     */
    @Test
    public void aLostRace_atTheAwaitedIndexItself_is409Version_andOneRowStays() throws Exception {
        int awaited = insertRow(1003, "REQUESTED");

        MockHttpServletResponse response = withADaoBlindToAwaitedRows(() -> postAsTechnician(BASE, ASK_CONSULTED));

        assertEquals(HttpStatus.CONFLICT.value(), response.getStatus());
        assertEquals("version", bodyOf(response).get("conflict"));
        assertEquals("one row", 1, consultationCount());
        assertEquals("REQUESTED", row(awaited).get("status"));
    }

    @Test
    public void anUnknownCase_is404() throws Exception {
        String unknown = "/rest/pathology/caseView/424242/consultations";

        assertEquals(HttpStatus.NOT_FOUND.value(), postAsTechnician(unknown, ASK_CONSULTED).getStatus());
        assertEquals(HttpStatus.NOT_FOUND.value(),
                postAsTechnician(unknown + "/1/answer", "{\"outcome\":\"AGREE\"}").getStatus());
        assertEquals(HttpStatus.NOT_FOUND.value(), postAsTechnician(unknown + "/1/withdraw", null).getStatus());
    }

    // helpers

    private interface Call {
        MockHttpServletResponse run() throws Exception;
    }

    private interface Stub {
        void on(PathologyConsultationService losing);
    }

    private MockHttpServletResponse withALosingService(Stub stub, Call call) throws Exception {
        Object controller = AopTestUtils
                .getUltimateTargetObject(securityContext.getBean(PathologyConsultationRestController.class));
        PathologyConsultationService real = (PathologyConsultationService) ReflectionTestUtils.getField(controller,
                "pathologyConsultationService");
        PathologyConsultationService losing = Mockito.mock(PathologyConsultationService.class,
                AdditionalAnswers.delegatesTo(real));
        stub.on(losing);
        ReflectionTestUtils.setField(controller, "pathologyConsultationService", losing);
        try {
            return call.run();
        } finally {
            ReflectionTestUtils.setField(controller, "pathologyConsultationService", real);
        }
    }

    private MockHttpServletResponse withADaoBlindToAwaitedRows(Call call) throws Exception {
        Object controller = AopTestUtils
                .getUltimateTargetObject(securityContext.getBean(PathologyConsultationRestController.class));
        Object service = AopTestUtils
                .getUltimateTargetObject(ReflectionTestUtils.getField(controller, "pathologyConsultationService"));
        PathologyConsultationDAO real = (PathologyConsultationDAO) ReflectionTestUtils.getField(service,
                "pathologyConsultationDAO");
        PathologyConsultationDAO blind = Mockito.mock(PathologyConsultationDAO.class,
                AdditionalAnswers.delegatesTo(real));
        doReturn(List.of()).when(blind).getForCase(any());
        ReflectionTestUtils.setField(service, "pathologyConsultationDAO", blind);
        try {
            return call.run();
        } finally {
            ReflectionTestUtils.setField(service, "pathologyConsultationDAO", real);
        }
    }

    private int consultationCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM clinlims.pathology_consultation", Integer.class);
    }

    private static String answerOf(int id) {
        return BASE + "/" + id + "/answer";
    }

    private static String withdrawalOf(int id) {
        return BASE + "/" + id + "/withdraw";
    }

    /** A second opinion on the case asked by 1002 of the user given. */
    private int insertRow(int consulted, String status) {
        boolean answered = "ANSWERED".equals(status);
        return jdbcTemplate.queryForObject("INSERT INTO clinlims.pathology_consultation (id, pathology_sample_id,"
                + " requested_by, consulted_user_id, pre_treatment, status, outcome, requested_at, answered_at,"
                + " last_updated) VALUES (nextval('clinlims.pathology_consultation_seq'), ?, 1002, ?, true, ?, ?::varchar,"
                + " now(), ?::timestamp, now()) RETURNING id", Integer.class, CASE_ID, consulted, status,
                answered ? "AGREE" : null, answered ? new Timestamp(System.currentTimeMillis()) : null);
    }

    private Map<String, Object> row(int id) {
        return jdbcTemplate.queryForMap("SELECT * FROM clinlims.pathology_consultation WHERE id = ?", id);
    }
}
