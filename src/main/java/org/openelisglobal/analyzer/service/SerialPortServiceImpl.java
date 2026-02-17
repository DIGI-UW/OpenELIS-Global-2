package org.openelisglobal.analyzer.service;

import com.fazecast.jSerialComm.SerialPort;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.analyzer.dao.SerialPortConfigurationDAO;
import org.openelisglobal.analyzer.valueholder.SerialPortConfiguration;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for SerialPortConfiguration operations
 * 
 * Manages serial port configurations and connection lifecycle using jSerialComm
 * library.
 */
@Service
@Transactional
public class SerialPortServiceImpl extends BaseObjectServiceImpl<SerialPortConfiguration, String>
        implements SerialPortService {

    @Autowired
    private SerialPortConfigurationDAO serialPortConfigurationDAO;

    // In-memory connection tracking (could be enhanced with database persistence)
    private final Map<String, SerialPort> activeConnections = new HashMap<>();
    private final Map<String, String> connectionStatuses = new HashMap<>();

    public SerialPortServiceImpl() {
        super(SerialPortConfiguration.class);
    }

    @Override
    protected SerialPortConfigurationDAO getBaseObjectDAO() {
        return serialPortConfigurationDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SerialPortConfiguration> getById(String id) {
        return serialPortConfigurationDAO.get(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SerialPortConfiguration> getByAnalyzerId(Integer analyzerId) {
        return serialPortConfigurationDAO.findByAnalyzerId(analyzerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SerialPortConfiguration> getByPortName(String portName) {
        return serialPortConfigurationDAO.findByPortName(portName);
    }

    @Override
    public boolean openConnection(String configId) {
        try {
            SerialPortConfiguration config = get(configId);

            if (!config.getActive()) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "openConnection",
                        "Cannot open connection for inactive configuration: " + configId);
                return false;
            }

            if (activeConnections.containsKey(configId)) {
                SerialPort existingPort = activeConnections.get(configId);
                if (existingPort.isOpen()) {
                    LogEvent.logInfo(this.getClass().getSimpleName(), "openConnection",
                            "Port already open for configuration: " + configId);
                    return true;
                } else {
                    activeConnections.remove(configId);
                    connectionStatuses.remove(configId);
                }
            }

            SerialPort serialPort = SerialPort.getCommPort(config.getPortName());
            if (serialPort == null) {
                LogEvent.logError(this.getClass().getSimpleName(), "openConnection",
                        "Serial port not found: " + config.getPortName());
                connectionStatuses.put(configId, "ERROR");
                return false;
            }

            serialPort.setBaudRate(config.getBaudRate());
            serialPort.setNumDataBits(config.getDataBits());

            int stopBitsValue;
            switch (config.getStopBits()) {
            case ONE:
                stopBitsValue = SerialPort.ONE_STOP_BIT;
                break;
            case ONE_POINT_FIVE:
                stopBitsValue = SerialPort.ONE_POINT_FIVE_STOP_BITS;
                break;
            case TWO:
                stopBitsValue = SerialPort.TWO_STOP_BITS;
                break;
            default:
                stopBitsValue = SerialPort.ONE_STOP_BIT;
            }
            serialPort.setNumStopBits(stopBitsValue);

            int parityValue;
            switch (config.getParity()) {
            case NONE:
                parityValue = SerialPort.NO_PARITY;
                break;
            case EVEN:
                parityValue = SerialPort.EVEN_PARITY;
                break;
            case ODD:
                parityValue = SerialPort.ODD_PARITY;
                break;
            case MARK:
                parityValue = SerialPort.MARK_PARITY;
                break;
            case SPACE:
                parityValue = SerialPort.SPACE_PARITY;
                break;
            default:
                parityValue = SerialPort.NO_PARITY;
            }
            serialPort.setParity(parityValue);

            int flowControlValue = 0;
            switch (config.getFlowControl()) {
            case NONE:
                flowControlValue = 0;
                break;
            case RTS_CTS:
                flowControlValue = SerialPort.FLOW_CONTROL_RTS_ENABLED | SerialPort.FLOW_CONTROL_CTS_ENABLED;
                break;
            case XON_XOFF:
                flowControlValue = SerialPort.FLOW_CONTROL_XONXOFF_IN_ENABLED
                        | SerialPort.FLOW_CONTROL_XONXOFF_OUT_ENABLED;
                break;
            }
            serialPort.setFlowControl(flowControlValue);

            // Set read timeout (5 seconds)
            serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 5000, 0);

            if (serialPort.openPort()) {
                activeConnections.put(configId, serialPort);
                connectionStatuses.put(configId, "CONNECTED");
                LogEvent.logInfo(this.getClass().getSimpleName(), "openConnection",
                        "Successfully opened serial port: " + config.getPortName() + " for configuration: " + configId);
                return true;
            } else {
                LogEvent.logError(this.getClass().getSimpleName(), "openConnection",
                        "Failed to open serial port: " + config.getPortName());
                connectionStatuses.put(configId, "ERROR");
                return false;
            }
        } catch (Exception e) {
            LogEvent.logError(e);
            connectionStatuses.put(configId, "ERROR");
            throw new LIMSRuntimeException("Error opening serial port connection for configuration: " + configId, e);
        }
    }

    @Override
    public boolean closeConnection(String configId) {
        try {
            if (!activeConnections.containsKey(configId)) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "closeConnection",
                        "No active connection found for configuration: " + configId);
                return true; // Already closed
            }

            SerialPort serialPort = activeConnections.get(configId);
            if (serialPort != null && serialPort.isOpen()) {
                serialPort.closePort();
                LogEvent.logInfo(this.getClass().getSimpleName(), "closeConnection",
                        "Closed serial port for configuration: " + configId);
            }

            activeConnections.remove(configId);
            connectionStatuses.put(configId, "DISCONNECTED");
            return true;
        } catch (Exception e) {
            LogEvent.logError(e);
            connectionStatuses.put(configId, "ERROR");
            throw new LIMSRuntimeException("Error closing serial port connection for configuration: " + configId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isConnected(String configId) {
        if (!activeConnections.containsKey(configId)) {
            return false;
        }
        SerialPort serialPort = activeConnections.get(configId);
        return serialPort != null && serialPort.isOpen();
    }

    @Override
    @Transactional(readOnly = true)
    public String getConnectionStatus(String configId) {
        if (!connectionStatuses.containsKey(configId)) {
            return "DISCONNECTED";
        }

        if (isConnected(configId)) {
            return "CONNECTED";
        } else {
            connectionStatuses.remove(configId);
            return "DISCONNECTED";
        }
    }

    /**
     * Cleanup method to close all active connections (useful for shutdown)
     */
    public void closeAllConnections() {
        for (String configId : activeConnections.keySet()) {
            closeConnection(configId);
        }
    }
}
