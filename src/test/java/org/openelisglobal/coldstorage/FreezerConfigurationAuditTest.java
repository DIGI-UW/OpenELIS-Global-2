package org.openelisglobal.coldstorage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hibernate.Hibernate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.audittrail.dao.AuditTrailService;
import org.openelisglobal.audittrail.daoimpl.AuditTrailServiceImpl;
import org.openelisglobal.coldstorage.service.FreezerService;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The test profile mocks {@link AuditTrailService}; the real one is swapped in
 * so history is actually written.
 */
public class FreezerConfigurationAuditTest extends BaseWebContextSensitiveTest {

    private static final String FREEZER_HISTORY_COUNT = "SELECT COUNT(*) FROM clinlims.history h"
            + " JOIN clinlims.reference_tables rt ON rt.id = h.reference_table"
            + " WHERE upper(rt.name) = 'FREEZER' AND h.reference_id = ?";

    @Autowired
    private FreezerService freezerService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ReferenceTablesService referenceTablesService;

    private Object freezerServiceTarget;
    private Object mockedAuditTrailService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        AuditTrailServiceImpl realAuditTrailService = new AuditTrailServiceImpl();
        ReflectionTestUtils.setField(realAuditTrailService, "referenceTablesService", referenceTablesService);
        ReflectionTestUtils.setField(realAuditTrailService, "historyService", historyService);
        freezerServiceTarget = AopTestUtils.getUltimateTargetObject(freezerService);
        mockedAuditTrailService = ReflectionTestUtils.getField(freezerServiceTarget, "auditTrailService");
        ReflectionTestUtils.setField(freezerServiceTarget, "auditTrailService", realAuditTrailService);

