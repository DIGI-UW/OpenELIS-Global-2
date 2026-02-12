package org.openelisglobal.analyzer.service;

import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.SerialPortConfiguration;
import org.openelisglobal.common.service.BaseObjectService;

/**
 * Service interface for SerialPortConfiguration operations
 * 
 * Provides business logic for managing serial port configurations and
 * connection lifecycle.
 */
public interface SerialPortService extends BaseObjectService<SerialPortConfiguration, String> {

    /**
     * Get SerialPortConfiguration by ID (optional lookup, returns empty if not
     * found)
     * 
     * @param id The configuration ID
     * @return Optional SerialPortConfiguration
     */
    Optional<SerialPortConfiguration> getById(String id);

    /**
     * Get SerialPortConfiguration by analyzer ID
     * 
     * @param analyzerId The analyzer ID
     * @return Optional SerialPortConfiguration
     */
    Optional<SerialPortConfiguration> getByAnalyzerId(Integer analyzerId);

    /**
     * Get SerialPortConfiguration by port name
     * 
     * @param portName The port name (e.g., "/dev/ttyUSB0", "COM3")
     * @return Optional SerialPortConfiguration
     */
    Optional<SerialPortConfiguration> getByPortName(String portName);

    /**
     * Open serial port connection for the given configuration
     * 
     * @param configId The SerialPortConfiguration ID
     * @return true if connection opened successfully, false otherwise
     */
    boolean openConnection(String configId);

    /**
     * Close serial port connection for the given configuration
     * 
     * @param configId The SerialPortConfiguration ID
     * @return true if connection closed successfully, false otherwise
     */
    boolean closeConnection(String configId);

    /**
     * Check if serial port is currently connected
     * 
     * @param configId The SerialPortConfiguration ID
     * @return true if connected, false otherwise
     */
    boolean isConnected(String configId);

    /**
     * Get connection status for the given configuration
     * 
     * @param configId The SerialPortConfiguration ID
     * @return Connection status string (CONNECTED, DISCONNECTED, ERROR)
     */
    String getConnectionStatus(String configId);
}
