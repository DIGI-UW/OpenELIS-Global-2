package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.testterminology.service.TestTerminologyMappingService;
import org.openelisglobal.testterminology.valueholder.TestTerminologyMapping;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerMappingCatalogServiceTest {

    @Mock
    private TestService testService;
    @Mock
    private TestResultService testResultService;
    @Mock
    private TestTerminologyMappingService terminologyService;
    @Mock
    private DictionaryService dictionaryService;

    private AnalyzerMappingCatalogService service;

    @Before
    public void setUp() {
        service = new AnalyzerMappingCatalogServiceImpl(testService, testResultService, terminologyService,
                dictionaryService);
    }

    @Test
    public void searchActiveTestsUsesCompleteNameCodeAndLoincCatalog() {
        org.openelisglobal.test.valueholder.Test hiv = test("1", "HIV Viral Load", "HIVVL", "25836-8", true);
        org.openelisglobal.test.valueholder.Test wbc = test("2", "White blood cell count", "WBC", null, true);
        org.openelisglobal.test.valueholder.Test inactive = test("3", "Retired test", "OLD", "1111-1", false);
        when(testService.getAllActiveTests(false)).thenReturn(List.of(hiv, wbc, inactive));
        when(terminologyService.getActiveBySource("LOINC"))
                .thenReturn(List.of(loinc("2", "6690-2"), loinc("3", "1111-1")));

        assertEquals(List.of("1", "2"), ids(service.searchActiveTests(null)));
        assertEquals(List.of("1"), ids(service.searchActiveTests("viral")));
        assertEquals(List.of("2"), ids(service.searchActiveTests("wbc")));
        assertEquals(List.of("1"), ids(service.searchActiveTests("25836-8")));
        assertEquals(List.of("2"), ids(service.searchActiveTests("6690")));
        assertEquals(List.of(), ids(service.searchActiveTests("1111-1")));
        assertEquals(List.of("25836-8"), service.searchActiveTests("HIVVL").get(0).loincCodes());
        assertEquals(List.of("6690-2"), service.searchActiveTests("WBC").get(0).loincCodes());
    }

    @Test
    public void activeResultOptionsDeriveValuesAndLabelsAndExcludeInvalidRows() {
        org.openelisglobal.test.valueholder.Test mapped = test("1", "HIV", "HIV", null, true);
        org.openelisglobal.test.valueholder.Test other = test("2", "Other", "OTHER", null, true);
        TestResult dictionaryOption = option("11", mapped, "501", true);
        TestResult textOption = option("12", mapped, "Not detected", true);
        TestResult inactiveOption = option("13", mapped, "Inactive", false);
        TestResult foreignOption = option("14", other, "Foreign", true);
        TestResult numericRow = option("15", mapped, "5.2", true);
        numericRow.setTestResultType("N");
        Dictionary dictionary = new Dictionary();
        dictionary.setId("501");
        dictionary.setDictEntry("Detected");
        when(testService.get("1")).thenReturn(mapped);
        when(testResultService.getActiveTestResultsByTest("1"))
                .thenReturn(List.of(textOption, inactiveOption, foreignOption, numericRow, dictionaryOption));
        when(dictionaryService.getDictionaryById("501")).thenReturn(dictionary);

        List<AnalyzerMappingCatalogService.ResultOption> options = service.getActiveResultOptions("1");

        assertEquals(List.of("11", "12"),
                options.stream().map(AnalyzerMappingCatalogService.ResultOption::id).toList());
        assertEquals("501", options.get(0).value());
        assertEquals("Detected", options.get(0).label());
        assertEquals("Not detected", options.get(1).value());
        assertEquals("Not detected", options.get(1).label());
    }

    @Test
    public void resultOptionsRejectInactiveTest() {
        when(testService.get("3")).thenReturn(test("3", "Retired", "OLD", null, false));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.getActiveResultOptions("3"));

        assertEquals("Result Options require an active Test", error.getMessage());
    }

    private static List<String> ids(List<AnalyzerMappingCatalogService.TestOption> options) {
        return options.stream().map(AnalyzerMappingCatalogService.TestOption::id).toList();
    }

    private static org.openelisglobal.test.valueholder.Test test(String id, String name, String code, String loinc,
            boolean active) {
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId(id);
        test.setDescription(name);
        test.setLocalCode(code);
        test.setLoinc(loinc);
        test.setIsActive(active ? "Y" : "N");
        return test;
    }

    private static TestTerminologyMapping loinc(String testId, String code) {
        TestTerminologyMapping mapping = new TestTerminologyMapping();
        mapping.setTestId(testId);
        mapping.setSource("LOINC");
        mapping.setCode(code);
        mapping.setIsActive("Y");
        return mapping;
    }

    private static TestResult option(String id, org.openelisglobal.test.valueholder.Test test, String value,
            boolean active) {
        TestResult result = new TestResult();
        result.setId(id);
        result.setTest(test);
        result.setValue(value);
        result.setIsActive(active);
        result.setTestResultType("D");
        return result;
    }
}
