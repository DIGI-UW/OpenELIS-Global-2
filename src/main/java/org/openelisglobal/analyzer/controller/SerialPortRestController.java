package org.openelisglobal.analyzer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.analyzer.service.SerialPortService;
import org.openelisglobal.analyzer.valueholder.SerialPortConfiguration;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.rest.BaseRestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Serial Port Configuration management
 * 
 * Handles CRUD operations for serial port configurations and connection
 * management.
 */
@RestController
@RequestMapping("/rest/analyzer/serial-port")
public class SerialPortRestController extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(SerialPortRestController.class);

    @Autowired
    private SerialPortService serialPortService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * GET /rest/analyzer/serial-port/configurations Retrieve all serial port
     * configurations
     */
    @GetMapping("/configurations")
    public ResponseEntity<Map<String, Object>> getAllConfigurations() {
        try {
            List<SerialPortConfiguration> configurations = serialPortService.getAll();
            Map<String, Object> response = new HashMap<>();
            response.put("configurations", configurations);
            response.put("count", configurations.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error retrieving serial port configurations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving configurations: " + e.getMessage()));
        }
    }

    /**
     * GET /rest/analyzer/serial-port/configurations/{id} Retrieve serial port
     * configuration by ID
     */
    @GetMapping("/configurations/{id}")
    public ResponseEntity<Map<String, Object>> getConfiguration(@PathVariable String id) {
        try {
            Optional<SerialPortConfiguration> configOpt = serialPortService.getById(id);
            if (configOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Serial port configuration not found: " + id));
            }
            Map<String, Object> response = new HashMap<>();
            response.put("configuration", configOpt.get());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error retrieving serial port configuration: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving configuration: " + e.getMessage()));
        }
    }

    /**
     * GET /rest/analyzer/serial-port/configurations/analyzer/{analyzerId} Retrieve
     * serial port configuration by analyzer ID
     */
    @GetMapping("/configurations/analyzer/{analyzerId}")
    public ResponseEntity<Map<String, Object>> getConfigurationByAnalyzerId(@PathVariable Integer analyzerId) {
        try {
            Optional<SerialPortConfiguration> configOpt = serialPortService.getByAnalyzerId(analyzerId);
            if (configOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Serial port configuration not found for analyzer: " + analyzerId));
            }
            Map<String, Object> response = new HashMap<>();
            response.put("configuration", configOpt.get());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error retrieving serial port configuration for analyzer: " + analyzerId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving configuration: " + e.getMessage()));
        }
    }

    /**
     * POST /rest/analyzer/serial-port/configurations Create a new serial port
     * configuration
     */
    @PostMapping("/configurations")
    public ResponseEntity<Map<String, Object>> createConfiguration(@RequestBody SerialPortConfiguration config) {
        try {
            // Validate required fields
            if (config.getAnalyzerId() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("Analyzer ID is required"));
            }
            if (config.getPortName() == null || config.getPortName().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(createErrorResponse("Port name is required"));
            }

            // Check if configuration already exists for this analyzer
            Optional<SerialPortConfiguration> existing = serialPortService.getByAnalyzerId(config.getAnalyzerId());
            if (existing.isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(createErrorResponse(
                        "Serial port configuration already exists for analyzer: " + config.getAnalyzerId()));
            }

            String id = serialPortService.insert(config);
            Map<String, Object> response = new HashMap<>();
            response.put("id", id);
            response.put("message", "Serial port configuration created successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (LIMSRuntimeException e) {
            logger.error("Error creating serial port configuration", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Error creating configuration: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating serial port configuration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error creating configuration: " + e.getMessage()));
        }
    }

    /**
     * PUT /rest/analyzer/serial-port/configurations/{id} Update an existing serial
     * port configuration
     */
    @PutMapping("/configurations/{id}")
    public ResponseEntity<Map<String, Object>> updateConfiguration(@PathVariable String id,
            @RequestBody SerialPortConfiguration config) {
        try {
            Optional<SerialPortConfiguration> existingOpt = serialPortService.getById(id);
            if (existingOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Serial port configuration not found: " + id));
            }

            SerialPortConfiguration existing = existingOpt.get();

            // Update fields
            if (config.getPortName() != null) {
                existing.setPortName(config.getPortName());
            }
            if (config.getBaudRate() != null) {
                existing.setBaudRate(config.getBaudRate());
            }
            if (config.getDataBits() != null) {
                existing.setDataBits(config.getDataBits());
            }
            if (config.getStopBits() != null) {
                existing.setStopBits(config.getStopBits());
            }
            if (config.getParity() != null) {
                existing.setParity(config.getParity());
            }
            if (config.getFlowControl() != null) {
                existing.setFlowControl(config.getFlowControl());
            }
            if (config.getActive() != null) {
                existing.setActive(config.getActive());
            }

            serialPortService.update(existing);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Serial port configuration updated successfully");
            return ResponseEntity.ok(response);
        } catch (LIMSRuntimeException e) {
            logger.error("Error updating serial port configuration: " + id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Error updating configuration: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating serial port configuration: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error updating configuration: " + e.getMessage()));
        }
    }

    /**
     * DELETE /rest/analyzer/serial-port/configurations/{id} Delete a serial port
     * configuration
     */
    @DeleteMapping("/configurations/{id}")
    public ResponseEntity<Map<String, Object>> deleteConfiguration(@PathVariable String id) {
        try {
            Optional<SerialPortConfiguration> configOpt = serialPortService.getById(id);
            if (configOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Serial port configuration not found: " + id));
            }

            // Close connection if open
            if (serialPortService.isConnected(id)) {
                serialPortService.closeConnection(id);
            }

            serialPortService.delete(configOpt.get());
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Serial port configuration deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error deleting serial port configuration: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error deleting configuration: " + e.getMessage()));
        }
    }

    /**
     * POST /rest/analyzer/serial-port/configurations/{id}/connect Open serial port
     * connection
     */
    @PostMapping("/configurations/{id}/connect")
    public ResponseEntity<Map<String, Object>> connect(@PathVariable String id) {
        try {
            boolean success = serialPortService.openConnection(id);
            Map<String, Object> response = new HashMap<>();
            if (success) {
                response.put("status", "CONNECTED");
                response.put("message", "Serial port connection opened successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "ERROR");
                response.put("message", "Failed to open serial port connection");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (Exception e) {
            logger.error("Error opening serial port connection: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error opening connection: " + e.getMessage()));
        }
    }

    /**
     * POST /rest/analyzer/serial-port/configurations/{id}/disconnect Close serial
     * port connection
     */
    @PostMapping("/configurations/{id}/disconnect")
    public ResponseEntity<Map<String, Object>> disconnect(@PathVariable String id) {
        try {
            boolean success = serialPortService.closeConnection(id);
            Map<String, Object> response = new HashMap<>();
            if (success) {
                response.put("status", "DISCONNECTED");
                response.put("message", "Serial port connection closed successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "ERROR");
                response.put("message", "Failed to close serial port connection");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (Exception e) {
            logger.error("Error closing serial port connection: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error closing connection: " + e.getMessage()));
        }
    }

    /**
     * GET /rest/analyzer/serial-port/configurations/{id}/status Get connection
     * status
     */
    @GetMapping("/configurations/{id}/status")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable String id) {
        try {
            String status = serialPortService.getConnectionStatus(id);
            boolean connected = serialPortService.isConnected(id);
            Map<String, Object> response = new HashMap<>();
            response.put("status", status);
            response.put("connected", connected);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting serial port connection status: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error getting status: " + e.getMessage()));
        }
    }

    /**
     * POST /rest/analyzer/serial-port/configurations/{id}/read-once Trigger a
     * one-time read from the serial port
     * 
     * This endpoint is primarily for automated testing of RS232 analyzers. It reads
     * available data from the serial port, processes it through the analyzer
     * plugin, and returns the result.
     * 
     * @param id The serial port configuration ID
     * @return Response with success status, lines read, and any errors
     */
    @PostMapping("/configurations/{id}/read-once")
    public ResponseEntity<Map<String, Object>> readOnce(@PathVariable String id) {
        try {
            Optional<SerialPortConfiguration> configOpt = serialPortService.getById(id);
            if (configOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Serial port configuration not found: " + id));
            }

            SerialPortConfiguration config = configOpt.get();
            Integer analyzerId = config.getAnalyzerId();

            if (analyzerId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("Analyzer ID not configured for serial port: " + id));
            }

            // Create SerialAnalyzerReader instance
            org.openelisglobal.analyzerimport.analyzerreaders.SerialAnalyzerReader reader = new org.openelisglobal.analyzerimport.analyzerreaders.SerialAnalyzerReader(
                    analyzerId);

            // Read from serial port
            boolean readSuccess = reader.readFromSerialPort();

            Map<String, Object> response = new HashMap<>();
            response.put("configurationId", id);
            response.put("analyzerId", analyzerId);
            response.put("readSuccess", readSuccess);

            if (readSuccess) {
                // Process the data
                boolean processSuccess = reader.processData("1"); // System user ID
                response.put("processSuccess", processSuccess);
                response.put("message", "Data read and processed successfully");

                if (reader.hasResponse()) {
                    response.put("analyzerResponse", reader.getResponse());
                }
            } else {
                response.put("processSuccess", false);
                response.put("message", "Failed to read from serial port");
                if (reader.getError() != null) {
                    response.put("error", reader.getError());
                }
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error reading from serial port: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error reading from serial port: " + e.getMessage()));
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}
