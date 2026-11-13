package org.openelisglobal.resultvalidation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.validationpolicy.dao.AnalysisReviewSignOffDAO;
import org.openelisglobal.validationpolicy.form.ValidationPolicyForm;
import org.openelisglobal.validationpolicy.service.AnalysisReviewService;
import org.openelisglobal.validationpolicy.service.ValidationPolicyService;
import org.openelisglobal.validationpolicy.valueholder.AnalysisReviewSignOff;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * OGC-1364 v2: sequential sign-off on the Validation page. With two reviews,
 * the first validator's signature keeps the result in review, the same person
 * cannot sign the second level, and another validator's signature releases it.
 * Modifying or retesting restarts review at level 1. Fixture:
 * {@code testdata/validation-review-panel.xml}, analysis 100 awaiting
 * validation; a second validator is added here.
 */
@Transactional
public class AccessionValidationSequentialSignOffTest extends BaseWebContextSensitiveTest {

    private static final String ACCESSION = "VAL-RP-001";
    private static final String ANALYSIS_ID = "100";
    private static final String SECOND_VALIDATOR = "9501";

    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private IStatusService statusService;
    @Autowired
    private UserService userService;
    @Autowired
    private SystemUserService systemUserService;
    @Autowired
    private ValidationPolicyService validationPolicyService;
    @Autowired
    private ResultService resultService;
    @Autowired
    private AnalysisReviewService analysisReviewService;

