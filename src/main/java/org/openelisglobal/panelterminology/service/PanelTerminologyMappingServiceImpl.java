package org.openelisglobal.panelterminology.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.panelterminology.dao.PanelTerminologyMappingDAO;
import org.openelisglobal.panelterminology.valueholder.PanelTerminologyMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PanelTerminologyMappingServiceImpl extends AuditableBaseObjectServiceImpl<PanelTerminologyMapping, String>
        implements PanelTerminologyMappingService {

    /** SAME_AS is the relationship that qualifies a code as the identifier. */
    private static final String SAME_AS = "SAME_AS";
    private static final String LOINC = "LOINC";
    /** panel.loinc is varchar(10) — longer codes stay in the mapping store only. */
    private static final int PANEL_LOINC_MAX_LENGTH = 10;

    @Autowired
    protected PanelTerminologyMappingDAO baseObjectDAO;
    @Autowired
    private PanelService panelService;

    PanelTerminologyMappingServiceImpl() {
        super(PanelTerminologyMapping.class);
    }

    @Override
    protected PanelTerminologyMappingDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PanelTerminologyMapping> getActiveByPanelId(String panelId) {
        List<PanelTerminologyMapping> active = new ArrayList<>();
        for (PanelTerminologyMapping m : getAllMatching("panelId", panelId)) {
            if ("Y".equals(m.getIsActive())) {
                active.add(m);
            }
        }
        return active;
    }

    @Override
    @Transactional
    public void saveMappingsForPanel(String panelId, List<PanelTerminologyMapping> desired, String sysUserId) {
        // Key everything (active + soft-deleted) by the natural key the DB enforces
        // unique, so a re-added (source, code) reactivates its row instead of
        // colliding on insert.
        List<PanelTerminologyMapping> all = getAllMatching("panelId", panelId);
        Map<String, PanelTerminologyMapping> byKey = new HashMap<>();
        for (PanelTerminologyMapping m : all) {
            byKey.put(key(m.getSource(), m.getCode()), m);
        }
        Set<String> desiredKeys = new HashSet<>();
        for (PanelTerminologyMapping d : desired) {
            String k = key(d.getSource(), d.getCode());
            desiredKeys.add(k);
            PanelTerminologyMapping target = byKey.get(k);
            if (target != null) {
                target.setRelationship(d.getRelationship());
                target.setIsActive("Y");
                target.setSysUserId(sysUserId);
                update(target);
            } else {
                PanelTerminologyMapping fresh = new PanelTerminologyMapping();
                fresh.setPanelId(panelId);
                fresh.setSource(d.getSource());
                fresh.setCode(d.getCode());
                fresh.setRelationship(d.getRelationship());
                fresh.setIsActive("Y");
                fresh.setSysUserId(sysUserId);
                insert(fresh);
            }
        }
        for (PanelTerminologyMapping m : all) {
            if ("Y".equals(m.getIsActive()) && !desiredKeys.contains(key(m.getSource(), m.getCode()))) {
                m.setIsActive("N");
                m.setSysUserId(sysUserId);
                update(m);
            }
        }
        applyLoincToLegacyPanel(panelId, desired, sysUserId);
    }

    /**
     * The primary LOINC (the first active SAME_AS LOINC mapping) stays denormalized
     * on panel.loinc — FHIR intake routes e-orders by that column
     * (getPanelByLoincCode) and the list displays it. Cleared when no SAME_AS LOINC
     * mapping remains; codes longer than the legacy column live in the mapping
     * store only.
     */
    private void applyLoincToLegacyPanel(String panelId, List<PanelTerminologyMapping> desired, String sysUserId) {
        String primary = null;
        for (PanelTerminologyMapping d : desired) {
            if (LOINC.equals(d.getSource()) && (d.getRelationship() == null || SAME_AS.equals(d.getRelationship()))) {
                primary = d.getCode();
                break;
            }
        }
        Panel panel = panelService.getPanelById(panelId);
        if (panel == null) {
            return;
        }
        if (primary != null && primary.length() > PANEL_LOINC_MAX_LENGTH) {
            LogEvent.logWarn(getClass().getSimpleName(), "applyLoincToLegacyPanel", "LOINC '" + primary
                    + "' exceeds panel.loinc's length — kept in the mapping store, legacy column unchanged");
            return;
        }
        String current = panel.getLoinc();
        if ((primary == null && current == null) || (primary != null && primary.equals(current))) {
            return;
        }
        panel.setLoinc(primary);
        panel.setSysUserId(sysUserId);
        panelService.update(panel);
    }

    private static String key(String source, String code) {
        return (source == null ? "" : source) + " " + (code == null ? "" : code);
    }
}
