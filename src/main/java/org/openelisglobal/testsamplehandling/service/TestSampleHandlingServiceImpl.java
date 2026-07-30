package org.openelisglobal.testsamplehandling.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.testsamplehandling.dao.TestSampleHandlingDAO;
import org.openelisglobal.testsamplehandling.valueholder.TestSampleHandling;
import org.openelisglobal.testsamplehandling.valueholder.TestSampleHandlingHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestSampleHandlingServiceImpl extends AuditableBaseObjectServiceImpl<TestSampleHandling, String>
        implements TestSampleHandlingService {

    @Autowired
    protected TestSampleHandlingDAO baseObjectDAO;

    @Autowired
    private TestSampleHandlingHistoryService historyService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    TestSampleHandlingServiceImpl() {
        super(TestSampleHandling.class);
    }

    @Override
    protected TestSampleHandlingDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public TestSampleHandling getByTestId(String testId) {
        List<TestSampleHandling> matches = getAllMatching("testId", testId);
        return matches.isEmpty() ? null : matches.get(0);
    }

    @Override
    @Transactional
    public TestSampleHandling saveForTest(String testId, TestSampleHandling desired, String sysUserId) {
        TestSampleHandling existing = getByTestId(testId);
        // Snapshot the prior business state BEFORE mutating (target == existing).
        String previousJson = existing != null ? snapshot(existing) : null;
        TestSampleHandling target = existing != null ? existing : new TestSampleHandling();
        target.setTestId(testId);
        target.setStorageCondition(desired.getStorageCondition());
        target.setStorageConditionCustom(desired.getStorageConditionCustom());
        target.setStorageDuration(desired.getStorageDuration());
        target.setStorageDurationUnit(desired.getStorageDurationUnit());
        target.setStabilityNotes(desired.getStabilityNotes());
        target.setProtectFromLight(desired.getProtectFromLight());
        target.setDoNotFreeze(desired.getDoNotFreeze());
        target.setDoNotRefrigerate(desired.getDoNotRefrigerate());
        target.setDisposalMethod(desired.getDisposalMethod());
        target.setDisposalTimeframe(desired.getDisposalTimeframe());
        target.setDisposalUnit(desired.getDisposalUnit());
        target.setSpecialInstructions(desired.getSpecialInstructions());
        target.setOverrideRestricted(desired.getOverrideRestricted());
        // Bump the app-level config-version counter for the v2 audit trail.
        int prior = existing != null && existing.getVersion() != null ? existing.getVersion() : 0;
        target.setVersion(prior + 1);
        target.setIsActive("Y");
        target.setSysUserId(sysUserId);
        if (existing != null) {
            update(target);
        } else {
            insert(target);
        }

        // OGC-766: record an audit snapshot when the business state changed
        // (no-change re-saves are skipped). Same transaction as the upsert.
        String newJson = snapshot(target);
        boolean changed = previousJson == null || !previousJson.equals(newJson);
        if (changed) {
            TestSampleHandlingHistory history = new TestSampleHandlingHistory();
            history.setTestSampleHandlingId(target.getId());
            history.setChangedBy(sysUserId);
            history.setChangeType(existing == null ? "INSERT" : "UPDATE");
            history.setPreviousValues(previousJson);
            history.setNewValues(newJson);
            history.setSysUserId(sysUserId);
            historyService.insert(history);
        }
        return target;
    }

    /** Serialize the auditable business fields (not version/active) as JSON. */
    private String snapshot(TestSampleHandling h) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("storageCondition", h.getStorageCondition());
        map.put("storageConditionCustom", h.getStorageConditionCustom());
        map.put("storageDuration", h.getStorageDuration());
        map.put("storageDurationUnit", h.getStorageDurationUnit());
        map.put("stabilityNotes", h.getStabilityNotes());
        map.put("protectFromLight", h.getProtectFromLight());
        map.put("doNotFreeze", h.getDoNotFreeze());
        map.put("doNotRefrigerate", h.getDoNotRefrigerate());
        map.put("disposalMethod", h.getDisposalMethod());
        map.put("disposalTimeframe", h.getDisposalTimeframe());
        map.put("disposalUnit", h.getDisposalUnit());
        map.put("specialInstructions", h.getSpecialInstructions());
        map.put("overrideRestricted", h.getOverrideRestricted());
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            LogEvent.logError(e);
            return "{}";
        }
    }
}
