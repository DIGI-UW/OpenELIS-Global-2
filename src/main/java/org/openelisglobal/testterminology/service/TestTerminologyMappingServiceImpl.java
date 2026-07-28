package org.openelisglobal.testterminology.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testterminology.dao.TestTerminologyMappingDAO;
import org.openelisglobal.testterminology.valueholder.TestTerminologyMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestTerminologyMappingServiceImpl extends AuditableBaseObjectServiceImpl<TestTerminologyMapping, String>
        implements TestTerminologyMappingService {

    private static final String LOINC = "LOINC";

    private static final String SAME_AS = "SAME_AS";

    @Autowired
    protected TestTerminologyMappingDAO baseObjectDAO;

    @Autowired
    private TestService testService;

    TestTerminologyMappingServiceImpl() {
        super(TestTerminologyMapping.class);
    }

    @Override
    protected TestTerminologyMappingDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestTerminologyMapping> getActiveByTestId(String testId) {
        List<TestTerminologyMapping> active = new ArrayList<>();
        for (TestTerminologyMapping m : getAllMatching("testId", testId)) {
            if ("Y".equals(m.getIsActive())) {
                active.add(m);
            }
        }
        return active;
    }

    @Override
    @Transactional
    public void saveMappingsForTest(String testId, List<TestTerminologyMapping> desired, String sysUserId) {
        // Key everything (active + soft-deleted) by the natural key the DB enforces
        // unique, so a re-added (source, code) reactivates its row instead of
        // colliding on insert.
        List<TestTerminologyMapping> all = getAllMatching("testId", testId);
        Map<String, TestTerminologyMapping> byKey = new HashMap<>();
        for (TestTerminologyMapping m : all) {
            byKey.put(key(m.getComponentId(), m.getSampleTypeId(), m.getSource(), m.getCode()), m);
        }
        Set<String> desiredKeys = new HashSet<>();
        for (TestTerminologyMapping d : desired) {
            String k = key(d.getComponentId(), d.getSampleTypeId(), d.getSource(), d.getCode());
            desiredKeys.add(k);
            TestTerminologyMapping target = byKey.get(k);
            if (target != null) {
                target.setRelationship(d.getRelationship());
                target.setDisplayName(d.getDisplayName());
                target.setIsActive("Y");
                target.setSysUserId(sysUserId);
                update(target);
            } else {
                TestTerminologyMapping fresh = new TestTerminologyMapping();
                fresh.setTestId(testId);
                fresh.setComponentId(d.getComponentId());
                fresh.setSampleTypeId(d.getSampleTypeId());
                fresh.setSource(d.getSource());
                fresh.setCode(d.getCode());
                fresh.setRelationship(d.getRelationship());
                fresh.setDisplayName(d.getDisplayName());
                fresh.setIsActive("Y");
                fresh.setSysUserId(sysUserId);
                insert(fresh);
            }
        }
        for (TestTerminologyMapping m : all) {
            if ("Y".equals(m.getIsActive()) && !desiredKeys
                    .contains(key(m.getComponentId(), m.getSampleTypeId(), m.getSource(), m.getCode()))) {
                m.setIsActive("N");
                m.setSysUserId(sysUserId);
                update(m);
            }
        }
        applyLoincToLegacyTest(testId, sysUserId);
    }

    /**
     * OGC-1145 (FR-14) — active mappings for a code, honoring specimen scope: a
     * mapping whose {@code sampleTypeId} matches wins over shared (null) rows.
     * Returns specimen-scoped matches when any exist, else the shared matches.
     */
    @Override
    @Transactional(readOnly = true)
    public List<TestTerminologyMapping> getActiveMappingsForCode(String source, String code, String sampleTypeId) {
        List<TestTerminologyMapping> scoped = new ArrayList<>();
        List<TestTerminologyMapping> shared = new ArrayList<>();
        for (TestTerminologyMapping m : getAllMatching("code", code)) {
            if (!"Y".equals(m.getIsActive()) || !Objects.equals(source, m.getSource())) {
                continue;
            }
            if (m.getSampleTypeId() == null) {
                shared.add(m);
            } else if (sampleTypeId != null && sampleTypeId.equals(m.getSampleTypeId())) {
                scoped.add(m);
            }
        }
        return scoped.isEmpty() ? shared : scoped;
    }

    @Override
    @Transactional
    public void syncLegacyLoinc(String testId, String loinc, String sysUserId) {
        String code = isBlank(loinc) ? null : loinc.trim();
        List<TestTerminologyMapping> all = getAllMatching("testId", testId);
        for (TestTerminologyMapping m : all) {
            // shared (specimen-unscoped) test-level LOINC only — a Phase 2
            // per-specimen override is not the legacy code's concern (OGC-1145)
            if (m.getComponentId() == null && m.getSampleTypeId() == null && LOINC.equals(m.getSource())
                    && "Y".equals(m.getIsActive()) && !Objects.equals(m.getCode(), code)) {
                m.setIsActive("N");
                m.setSysUserId(sysUserId);
                update(m);
            }
        }
        if (code == null) {
            return;
        }
        TestTerminologyMapping existing = null;
        for (TestTerminologyMapping m : all) {
            if (m.getComponentId() == null && m.getSampleTypeId() == null && LOINC.equals(m.getSource())
                    && code.equals(m.getCode())) {
                existing = m;
                break;
            }
        }
        if (existing != null) {
            if (existing.getRelationship() == null) {
                existing.setRelationship(SAME_AS);
            }
            existing.setIsActive("Y");
            existing.setSysUserId(sysUserId);
            update(existing);
        } else {
            TestTerminologyMapping fresh = new TestTerminologyMapping();
            fresh.setTestId(testId);
            fresh.setSource(LOINC);
            fresh.setCode(code);
            fresh.setRelationship(SAME_AS);
            fresh.setIsActive("Y");
            fresh.setSysUserId(sysUserId);
            insert(fresh);
        }
    }

    private void applyLoincToLegacyTest(String testId, String sysUserId) {
        String loinc = pickLegacyLoinc(getActiveByTestId(testId));
        Test test = testService.getTestById(testId);
        if (test == null || Objects.equals(test.getLoinc(), loinc)) {
            return;
        }
        test.setLoinc(loinc);
        test.setSysUserId(sysUserId);
        testService.update(test);
    }

    private static String pickLegacyLoinc(List<TestTerminologyMapping> active) {
        String firstLoinc = null;
        for (TestTerminologyMapping m : active) {
            // Only test-level (component-unscoped), SHARED (specimen-unscoped)
            // LOINC feeds the legacy test.loinc column — a specimen-specific
            // override (OGC-1145 Phase 2) never overwrites the shared code.
            if (m.getComponentId() == null && m.getSampleTypeId() == null && LOINC.equals(m.getSource())) {
                if (SAME_AS.equals(m.getRelationship())) {
                    return m.getCode();
                }
                if (firstLoinc == null) {
                    firstLoinc = m.getCode();
                }
            }
        }
        return firstLoinc;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String key(String componentId, String sampleTypeId, String source, String code) {
        return (componentId == null ? "" : componentId) + " " + (sampleTypeId == null ? "" : sampleTypeId) + " "
                + (source == null ? "" : source) + " " + (code == null ? "" : code);
    }
}
