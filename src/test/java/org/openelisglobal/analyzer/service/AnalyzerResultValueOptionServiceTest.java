package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.form.AnalyzerResultValueOption;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerResultValueOptionServiceTest {

    @Mock
    private AnalyzerTestMappingService analyzerTestMappingService;

    @Mock
    private TestResultComponentService componentService;

    @Mock
    private TestResultService testResultService;

    @Mock
    private DictionaryService dictionaryService;

    @InjectMocks
    private AnalyzerResultValueOptionServiceImpl service;

    private AnalyzerTestMapping mapping;
    private TestResultComponent primary;
    private TestResult option;

    @Before
    public void setUp() {
        mapping = new AnalyzerTestMapping();
        mapping.setAnalyzerId("101");
        mapping.setAnalyzerTestName("MTB");
        mapping.setTestId("501");

        primary = new TestResultComponent();
        primary.setId("component-primary");
        primary.setTestId("501");
        primary.setCode("PRIMARY");
        primary.setLabel("Result");
        primary.setIsPrimary(true);

        option = new TestResult();
        option.setId("result-option-1");
        option.setValue("9001");
        option.setTestResultType("D");
        option.setSortOrder("1");
        option.setIsNormal(false);
        option.setIsActive(true);

        when(analyzerTestMappingService.getAllForAnalyzer("101")).thenReturn(List.of(mapping));
        when(componentService.getActiveComponentsByTestId("501")).thenReturn(List.of(primary));
        when(testResultService.getActiveOptionsByComponentId("component-primary")).thenReturn(List.of(option));

        Dictionary dictionary = new Dictionary();
        dictionary.setId("9001");
        dictionary.setDictEntry("Detected");
        when(dictionaryService.getDictionaryById("9001")).thenReturn(dictionary);
    }

    @Test
    public void getOptions_UsesMappedTestsPrimaryComponentAndReturnsStableIds() {
        List<AnalyzerResultValueOption> options = service.getOptions("101", "MTB");

        assertEquals(1, options.size());
        assertEquals("result-option-1", options.get(0).getId());
        assertEquals("9001", options.get(0).getValue());
        assertEquals("Detected", options.get(0).getLabel());
        assertEquals("501", options.get(0).getTestId());
        assertEquals("Detected", options.get(0).getLabel());
    }

    @Test
    public void getOptions_UsesExplicitMappedComponentWhenPresent() {
        mapping.setComponentId("component-explicit");
        TestResultComponent explicit = new TestResultComponent();
        explicit.setId("component-explicit");
        explicit.setTestId("501");
        explicit.setCode("TARGET");
        explicit.setLabel("Target");
        explicit.setIsPrimary(false);
        when(componentService.getActiveComponentsByTestId("501")).thenReturn(List.of(primary, explicit));
        when(testResultService.getActiveOptionsByComponentId("component-explicit")).thenReturn(List.of(option));

        List<AnalyzerResultValueOption> options = service.getOptions("101", "MTB");

        assertEquals("Detected", options.get(0).getLabel());
    }

    @Test
    public void requireValidOption_RejectsOptionOutsideMappedTest() {
        assertThrows(IllegalArgumentException.class,
                () -> service.requireValidOption("101", "MTB", "result-option-other"));
    }

    @Test
    public void getOptions_RejectsUnknownAnalyzerTestCode() {
        assertThrows(IllegalArgumentException.class, () -> service.getOptions("101", "UNKNOWN"));
    }

    @Test
    public void findOptions_ReturnsEmptyWhenAnalyzerTestCodeIsNotMapped() {
        assertEquals(List.of(), service.findOptions("101", "UNKNOWN"));
    }
}
