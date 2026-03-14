package org.openelisglobal.analyzerimport.analyzerreaders;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.openelisglobal.analyzer.valueholder.FileImportConfiguration;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.plugin.AnalyzerImporterPlugin;
import org.openelisglobal.spring.util.SpringContext;

/**
 * XML reader for FILE analyzers (e.g. DNA Technology DT-Prime).
 *
 * <p>
 * Parses package/plate/cell/test/result schema with windows-1251 encoding.
 * Produces tab-delimited lines compatible with AnalyzerLineInserter.
 */
public class XmlAnalyzerReader extends AnalyzerReader {

    private static final Charset DT_PRIME_ENCODING = Charset.forName("windows-1251");
    private static final List<String> PREFERRED_FIELD_ORDER = List.of("sampleId", "testCode", "result",
            "interpretation", "position");

    private final FileImportConfiguration configuration;
    private final List<String> lines = new ArrayList<>();
    private final List<Map<String, String>> parsedRecords = new ArrayList<>();
    private AnalyzerLineInserter inserter;
    private String error;

    public XmlAnalyzerReader(FileImportConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public boolean readStream(InputStream stream) {
        error = null;
        inserter = null;
        lines.clear();
        parsedRecords.clear();

        if (configuration == null) {
            error = "FileImportConfiguration not provided";
            return false;
        }

        try (InputStreamReader reader = new InputStreamReader(stream, DT_PRIME_ENCODING)) {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.IS_COALESCING, true);
            XMLStreamReader xml = factory.createXMLStreamReader(reader);

            String headerLine = "SampleID\tWellPosition\tResult\tInterpretation";
            lines.add(headerLine);

            while (xml.hasNext()) {
                int event = xml.next();
                if (event == XMLStreamConstants.START_ELEMENT && "cell".equals(xml.getLocalName())) {
                    processCell(xml);
                }
            }
            xml.close();

            if (lines.size() <= 1) {
                error = "No valid cell data found in XML";
                return false;
            }

            setInserter();
            if (inserter == null) {
                error = "Unable to understand which analyzer sent the file";
                return false;
            }
            return true;
        } catch (XMLStreamException e) {
            error = "XML parse error: " + e.getMessage();
            LogEvent.logError(this.getClass().getSimpleName(), "readStream", error);
            return false;
        } catch (IOException e) {
            error = "Unable to read XML file: " + e.getMessage();
            LogEvent.logError(this.getClass().getSimpleName(), "readStream", error);
            return false;
        }
    }

    private void processCell(XMLStreamReader xml) throws XMLStreamException {
        String name = xml.getAttributeValue(null, "name");
        String state = xml.getAttributeValue(null, "state");
        String x = xml.getAttributeValue(null, "x");
        String y = xml.getAttributeValue(null, "y");

        if (name == null || name.isEmpty()) {
            return;
        }
        if (state != null && !"complete".equals(state)) {
            return;
        }

        String wellPosition = toWellPosition(x, y);
        String resultValue = null;
        String interpretation = null;

        while (xml.hasNext()) {
            int event = xml.next();
            if (event == XMLStreamConstants.END_ELEMENT && "cell".equals(xml.getLocalName())) {
                break;
            }
            if (event == XMLStreamConstants.START_ELEMENT && "test".equals(xml.getLocalName())) {
                String testValue = xml.getAttributeValue(null, "value");
                if (testValue != null) {
                    resultValue = testValue;
                    interpretation = "+".equals(testValue) ? "Detected" : "Not Detected";
                }
            }
            if (event == XMLStreamConstants.START_ELEMENT && "result".equals(xml.getLocalName())) {
                String resultAttr = xml.getAttributeValue(null, "value");
                if (resultAttr != null && resultValue == null) {
                    resultValue = resultAttr;
                    interpretation = "+".equals(resultAttr) ? "Detected" : "Not Detected";
                }
            }
        }

        String sampleId = name.replaceAll("[AB]$", "");
        Map<String, String> record = new HashMap<>();
        record.put("sampleId", sampleId);
        record.put("SampleID", sampleId);
        record.put("position", wellPosition);
        record.put("WellPosition", wellPosition);
        record.put("result", resultValue != null ? resultValue : "");
        record.put("Result", resultValue != null ? resultValue : "");
        record.put("interpretation", interpretation != null ? interpretation : "");
        record.put("Interpretation", interpretation != null ? interpretation : "");
        parsedRecords.add(record);

        StringBuilder line = new StringBuilder();
        line.append(sampleId).append("\t");
        line.append(wellPosition).append("\t");
        line.append(resultValue != null ? resultValue : "").append("\t");
        line.append(interpretation != null ? interpretation : "");
        lines.add(line.toString());
    }

    private String toWellPosition(String x, String y) {
        if (x == null || y == null) {
            return "";
        }
        try {
            int col = Integer.parseInt(x.trim());
            int row = Integer.parseInt(y.trim());
            if (row >= 1 && row <= 8 && col >= 1 && col <= 12) {
                return String.valueOf((char) ('A' + row - 1)) + col;
            }
        } catch (NumberFormatException ignored) {
        }
        return "R" + y + "C" + x;
    }

    @Override
    public boolean insertAnalyzerData(String systemUserId) {
        if (inserter == null) {
            error = "Unable to understand which analyzer sent the file";
            return false;
        }
        boolean success = inserter.insert(lines, systemUserId);
        if (!success) {
            error = inserter.getError();
        }
        return success;
    }

    @Override
    public String getError() {
        return error;
    }

    @Override
    public List<Map<String, String>> getParsedRecords() {
        return parsedRecords == null ? List.of() : new ArrayList<>(parsedRecords);
    }

    @Override
    public List<String> getLines() {
        return new ArrayList<>(lines);
    }

    private void setInserter() {
        PluginAnalyzerService pluginService = SpringContext.getBean(PluginAnalyzerService.class);
        if (configuration != null && configuration.getAnalyzerId() != null) {
            AnalyzerImporterPlugin configuredPlugin = pluginService
                    .getPluginByAnalyzerId(configuration.getAnalyzerId().toString());
            if (configuredPlugin != null) {
                inserter = configuredPlugin.getAnalyzerLineInserter();
                inserter.setContextAnalyzerId(String.valueOf(configuration.getAnalyzerId()));
                return;
            }
        }
        for (AnalyzerImporterPlugin plugin : pluginService.getAnalyzerPlugins()) {
            try {
                if (plugin.isTargetAnalyzer(lines)) {
                    inserter = plugin.getAnalyzerLineInserter();
                    if (configuration != null && configuration.getAnalyzerId() != null) {
                        inserter.setContextAnalyzerId(String.valueOf(configuration.getAnalyzerId()));
                    }
                    return;
                }
            } catch (RuntimeException e) {
                LogEvent.logError(e);
            }
        }
    }
}