    private String flagBefore;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/validation-review-panel.xml");
        authenticateAs("testUser");
        jdbcTemplate.update(
                "INSERT INTO clinlims.system_user (id, login_name, last_name, first_name, is_active,"
                        + " is_employee, lastupdated) VALUES (?, 'secondValidator', 'Kila', 'Ama', 'Y', 'Y', now())",
                Integer.valueOf(SECOND_VALIDATOR));
        userService.saveUserLabUnitRoles(systemUserService.get("1"), Map.of("AllLabUnits", Set.of("9400")), "1");
        userService.saveUserLabUnitRoles(systemUserService.get(SECOND_VALIDATOR), Map.of("AllLabUnits", Set.of("9400")),
                "1");
        flagBefore = ConfigurationProperties.getInstance().getPropertyValue(Property.USE_MULTI_LEVEL_VALIDATION);
        ConfigurationProperties.getInstance().setPropertyValue(Property.USE_MULTI_LEVEL_VALIDATION, "true");
        validationPolicyService.saveLabWideDefault(new ValidationPolicyForm("ALL", 2, null), "1");
        entityManager.flush();
        entityManager.clear();
    }

    @After
    public void restoreFlag() {
        ConfigurationProperties.getInstance().setPropertyValue(Property.USE_MULTI_LEVEL_VALIDATION,
                flagBefore == null ? "false" : flagBefore);
    }

    @Test
    public void theFirstSignatureKeepsTheResultInReviewAndAnotherValidatorReleasesIt() throws Exception {
        release("1").andExpect(status().isOk()).andExpect(jsonPath("$.outcome").value("levelSigned"))
                .andExpect(jsonPath("$.level").value(1)).andExpect(jsonPath("$.levels").value(2));
        entityManager.flush();
        entityManager.clear();
        assertEquals("level 1 of 2 does not release", statusId(AnalysisStatus.TechnicalAcceptance),
                analysisService.get(ANALYSIS_ID).getStatusId());

        queue("1").andExpect(jsonPath(row() + ".reviewLevels").value(2))
                .andExpect(jsonPath(row() + ".reviewLevel").value(2))
                .andExpect(jsonPath(row() + ".reviewBlockedReason").value("alreadySigned"))
                .andExpect(jsonPath(row() + ".signOffs[0].level").value(1))
                .andExpect(jsonPath(row() + ".signOffs[0].signerName").value("John Doe"));
        release("1").andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("alreadySigned"));

        queue(SECOND_VALIDATOR).andExpect(jsonPath(row() + ".reviewBlockedReason").doesNotExist());
        release(SECOND_VALIDATOR).andExpect(status().isOk()).andExpect(jsonPath("$.outcome").value("released"));
        entityManager.flush();
        entityManager.clear();

        assertEquals(statusId(AnalysisStatus.Finalized), analysisService.get(ANALYSIS_ID).getStatusId());
        List<Map<String, Object>> signOffs = signOffs();
        assertEquals(2, signOffs.size());
        assertEquals(1, ((Number) signOffs.get(0).get("signer_id")).intValue());
        assertEquals(9501, ((Number) signOffs.get(1).get("signer_id")).intValue());
        assertEquals(2, ((Number) signOffs.get(1).get("level")).intValue());
    }

    @Test
    public void modifyingTheValueSupersedesTheSignaturesAndReviewRestartsAtLevelOne() throws Exception {
        release("1").andExpect(status().isOk());
        mockMvc.perform(post("/rest/AccessionValidation/analysis/100/modify").session(session(SECOND_VALIDATOR))
                .contentType(MediaType.APPLICATION_JSON).content(rowBody("11.0", "Transcription error")))
                .andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();

        Map<String, Object> superseded = signOffs().get(0);
        assertEquals("MODIFIED", superseded.get("superseded_reason"));
        assertEquals(9501, ((Number) superseded.get("superseded_by")).intValue());
        queue("1").andExpect(jsonPath(row() + ".reviewLevel").value(1))
                .andExpect(jsonPath(row() + ".signOffs.length()").value(0))
                .andExpect(jsonPath(row() + ".supersededSignOffs[0].supersededReason").value("MODIFIED"))
                .andExpect(jsonPath(row() + ".reviewBlockedReason").doesNotExist());
        release("1").andExpect(status().isOk()).andExpect(jsonPath("$.outcome").value("levelSigned"))
                .andExpect(jsonPath("$.level").value(1));
    }

    @Test
    public void sendingForRetestSupersedesTheSignatures() throws Exception {
        release("1").andExpect(status().isOk());
        mockMvc.perform(post("/rest/AccessionValidation/analysis/100/retest").session(session(SECOND_VALIDATOR))
                .contentType(MediaType.APPLICATION_JSON).content(rowBody("10.5", "Repeat please")))
                .andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();

        assertEquals("RETEST", signOffs().get(0).get("superseded_reason"));
    }

    @Test
    public void reviewsAlreadyUnderWayKeepTheLevelsTheyStartedWith() throws Exception {
        release("1").andExpect(status().isOk());
        validationPolicyService.saveLabWideDefault(new ValidationPolicyForm("ALL", 1, null), "1");
        entityManager.flush();
        entityManager.clear();

        queue(SECOND_VALIDATOR).andExpect(jsonPath(row() + ".reviewLevels").value(2))
                .andExpect(jsonPath(row() + ".reviewLevel").value(2));
        queue(SECOND_VALIDATOR).andExpect(jsonPath("$.resultList[?(@.analysisId=='102')].reviewLevels").value(1));
    }

    @Test
    public void withTheFlagOffOneValidatorReleasesAndNoSignOffIsRecorded() throws Exception {
        ConfigurationProperties.getInstance().setPropertyValue(Property.USE_MULTI_LEVEL_VALIDATION, "false");

        release("1").andExpect(status().isOk()).andExpect(jsonPath("$.outcome").value("released"));
        entityManager.flush();
        entityManager.clear();

        assertEquals(statusId(AnalysisStatus.Finalized), analysisService.get(ANALYSIS_ID).getStatusId());
        assertEquals(0, signOffs().size());
    }

    @Test
    public void aSignatureThatIsNotTheSignersOrMeansSomethingElseIsRefused() throws Exception {
        Long othersSignature = signature(SECOND_VALIDATOR, "REVIEWED");
        Long releaseSignature = signature("1", "VALIDATED_AND_RELEASED");
        Long reviewSignature = signature("1", "REVIEWED");

        release("1", othersSignature).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("signatureMismatch"));
        release("1", releaseSignature).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("signatureMismatch"));
        release("1", 987654321L).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("signatureUnknown"));
        assertEquals("a refused signature records nothing", 0, signOffs().size());

        release("1", reviewSignature).andExpect(status().isOk());
        entityManager.flush();
        assertEquals(reviewSignature.longValue(),
                ((Number) signOffs().get(0).get("electronic_signature_id")).longValue());
    }

    @Test
    public void aLevelRoleTheValidatorDoesNotHoldIsRefused() throws Exception {
        jdbcTemplate.update("INSERT INTO clinlims.system_role (id, name, description, is_grouping_role, active,"
                + " editable) VALUES (9410, 'Global Roles', 'grouping', true, true, false)");
        jdbcTemplate.update("INSERT INTO clinlims.system_role (id, name, description, is_grouping_role,"
                + " grouping_parent, active, editable) VALUES (9411, 'Pathologist', 'signs reports', false, 9410,"
                + " true, false)");
        validationPolicyService.saveLabWideDefault(new ValidationPolicyForm("ALL", 2, List.of(9400, 9411)), "1");
        entityManager.flush();
        entityManager.clear();
        release("1").andExpect(status().isOk());

        queue(SECOND_VALIDATOR).andExpect(jsonPath(row() + ".reviewRoleName").value("Pathologist"))
                .andExpect(jsonPath(row() + ".reviewBlockedReason").value("roleRequired"));
        release(SECOND_VALIDATOR).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("roleRequired"));

        jdbcTemplate.update("INSERT INTO clinlims.system_user_role (system_user_id, role_id) VALUES (?, 9411)",
                Integer.valueOf(SECOND_VALIDATOR));
        release(SECOND_VALIDATOR).andExpect(status().isOk()).andExpect(jsonPath("$.outcome").value("released"));
        entityManager.flush();
        entityManager.clear();
        assertNull(signOffs().get(1).get("superseded_at"));
        assertEquals(9411, ((Number) signOffs().get(1).get("role_id")).intValue());
    }

    @Test
    public void aValueEditedOutsideTheReviewPanelRestartsTheReview() throws Exception {
        release("1").andExpect(status().isOk());
        saveValue("9.5", SECOND_VALIDATOR);
        entityManager.flush();
        entityManager.clear();

        Map<String, Object> superseded = signOffs().get(0);
        assertEquals("MODIFIED", superseded.get("superseded_reason"));
        assertEquals(9501, ((Number) superseded.get("superseded_by")).intValue());
        queue(SECOND_VALIDATOR).andExpect(jsonPath(row() + ".reviewLevel").value(1));
        release(SECOND_VALIDATOR).andExpect(status().isOk()).andExpect(jsonPath("$.outcome").value("levelSigned"));
        assertEquals("one reviewer does not release the edited value", statusId(AnalysisStatus.TechnicalAcceptance),
                analysisService.get(ANALYSIS_ID).getStatusId());
    }

    @Test
    public void savingTheSameValueKeepsTheSignatures() throws Exception {
        release("1").andExpect(status().isOk());
        saveValue("10.50", SECOND_VALIDATOR);
        entityManager.flush();
        entityManager.clear();

        assertNull(signOffs().get(0).get("superseded_at"));
    }

    @Test
    public void aReleasedResultEditedAgainStartsItsReviewOver() throws Exception {
        release("1").andExpect(status().isOk());
        release(SECOND_VALIDATOR).andExpect(status().isOk()).andExpect(jsonPath("$.outcome").value("released"));
        entityManager.flush();
        entityManager.clear();
        jdbcTemplate.update("UPDATE clinlims.analysis SET status_id = ? WHERE id = ?",
                Integer.valueOf(statusId(AnalysisStatus.TechnicalAcceptance)), Integer.valueOf(ANALYSIS_ID));
        saveValue("12.0", "1");
        entityManager.flush();
        entityManager.clear();

        assertEquals(2, signOffs().stream().filter(signOff -> signOff.get("superseded_at") != null).count());
        release("1").andExpect(status().isOk()).andExpect(jsonPath("$.outcome").value("levelSigned"))
                .andExpect(jsonPath("$.level").value(1));
    }

    @Test
    public void theFormSaveCannotReleaseBeforeEveryLevelIsSigned() throws Exception {
        MockHttpSession validator = session(SECOND_VALIDATOR);
        formSave(validator).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("earlierLevelsUnsigned"));
        entityManager.clear();
        assertEquals(0, signOffs().size());
    }

    @Test
    public void theFormSaveRecordsTheLastLevelWhenItReleases() throws Exception {
        release("1").andExpect(status().isOk());
        formSave(session(SECOND_VALIDATOR)).andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();
        assertEquals(statusId(AnalysisStatus.Finalized), analysisService.get(ANALYSIS_ID).getStatusId());
        assertEquals(9501, ((Number) signOffs().get(1).get("signer_id")).intValue());
        assertEquals(2, ((Number) signOffs().get(1).get("level")).intValue());
    }

    @Test
    public void aLevelSignedAMomentAgoIsRefusedWithAConflict() throws Exception {
        release("1").andExpect(status().isOk());
        jdbcTemplate.update("INSERT INTO clinlims.system_user (id, login_name, last_name, first_name, is_active,"
                + " is_employee, lastupdated) VALUES (9502, 'thirdValidator', 'Tano', 'Esi', 'Y', 'Y', now())");
        jdbcTemplate.update("INSERT INTO clinlims.analysis_review_sign_off (id, analysis_id, level, review_levels,"
                + " signer_id, role_id, signed_at) VALUES (nextval('clinlims.analysis_review_sign_off_seq'), 100, 2, 2,"
                + " 9502, 9400, now())");
        entityManager.flush();
        entityManager.clear();
        Object reviewService = AopTestUtils.getTargetObject(analysisReviewService);
        AnalysisReviewSignOffDAO dao = (AnalysisReviewSignOffDAO) ReflectionTestUtils.getField(reviewService,
                "signOffDAO");
        AnalysisReviewSignOffDAO missesTheOtherSignature = (AnalysisReviewSignOffDAO) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[] { AnalysisReviewSignOffDAO.class },
                (proxy, method, args) -> {
                    try {
                        Object value = method.invoke(dao, args);
                        return "findByAnalysisIds".equals(method.getName())
                                ? ((List<?>) value).stream()
                                        .filter(signOff -> ((AnalysisReviewSignOff) signOff).getLevel() != 2).toList()
                                : value;
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
        ReflectionTestUtils.setField(reviewService, "signOffDAO", missesTheOtherSignature);
        try {
            release(SECOND_VALIDATOR).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("levelAlreadySigned"));
        } finally {
            ReflectionTestUtils.setField(reviewService, "signOffDAO", dao);
        }
    }

    @Test
    public void releasingAnotherValueAtTheLastLevelIsRefusedAndRestartsTheReview() throws Exception {
        release("1").andExpect(status().isOk());
        mockMvc.perform(post("/rest/AccessionValidation/analysis/100/release").session(session(SECOND_VALIDATOR))
                .contentType(MediaType.APPLICATION_JSON).content(rowBody("12.0", ""))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("resultChanged"));
    }

    private void saveValue(String value, String userId) {
        Result result = resultService.get("100");
        result.setValue(value);
        result.setSysUserId(userId);
        resultService.update(result);
    }

    private org.springframework.test.web.servlet.ResultActions formSave(MockHttpSession httpSession) throws Exception {
        mockMvc.perform(get("/rest/AccessionValidation").param("accessionNumber", ACCESSION).param("doRange", "false")
                .session(httpSession)).andExpect(status().isOk());
        httpSession.setAttribute(IActionConstants.RESULTS_SESSION_CACHE,
                new java.util.ArrayList<>(List.of(new java.util.ArrayList<>(List.of(new Result())))));
        String row = rowBody("10.5", "").replace("}", ",\"isAccepted\":true,\"isRejected\":false}");
        return mockMvc
                .perform(post("/rest/AccessionValidation").session(httpSession).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultList\":[" + row + "],\"paging\":{\"currentPage\":\"1\"}}"));
    }

    private org.springframework.test.web.servlet.ResultActions release(String userId) throws Exception {
        return release(userId, null);
    }

    private org.springframework.test.web.servlet.ResultActions release(String userId, Long signatureId)
            throws Exception {
        String signature = signatureId == null ? "" : ",\"signatureId\":" + signatureId;
        return mockMvc.perform(post("/rest/AccessionValidation/analysis/100/release").session(session(userId))
                .contentType(MediaType.APPLICATION_JSON).content(rowBody("10.5", "").replace("}", signature + "}")));
    }

    private org.springframework.test.web.servlet.ResultActions queue(String userId) throws Exception {
        return mockMvc.perform(get("/rest/AccessionValidation").param("accessionNumber", ACCESSION)
                .param("doRange", "false").session(session(userId))).andExpect(status().isOk());
    }

    private String row() {
        return "$.resultList[?(@.analysisId=='" + ANALYSIS_ID + "')]";
    }

    private String rowBody(String value, String note) {
        return "{\"analysisId\":\"" + ANALYSIS_ID + "\",\"accessionNumber\":\"" + ACCESSION + "\",\"resultId\":\"100\","
                + "\"testId\":\"1\",\"resultType\":\"N\",\"result\":\"" + value + "\",\"note\":\"" + note + "\","
                + "\"noteVisibility\":\"I\",\"noteContext\":\"VALIDATION\"}";
    }

    private List<Map<String, Object>> signOffs() {
        return jdbcTemplate.queryForList("SELECT level, signer_id, role_id, electronic_signature_id, superseded_at,"
                + " superseded_by, superseded_reason FROM clinlims.analysis_review_sign_off WHERE analysis_id = ?"
                + " ORDER BY id", Integer.valueOf(ANALYSIS_ID));
    }

    private Long signature(String signerId, String meaning) {
        return jdbcTemplate.queryForObject("INSERT INTO clinlims.electronic_signature (id, signer_id,"
                + " signer_name_printed, signature_meaning, signed_at, record_type, record_id, session_signing_sequence,"
                + " auth_method) VALUES (nextval('clinlims.electronic_signature_seq'), ?, 'Signer', ?, now(),"
                + " 'VALIDATION_BATCH', 100, 1, 'LOCAL') RETURNING id", Long.class, Long.valueOf(signerId), meaning);
    }

    private String statusId(AnalysisStatus status) {
        return statusService.getStatusID(status);
    }

    private MockHttpSession session(String userId) {
        UserDetails userDetails = User.withUsername("1".equals(userId) ? "testUser" : "secondValidator").password("N/A")
                .authorities("ROLE_ADMIN", "ROLE_VALIDATION").build();
        SecurityContext securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, "N/A", userDetails.getAuthorities()));
        UserSessionData userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(Integer.parseInt(userId));
        MockHttpSession httpSession = new MockHttpSession();
        httpSession.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        httpSession.setAttribute(IActionConstants.USER_SESSION_DATA, userSessionData);
        return httpSession;
    }
}
