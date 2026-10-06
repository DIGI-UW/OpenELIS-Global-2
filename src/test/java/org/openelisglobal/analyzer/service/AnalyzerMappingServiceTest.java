package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.dao.AnalyzerMappingDAO;
import org.openelisglobal.analyzer.dao.AnalyzerMappingResultDAO;
import org.openelisglobal.analyzer.dao.AnalyzerMappingTestDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerMapping;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingOrigin;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingResult;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingTest;
import org.openelisglobal.audittrail.dao.AuditTrailService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerMappingServiceTest {

    private static final String PROFILE_ID = "site.mock-hematology";
    private static final int PROFILE_REVISION = 3;
    private static final String PROFILE_FINGERPRINT = "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @Mock
    private AnalyzerMappingDAO mappingDAO;
    @Mock
    private AnalyzerMappingTestDAO testDAO;
    @Mock
    private AnalyzerMappingResultDAO resultDAO;
    @Mock
    private AuditTrailService auditTrailService;
    @Mock
    private TestService testService;
    @Mock
    private TestResultService testResultService;
    @Mock
    private TestResultComponentService componentService;
    @Mock
    private BridgeProfileCatalogService profileCatalogService;

    private AnalyzerMappingService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setUp() {
        service = new AnalyzerMappingServiceImpl(mappingDAO, testDAO, resultDAO, auditTrailService, testService,
                testResultService, componentService,
                new AnalyzerMappingDefaults(mock(AnalyzerMappingCatalogService.class), testResultService),
                profileCatalogService);
        when(mappingDAO.insert(any(AnalyzerMapping.class))).thenAnswer(invocation -> {
            AnalyzerMapping mapping = invocation.getArgument(0);
            mapping.setId("61");
            return "61";
        });
    }

    @Test
    public void assignProfilePinsTheAnalyzerAndWritesRevisionOneOfItsOwnMapping() throws Exception {
        Analyzer analyzer = analyzer("7");
        when(profileCatalogService.getCatalog()).thenReturn(catalog("ACTIVE"));
        when(mappingDAO.findLatestByAnalyzerId("7")).thenReturn(Optional.empty());

        AnalyzerMappingSnapshot created = service.assignProfile(analyzer, PROFILE_ID, PROFILE_REVISION, "17");

        assertSame(analyzer, created.mapping().getAnalyzer());
        assertEquals(1, created.mapping().getRevisionNumber());
        assertEquals(PROFILE_ID, created.mapping().getProfileId());
        assertEquals(PROFILE_REVISION, created.mapping().getProfileRevision());
        assertEquals(PROFILE_FINGERPRINT, created.mapping().getProfileFingerprint());
        assertEquals(List.of("HIV", "WBC"),
                created.tests().stream().map(row -> row.getId().getSourceRowKey()).toList());
        assertEquals(List.of("NEG", "POS"), created.results().stream().map(row -> row.getId().getRawValue()).toList());
        created.tests().forEach(row -> {
            assertEquals(AnalyzerMappingState.UNRESOLVED, row.getMappingState());
            assertEquals(AnalyzerMappingOrigin.DEFAULT, row.getOrigin());
        });
        verify(mappingDAO).insert(created.mapping());
        verify(testDAO).insert(created.tests().get(0));
        verify(resultDAO).insert(created.results().get(0));
        verify(auditTrailService).saveNewHistory(created.mapping(), "17", "analyzer_mapping");
    }

    @Test
    public void assignProfileRefusesAProfileThatIsNotActiveOrNotInTheCatalog() throws Exception {
        when(profileCatalogService.getCatalog()).thenReturn(catalog("INACTIVE"));

        IllegalArgumentException inactive = assertThrows(IllegalArgumentException.class,
                () -> service.assignProfile(analyzer("7"), PROFILE_ID, PROFILE_REVISION, "17"));
        IllegalArgumentException missing = assertThrows(IllegalArgumentException.class,
                () -> service.assignProfile(analyzer("7"), "no.such.profile", 1, "17"));

        assertEquals("Bridge profile site.mock-hematology revision 3 is not active", inactive.getMessage());
        assertEquals("Bridge profile no.such.profile revision 1 was not found", missing.getMessage());
        verifyZeroInteractions(testDAO, resultDAO, auditTrailService);
    }

    @Test
    public void assignProfileNeedsASavedAnalyzer() {
        assertThrows(IllegalArgumentException.class,
                () -> service.assignProfile(new Analyzer(), PROFILE_ID, PROFILE_REVISION, "17"));
        verifyZeroInteractions(mappingDAO, testDAO, resultDAO, auditTrailService);
    }

    @Test
    public void twoAnalyzersOnOneProfileEachWriteTheirOwnRevisionOne() throws Exception {
        Analyzer first = analyzer("7");
        Analyzer second = analyzer("8");
        when(profileCatalogService.getCatalog()).thenReturn(catalog("ACTIVE"));
        when(mappingDAO.findLatestByAnalyzerId("7")).thenReturn(Optional.empty());
        when(mappingDAO.findLatestByAnalyzerId("8")).thenReturn(Optional.empty());

        AnalyzerMappingSnapshot one = service.assignProfile(first, PROFILE_ID, PROFILE_REVISION, "17");
        AnalyzerMappingSnapshot two = service.assignProfile(second, PROFILE_ID, PROFILE_REVISION, "17");

        assertSame(first, one.mapping().getAnalyzer());
        assertSame(second, two.mapping().getAnalyzer());
        assertEquals(1, one.mapping().getRevisionNumber());
        assertEquals(1, two.mapping().getRevisionNumber());
    }

    @Test
    public void findLatestByAnalyzerIdReturnsThatAnalyzersNewestRevisionWithItsRows() {
        AnalyzerMapping mapping = mapping("7", 2);
        AnalyzerMappingTest test = new AnalyzerMappingTest();
        AnalyzerMappingResult result = new AnalyzerMappingResult();
        when(mappingDAO.findLatestByAnalyzerId("7")).thenReturn(Optional.of(mapping));
        when(testDAO.findByMappingId("61")).thenReturn(List.of(test));
        when(resultDAO.findByMappingId("61")).thenReturn(List.of(result));

        AnalyzerMappingSnapshot found = service.findLatestByAnalyzerId("7").orElseThrow();

        assertSame(mapping, found.mapping());
        assertEquals(List.of(test), found.tests());
        assertEquals(List.of(result), found.results());
        verify(mappingDAO, never()).insert(any());
        verifyZeroInteractions(auditTrailService);
    }

    @Test
    public void findLatestByAnalyzerIdIsEmptyWhenTheAnalyzerHasNoMapping() {
        when(mappingDAO.findLatestByAnalyzerId("7")).thenReturn(Optional.empty());

        assertEquals(Optional.empty(), service.findLatestByAnalyzerId("7"));
    }

    @Test
    public void findByIdReturnsThatExactRevisionWithoutWriting() {
        AnalyzerMapping mapping = mapping("7", 1);
        when(mappingDAO.get("61")).thenReturn(Optional.of(mapping));
        when(testDAO.findByMappingId("61")).thenReturn(List.of());
        when(resultDAO.findByMappingId("61")).thenReturn(List.of());

        assertSame(mapping, service.findById("61").orElseThrow().mapping());
        verifyZeroInteractions(auditTrailService);
    }

    @Test
    public void appendRevisionRejectsDuplicateRowsAndInvalidTargetsBeforeWriting() {
        Analyzer analyzer = analyzer("7");
        AnalyzerMappingDraft duplicateTest = new AnalyzerMappingDraft(List
                .of(test("hiv", AnalyzerMappingState.EXCLUDED, null), test("hiv", AnalyzerMappingState.EXCLUDED, null)),
                List.of());
        AnalyzerMappingDraft boundWithoutTarget = new AnalyzerMappingDraft(
                List.of(test("hiv", AnalyzerMappingState.BOUND, null)), List.of());
        AnalyzerMappingDraft excludedWithTarget = new AnalyzerMappingDraft(
                List.of(test("hiv", AnalyzerMappingState.EXCLUDED, "9701")), List.of());
        AnalyzerMappingDraft orphanResult = new AnalyzerMappingDraft(List.of(),
                List.of(result("hiv", "POS", AnalyzerMappingState.EXCLUDED, null)));

        assertEquals("Duplicate test source row: hiv", assertThrows(IllegalArgumentException.class,
                () -> service.appendRevision(analyzer, duplicateTest, "17")).getMessage());
        assertEquals("BOUND test row hiv requires a local target", assertThrows(IllegalArgumentException.class,
                () -> service.appendRevision(analyzer, boundWithoutTarget, "17")).getMessage());
        assertEquals("EXCLUDED test row hiv cannot have a local target", assertThrows(IllegalArgumentException.class,
                () -> service.appendRevision(analyzer, excludedWithTarget, "17")).getMessage());
        assertEquals("Result row has no matching test row: hiv",
                assertThrows(IllegalArgumentException.class, () -> service.appendRevision(analyzer, orphanResult, "17"))
                        .getMessage());
        verify(mappingDAO, never()).insert(any());
        verifyZeroInteractions(testDAO, resultDAO, auditTrailService);
    }

    @Test
    public void appendRevisionKeepsThePinAndSupersedesWithoutMutatingTheOldRevision() {
        Analyzer analyzer = analyzer("7");
        AnalyzerMapping current = mapping("7", 1);
        when(mappingDAO.findLatestByAnalyzerId("7")).thenReturn(Optional.of(current));
        org.openelisglobal.test.valueholder.Test mappedTest = activeTest("9701");
        when(testService.get("9701")).thenReturn(mappedTest);
        AnalyzerMappingDraft draft = new AnalyzerMappingDraft(List.of(test("hiv", AnalyzerMappingState.BOUND, "9701"),
                test("wbc", AnalyzerMappingState.EXCLUDED, null)), List.of());

        AnalyzerMappingSnapshot saved = service.appendRevision(analyzer, draft, "17");

        assertEquals(2, saved.mapping().getRevisionNumber());
        assertSame(current, saved.mapping().getSupersedes());
        assertEquals(1, current.getRevisionNumber());
        assertEquals(PROFILE_ID, saved.mapping().getProfileId());
        assertEquals(PROFILE_REVISION, saved.mapping().getProfileRevision());
        assertEquals(PROFILE_FINGERPRINT, saved.mapping().getProfileFingerprint());
        assertEquals("9701", saved.tests().get(0).getTestId());
        verify(auditTrailService).saveNewHistory(saved.mapping(), "17", "analyzer_mapping");
    }

    @Test
    public void appendRevisionReturnsTheCurrentRevisionWhenNothingChanged() {
        Analyzer analyzer = analyzer("7");
        AnalyzerMappingDraft draft = new AnalyzerMappingDraft(List.of(test("wbc", AnalyzerMappingState.EXCLUDED, null)),
                List.of());
        AnalyzerMapping current = mapping("7", 1);
        current.setMappingFingerprint(AnalyzerMappingFingerprint.calculate(draft));
        when(mappingDAO.findLatestByAnalyzerId("7")).thenReturn(Optional.of(current));
        when(testDAO.findByMappingId("61")).thenReturn(List.of());
        when(resultDAO.findByMappingId("61")).thenReturn(List.of());

        assertSame(current, service.appendRevision(analyzer, draft, "17").mapping());
        verify(mappingDAO, never()).insert(any());
        verifyZeroInteractions(auditTrailService);
    }

    @Test
    public void appendRevisionRefusesAnAnalyzerWithNoMapping() {
        when(mappingDAO.findLatestByAnalyzerId("7")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.appendRevision(analyzer("7"),
                new AnalyzerMappingDraft(List.of(), List.of()), "17"));
    }

    @Test
    public void appendRevisionRejectsAnInactiveTestBeforeWriting() {
        org.openelisglobal.test.valueholder.Test inactive = activeTest("9701");
        inactive.setIsActive("N");
        when(testService.get("9701")).thenReturn(inactive);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.appendRevision(analyzer("7"),
                        new AnalyzerMappingDraft(List.of(test("hiv", AnalyzerMappingState.BOUND, "9701")), List.of()),
                        "17"));

        assertEquals("BOUND test row hiv must reference an active Test", error.getMessage());
        verify(mappingDAO, never()).insert(any());
    }

    @Test
    public void appendRevisionAcceptsAComponentOfTheMappedTestAndRecordsIt() {
        when(mappingDAO.findLatestByAnalyzerId("7")).thenReturn(Optional.of(mapping("7", 1)));
        when(testService.get("9701")).thenReturn(activeTest("9701"));
        when(componentService.getComponentsByTestId("9701")).thenReturn(List.of(component("c-log")));

        AnalyzerMappingSnapshot saved = service.appendRevision(analyzer("7"),
                new AnalyzerMappingDraft(List.of(new AnalyzerMappingTestDraft("LOG", AnalyzerMappingState.BOUND,
                        "9701", "c-log", null, AnalyzerMappingOrigin.OVERRIDE)), List.of()),
                "17");

        assertEquals("c-log", saved.tests().get(0).getComponentId());
        assertEquals(AnalyzerMappingOrigin.OVERRIDE, saved.tests().get(0).getOrigin());
    }

    @Test
    public void appendRevisionRejectsAComponentThatBelongsToAnotherTest() {
        when(testService.get("9701")).thenReturn(activeTest("9701"));
        when(componentService.getComponentsByTestId("9701")).thenReturn(List.of(component("c-other")));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.appendRevision(analyzer("7"),
                        new AnalyzerMappingDraft(List.of(new AnalyzerMappingTestDraft("LOG",
                                AnalyzerMappingState.BOUND, "9701", "c-log", null, AnalyzerMappingOrigin.OVERRIDE)),
                                List.of()),
                        "17"));

        assertEquals("Test row LOG must name a component of Test 9701", error.getMessage());
        verify(mappingDAO, never()).insert(any());
    }

    @Test
    public void appendRevisionRejectsAComponentOnARowThatIsNotBound() {

        assertThrows(IllegalArgumentException.class,
                () -> service.appendRevision(analyzer("7"),
                        new AnalyzerMappingDraft(List.of(new AnalyzerMappingTestDraft("LOG",
                                AnalyzerMappingState.EXCLUDED, null, "c-log", null, AnalyzerMappingOrigin.OVERRIDE)),
                                List.of()),
                        "17"));
    }

    @Test
    public void appendRevisionRejectsAnInactiveResultOptionBeforeWriting() {
        org.openelisglobal.test.valueholder.Test mappedTest = activeTest("9701");
        when(testService.get("9701")).thenReturn(mappedTest);
        when(testResultService.get("811")).thenReturn(resultOption("811", mappedTest, false));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.appendRevision(analyzer("7"),
                        new AnalyzerMappingDraft(List.of(test("hiv", AnalyzerMappingState.BOUND, "9701")),
                                List.of(result("hiv", "POS", AnalyzerMappingState.BOUND, "811"))),
                        "17"));

        assertEquals("BOUND result row hiv/POS must reference an active Result Option", error.getMessage());
        verify(mappingDAO, never()).insert(any());
    }

    @Test
    public void appendRevisionRejectsAResultOptionOwnedByAnotherTestBeforeWriting() {
        org.openelisglobal.test.valueholder.Test mappedTest = activeTest("9701");
        when(testService.get("9701")).thenReturn(mappedTest);
        when(testResultService.get("811")).thenReturn(resultOption("811", activeTest("9702"), true));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.appendRevision(analyzer("7"),
                        new AnalyzerMappingDraft(List.of(test("hiv", AnalyzerMappingState.BOUND, "9701")),
                                List.of(result("hiv", "POS", AnalyzerMappingState.BOUND, "811"))),
                        "17"));

        assertEquals("BOUND result row hiv/POS must belong to mapped Test 9701", error.getMessage());
    }

    @Test
    public void appendRevisionRejectsANonOptionTestResultBeforeWriting() {
        org.openelisglobal.test.valueholder.Test mappedTest = activeTest("9701");
        TestResult numeric = resultOption("811", mappedTest, true);
        numeric.setTestResultType("N");
        when(testService.get("9701")).thenReturn(mappedTest);
        when(testResultService.get("811")).thenReturn(numeric);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.appendRevision(analyzer("7"),
                        new AnalyzerMappingDraft(List.of(test("hiv", AnalyzerMappingState.BOUND, "9701")),
                                List.of(result("hiv", "POS", AnalyzerMappingState.BOUND, "811"))),
                        "17"));

        assertEquals("BOUND result row hiv/POS must reference an active Result Option", error.getMessage());
    }

    private static Analyzer analyzer(String id) {
        Analyzer analyzer = new Analyzer();
        analyzer.setId(id);
        return analyzer;
    }

    private static AnalyzerMapping mapping(String analyzerId, int revisionNumber) {
        AnalyzerMapping mapping = new AnalyzerMapping();
        mapping.setId("61");
        mapping.setAnalyzer(analyzer(analyzerId));
        mapping.setRevisionNumber(revisionNumber);
        mapping.setProfileId(PROFILE_ID);
        mapping.setProfileRevision(PROFILE_REVISION);
        mapping.setProfileFingerprint(PROFILE_FINGERPRINT);
        mapping.setMappingFingerprint("sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
        return mapping;
    }

    private BridgeProfileCatalog catalog(String status) throws Exception {
        JsonNode profile = objectMapper.readTree("""
                {
                  "profileMeta":{"id":"site.mock-hematology","displayName":"Mock Hematology"},
                  "protocol":{"name":"ASTM","version":"LIS2-A2"},
                  "communication":{"mode":"ANALYZER_INITIATED","supports_lis_initiated":false},
                  "configDefaults":{"connectionRole":"SERVER","aggregationMode":"PER_MESSAGE"},
                  "catalog":{
                    "revision":3,
                    "revisionFingerprint":"sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                    "source":"SITE",
                    "status":"%s"
                  },
                  "default_test_mappings":[
                    {"test_code":"WBC","loinc":"6690-2","result_type":"quantitative"},
                    {"test_code":"HIV","loinc":"20447-9","result_type":"qualitative","values":["POS","NEG"]}
                  ]
                }
                """.formatted(status));
        return new BridgeProfileCatalog("1.0", PROFILE_FINGERPRINT,
                List.of(new BridgeProfileCatalog.ProfileRevision(profile, JsonNodeFactory.instance.objectNode())));
    }

    private static AnalyzerMappingTestDraft test(String sourceRowKey, AnalyzerMappingState state, String testId) {
        return new AnalyzerMappingTestDraft(sourceRowKey, state, testId);
    }

    private static AnalyzerMappingResultDraft result(String sourceRowKey, String rawValue, AnalyzerMappingState state,
            String testResultId) {
        return new AnalyzerMappingResultDraft(sourceRowKey, rawValue, state, testResultId);
    }

    private static org.openelisglobal.test.valueholder.Test activeTest(String id) {
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId(id);
        test.setIsActive("Y");
        return test;
    }

    private static TestResult resultOption(String id, org.openelisglobal.test.valueholder.Test test, boolean active) {
        TestResult option = new TestResult();
        option.setId(id);
        option.setTest(test);
        option.setIsActive(active);
        option.setTestResultType("D");
        return option;
    }

    private static TestResultComponent component(String id) {
        TestResultComponent component = new TestResultComponent();
        component.setId(id);
        return component;
    }
}
