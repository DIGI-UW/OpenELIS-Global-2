package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.ProtocolVersion;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerBridgeSyncServiceTest {

    @Mock
    private AnalyzerService analyzerService;

    @Mock
    private AnalyzerTestMappingService analyzerTestMappingService;

    @Mock
    private BridgeRegistrationService bridgeRegistrationService;

    @InjectMocks
    private AnalyzerBridgeSyncService analyzerBridgeSyncService;

    private Analyzer analyzer;

    @Before
    public void setUp() {
        analyzer = new Analyzer();
        analyzer.setId("101");
        analyzer.setName("GeneXpert");
    }

    @Test
    public void pushAnalyzerTcp_ReregistersCurrentAnalyzerContract() {
        analyzer.setIpAddress("172.21.1.100");
        analyzer.setPort(5380);
        analyzer.setProtocolVersion(ProtocolVersion.HL7_V2_3_1);
        analyzer.setIdentifierPattern("GENEXPERT");
        when(analyzerService.get("101")).thenReturn(analyzer);

        analyzerBridgeSyncService.pushAnalyzer("101");

        ArgumentCaptor<String> protocol = ArgumentCaptor.forClass(String.class);
        verify(bridgeRegistrationService).registerTcp(eq("101"), eq("GeneXpert"), eq("172.21.1.100"), eq(5380),
                protocol.capture(), eq("GENEXPERT"));
        assertEquals("HL7", protocol.getValue());
    }

    @Test
    public void pushAnalyzerFile_ReregistersCurrentMappingsAndFileContract() {
        analyzer.setImportDirectory("/data/in");
        analyzer.setFilePattern("*.csv");
        analyzer.setColumnMappings(Map.of("test", "code"));
        analyzer.setFileFormat("CSV");
        analyzer.setDelimiter(",");
        analyzer.setSkipRows(1);
        AnalyzerTestMapping mtb = mapping("MTB");
        AnalyzerTestMapping duplicateMtb = mapping("MTB");
        AnalyzerTestMapping rif = mapping("RIF");
        when(analyzerService.get("101")).thenReturn(analyzer);
        when(analyzerTestMappingService.getAllForAnalyzer("101")).thenReturn(List.of(mtb, duplicateMtb, rif));

        analyzerBridgeSyncService.pushAnalyzer("101");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> mappingNames = ArgumentCaptor.forClass(List.class);
        verify(bridgeRegistrationService).registerFile(eq("101"), eq("GeneXpert"), eq("/data/in"), eq("*.csv"),
                eq(Map.of("test", "code")), eq("CSV"), eq(","), eq(1), mappingNames.capture());
        assertEquals(List.of("MTB", "RIF"), mappingNames.getValue());
    }

    private AnalyzerTestMapping mapping(String analyzerTestName) {
        AnalyzerTestMapping mapping = new AnalyzerTestMapping();
        mapping.setAnalyzerId("101");
        mapping.setAnalyzerTestName(analyzerTestName);
        return mapping;
    }
}
