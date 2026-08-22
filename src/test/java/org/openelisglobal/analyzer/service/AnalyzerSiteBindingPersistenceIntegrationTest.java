package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.dao.AnalyzerProfileBindingDAO;
import org.openelisglobal.analyzer.dao.AnalyzerSiteBindingConfirmationDAO;
import org.openelisglobal.analyzer.dao.AnalyzerSiteBindingDAO;
import org.openelisglobal.analyzer.dao.AnalyzerSiteBindingResultDAO;
import org.openelisglobal.analyzer.dao.AnalyzerSiteBindingRevisionDAO;
import org.openelisglobal.analyzer.dao.AnalyzerSiteBindingTestDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingMappingState;
import org.openelisglobal.audittrail.dao.AuditTrailService;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class AnalyzerSiteBindingPersistenceIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String PROFILE_FINGERPRINT = "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String RECOGNITION_FINGERPRINT = "sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    @Autowired
    private AnalyzerProfileBindingDAO profileBindingDAO;

    @Autowired
    private AnalyzerSiteBindingDAO siteBindingDAO;

    @Autowired
    private AnalyzerSiteBindingRevisionDAO revisionDAO;

    @Autowired
    private AnalyzerSiteBindingTestDAO siteBindingTestDAO;

    @Autowired
    private AnalyzerSiteBindingResultDAO siteBindingResultDAO;

    @Autowired
    private AnalyzerSiteBindingConfirmationDAO confirmationDAO;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    public void savedCatalogBindingsAndConfirmationReloadFromPostgres() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            String testId = jdbc.queryForObject("SELECT nextval('test_seq')", Long.class).toString();
            String resultOptionId = jdbc.queryForObject("SELECT nextval('test_result_seq')", Long.class).toString();
            jdbc.update(
                    "INSERT INTO test (id, name, description, guid, is_active, is_reportable, orderable, "
                            + "lastupdated) VALUES (?, ?, ?, ?, 'Y', 'Y', TRUE, CURRENT_TIMESTAMP)",
                    Long.valueOf(testId), "Analyzer binding persistence test", "Analyzer binding persistence test",
                    UUID.randomUUID());
            jdbc.update("INSERT INTO test_result (id, test_id, tst_rslt_type, value, sort_order, is_active, "
                    + "is_normal, lastupdated) VALUES (?, ?, 'D', 'POSITIVE', 1, TRUE, TRUE, CURRENT_TIMESTAMP)",
                    Long.valueOf(resultOptionId), Long.valueOf(testId));

            org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
            test.setId(testId);
            test.setIsActive("Y");
            TestResult resultOption = new TestResult();
            resultOption.setId(resultOptionId);
            resultOption.setIsActive(true);
            resultOption.setTestResultType("D");
            resultOption.setTest(test);

            TestService testService = mock(TestService.class);
            TestResultService testResultService = mock(TestResultService.class);
            AuditTrailService auditTrailService = mock(AuditTrailService.class);
            SystemUserService systemUserService = mock(SystemUserService.class);
            SystemUser actor = new SystemUser();
            actor.setId(TEST_SYS_USER_ID);
            actor.setFirstName("Integration");
            actor.setLastName("Reviewer");
            when(testService.get(testId)).thenReturn(test);
            when(testResultService.get(resultOptionId)).thenReturn(resultOption);
            when(systemUserService.getUserById(TEST_SYS_USER_ID)).thenReturn(actor);

            AnalyzerSiteBindingService siteBindingService = new AnalyzerSiteBindingServiceImpl(siteBindingDAO,
                    revisionDAO, siteBindingTestDAO, siteBindingResultDAO, auditTrailService, testService,
                    testResultService);
            AnalyzerSiteBindingConfirmationService confirmationService = new AnalyzerSiteBindingConfirmationServiceImpl(
                    confirmationDAO, auditTrailService, systemUserService);

            String profileId = "site.persistence." + UUID.randomUUID();
            AnalyzerProfileBinding profileBinding = new AnalyzerProfileBinding();
            profileBinding.setProfileId(profileId);
            profileBinding.setProfileRevision(1);
            profileBinding.setProfileFingerprint(PROFILE_FINGERPRINT);
            profileBinding.setSysUserId(TEST_SYS_USER_ID);
            profileBindingDAO.insert(profileBinding);

            ObjectNode profile = profile(profileId);
            AnalyzerSiteBindingSnapshot initial = siteBindingService.resolveInitialRevision(profileBinding, profile,
                    TEST_SYS_USER_ID);
            AnalyzerSiteBindingDraft decisions = new AnalyzerSiteBindingDraft(
                    List.of(new AnalyzerSiteBindingTestDraft("RAW-A", AnalyzerSiteBindingMappingState.BOUND, testId)),
                    List.of(new AnalyzerSiteBindingResultDraft("RAW-A", "POS", AnalyzerSiteBindingMappingState.BOUND,
                            resultOptionId)));
            AnalyzerSiteBindingSnapshot saved = siteBindingService.appendRevision(initial.binding(), decisions,
                    TEST_SYS_USER_ID);
            AnalyzerSiteBindingConfirmationRequest request = new AnalyzerSiteBindingConfirmationRequest(
                    saved.revision().getBindingFingerprint(), RECOGNITION_FINGERPRINT,
                    List.of(new AnalyzerSiteBindingSourceRow("RAW-A", null),
                            new AnalyzerSiteBindingSourceRow("RAW-A", "POS")),
                    List.of());
            confirmationService.confirm(saved, RECOGNITION_FINGERPRINT, request, TEST_SYS_USER_ID);

            entityManager.flush();
            entityManager.clear();

            AnalyzerSiteBindingSnapshot reloaded = siteBindingService
                    .findCurrentByProfileBindingId(profileBinding.getId()).orElseThrow();
            assertEquals(2, reloaded.revision().getRevisionNumber());
            assertEquals(saved.revision().getBindingFingerprint(), reloaded.revision().getBindingFingerprint());
            assertEquals(AnalyzerSiteBindingMappingState.BOUND, reloaded.tests().get(0).getMappingState());
            assertEquals(testId, reloaded.tests().get(0).getTestId());
            assertEquals(AnalyzerSiteBindingMappingState.BOUND, reloaded.results().get(0).getMappingState());
            assertEquals(resultOptionId, reloaded.results().get(0).getTestResultId());

            AnalyzerSiteBindingConfirmationView confirmation = confirmationService.getStatus(reloaded,
                    RECOGNITION_FINGERPRINT);
            assertEquals(AnalyzerSiteBindingConfirmationView.State.CURRENT, confirmation.state());
            assertEquals(TEST_SYS_USER_ID, confirmation.confirmedBy());
            assertEquals("Integration Reviewer", confirmation.confirmedByDisplayName());
            assertNotNull(confirmation.confirmedAt());
            assertEquals(request.confirmedRows(), confirmation.confirmedRows());
            assertTrue(confirmation.excludedRows().isEmpty());

            status.setRollbackOnly();
        });
    }

    private static ObjectNode profile(String profileId) {
        try {
            ObjectNode profile = (ObjectNode) new ObjectMapper().readTree("""
                    {
                      "profileMeta":{"id":"placeholder","displayName":"Persistence Test Analyzer"},
                      "protocol":{"name":"ASTM","version":"LIS2-A2"},
                      "configDefaults":{"connectionRole":"SERVER","defaultTransport":"TCP/IP"},
                      "catalog":{
                        "revision":1,
                        "revisionFingerprint":"sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                        "source":"SITE",
                        "status":"ACTIVE"
                      },
                      "default_test_mappings":[
                        {
                          "test_code":"RAW-A",
                          "loinc":"94500-6",
                          "result_type":"qualitative",
                          "values":["POS"]
                        }
                      ]
                    }
                    """);
            ((ObjectNode) profile.path("profileMeta")).put("id", profileId);
            return profile;
        } catch (Exception exception) {
            throw new AssertionError("Cannot build analyzer profile fixture", exception);
        }
    }
}
