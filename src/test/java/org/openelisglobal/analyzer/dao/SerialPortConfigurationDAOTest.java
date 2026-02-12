package org.openelisglobal.analyzer.dao;

import static org.junit.Assert.*;

import java.util.Optional;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.valueholder.FlowControl;
import org.openelisglobal.analyzer.valueholder.Parity;
import org.openelisglobal.analyzer.valueholder.SerialPortConfiguration;
import org.openelisglobal.analyzer.valueholder.StopBits;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * DAO tests for SerialPortConfigurationDAO
 * 
 * Tests persistence layer with real HQL query execution. Follows OpenELIS DAO
 * test pattern: JdbcTemplate for setup/teardown, DAO for queries.
 */
public class SerialPortConfigurationDAOTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private SerialPortConfigurationDAO serialPortConfigurationDAO;

    private JdbcTemplate jdbcTemplate;
    private SerialPortConfiguration testConfig;
    private String testConfigId;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);
        cleanTestData();

        // Load analyzer test data (IDs 1, 2, 3) required for foreign key constraint
        executeDataSetWithStateManagement("testdata/analyzer.xml");

        // Insert test configuration via JdbcTemplate (avoids transaction boundary
        // issues)
        testConfigId = "TEST-SERIAL-001";
        jdbcTemplate.update(
                "INSERT INTO serial_port_configuration (id, analyzer_id, port_name, baud_rate, data_bits, "
                        + "stop_bits, parity, flow_control, active, fhir_uuid, sys_user_id, last_updated) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, gen_random_uuid(), ?, CURRENT_TIMESTAMP)",
                testConfigId, 1, "/dev/ttyUSB0", 9600, 8, "ONE", "NONE", "NONE", true, "1");

        testConfig = createTestConfiguration();
    }

    @After
    public void tearDown() throws Exception {
        cleanTestData();
    }

    private void cleanTestData() {
        try {
            jdbcTemplate.execute("DELETE FROM serial_port_configuration WHERE id LIKE 'TEST-SERIAL-%'");
        } catch (Exception e) {
            // Ignore cleanup errors
        }
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
    public void testGetById() {
        // Act: Execute DAO query
        Optional<SerialPortConfiguration> found = serialPortConfigurationDAO.get(testConfigId);

        // Assert
        assertTrue("Configuration should be found", found.isPresent());
        assertEquals("/dev/ttyUSB0", found.get().getPortName());
        assertEquals(Integer.valueOf(9600), found.get().getBaudRate());
        assertEquals(StopBits.ONE, found.get().getStopBits());
    }

    @Test
    public void testFindByAnalyzerId() {
        // Act: Execute HQL query
        Optional<SerialPortConfiguration> found = serialPortConfigurationDAO.findByAnalyzerId(1);

        // Assert
        assertTrue("Configuration should be found by analyzer ID", found.isPresent());
        assertEquals(testConfigId, found.get().getId());
        assertEquals(Integer.valueOf(1), found.get().getAnalyzerId());
    }

    @Test
    public void testFindByPortName() {
        // Act: Execute HQL query
        Optional<SerialPortConfiguration> found = serialPortConfigurationDAO.findByPortName("/dev/ttyUSB0");

        // Assert
        assertTrue("Configuration should be found by port name", found.isPresent());
        assertEquals("/dev/ttyUSB0", found.get().getPortName());
        assertEquals(testConfigId, found.get().getId());
    }

    @Test
    public void testFindByPortName_NotFound_ReturnsEmpty() {
        // Act: Query with non-existent port name
        Optional<SerialPortConfiguration> found = serialPortConfigurationDAO.findByPortName("/dev/ttyNONEXISTENT");

        // Assert
        assertFalse("Configuration should not be found", found.isPresent());
    }

    @Test
    public void testFindByAnalyzerId_NotFound_ReturnsEmpty() {
        // Act: Query with non-existent analyzer ID
        Optional<SerialPortConfiguration> found = serialPortConfigurationDAO.findByAnalyzerId(999);

        // Assert
        assertFalse("Configuration should not be found", found.isPresent());
    }
}
