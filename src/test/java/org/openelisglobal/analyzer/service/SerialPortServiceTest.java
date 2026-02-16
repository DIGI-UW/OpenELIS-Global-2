package org.openelisglobal.analyzer.service;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.dao.SerialPortConfigurationDAO;
import org.openelisglobal.analyzer.valueholder.FlowControl;
import org.openelisglobal.analyzer.valueholder.Parity;
import org.openelisglobal.analyzer.valueholder.SerialPortConfiguration;
import org.openelisglobal.analyzer.valueholder.StopBits;

/**
 * Unit tests for SerialPortService implementation Coverage Goal: >80%
 */
@RunWith(MockitoJUnitRunner.class)
public class SerialPortServiceTest {

    @Mock
    private SerialPortConfigurationDAO serialPortConfigurationDAO;

    @InjectMocks
    private SerialPortServiceImpl serialPortService;

    private SerialPortConfiguration testConfig;

    @Before
    public void setUp() {
        testConfig = new SerialPortConfiguration();
        testConfig.setId("CONFIG-001");
        testConfig.setAnalyzerId(1);
        testConfig.setPortName("/dev/ttyUSB0");
        testConfig.setBaudRate(9600);
        testConfig.setDataBits(8);
        testConfig.setStopBits(StopBits.ONE);
        testConfig.setParity(Parity.NONE);
        testConfig.setFlowControl(FlowControl.NONE);
        testConfig.setActive(true);
    }

    @Test
    public void testGetByAnalyzerId_WithValidId_ReturnsConfiguration() {
        // Arrange
        when(serialPortConfigurationDAO.findByAnalyzerId(1)).thenReturn(Optional.of(testConfig));

        // Act
        Optional<SerialPortConfiguration> result = serialPortService.getByAnalyzerId(1);

        // Assert
        assertTrue("Configuration should be present", result.isPresent());
        assertEquals("CONFIG-001", result.get().getId());
        verify(serialPortConfigurationDAO).findByAnalyzerId(1);
    }

    @Test
    public void testGetByAnalyzerId_WithInvalidId_ReturnsEmpty() {
        // Arrange
        when(serialPortConfigurationDAO.findByAnalyzerId(999)).thenReturn(Optional.empty());

        // Act
        Optional<SerialPortConfiguration> result = serialPortService.getByAnalyzerId(999);

        // Assert
        assertFalse("Configuration should not be present", result.isPresent());
    }

    @Test
    public void testGetByPortName_WithValidPort_ReturnsConfiguration() {
        // Arrange
        when(serialPortConfigurationDAO.findByPortName("/dev/ttyUSB0")).thenReturn(Optional.of(testConfig));

        // Act
        Optional<SerialPortConfiguration> result = serialPortService.getByPortName("/dev/ttyUSB0");

        // Assert
        assertTrue("Configuration should be present", result.isPresent());
        assertEquals("/dev/ttyUSB0", result.get().getPortName());
    }

    @Test
    public void testIsConnected_WithNoConnection_ReturnsFalse() {
        // Act
        boolean result = serialPortService.isConnected("CONFIG-001");

        // Assert
        assertFalse("Should not be connected", result);
    }

    @Test
    public void testGetConnectionStatus_WithNoConnection_ReturnsDisconnected() {
        // Act
        String status = serialPortService.getConnectionStatus("CONFIG-001");

        // Assert
        assertEquals("DISCONNECTED", status);
    }
}
