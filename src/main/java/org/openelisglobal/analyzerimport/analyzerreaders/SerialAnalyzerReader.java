package org.openelisglobal.analyzerimport.analyzerreaders;

import com.fazecast.jSerialComm.SerialPort;
import com.ibm.icu.text.CharsetDetector;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.service.MappingApplicationService;
import org.openelisglobal.analyzer.service.MappingAwareAnalyzerLineInserter;
import org.openelisglobal.analyzer.service.SerialPortService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.SerialPortConfiguration;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.plugin.AnalyzerImporterPlugin;
import org.openelisglobal.spring.util.SpringContext;

/**
 * SerialAnalyzerReader - Reads ASTM messages from RS232 serial port
 * connections.
 * 
 * 
 * Extends AnalyzerReader to support serial port communication for analyzers.
 * Reads ASTM messages from serial ports and processes them using existing
 * plugin infrastructure.
 */
public class SerialAnalyzerReader extends AnalyzerReader {

    private List<String> lines;
    private AnalyzerImporterPlugin plugin;
    private AnalyzerLineInserter inserter;
    private AnalyzerResponder responder;
    private String error;
    private boolean hasResponse = false;
    private String responseBody;
    private Integer analyzerId;
    private SerialPort serialPort;
    private SerialPortConfiguration config;

    /**
     * Constructor with analyzer ID
     * 
     * @param analyzerId The analyzer ID to read from
     */
    public SerialAnalyzerReader(Integer analyzerId) {
        this.analyzerId = analyzerId;
    }

    /**
     * Read data from serial port for the configured analyzer
     * 
     * @return true if data read successfully, false otherwise
     */
    public boolean readFromSerialPort() {
        error = null;
        inserter = null;
        lines = new ArrayList<>();

        try {
            SerialPortService serialPortService = SpringContext.getBean(SerialPortService.class);
            if (serialPortService == null) {
                error = "SerialPortService not available";
                LogEvent.logError(this.getClass().getSimpleName(), "readFromSerialPort", error);
                return false;
            }

            Optional<SerialPortConfiguration> configOpt = serialPortService.getByAnalyzerId(analyzerId);
            if (configOpt.isEmpty()) {
                error = "Serial port configuration not found for analyzer ID: " + analyzerId;
                LogEvent.logError(this.getClass().getSimpleName(), "readFromSerialPort", error);
                return false;
            }

            config = configOpt.get();
            if (!config.getActive()) {
                error = "Serial port configuration is inactive for analyzer ID: " + analyzerId;
                LogEvent.logError(this.getClass().getSimpleName(), "readFromSerialPort", error);
                return false;
            }

            String configId = config.getId();
            if (!serialPortService.isConnected(configId)) {
                if (!serialPortService.openConnection(configId)) {
                    error = "Failed to open serial port connection for analyzer ID: " + analyzerId;
                    LogEvent.logError(this.getClass().getSimpleName(), "readFromSerialPort", error);
                    return false;
                }
            }

            // TODO: Refactor SerialPortService to expose SerialPort instead of
            // bypassing service encapsulation here
            SerialPort port = SerialPort.getCommPort(config.getPortName());
            if (port == null || !port.isOpen()) {
                error = "Serial port not available or not open: " + config.getPortName();
                LogEvent.logError(this.getClass().getSimpleName(), "readFromSerialPort", error);
                return false;
            }

            serialPort = port;

            InputStream stream = serialPort.getInputStream();
            return readStream(stream);

        } catch (Exception e) {
            error = "Error reading from serial port: " + e.getMessage();
            LogEvent.logError(e);
            return false;
        }
    }

