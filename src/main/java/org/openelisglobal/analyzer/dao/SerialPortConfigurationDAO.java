package org.openelisglobal.analyzer.dao;

import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.SerialPortConfiguration;
import org.openelisglobal.common.dao.BaseDAO;

/**
 * DAO interface for SerialPortConfiguration
 */
public interface SerialPortConfigurationDAO extends BaseDAO<SerialPortConfiguration, String> {

    /**
     * Find SerialPortConfiguration by analyzer ID
     * 
     * @param analyzerId The analyzer ID
     * @return Optional SerialPortConfiguration
     */
    Optional<SerialPortConfiguration> findByAnalyzerId(Integer analyzerId);

    /**
     * Find SerialPortConfiguration by port name
     * 
     * @param portName The port name (e.g., "/dev/ttyUSB0", "COM3")
     * @return Optional SerialPortConfiguration
     */
    Optional<SerialPortConfiguration> findByPortName(String portName);
}
