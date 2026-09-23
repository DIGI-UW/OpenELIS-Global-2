package org.openelisglobal.sampletypeterminology.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.sampletypeterminology.dao.SampleTypeTerminologyMappingDAO;
import org.openelisglobal.sampletypeterminology.valueholder.SampleTypeTerminologyMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SampleTypeTerminologyMappingServiceImpl
        extends AuditableBaseObjectServiceImpl<SampleTypeTerminologyMapping, String>
        implements SampleTypeTerminologyMappingService {

    @Autowired
    protected SampleTypeTerminologyMappingDAO baseObjectDAO;

    SampleTypeTerminologyMappingServiceImpl() {
        super(SampleTypeTerminologyMapping.class);
    }

    /** SAME_AS is the relationship that qualifies a code as the identifier. */
    private static final String SAME_AS = "SAME_AS";
    private static final String LOINC = "LOINC";

    @Override
    @Transactional
    public void syncConfiguredLoinc(String sampleTypeId, String loinc, String sysUserId) {
        if (loinc == null || loinc.trim().isEmpty()) {
            // Saying nothing about LOINC is not the same as asking for it to go.
            return;
        }
        String code = loinc.trim();
        List<SampleTypeTerminologyMapping> all = getAllMatching("sampleTypeId", sampleTypeId);

        // Retire an active LOINC mapping the configuration no longer names, first,
        // so the unique key is free for the upsert below.
        for (SampleTypeTerminologyMapping m : all) {
            if (LOINC.equals(m.getSource()) && "Y".equals(m.getIsActive()) && !code.equals(m.getCode())) {
                m.setIsActive("N");
                m.setSysUserId(sysUserId);
                update(m);
            }
        }

        for (SampleTypeTerminologyMapping m : all) {
            if (LOINC.equals(m.getSource()) && code.equals(m.getCode())) {
                // A relationship an administrator chose in the editor is kept: the
                // configuration says which code, not what it means.
                if (m.getRelationship() == null) {
                    m.setRelationship(SAME_AS);
                }
                m.setIsActive("Y");
                m.setSysUserId(sysUserId);
                update(m);
                return;
            }
        }
        SampleTypeTerminologyMapping fresh = new SampleTypeTerminologyMapping();
        fresh.setSampleTypeId(sampleTypeId);
        fresh.setSource(LOINC);
        fresh.setCode(code);
        fresh.setRelationship(SAME_AS);
        fresh.setIsActive("Y");
        fresh.setSysUserId(sysUserId);
        insert(fresh);
    }

    @Override
    protected SampleTypeTerminologyMappingDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleTypeTerminologyMapping> getActiveBySampleTypeId(String sampleTypeId) {
        List<SampleTypeTerminologyMapping> active = new ArrayList<>();
        for (SampleTypeTerminologyMapping m : getAllMatching("sampleTypeId", sampleTypeId)) {
            if ("Y".equals(m.getIsActive())) {
                active.add(m);
            }
        }
        return active;
    }

    @Override
    @Transactional
    public void saveMappingsForSampleType(String sampleTypeId, List<SampleTypeTerminologyMapping> desired,
            String sysUserId) {
        // Key everything (active + soft-deleted) by the natural key the DB enforces
        // unique, so a re-added (source, code) reactivates its row instead of
        // colliding on insert.
        List<SampleTypeTerminologyMapping> all = getAllMatching("sampleTypeId", sampleTypeId);
        Map<String, SampleTypeTerminologyMapping> byKey = new HashMap<>();
        for (SampleTypeTerminologyMapping m : all) {
            byKey.put(key(m.getSource(), m.getCode()), m);
        }
        Set<String> desiredKeys = new HashSet<>();
        for (SampleTypeTerminologyMapping d : desired) {
            String k = key(d.getSource(), d.getCode());
            desiredKeys.add(k);
            SampleTypeTerminologyMapping target = byKey.get(k);
            if (target != null) {
                target.setRelationship(d.getRelationship());
                target.setIsActive("Y");
                target.setSysUserId(sysUserId);
                update(target);
            } else {
                SampleTypeTerminologyMapping fresh = new SampleTypeTerminologyMapping();
                fresh.setSampleTypeId(sampleTypeId);
                fresh.setSource(d.getSource());
                fresh.setCode(d.getCode());
                fresh.setRelationship(d.getRelationship());
                fresh.setIsActive("Y");
                fresh.setSysUserId(sysUserId);
                insert(fresh);
            }
        }
        for (SampleTypeTerminologyMapping m : all) {
            if ("Y".equals(m.getIsActive()) && !desiredKeys.contains(key(m.getSource(), m.getCode()))) {
                m.setIsActive("N");
                m.setSysUserId(sysUserId);
                update(m);
            }
        }
    }

    private static String key(String source, String code) {
        return (source == null ? "" : source) + " " + (code == null ? "" : code);
    }
}
