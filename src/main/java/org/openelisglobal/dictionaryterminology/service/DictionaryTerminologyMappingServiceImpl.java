package org.openelisglobal.dictionaryterminology.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.dictionaryterminology.dao.DictionaryTerminologyMappingDAO;
import org.openelisglobal.dictionaryterminology.valueholder.DictionaryTerminologyMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DictionaryTerminologyMappingServiceImpl
        extends AuditableBaseObjectServiceImpl<DictionaryTerminologyMapping, String>
        implements DictionaryTerminologyMappingService {

    /** SAME_AS is the relationship that qualifies a code as the identifier. */
    private static final String SAME_AS = "SAME_AS";
    private static final String LOINC = "LOINC";
    /**
     * dictionary.loinc_code holds 20 characters; longer codes stay in the mapping
     * store only.
     */
    private static final int LEGACY_LOINC_MAX_LENGTH = 20;

    @Autowired
    protected DictionaryTerminologyMappingDAO baseObjectDAO;
    @Autowired
    private DictionaryService dictionaryService;

    DictionaryTerminologyMappingServiceImpl() {
        super(DictionaryTerminologyMapping.class);
    }

    @Override
    protected DictionaryTerminologyMappingDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DictionaryTerminologyMapping> getActiveByDictionaryId(String dictionaryId) {
        List<DictionaryTerminologyMapping> active = new ArrayList<>();
        for (DictionaryTerminologyMapping m : getAllMatching("dictionaryId", dictionaryId)) {
            if ("Y".equals(m.getIsActive())) {
                active.add(m);
            }
        }
        return active;
    }

    @Override
    @Transactional
    public void syncLegacyLoinc(String dictionaryId, String loinc, String sysUserId) {
        String code = (loinc == null || loinc.trim().isEmpty()) ? null : loinc.trim();
        List<DictionaryTerminologyMapping> all = getAllMatching("dictionaryId", dictionaryId);

        // Retire an active LOINC mapping the legacy column no longer names. Doing
        // this first keeps the unique key free for the upsert below.
        for (DictionaryTerminologyMapping m : all) {
            if (LOINC.equals(m.getSource()) && "Y".equals(m.getIsActive())
                    && !java.util.Objects.equals(m.getCode(), code)) {
                m.setIsActive("N");
                m.setSysUserId(sysUserId);
                update(m);
            }
        }
        if (code == null) {
            return;
        }

        DictionaryTerminologyMapping existing = null;
        for (DictionaryTerminologyMapping m : all) {
            if (LOINC.equals(m.getSource()) && code.equals(m.getCode())) {
                existing = m;
                break;
            }
        }
        if (existing != null) {
            // A code the editor had recorded as something other than SAME_AS keeps
            // that relationship: the legacy column says which code, not what it
            // means, and the editor is the more expressive of the two.
            if (existing.getRelationship() == null) {
                existing.setRelationship(SAME_AS);
            }
            existing.setIsActive("Y");
            existing.setSysUserId(sysUserId);
            update(existing);
            return;
        }
        DictionaryTerminologyMapping fresh = new DictionaryTerminologyMapping();
        fresh.setDictionaryId(dictionaryId);
        fresh.setSource(LOINC);
        fresh.setCode(code);
        fresh.setRelationship(SAME_AS);
        fresh.setIsActive("Y");
        fresh.setSysUserId(sysUserId);
        insert(fresh);
    }

    @Override
    @Transactional
    public void saveMappingsForDictionary(String dictionaryId, List<DictionaryTerminologyMapping> desired,
            String sysUserId) {
        // Key everything (active + soft-deleted) by the natural key the DB enforces
        // unique, so a re-added (source, code) reactivates its row instead of
        // colliding on insert.
        List<DictionaryTerminologyMapping> all = getAllMatching("dictionaryId", dictionaryId);
        Map<String, DictionaryTerminologyMapping> byKey = new HashMap<>();
        for (DictionaryTerminologyMapping m : all) {
            byKey.put(key(m.getSource(), m.getCode()), m);
        }
        Set<String> desiredKeys = new HashSet<>();
        for (DictionaryTerminologyMapping d : desired) {
            String k = key(d.getSource(), d.getCode());
            desiredKeys.add(k);
            DictionaryTerminologyMapping target = byKey.get(k);
            if (target != null) {
                target.setRelationship(d.getRelationship());
                target.setDisplayName(d.getDisplayName());
                target.setIsActive("Y");
                target.setSysUserId(sysUserId);
                update(target);
            } else {
                DictionaryTerminologyMapping fresh = new DictionaryTerminologyMapping();
                fresh.setDictionaryId(dictionaryId);
                fresh.setSource(d.getSource());
                fresh.setCode(d.getCode());
                fresh.setRelationship(d.getRelationship());
                fresh.setDisplayName(d.getDisplayName());
                fresh.setIsActive("Y");
                fresh.setSysUserId(sysUserId);
                insert(fresh);
            }
        }
        for (DictionaryTerminologyMapping m : all) {
            if ("Y".equals(m.getIsActive()) && !desiredKeys.contains(key(m.getSource(), m.getCode()))) {
                m.setIsActive("N");
                m.setSysUserId(sysUserId);
                update(m);
            }
        }
        applyLoincToLegacyColumn(dictionaryId, desired, sysUserId);
    }

    /**
     * The primary LOINC (the first active SAME_AS LOINC mapping) stays denormalized
     * on dictionary.loinc_code, which existing readers of the answer's one LOINC
     * code use. Cleared when no SAME_AS LOINC mapping remains; codes longer than
     * the legacy column live in the mapping store only.
     */
    private void applyLoincToLegacyColumn(String dictionaryId, List<DictionaryTerminologyMapping> desired,
            String sysUserId) {
        String primary = null;
        for (DictionaryTerminologyMapping d : desired) {
            if (LOINC.equals(d.getSource()) && (d.getRelationship() == null || SAME_AS.equals(d.getRelationship()))) {
                primary = d.getCode();
                break;
            }
        }
        Dictionary answer = dictionaryService.getDictionaryById(dictionaryId);
        if (answer == null) {
            return;
        }
        if (primary != null && primary.length() > LEGACY_LOINC_MAX_LENGTH) {
            LogEvent.logWarn(getClass().getSimpleName(), "applyLoincToLegacyColumn", "LOINC '" + primary
                    + "' exceeds dictionary.loinc_code's length; kept in the mapping store, legacy column unchanged");
            return;
        }
        String current = answer.getLoincCode();
        if ((primary == null && current == null) || (primary != null && primary.equals(current))) {
            return;
        }
        answer.setLoincCode(primary);
        answer.setSysUserId(sysUserId);
        dictionaryService.update(answer);
    }

    private static String key(String source, String code) {
        return (source == null ? "" : source) + " " + (code == null ? "" : code);
    }
}
