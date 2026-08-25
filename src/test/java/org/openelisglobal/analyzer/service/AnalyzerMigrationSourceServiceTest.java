package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.CommunicationMode;
import org.openelisglobal.analyzer.valueholder.FlowControl;
import org.openelisglobal.analyzer.valueholder.Parity;
import org.openelisglobal.analyzer.valueholder.ProtocolVersion;
import org.openelisglobal.analyzer.valueholder.SerialPortConfiguration;
import org.openelisglobal.analyzer.valueholder.StopBits;

public class AnalyzerMigrationSourceServiceTest {

    @Test
    public void exportsFiniteReleasedConnectionFieldsWithoutSelectingAProfile() {
        AnalyzerService analyzers = mock(AnalyzerService.class);
        SerialPortService serialPorts = mock(SerialPortService.class);
        Analyzer fileAnalyzer = analyzer("43", "File analyzer");
        fileAnalyzer.setImportDirectory("/srv/analyzers/file");
        fileAnalyzer.setFilePattern("*.csv");
        fileAnalyzer.setFileFormat("CSV");
        fileAnalyzer.setDelimiter(",");
        fileAnalyzer.setHasHeader(true);
        fileAnalyzer.setSkipRows(1);
        fileAnalyzer.setColumnMappingsJson("{\"sampleId\":\"A\",\"result\":\"C\"}");

        Analyzer serialAnalyzer = analyzer("42", "GeneXpert");
        serialAnalyzer.setProtocolVersion(ProtocolVersion.ASTM_LIS2_A2);
        serialAnalyzer.setCommunicationMode(CommunicationMode.BOTH);
        serialAnalyzer.setIdentifierPattern("GENEXPERT|CEPHEID");
        SerialPortConfiguration serial = new SerialPortConfiguration();
        serial.setPortName("/dev/ttyUSB0");
        serial.setBaudRate(9600);
        serial.setDataBits(8);
        serial.setStopBits(StopBits.ONE);
        serial.setParity(Parity.NONE);
        serial.setFlowControl(FlowControl.NONE);
        serial.setActive(true);

        when(analyzers.getAllWithTypes()).thenReturn(List.of(fileAnalyzer, serialAnalyzer));
        when(serialPorts.getByAnalyzerId(42)).thenReturn(Optional.of(serial));
        when(serialPorts.getByAnalyzerId(43)).thenReturn(Optional.empty());

        ObjectNode snapshot = new AnalyzerMigrationSourceService(analyzers, serialPorts).snapshot();

        assertEquals(2, snapshot.path("analyzers").size());
        JsonNode exportedSerial = snapshot.path("analyzers").path(0);
        assertEquals("42", exportedSerial.path("sourceAnalyzerId").asText());
        assertEquals("GeneXpert", exportedSerial.path("displayName").asText());
        assertEquals("ASTM_LIS2_A2", exportedSerial.path("configuration").path("protocolVersion").asText());
        assertEquals("BOTH", exportedSerial.path("configuration").path("communicationMode").asText());
        assertEquals("/dev/ttyUSB0", exportedSerial.path("configuration").path("portName").asText());
        assertEquals(9600, exportedSerial.path("configuration").path("baudRate").asInt());
        assertEquals("ONE", exportedSerial.path("configuration").path("stopBits").asText());
        assertFalse(exportedSerial.has("profileId"));
        assertFalse(exportedSerial.path("configuration").has("active"));
        assertTrue(exportedSerial.path("sourceConfigFingerprint").asText().matches("sha256:[0-9a-f]{64}"));

        JsonNode exportedFile = snapshot.path("analyzers").path(1);
        assertEquals("43", exportedFile.path("sourceAnalyzerId").asText());
        assertEquals("A", exportedFile.path("configuration").path("columnMappings").path("sampleId").asText());
        assertTrue(exportedFile.path("sourceErrors").isEmpty());
    }

    @Test
    public void reportsMalformedReleasedColumnMappingsWithoutDroppingTheAnalyzer() {
        AnalyzerService analyzers = mock(AnalyzerService.class);
        SerialPortService serialPorts = mock(SerialPortService.class);
        Analyzer analyzer = analyzer("42", "Malformed file analyzer");
        analyzer.setColumnMappingsJson("not-json");
        when(analyzers.getAllWithTypes()).thenReturn(List.of(analyzer));
        when(serialPorts.getByAnalyzerId(42)).thenReturn(Optional.empty());

        ObjectNode snapshot = new AnalyzerMigrationSourceService(analyzers, serialPorts).snapshot();

        assertEquals(1, snapshot.path("analyzers").size());
        assertEquals("COLUMN_MAPPINGS_INVALID_JSON",
                snapshot.path("analyzers").path(0).path("sourceErrors").path(0).asText());
        assertFalse(snapshot.path("analyzers").path(0).path("configuration").has("columnMappings"));
    }

    private static Analyzer analyzer(String id, String name) {
        Analyzer analyzer = new Analyzer();
        analyzer.setId(id);
        analyzer.setName(name);
        analyzer.setStatus(Analyzer.AnalyzerStatus.SETUP);
        return analyzer;
    }
}
