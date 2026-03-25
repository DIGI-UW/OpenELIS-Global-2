package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerType;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerBidirectionalServiceImplTest {

    @Mock
    private AnalyzerService analyzerService;

    @Mock
    private SampleService sampleService;

    @Mock
    private SampleHumanService sampleHumanService;

    @Mock
    private AnalysisService analysisService;

    @Mock
    private AnalyzerTestMappingService analyzerTestMappingService;

    @Mock
    private AnalyzerBridgeTransportService bridgeTransportService;

    private AnalyzerBidirectionalServiceImpl service;

    @Before
    public void setUp() {
        service = new AnalyzerBidirectionalServiceImpl(analyzerService, sampleService, sampleHumanService,
                analysisService, analyzerTestMappingService, bridgeTransportService);
    }

    @Test
    public void sendOrder_WithMappedTests_BuildsAndSendsOrderMessage() {
        Analyzer analyzer = analyzer("2006", "atype-1");
        Sample sample = sample("sample-1", "ACC-12345");
        Patient patient = patient("PAT-9", "Doe", "Jane");
        Analysis analysis = analysis("test-1");
        AnalyzerTestMapping mapping = mapping("atype-1", "test-1", "MTB-RIF");

        when(analyzerService.get("2006")).thenReturn(analyzer);
        when(sampleService.getSampleByAccessionNumber("ACC-12345")).thenReturn(sample);
        when(sampleHumanService.getPatientForSample(sample)).thenReturn(patient);
        when(analysisService.getAnalysesBySampleId("sample-1")).thenReturn(Collections.singletonList(analysis));
        when(analyzerTestMappingService.getAll()).thenReturn(Collections.singletonList(mapping));
        when(bridgeTransportService.sendMessage(eq(analyzer), any(String.class))).thenReturn("ACK");

        Map<String, Object> response = service.sendOrder("2006", "ACC-12345");

        assertTrue((Boolean) response.get("success"));
        assertEquals(1, response.get("orderCount"));

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(bridgeTransportService).sendMessage(eq(analyzer), messageCaptor.capture());
        String message = messageCaptor.getValue();
        assertTrue(message.contains("H|\\^&|||OpenELIS^OrderSend^1.0"));
        assertTrue(message.contains("P|1|PAT-9||Doe^Jane"));
        assertTrue(message.contains("O|1|ACC-12345||^^^MTB-RIF|R|"));
        assertTrue(message.endsWith("L|1|N\r\n"));
    }

    @Test(expected = LIMSRuntimeException.class)
    public void sendOrder_WithoutMappings_ThrowsValidationError() {
        Analyzer analyzer = analyzer("2006", "atype-1");
        Sample sample = sample("sample-1", "ACC-00001");
        Analysis analysis = analysis("test-1");

        when(analyzerService.get("2006")).thenReturn(analyzer);
        when(sampleService.getSampleByAccessionNumber("ACC-00001")).thenReturn(sample);
        when(analysisService.getAnalysesBySampleId("sample-1")).thenReturn(Collections.singletonList(analysis));
        when(analyzerTestMappingService.getAll()).thenReturn(Collections.emptyList());

        service.sendOrder("2006", "ACC-00001");
    }

    @Test(expected = LIMSRuntimeException.class)
    public void sendOrder_WithBlankMappedCode_ThrowsValidationError() {
        Analyzer analyzer = analyzer("2006", "atype-1");
        Sample sample = sample("sample-1", "ACC-00002");
        Analysis analysis = analysis("test-1");
        AnalyzerTestMapping blankMapping = mapping("atype-1", "test-1", "   ");

        when(analyzerService.get("2006")).thenReturn(analyzer);
        when(sampleService.getSampleByAccessionNumber("ACC-00002")).thenReturn(sample);
        when(analysisService.getAnalysesBySampleId("sample-1")).thenReturn(Collections.singletonList(analysis));
        when(analyzerTestMappingService.getAll()).thenReturn(Collections.singletonList(blankMapping));

        service.sendOrder("2006", "ACC-00002");
    }

    @Test(expected = LIMSRuntimeException.class)
    public void sendOrder_WithNullMappings_ThrowsValidationError() {
        Analyzer analyzer = analyzer("2006", "atype-1");
        Sample sample = sample("sample-1", "ACC-00003");
        Analysis analysis = analysis("test-1");

        when(analyzerService.get("2006")).thenReturn(analyzer);
        when(sampleService.getSampleByAccessionNumber("ACC-00003")).thenReturn(sample);
        when(analysisService.getAnalysesBySampleId("sample-1")).thenReturn(Collections.singletonList(analysis));
        when(analyzerTestMappingService.getAll()).thenReturn(null);

        service.sendOrder("2006", "ACC-00003");
    }

    @Test
    public void queryResults_WithEmptyBridgeResponse_ReturnsZeroImportedCount() {
        Analyzer analyzer = analyzer("2006", "atype-1");
        when(analyzerService.get("2006")).thenReturn(analyzer);
        when(bridgeTransportService.sendMessage(eq(analyzer), any(String.class))).thenReturn("");

        Map<String, Object> response = service.queryResults("2006", "ACC-XYZ", Collections.singletonList("GLUCOSE"),
                "42");

        assertTrue((Boolean) response.get("success"));
        assertEquals(0, response.get("importedResultCount"));

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(bridgeTransportService).sendMessage(eq(analyzer), messageCaptor.capture());
        String message = messageCaptor.getValue();
        assertTrue(message.contains("H|\\^&|||OpenELIS^ResultsQuery^1.0"));
        assertTrue(message.contains("Q|1|ACC-XYZ||GLUCOSE"));
    }

    @Test
    public void queryResults_WithBlankTestCodes_FallsBackToAll() {
        Analyzer analyzer = analyzer("2006", "atype-1");
        when(analyzerService.get("2006")).thenReturn(analyzer);
        when(bridgeTransportService.sendMessage(eq(analyzer), any(String.class))).thenReturn("");

        service.queryResults("2006", "ACC-XYZ", java.util.Arrays.asList(" ", ""), "42");

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(bridgeTransportService).sendMessage(eq(analyzer), messageCaptor.capture());
        String message = messageCaptor.getValue();
        assertTrue(message.contains("Q|1|ACC-XYZ||ALL"));
    }

    private Analyzer analyzer(String id, String analyzerTypeId) {
        Analyzer analyzer = new Analyzer();
        analyzer.setId(id);
        analyzer.setIpAddress("127.0.0.1");
        analyzer.setPort(12000);
        AnalyzerType type = new AnalyzerType();
        type.setId(analyzerTypeId);
        analyzer.setAnalyzerType(type);
        return analyzer;
    }

    private Sample sample(String id, String accession) {
        Sample sample = new Sample();
        sample.setId(id);
        sample.setAccessionNumber(accession);
        return sample;
    }

    private Patient patient(String nationalId, String lastName, String firstName) {
        Patient patient = new Patient();
        patient.setNationalId(nationalId);
        Person person = new Person();
        person.setLastName(lastName);
        person.setFirstName(firstName);
        patient.setPerson(person);
        return patient;
    }

    private Analysis analysis(String testId) {
        Analysis analysis = new Analysis();
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId(testId);
        analysis.setTest(test);
        return analysis;
    }

    private AnalyzerTestMapping mapping(String analyzerTypeId, String testId, String analyzerCode) {
        AnalyzerTestMapping mapping = new AnalyzerTestMapping();
        mapping.setAnalyzerTypeId(analyzerTypeId);
        mapping.setTestId(testId);
        mapping.setAnalyzerTestName(analyzerCode);
        return mapping;
    }
}
