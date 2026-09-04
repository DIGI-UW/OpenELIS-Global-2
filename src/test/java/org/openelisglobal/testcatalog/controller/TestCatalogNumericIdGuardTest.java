package org.openelisglobal.testcatalog.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.TreeSet;
import org.junit.Before;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.config.ControllerSetup;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testactivation.service.TestActivationAcknowledgmentService;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogActivationRestController;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogNumericIdGuard;
import org.openelisglobal.testcatalog.controller.rest.TestReflexCalcRestController;
import org.openelisglobal.testcatalog.controller.rest.TestStorageHistoryRestController;
import org.openelisglobal.testcatalog.service.RangeCoverageValidationService;
import org.openelisglobal.testcatalog.service.ReflexCalcViewService;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultinterpretation.service.TestResultInterpretationService;
import org.openelisglobal.testsamplehandling.service.TestSampleHandlingHistoryService;
import org.openelisglobal.testsamplehandling.service.TestSampleHandlingService;
import org.openelisglobal.testterminology.service.TestTerminologyMappingService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OGC-1153 defect 4 — a non-numeric entity id in a Test Catalog URL must answer
 * 404 (the same answer a well-formed but absent id gives), never 500.
 *
 * <p>
 * The stubbed {@link TestService} reproduces the production failure exactly
 * rather than returning null for everything: catalog ids bind through
 * {@code LIMSStringNumberUserType}, whose {@code nullSafeSet} is a bare
 * {@code Integer.parseInt}, so the stub runs that same parse. That gives the
 * three distinct outcomes QA saw — {@code "notanumber"} throws
 * {@code NumberFormatException} out of the DAO, {@code "-1"} parses fine and
 * finds no row, {@code "999999"} likewise finds nothing. The real
 * {@link ControllerSetup} is registered as advice because it is what turned the
 * exception into the reported 500 — its {@code @Order(HIGHEST_PRECEDENCE)}
 * {@code RuntimeException} handler. Drop {@link TestCatalogNumericIdGuard} from
 * the chain and the six {@code nonNumericTestId_*} cases plus
 * {@link #overflowingTestId_returns404} all go back to 500.
 */
public class TestCatalogNumericIdGuardTest {

    private static final String EXISTING_TEST_ID = "5";

    private static final String ABSENT_TEST_ID = "999999";

    /**
     * Path variables in this URL space that are deliberately NOT numeric:
     * {@code test_alert_rule.id} is a 36-char UUID and {@code reagentId} is typed
     * {@code Long} on its handler, so Spring's own conversion answers 400 before
     * any DAO is reached. Everything else must be classified — see
     * {@link #everyCatalogPathVariableIsClassified}.
     */
    private static final Set<String> KNOWN_NON_NUMERIC_PATH_VARIABLES = Set.of("ruleId", "reagentId");

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        Test existing = new Test();
        existing.setId(EXISTING_TEST_ID);
        existing.setDescription("GuardIT");
        existing.setIsActive("Y");

        TestService testService = mock(TestService.class);
        when(testService.getTestById(anyString())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            Integer.parseInt(id);
            return EXISTING_TEST_ID.equals(id) ? existing : null;
        });

        TestSampleHandlingService handlingService = mock(TestSampleHandlingService.class);
        TestCatalogEditorRestController editor = new TestCatalogEditorRestController(testService,
                mock(TestResultComponentService.class), mock(TestResultInterpretationService.class),
                mock(TestResultService.class), mock(ResultLimitService.class),
                mock(RangeCoverageValidationService.class), handlingService, mock(AnalyzerService.class),
                mock(TypeOfSampleService.class), mock(TypeOfSampleTestService.class),
                mock(TestTerminologyMappingService.class), mock(PanelService.class), mock(PanelItemService.class));
        TestCatalogActivationRestController activation = new TestCatalogActivationRestController(testService,
                mock(ResultLimitService.class), mock(RangeCoverageValidationService.class),
                mock(TestActivationAcknowledgmentService.class), mock(TestResultComponentService.class),
                mock(TestResultService.class));
        TestReflexCalcRestController reflexCalc = new TestReflexCalcRestController(mock(ReflexCalcViewService.class),
                testService);
        TestStorageHistoryRestController storageHistory = new TestStorageHistoryRestController(handlingService,
                mock(TestSampleHandlingHistoryService.class), testService);

        mockMvc = MockMvcBuilders.standaloneSetup(editor, activation, reflexCalc, storageHistory)
                .addMappedInterceptors(new String[] { TestCatalogNumericIdGuard.GUARDED_PATH_PATTERN },
                        new TestCatalogNumericIdGuard())
                .setControllerAdvice(new ControllerSetup()).build();
    }

    @org.junit.Test
    public void nonNumericTestId_basicInfo_returns404() throws Exception {
        mockMvc.perform(get("/rest/test-catalog/tests/notanumber/basic-info")).andExpect(status().isNotFound());
    }

    @org.junit.Test
    public void nonNumericTestId_sampleResults_returns404() throws Exception {
        mockMvc.perform(get("/rest/test-catalog/tests/notanumber/sample-results")).andExpect(status().isNotFound());
    }

    @org.junit.Test
    public void nonNumericTestId_localization_returns404() throws Exception {
        mockMvc.perform(get("/rest/test-catalog/tests/abc/localization")).andExpect(status().isNotFound());
    }

    /**
     * A controller other than the editor: activation is its own @RestController.
     */
    @org.junit.Test
    public void nonNumericTestId_activate_returns404() throws Exception {
        mockMvc.perform(post("/rest/test-catalog/tests/notanumber/activate").contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isNotFound());
    }

    /** The reflex/calc controller carries {testId} on its class-level mapping. */
    @org.junit.Test
    public void nonNumericTestId_reflexCalc_returns404() throws Exception {
        mockMvc.perform(get("/rest/test-catalog/abc/reflex-calc")).andExpect(status().isNotFound());
    }

    @org.junit.Test
    public void nonNumericTestId_storageHistory_returns404() throws Exception {
        mockMvc.perform(get("/rest/test-catalog/abc/storage/history")).andExpect(status().isNotFound());
    }

    /** {@code -1} answered 404 before the guard existed; it still must. */
    @org.junit.Test
    public void negativeTestId_returns404() throws Exception {
        mockMvc.perform(get("/rest/test-catalog/tests/-1/basic-info")).andExpect(status().isNotFound());
    }

    /**
     * Digits that overflow the NUMERIC(10) id column are "no such test", not 500.
     */
    @org.junit.Test
    public void overflowingTestId_returns404() throws Exception {
        mockMvc.perform(get("/rest/test-catalog/tests/99999999999/basic-info")).andExpect(status().isNotFound());
    }

    @org.junit.Test
    public void numericButAbsentTestId_basicInfo_stillReturns404() throws Exception {
        mockMvc.perform(get("/rest/test-catalog/tests/" + ABSENT_TEST_ID + "/basic-info"))
                .andExpect(status().isNotFound());
    }

    @org.junit.Test
    public void numericButAbsentTestId_activate_stillReturns404() throws Exception {
        mockMvc.perform(post("/rest/test-catalog/tests/" + ABSENT_TEST_ID + "/activate")
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isNotFound());
    }

    @org.junit.Test
    public void validTestId_basicInfo_stillReturns200() throws Exception {
        mockMvc.perform(get("/rest/test-catalog/tests/" + EXISTING_TEST_ID + "/basic-info")).andExpect(status().isOk());
    }

    /**
     * The accept/reject boundary itself. A padded id is accepted because
     * {@code ControllerSetup}'s global {@code StringTrimmerEditor} trims it before
     * the handler sees it, so the guard must not reject a URL that resolves today;
     * everything the {@code NUMERIC(10)} id column cannot hold is rejected.
     */
    @org.junit.Test
    public void isNumericId_acceptsOnlyWhatTheIdColumnCanHold() {
        assertTrue(TestCatalogNumericIdGuard.isNumericId("5"));
        assertTrue(TestCatalogNumericIdGuard.isNumericId("2147483647"));
        assertTrue(TestCatalogNumericIdGuard.isNumericId(" 5 "));
        assertFalse(TestCatalogNumericIdGuard.isNumericId("notanumber"));
        assertFalse(TestCatalogNumericIdGuard.isNumericId("5abc"));
        assertFalse(TestCatalogNumericIdGuard.isNumericId("-1"));
        assertFalse(TestCatalogNumericIdGuard.isNumericId("2147483648"));
        assertFalse(TestCatalogNumericIdGuard.isNumericId("99999999999"));
        assertFalse(TestCatalogNumericIdGuard.isNumericId(""));
        assertFalse(TestCatalogNumericIdGuard.isNumericId(null));
    }

    @org.junit.Test
    public void validTestId_localization_stillReturns200() throws Exception {
        mockMvc.perform(get("/rest/test-catalog/tests/" + EXISTING_TEST_ID + "/localization"))
                .andExpect(status().isOk());
    }

    @org.junit.Test
    public void validTestId_storageHistory_stillReturns200() throws Exception {
        mockMvc.perform(get("/rest/test-catalog/" + EXISTING_TEST_ID + "/storage/history")).andExpect(status().isOk());
    }

    /**
     * The fail-safe net for endpoints added later: every path variable on every
     * {@code /rest/test-catalog} mapping — in any package, including the
     * {@code testreagentlink} / {@code testalertrule} controllers that share the
     * prefix — must be either guarded as numeric or explicitly known to be
     * non-numeric. A new endpoint with an unclassified id variable fails this test
     * instead of silently 500-ing in production.
     */
    @org.junit.Test
    public void everyCatalogPathVariableIsClassified() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        Set<String> unclassified = new TreeSet<>();
        Set<String> scanned = new TreeSet<>();
        for (BeanDefinition definition : scanner.findCandidateComponents("org.openelisglobal")) {
            Class<?> controller = ClassUtils.forName(definition.getBeanClassName(), getClass().getClassLoader());
            RequestMapping classMapping = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
            if (classMapping == null || !isCatalogMapping(classMapping)) {
                continue;
            }
            scanned.add(controller.getSimpleName());
            collectPathVariables(classMapping.value(), unclassified);
            for (Method method : controller.getDeclaredMethods()) {
                RequestMapping methodMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                if (methodMapping != null) {
                    collectPathVariables(methodMapping.value(), unclassified);
                }
            }
        }
        assertEquals("the classpath scan must find every controller on the guarded prefix, not pass vacuously",
                Set.of("TestAlertRuleRestController", "TestCatalogActivationRestController",
                        "TestCatalogEditorRestController", "TestReagentLinkRestController",
                        "TestReflexCalcRestController", "TestStorageHistoryRestController"),
                scanned);
        assertEquals(
                "unclassified /rest/test-catalog path variables — guard them in"
                        + " TestCatalogNumericIdGuard.NUMERIC_ID_PATH_VARIABLES or list them as known non-numeric",
                Set.of(), unclassified);
    }

    private static boolean isCatalogMapping(RequestMapping mapping) {
        for (String path : mapping.value()) {
            if (path.startsWith("/rest/test-catalog")) {
                return true;
            }
        }
        return false;
    }

    private static void collectPathVariables(String[] paths, Set<String> unclassified) {
        for (String path : paths) {
            int open = path.indexOf('{');
            while (open >= 0) {
                int close = path.indexOf('}', open);
                if (close < 0) {
                    return;
                }
                String name = path.substring(open + 1, close);
                int regex = name.indexOf(':');
                if (regex >= 0) {
                    name = name.substring(0, regex);
                }
                if (!TestCatalogNumericIdGuard.NUMERIC_ID_PATH_VARIABLES.contains(name)
                        && !KNOWN_NON_NUMERIC_PATH_VARIABLES.contains(name)) {
                    unclassified.add(name);
                }
                open = path.indexOf('{', close);
            }
        }
    }
}
