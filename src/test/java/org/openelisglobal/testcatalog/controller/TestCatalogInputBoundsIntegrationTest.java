package org.openelisglobal.testcatalog.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import org.junit.After;
import org.junit.Before;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.CreateTestRequest;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.DisplayOrderUpdate;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.InterpretationDto;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.MappingDto;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.OptionDto;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.RangesResponse;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.ResultComponentDto;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.SampleResults;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.StorageDto;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.TerminologyResponse;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultinterpretation.service.TestResultInterpretationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * OGC-1467: the Test Catalog editor refuses values its columns cannot hold,
 * lists it was not sent and references that do not exist, instead of failing
 * with a 500 or writing half a request; two first Storage saves of one test
 * both succeed.
 */
public class TestCatalogInputBoundsIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_ID = 95661L;
    private static final long SAMPLE_TYPE = 95662L;
    private static final long SAMPLE_TYPE_LOCALIZATION = 95663L;
    private static final String PREFIX = "IB1467";

    @Autowired
    private TestService testService;
    @Autowired
    private TestResultComponentService componentService;
    @Autowired
    private TestResultInterpretationService interpretationService;
    @Autowired
    private TestResultService testResultService;
    @Autowired
    private org.openelisglobal.resultlimit.service.ResultLimitService resultLimitService;
    @Autowired
    private org.openelisglobal.testcatalog.service.RangeCoverageValidationService coverageService;
    @Autowired
    private org.openelisglobal.testsamplehandling.service.TestSampleHandlingService handlingService;
    @Autowired
    private org.openelisglobal.analyzer.service.AnalyzerService analyzerService;
    @Autowired
    private org.openelisglobal.typeofsample.service.TypeOfSampleService typeOfSampleService;
    @Autowired
    private org.openelisglobal.typeofsample.service.TypeOfSampleTestService typeOfSampleTestService;
    @Autowired
    private org.openelisglobal.testterminology.service.TestTerminologyMappingService terminologyService;
    @Autowired
    private org.openelisglobal.panel.service.PanelService panelService;
    @Autowired
    private org.openelisglobal.panelitem.service.PanelItemService panelItemService;
    @Autowired
    private org.openelisglobal.testcatalog.service.TestCatalogCreationService creationService;
    @Autowired
    private org.openelisglobal.test.service.TestSectionService testSectionService;
    @Autowired
    private org.openelisglobal.unitofmeasure.service.UnitOfMeasureService unitOfMeasureService;
    @Autowired
    private javax.sql.DataSource dataSource;

    private TestCatalogEditorRestController controller;
    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        controller = new TestCatalogEditorRestController(testService, componentService, interpretationService,
                testResultService, resultLimitService, coverageService, handlingService, analyzerService,
                typeOfSampleService, typeOfSampleTestService, terminologyService, panelService, panelItemService);
        ReflectionTestUtils.setField(controller, "testCatalogCreationService", creationService);
        ReflectionTestUtils.setField(controller, "testSectionService", testSectionService);
        ReflectionTestUtils.setField(controller, "unitOfMeasureService", unitOfMeasureService);
        cleanup();
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, local_code, is_active, guid, domain,"
                        + " antimicrobial_resistance, orderable, lastupdated) VALUES (?, ?, ?, ?, 'Y', ?, 'CLINICAL',"
                        + " false, true, NOW())",
                TEST_ID, PREFIX + " test", PREFIX + " test", PREFIX + "T", UUID.randomUUID().toString());
        jdbc.update(
                "INSERT INTO clinlims.test_result_component (id, test_id, code, label, display_order, result_type,"
                        + " allow_multiple_readings, is_primary, show_on_report, is_active, lastupdated, last_updated)"
                        + " VALUES (?, ?, 'PRIMARY', 'Glucose', 0, 'N', false, true, true, 'Y', NOW(), NOW())",
                UUID.randomUUID().toString(), TEST_ID);
        jdbc.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?, ?, NOW())",
                SAMPLE_TYPE_LOCALIZATION, "sampleType name");
        jdbc.update(
                "INSERT INTO clinlims.localization_value (id, localization_id, locale, value, last_updated)"
                        + " VALUES (?, ?, 'en', 'IB1467 type', NOW())",
                SAMPLE_TYPE_LOCALIZATION, SAMPLE_TYPE_LOCALIZATION);
        jdbc.update(
                "INSERT INTO clinlims.type_of_sample (id, description, domain, is_active, sort_order,"
                        + " name_localization_id, lastupdated) VALUES (?, 'IB1467 type', 'H', true, 1, ?, NOW())",
                SAMPLE_TYPE, SAMPLE_TYPE_LOCALIZATION);
        typeOfSampleService.clearCache();
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        String tests = "SELECT id FROM clinlims.test WHERE local_code LIKE 'IB1467%' OR id = " + TEST_ID;
        jdbc.update("DELETE FROM clinlims.test_sample_handling_history WHERE test_sample_handling_id IN"
                + " (SELECT id FROM clinlims.test_sample_handling WHERE test_id IN (" + tests + "))");
        jdbc.update("DELETE FROM clinlims.test_sample_handling WHERE test_id IN (" + tests + ")");
        jdbc.update("DELETE FROM clinlims.test_result WHERE test_id IN (" + tests + ")");
        jdbc.update("DELETE FROM clinlims.test_result_component WHERE test_id IN (" + tests + ")");
        jdbc.update("DELETE FROM clinlims.sampletype_test WHERE test_id IN (" + tests + ")");
        jdbc.update("DELETE FROM clinlims.test WHERE id IN (" + tests + ")");
        jdbc.update("DELETE FROM clinlims.type_of_sample WHERE id = ?", SAMPLE_TYPE);
        jdbc.update("DELETE FROM clinlims.localization_value WHERE localization_id = ?", SAMPLE_TYPE_LOCALIZATION);
        jdbc.update("DELETE FROM clinlims.localization WHERE id = ?", SAMPLE_TYPE_LOCALIZATION);
    }

    private static MockHttpServletRequest authedRequest() {
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(IActionConstants.USER_SESSION_DATA, usd);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        return request;
    }

    private String testId() {
        return String.valueOf(TEST_ID);
    }

    private ResponseEntity<SampleResults> saveChanged(Consumer<ResultComponentDto> change) {
        SampleResults body = controller.getSampleResults(testId()).getBody();
        change.accept(body.components.get(0));
        return controller.saveSampleResults(testId(), body, authedRequest());
    }

    @org.junit.Test
    public void sampleResults_valuesTheColumnsCannotHold_andUnknownUnits_areRefusedByName() {
        List<Consumer<ResultComponentDto>> tooLong = List.of(c -> c.code = "C".repeat(51),
                c -> c.label = "L".repeat(256), c -> c.defaultResult = "D".repeat(81), c -> {
                    InterpretationDto interpretation = new InterpretationDto();
                    interpretation.valueMatch = "V".repeat(81);
                    interpretation.text = "high";
                    c.interpretations = List.of(interpretation);
                }, c -> {
                    OptionDto option = new OptionDto();
                    option.value = "O".repeat(81);
                    option.resultType = "A";
                    c.options = List.of(option);
                });
        for (Consumer<ResultComponentDto> change : tooLong) {
            ResponseEntity<SampleResults> refused = saveChanged(change);
            assertEquals(422, refused.getStatusCode().value());
            assertEquals("value.tooLong", refused.getBody().refusal);
        }
        for (String unit : new String[] { "abc", "99999999" }) {
            ResponseEntity<SampleResults> refused = saveChanged(c -> c.uomId = unit);
            assertEquals(unit, 422, refused.getStatusCode().value());
            assertEquals("unit.unknown", refused.getBody().refusal);
        }
        ResponseEntity<SampleResults> longLabel = saveChanged(c -> c.label = "L".repeat(256));
        assertEquals("L".repeat(60) + "…", longLabel.getBody().refusedComponent);
        assertEquals("Glucose", controller.getSampleResults(testId()).getBody().components.get(0).label);
        assertEquals(200, saveChanged(c -> c.label = "L".repeat(255)).getStatusCode().value());
    }

    @org.junit.Test
    public void saves_thatSendANullList_are400_andWriteNothing() {
        SampleResults noComponents = controller.getSampleResults(testId()).getBody();
        noComponents.components = null;
        assertEquals(400,
                controller.saveSampleResults(testId(), noComponents, authedRequest()).getStatusCode().value());
        assertEquals(400, saveChanged(c -> c.options = null).getStatusCode().value());
        assertEquals(400, saveChanged(c -> c.interpretations = null).getStatusCode().value());

        TerminologyResponse terminology = new TerminologyResponse();
        assertEquals(400, controller.saveTerminology(testId(), terminology, authedRequest()).getStatusCode().value());
        RangesResponse ranges = new RangesResponse();
        assertEquals("an omitted list no longer clears every range", 400,
                controller.saveRanges(testId(), ranges, authedRequest()).getStatusCode().value());
        DisplayOrderUpdate order = new DisplayOrderUpdate();
        order.items = null;
        assertEquals(400,
                controller.saveTestOrder(String.valueOf(SAMPLE_TYPE), order, authedRequest()).getStatusCode().value());
        assertEquals(1, controller.getSampleResults(testId()).getBody().components.size());
    }

    @org.junit.Test
    public void terminology_aCodeOrNameTheColumnsCannotHold_is422() {
        for (String[] codeAndName : new String[][] { { "C".repeat(81), null }, { "2345-7", "N".repeat(256) } }) {
            TerminologyResponse body = new TerminologyResponse();
            MappingDto mapping = new MappingDto();
            mapping.source = "LOINC";
            mapping.code = codeAndName[0];
            mapping.displayName = codeAndName[1];
            mapping.relationship = "SAME_AS";
            body.mappings = List.of(mapping);
            ResponseEntity<TerminologyResponse> refused = controller.saveTerminology(testId(), body, authedRequest());
            assertEquals(422, refused.getStatusCode().value());
            assertEquals("error.testCatalog.terminology.tooLong", refused.getBody().messageKey);
        }
        assertTrue(controller.getTerminology(testId()).getBody().mappings.isEmpty());
    }

    private int createWith(Consumer<CreateTestRequest> change, String code) {
        CreateTestRequest body = new CreateTestRequest();
        body.name = PREFIX + " " + code;
        body.reportingName = body.name;
        body.code = code;
        body.domain = "CLINICAL";
        body.sampleTypeIds = List.of(String.valueOf(SAMPLE_TYPE));
        change.accept(body);
        return controller.createTest(body, authedRequest()).getStatusCode().value();
    }

    @org.junit.Test
    public void create_withAReferenceThatDoesNotExist_is422_andCreatesNothing() {
        assertEquals(422, createWith(b -> b.sampleTypeIds = List.of("99999999"), PREFIX + "A"));
        assertEquals(422, createWith(b -> b.sampleTypeIds = List.of("abc"), PREFIX + "B"));
        assertEquals(422, createWith(b -> b.labUnitId = "99999999", PREFIX + "C"));
        assertEquals(422, createWith(b -> b.copyFromId = "x", PREFIX + "D"));
        assertEquals(422, createWith(b -> b.copyFromId = "99999999", PREFIX + "E"));
        assertEquals(Integer.valueOf(0),
                jdbc.queryForObject("SELECT count(*) FROM clinlims.test WHERE local_code LIKE 'IB1467%' AND id <> ?",
                        Integer.class, TEST_ID));

        assertEquals(201, createWith(b -> b.copyFromId = testId(), PREFIX + "F"));
    }

    @org.junit.Test
    public void list_pagesBeyondIntAndANonNumericFilter_answerAnEmptyPage() {
        assertTrue(controller.listTests(null, "all", null, null, null, null, false, 1073741825, 2).rows.isEmpty());
        assertTrue(controller.listTests(null, "all", null, null, null, null, false, Integer.MAX_VALUE,
                Integer.MAX_VALUE).rows.isEmpty());
        assertTrue(controller.listTests(null, "all", null, "abc", null, null, false, 1, 25).rows.isEmpty());
        assertEquals(1,
                controller.listTests(null, "all", null, null, null, PREFIX + " test", false, 1, 25).rows.size());

        assertEquals(400, controller.groupSummary(testId() + ",abc").getStatusCode().value());
        assertEquals(1, controller.groupSummary(testId()).getBody().size());
    }

    @org.junit.Test
    public void twoFirstStorageSavesAtTheSameMoment_bothSucceed() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        try {
            for (int duration : new int[] { 3, 4 }) {
                Callable<Integer> call = () -> {
                    StorageDto body = new StorageDto();
                    body.storageDuration = duration;
                    body.storageDurationUnit = "DAYS";
                    start.await();
                    return controller.saveStorage(testId(), body, authedRequest()).getStatusCode().value();
                };
                results.add(pool.submit(call));
            }
            start.countDown();
            for (Future<Integer> result : results) {
                assertEquals(Integer.valueOf(200), result.get());
            }
            assertEquals(Integer.valueOf(1), jdbc.queryForObject(
                    "SELECT count(*) FROM clinlims.test_sample_handling WHERE test_id = ?", Integer.class, TEST_ID));
        } finally {
            pool.shutdownNow();
        }
    }
}
