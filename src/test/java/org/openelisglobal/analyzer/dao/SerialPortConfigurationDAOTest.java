package org.openelisglobal.analyzer.dao;

import static org.junit.Assert.*;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.valueholder.FlowControl;
import org.openelisglobal.analyzer.valueholder.Parity;
import org.openelisglobal.analyzer.valueholder.SerialPortConfiguration;
import org.openelisglobal.analyzer.valueholder.StopBits;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * DAO tests for SerialPortConfigurationDAO Task Reference: T024, M2
 * 
 * Tests persistence layer with real HQL query execution.
 * 
 * NOTE: These tests require analyzer records with IDs 1, 2, 3 to exist in the
 * test database (via DBUnit fixtures or test setup). The foreign key constraint
 * requires valid analyzer_id references.
 */
public class SerialPortConfigurationDAOTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SerialPortConfigurationDAO serialPortConfigurationDAO;

    private SerialPortConfiguration testConfig;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        testConfig = createTestConfiguration();
    }

    private SerialPortConfiguration createTestConfiguration() {
        SerialPortConfiguration config = new SerialPortConfiguration();
        config.setAnalyzerId(1);
        config.setPortName("/dev/ttyUSB0");
        config.setBaudRate(9600);
        config.setDataBits(8);
        config.setStopBits(StopBits.ONE);
        config.setParity(Parity.NONE);
        config.setFlowControl(FlowControl.NONE);
        config.setActive(true);
        config.setSysUserId("1");
        return config;
    }

    @Test
    public void testInsertAndFindById() {
        // Insert
        String id = serialPortConfigurationDAO.insert(testConfig);
        assertNotNull("ID should be generated", id);

        // Find by ID
        Optional<SerialPortConfiguration> found = serialPortConfigurationDAO.get(id);
        assertTrue("Configuration should be found", found.isPresent());
        assertEquals("/dev/ttyUSB0", found.get().getPortName());
        assertEquals(Integer.valueOf(9600), found.get().getBaudRate());
    }

    @Test
    public void testFindByAnalyzerId() {
        // Insert
        String id = serialPortConfigurationDAO.insert(testConfig);

        // Find by analyzer ID
        Optional<SerialPortConfiguration> found = serialPortConfigurationDAO.findByAnalyzerId(1);
        assertTrue("Configuration should be found by analyzer ID", found.isPresent());
        assertEquals(id, found.get().getId());
    }

    @Test
    public void testFindByPortName() {
        // Insert
        serialPortConfigurationDAO.insert(testConfig);

        // Find by port name
        Optional<SerialPortConfiguration> found = serialPortConfigurationDAO.findByPortName("/dev/ttyUSB0");
        assertTrue("Configuration should be found by port name", found.isPresent());
        assertEquals("/dev/ttyUSB0", found.get().getPortName());
    }

    @Test
    public void testUpdate() {
        // Insert
        String id = serialPortConfigurationDAO.insert(testConfig);

        // Update
        SerialPortConfiguration toUpdate = serialPortConfigurationDAO.get(id).get();
        toUpdate.setBaudRate(19200);
        serialPortConfigurationDAO.update(toUpdate);

        // Verify
        Optional<SerialPortConfiguration> updated = serialPortConfigurationDAO.get(id);
        assertTrue("Configuration should be found", updated.isPresent());
        assertEquals(Integer.valueOf(19200), updated.get().getBaudRate());
    }

    @Test
    public void testDelete() {
        // Insert
        String id = serialPortConfigurationDAO.insert(testConfig);

        // Delete
        SerialPortConfiguration toDelete = serialPortConfigurationDAO.get(id).get();
        serialPortConfigurationDAO.delete(toDelete);

        // Verify
        Optional<SerialPortConfiguration> deleted = serialPortConfigurationDAO.get(id);
        assertFalse("Configuration should be deleted", deleted.isPresent());
    }
}