    @Override
    public boolean readStream(InputStream stream) {
        error = null;
        inserter = null;
        lines = new ArrayList<>();
        BufferedInputStream bis = new BufferedInputStream(stream);
        CharsetDetector detector = new CharsetDetector();
        try {
            detector.setText(bis);
            String charsetName = detector.detect().getName();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(bis, charsetName));

            try {
                // Read lines until we get a complete ASTM message (ends with ETX + CR)
                // Or timeout after reading available data
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    lines.add(line);
                    // Check if we have a complete ASTM message (ends with ETX)
                    if (line.contains("\u0003") || line.endsWith("\r")) {
                        break;
                    }
                }
            } catch (IOException e) {
                error = "Unable to read from serial port";
                LogEvent.logError(e);
                return false;
            }
        } catch (IOException e) {
            error = "Unable to determine message encoding";
            LogEvent.logError("an error occurred detecting the encoding of the serial port message", e);
            return false;
        }

        if (lines.isEmpty()) {
            error = "Empty message from serial port";
            return false;
        }
        return true;
    }

    /**
     * Resolve plugin/inserter/responder from message lines (HL7-aligned: match at
     * process time).
     */
    private void ensureInserterResponder() {
        if (plugin != null) {
            return;
        }
        setInserterResponder();
    }

    public boolean processData(String currentUserId) {
        error = null;
        ensureInserterResponder();
        if (plugin == null) {
            error = "No ASTM plugin matched this message (e.g. configure GenericASTM with matching identifier pattern)";
            LogEvent.logError(getClass().getSimpleName(), "processData", error);
            return false;
        }
        if (plugin.isAnalyzerResult(lines)) {
            return insertAnalyzerData(currentUserId);
        } else {
            responseBody = buildResponseForQuery();
            hasResponse = true;
            return true;
        }
    }

    public boolean hasResponse() {
        return hasResponse;
    }

    public String getResponse() {
        return responseBody;
    }

    private void setInserterResponder() {
        PluginAnalyzerService pluginService = SpringContext.getBean(PluginAnalyzerService.class);
        List<AnalyzerImporterPlugin> plugins = choosePluginOrder(pluginService);
        for (AnalyzerImporterPlugin plugin : plugins) {
            if (plugin.isTargetAnalyzer(lines)) {
                try {
                    this.plugin = plugin;
                    inserter = plugin.getAnalyzerLineInserter();
                    responder = plugin.getAnalyzerResponder();
                    return;
                } catch (RuntimeException e) {
                    LogEvent.logError(e);
                }
            }
        }
    }

    /**
     * Return the plugin list in default order. (preferGenericPlugin flag has been
     * removed.)
     */
    private List<AnalyzerImporterPlugin> choosePluginOrder(PluginAnalyzerService pluginService) {
        return pluginService.getAnalyzerPlugins();
    }

    private String parseIdentifierFromAstmHeader() {
        if (lines == null || lines.isEmpty()) {
            return null;
        }
        for (String line : lines) {
            if (line != null && line.startsWith("H|")) {
                String[] fields = line.split("\\|");
                if (fields.length > 4 && fields[4] != null && !fields[4].trim().isEmpty()) {
                    return fields[4].trim();
                }
                break;
            }
        }
        return null;
    }

    private String buildResponseForQuery() {
        if (responder == null) {
            error = "Unable to understand which analyzer sent the query or plugin doesn't support responding";
            LogEvent.logError(this.getClass().getSimpleName(), "buildResponseForQuery", error);
            return "";
        } else {
            LogEvent.logDebug(this.getClass().getSimpleName(), "buildResponseForQuery", "building response");
            return responder.buildResponse(lines);
        }
    }

    @Override
    public boolean insertAnalyzerData(String systemUserId) {
        ensureInserterResponder();
        if (inserter == null) {
            error = "No ASTM plugin matched this message (e.g. configure GenericASTM with matching identifier pattern)";
            LogEvent.logError(this.getClass().getSimpleName(), "insertAnalyzerData", error);
            return false;
        } else {
            AnalyzerLineInserter finalInserter = wrapInserterIfMappingsExist(inserter);

            boolean success = finalInserter.insert(lines, systemUserId);
            if (!success) {
                error = finalInserter.getError();
                LogEvent.logError(this.getClass().getSimpleName(), "insertAnalyzerData", error);
            }
            return success;
        }
    }

    /**
     * Wrap inserter with MappingAwareAnalyzerLineInserter if analyzer has active
     * mappings
     */
    private AnalyzerLineInserter wrapInserterIfMappingsExist(AnalyzerLineInserter originalInserter) {
        try {
            if (analyzerId == null) {
                return originalInserter;
            }

            AnalyzerService analyzerService = SpringContext.getBean(AnalyzerService.class);
            if (analyzerService == null) {
                return originalInserter;
            }

            Analyzer analyzer = analyzerService.get(analyzerId.toString());
            if (analyzer == null) {
                return originalInserter;
            }

            MappingApplicationService mappingApplicationService = SpringContext
                    .getBean(MappingApplicationService.class);
            if (mappingApplicationService != null && mappingApplicationService.hasActiveMappings(analyzer.getId())) {
                return new MappingAwareAnalyzerLineInserter(originalInserter, analyzer);
            }

            return originalInserter;

        } catch (Exception e) {
            // Error checking mappings - use original inserter
            LogEvent.logError("Error checking mappings, using original inserter: " + e.getMessage(), e);
            return originalInserter;
        }
    }

    @Override
    public String getError() {
        return error;
    }

    /**
     * Close serial port connection
     */
    public void close() {
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
        }
    }
}
