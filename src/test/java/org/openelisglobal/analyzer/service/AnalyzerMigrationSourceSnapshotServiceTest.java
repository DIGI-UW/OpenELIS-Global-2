package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;

public class AnalyzerMigrationSourceSnapshotServiceTest {

    @Test
    public void snapshotIsStableUntilReleasedAnalyzerConfigurationChanges() {
        AnalyzerService analyzerService = mock(AnalyzerService.class);
        AnalyzerPluginConfigService pluginConfigService = mock(AnalyzerPluginConfigService.class);
        AnalyzerTestMappingService testMappingService = mock(AnalyzerTestMappingService.class);
        AnalyzerFieldService fieldService = mock(AnalyzerFieldService.class);
        Analyzer analyzer = new Analyzer();
        analyzer.setId("42");
        analyzer.setName("Released analyzer");
        when(analyzerService.getAllWithTypes()).thenReturn(List.of(analyzer));
        when(pluginConfigService.getConfigAsMap("42")).thenReturn(Map.of("host", "10.0.0.4"));
        when(testMappingService.getAllForAnalyzer("42")).thenReturn(List.of());
        when(fieldService.getFieldsByAnalyzerId("42")).thenReturn(List.of());
        AnalyzerMigrationSourceSnapshotServiceImpl service = new AnalyzerMigrationSourceSnapshotServiceImpl(
                analyzerService, pluginConfigService, testMappingService, fieldService);

        AnalyzerMigrationSourceSnapshot first = service.snapshot();
        AnalyzerMigrationSourceSnapshot unchanged = service.snapshot();
        when(pluginConfigService.getConfigAsMap("42")).thenReturn(Map.of("host", "10.0.0.5"));
        AnalyzerMigrationSourceSnapshot changed = service.snapshot();

        assertEquals(first, unchanged);
        assertNotEquals(first.fingerprint(), changed.fingerprint());
        assertNotEquals(first.analyzers().get(0).fingerprint(), changed.analyzers().get(0).fingerprint());
    }
}