        cleanRowsInCurrentConnection(new String[] { "history" });
        executeDataSetWithStateManagement("testdata/freezer.xml");
        resyncSequence("clinlims.storage_device_seq", "clinlims.storage_device");
        resyncSequence("clinlims.freezer_seq", "clinlims.freezer");
    }

    @After
    public void restoreAuditTrailService() {
        ReflectionTestUtils.setField(freezerServiceTarget, "auditTrailService", mockedAuditTrailService);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    public void auditTrail_shouldShowADevicesCreationThresholdChangeAndDeletion() throws Exception {
        Freezer created = freezerService.createFreezer(newFreezer("Audited Freezer"), 1L, "1");
        freezerService.updateThresholds(created.getId(), new BigDecimal("-20"), new BigDecimal("-15"),
                new BigDecimal("5"), null, "1");
        freezerService.deleteFreezer(created.getId(), "1");

        List<Map<String, Object>> events = auditTrail(created.getId());
        Map<String, Map<String, Object>> byType = events.stream()
                .collect(Collectors.toMap(e -> (String) e.get("actionType"), e -> e, (a, b) -> a));

        assertTrue("Creation should be audited: " + events, byType.containsKey("FREEZER_CREATED"));
        assertTrue("Threshold change should be audited: " + events, byType.containsKey("THRESHOLD_UPDATED"));
        assertTrue("Deletion should be audited: " + events, byType.containsKey("FREEZER_DELETED"));
        String thresholdComment = (String) byType.get("THRESHOLD_UPDATED").get("comment");
        assertTrue("History stores the previous value, and it should read as one: " + thresholdComment,
                thresholdComment.contains("was -18"));
    }

    /**
     * Keep the scales as {@code 1}: the database holds {@code 1.0000}, and that
     * mismatch is the case under test.
     */
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    public void updateDevice_shouldWriteNoHistoryWhenNothingChanged() throws Exception {
        String unchanged = "{\"name\":\"Test Freezer 1\",\"protocol\":\"TCP\",\"host\":\"192.168.1.100\",\"port\":502,"
                + "\"slaveId\":1,\"temperatureRegister\":0,\"temperatureScale\":1,\"temperatureOffset\":-80,"
                + "\"humidityRegister\":1,\"humidityScale\":1,\"humidityOffset\":0,\"pollingIntervalSeconds\":60,"
                + "\"storageDevice\":{\"id\":1}}";

        mockMvc.perform(put("/rest/coldstorage/devices/100?roomId=1").contentType(MediaType.APPLICATION_JSON)
                .content(unchanged)).andExpect(status().isOk());

        assertEquals("An unchanged save should leave no history row", Integer.valueOf(0),
                jdbcTemplate.queryForObject(FREEZER_HISTORY_COUNT, Integer.class, 100L));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    public void updateDevice_shouldAuditAChangedConnectionSetting() throws Exception {
        String changedHost = "{\"name\":\"Test Freezer 1\",\"protocol\":\"TCP\",\"host\":\"10.0.0.9\",\"port\":502,"
                + "\"slaveId\":1,\"temperatureRegister\":0,\"temperatureScale\":1,\"temperatureOffset\":-80,"
                + "\"humidityRegister\":1,\"humidityScale\":1,\"humidityOffset\":0,\"pollingIntervalSeconds\":60,"
                + "\"storageDevice\":{\"id\":1}}";

        mockMvc.perform(put("/rest/coldstorage/devices/100?roomId=1").contentType(MediaType.APPLICATION_JSON)
                .content(changedHost)).andExpect(status().isOk());

        List<Map<String, Object>> events = auditTrail(100L);
        assertEquals("One configuration change: " + events, 1, events.size());
        assertEquals("CONFIGURATION_UPDATED", events.get(0).get("actionType"));
        assertTrue("Details carry the previous host: " + events.get(0).get("details"),
                ((String) events.get(0).get("details")).contains("<host>192.168.1.100</host>"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    public void auditTrail_shouldReportADeactivationAsADeactivation() throws Exception {
        freezerService.setDeviceStatus(100L, false, "1");

        List<Map<String, Object>> events = auditTrail(100L);

        assertEquals("One status change: " + events, 1, events.size());
        assertEquals("FREEZER_STATUS_CHANGED", events.get(0).get("actionType"));
        assertEquals("Freezer deactivated", events.get(0).get("comment"));
    }

    /**
     * Must change the target temperature: only the linked storage-device save
     * flushes and bumps the version.
     */
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    public void updateThresholds_shouldNotRecordTheVersionTimestampAsAChange() {
        freezerService.updateThresholds(100L, new BigDecimal("-20"), new BigDecimal("-15"), new BigDecimal("5"), null,
                "1");

        String changes = jdbcTemplate.queryForObject("SELECT convert_from(h.changes, 'UTF8') FROM clinlims.history h"
                + " JOIN clinlims.reference_tables rt ON rt.id = h.reference_table"
                + " WHERE upper(rt.name) = 'FREEZER' AND h.reference_id = ?", String.class, 100L);
        assertTrue("The threshold change is recorded: " + changes, changes.contains("targetTemperature"));
        assertFalse("The version timestamp is not a configuration change: " + changes, changes.contains("lastupdated"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    public void updateThresholds_shouldNotLoadTheReadingHistoryToAuditIt() {
        Freezer updated = freezerService.updateThresholds(100L, new BigDecimal("-20"), new BigDecimal("-15"),
                new BigDecimal("5"), null, "1");

        assertFalse("Auditing a threshold change must not load every reading the freezer has kept",
                Hibernate.isInitialized(updated.getReadings()));
    }

    private List<Map<String, Object>> auditTrail(Long freezerId) throws Exception {
        String body = performGet("/rest/coldstorage/audit-trail?freezerId=" + freezerId).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return new ObjectMapper().readValue(body, new TypeReference<List<Map<String, Object>>>() {
        });
    }

    private Freezer newFreezer(String name) {
        Freezer freezer = new Freezer();
        freezer.setName(name);
        freezer.setProtocol(Freezer.Protocol.TCP);
        freezer.setHost("192.168.1.201");
        freezer.setPort(502);
        freezer.setSlaveId(11);
        freezer.setTemperatureRegister(0);
        freezer.setTemperatureScale(BigDecimal.ONE);
        freezer.setTemperatureOffset(BigDecimal.ZERO);
        freezer.setWarningThreshold(new BigDecimal("-18.00"));
        return freezer;
    }
}
