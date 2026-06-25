package org.openelisglobal.testResult;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testresult.service.TestResultConfigurationHandler;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for the test-result catalog loader's new {@code significance}
 * column (POSITIVE / NEGATIVE / INDETERMINATE) -- the surveillance
 * classification that the V-04 vector dashboard reads instead of the (inverted)
 * deconvolutionStatus proxy.
 *
 * <p>
 * Level rationale: the only feature logic added to the handler is "find the
 * significance column by name, normalize case, and map it onto the TestResult
 * before persisting (on BOTH the create and update paths)." That is a pure
 * parse-and-map concern, fully exercised at the unit level by mocking the
 * persistence services and capturing the entity actually handed to
 * insert()/update() -- we assert on the field VALUE of the captured object, not
 * merely that a method was called (no assert-on-mock-return theater). The HBM
 * mapping itself (a plain {@code type="java.lang.String"} column) is Hibernate
 * boilerplate; a DB round-trip would exercise Hibernate, not this code, so it
 * is not the right level for the primary guard.
 *
 * <p>
 * Convention follows {@code AddressHierarchyConfigurationHandlerMetadataTest}
 * (sibling config-handler unit test): swap the {@code DisplayListService}
 * static singleton with a mock and inject service mocks via
 * {@link ReflectionTestUtils}, then assert on captured entity field values.
 * Placed in the same package as the sibling {@code TestResultServiceTest}; the
 * production handler under test lives in the lowercase {@code testresult}
 * package (imported below).
 */
public class TestResultConfigurationHandlerSignificanceTest {

    private DisplayListService previousDisplayListService;
    private TestService testService;
    private TestResultService testResultService;
    private DictionaryService dictionaryService;
    private TestResultConfigurationHandler handler;

    // Fully qualified: org.junit.Test (the annotation) already owns the simple
    // name.
    private org.openelisglobal.test.valueholder.Test malariaTest;

    @Before
    public void setUp() {
        previousDisplayListService = (DisplayListService) ReflectionTestUtils.getField(DisplayListService.class,
                "instance");
        ReflectionTestUtils.setField(DisplayListService.class, "instance", mock(DisplayListService.class));

        testService = mock(TestService.class);
        testResultService = mock(TestResultService.class);
        dictionaryService = mock(DictionaryService.class);

        handler = new TestResultConfigurationHandler();
        ReflectionTestUtils.setField(handler, "testService", testService);
        ReflectionTestUtils.setField(handler, "testResultService", testResultService);
        ReflectionTestUtils.setField(handler, "dictionaryService", dictionaryService);

        malariaTest = new org.openelisglobal.test.valueholder.Test();
        malariaTest.setId("42");
        malariaTest.setDescription("Plasmodium CSP-ELISA");

        // findTestsByName(): exact match by localized name short-circuits.
        when(testService.getTestByLocalizedName("Plasmodium CSP-ELISA")).thenReturn(malariaTest);
        when(testResultService.insert(any(TestResult.class))).thenReturn("100");
    }

    @After
    public void tearDown() {
        ReflectionTestUtils.setField(DisplayListService.class, "instance", previousDisplayListService);
    }

    private InputStream csv(String body) {
        return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
    }

    private ArgumentCaptor<TestResult> insertCaptor() {
        return ArgumentCaptor.forClass(TestResult.class);
    }

    // --- CREATE path: significance is parsed, case-normalized, and set on the
    // entity ---

    @Test
    public void create_setsSignificanceFromColumn_uppercased() throws Exception {
        // Lower-case in the file to prove the handler normalizes to canonical form.
        String body = "testName,resultType,resultValue,significance\n" + "Plasmodium CSP-ELISA,A,reactive,positive\n";
        // No pre-existing result -> create path.
        when(testResultService.getActiveTestResultsByTest("42")).thenReturn(Collections.emptyList());

        handler.processConfiguration(csv(body), "test-results.csv");

        ArgumentCaptor<TestResult> captor = insertCaptor();
        verify(testResultService, times(1)).insert(captor.capture());
        assertEquals("the catalog significance must be carried onto the persisted TestResult", "POSITIVE",
                captor.getValue().getSignificance());
    }

    // --- UPDATE path: significance lands on an already-existing result too ---

    @Test
    public void update_setsSignificanceOnExistingResult() throws Exception {
        String body = "testName,resultType,resultValue,significance\n" + "Plasmodium CSP-ELISA,A,reactive,NEGATIVE\n";

        TestResult existing = new TestResult();
        existing.setId("777");
        existing.setTestResultType("A");
        existing.setValue("reactive");
        when(testResultService.getActiveTestResultsByTest("42")).thenReturn(List.of(existing));
        when(testResultService.update(any(TestResult.class))).thenReturn(existing);

        handler.processConfiguration(csv(body), "test-results.csv");

        // The handler mutates the SAME existing instance, then calls update() on it.
        assertEquals("NEGATIVE", existing.getSignificance());
        verify(testResultService, times(1)).update(existing);
        verify(testResultService, times(0)).insert(any(TestResult.class));
    }

    // C5: a present-but-blank significance cell CLEARS a prior classification on
    // the
    // update path (red on the old code, which only set significance when non-blank
    // and so could never unset it).
    @Test
    public void update_blankSignificanceCell_clearsExistingSignificance() throws Exception {
        String body = "testName,resultType,resultValue,significance\n" + "Plasmodium CSP-ELISA,A,reactive,\n";

        TestResult existing = new TestResult();
        existing.setId("777");
        existing.setTestResultType("A");
        existing.setValue("reactive");
        existing.setSignificance("POSITIVE");
        when(testResultService.getActiveTestResultsByTest("42")).thenReturn(List.of(existing));
        when(testResultService.update(any(TestResult.class))).thenReturn(existing);

        handler.processConfiguration(csv(body), "test-results.csv");

        assertNull("a blank significance cell (column present) must clear the prior classification",
                existing.getSignificance());
        verify(testResultService, times(1)).update(existing);
    }

    // --- Degradation guard: no significance column -> null, loader must not break.
    // Encodes "missing catalog metadata must NOT break OE (develop without the
    // SILNAS catalog)": legacy catalogs have no significance column at all.

    @Test
    public void create_noSignificanceColumn_leavesSignificanceNull_andStillLoads() throws Exception {
        String body = "testName,resultType,resultValue\n" + "Plasmodium CSP-ELISA,A,reactive\n";
        when(testResultService.getActiveTestResultsByTest("42")).thenReturn(Collections.emptyList());

        handler.processConfiguration(csv(body), "test-results.csv");

        ArgumentCaptor<TestResult> captor = insertCaptor();
        verify(testResultService, times(1)).insert(captor.capture());
        assertNull("absent significance column must leave significance null (degrades to 'not configured'), not blank",
                captor.getValue().getSignificance());
    }

    // --- Empty significance cell -> null (do not store an empty string) ---

    @Test
    public void create_emptySignificanceCell_leavesNull() throws Exception {
        String body = "testName,resultType,resultValue,significance\n" + "Plasmodium CSP-ELISA,A,reactive,\n";
        when(testResultService.getActiveTestResultsByTest("42")).thenReturn(Collections.emptyList());

        handler.processConfiguration(csv(body), "test-results.csv");

        ArgumentCaptor<TestResult> captor = insertCaptor();
        verify(testResultService, times(1)).insert(captor.capture());
        assertNull(captor.getValue().getSignificance());
    }
}
