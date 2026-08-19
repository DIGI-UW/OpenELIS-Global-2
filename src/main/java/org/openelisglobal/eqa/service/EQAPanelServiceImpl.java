package org.openelisglobal.eqa.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analyte.service.AnalyteService;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.eqa.dao.EQAPanelDAO;
import org.openelisglobal.eqa.dao.EQAPanelSampleDAO;
import org.openelisglobal.eqa.valueholder.EQAPanel;
import org.openelisglobal.eqa.valueholder.EQAPanelSample;
import org.openelisglobal.eqa.valueholder.EQAPanelStatus;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.eqa.valueholder.EQAUnblindMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Panel lifecycle + sealed-target read rule (OGC-609). SCORED/CLOSED moves
 * belong to the scoring flows, not here.
 */
@Service
@Transactional
public class EQAPanelServiceImpl extends BaseObjectServiceImpl<EQAPanel, Long> implements EQAPanelService {

    /** FR-V2.1-11's forward edges for the moves this service owns. */
    private static final Map<EQAPanelStatus, Set<EQAPanelStatus>> EDGES = new EnumMap<>(EQAPanelStatus.class);

    /** Targets stay hidden until one of these states (FR-V2.1-16). */
    private static final Set<EQAPanelStatus> TARGETS_REVEALED = EnumSet.of(EQAPanelStatus.UNBLINDED,
            EQAPanelStatus.SCORED, EQAPanelStatus.CLOSED);

    static {
        EDGES.put(EQAPanelStatus.PREPARING, EnumSet.of(EQAPanelStatus.SEALED));
        EDGES.put(EQAPanelStatus.SEALED, EnumSet.of(EQAPanelStatus.DISTRIBUTED));
        EDGES.put(EQAPanelStatus.DISTRIBUTED, EnumSet.of(EQAPanelStatus.UNBLINDED));
    }

    @Autowired
    private EQAPanelDAO eqaPanelDAO;

    @Autowired
    private EQAPanelSampleDAO eqaPanelSampleDAO;

    @Autowired
    private AnalyteService analyteService;

    public EQAPanelServiceImpl() {
        super(EQAPanel.class);
    }

    @Override
    protected EQAPanelDAO getBaseObjectDAO() {
        return eqaPanelDAO;
    }

    @Override
    public EQAPanel seal(Long panelId, String sysUserId) {
        EQAPanel panel = get(panelId);
        requireEdge(panel, EQAPanelStatus.SEALED);

        List<EQAPanelSample> samples = samplesOf(panelId);
        if (samples.isEmpty()) {
            throw new IllegalArgumentException("Cannot seal a panel with no samples");
        }
        // EncryptionConverter passes blank values through unencrypted, so the seal is
        // the last gate that can keep plaintext-blank targets out of the column.
        if (samples.stream().anyMatch(s -> GenericValidator.isBlankOrNull(s.getTargetValue()))) {
            throw new IllegalArgumentException("Cannot seal a panel while a sample has no target value");
        }
        if (panel.getScheme() != null && panel.getScheme().getSchemeType() == EQASchemeType.IN_HOUSE
                && panel.getUnblindDate() == null) {
            throw new IllegalArgumentException("An in-house panel requires an unblind date before sealing");
        }

        return move(panel, EQAPanelStatus.SEALED, sysUserId);
    }

    @Override
    public EQAPanel distribute(Long panelId, String sysUserId) {
        EQAPanel panel = get(panelId);
        requireEdge(panel, EQAPanelStatus.DISTRIBUTED);
        return move(panel, EQAPanelStatus.DISTRIBUTED, sysUserId);
    }

    @Override
    public EQAPanel unblind(Long panelId, String sysUserId) {
        EQAPanel panel = get(panelId);
        requireEdge(panel, EQAPanelStatus.UNBLINDED);
        return move(panel, EQAPanelStatus.UNBLINDED, sysUserId);
    }

    @Override
    public EQAPanel unblindForUpdate(Long panelId, String sysUserId, EQAUnblindMethod method) {
        if (method == null) {
            throw new IllegalArgumentException("An unblind must record whether it was scheduled or manual");
        }
        EQAPanel panel = eqaPanelDAO.getForUpdate(panelId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown panel " + panelId));
        // The loser of a race re-reads the committed status here and fails the
        // edge check, rather than both callers passing it against the same
        // pre-lock read and scoring twice.
        requireEdge(panel, EQAPanelStatus.UNBLINDED);
        panel.setUnblindMethod(method);
        panel.setUnblindedBy(GenericValidator.isBlankOrNull(sysUserId) ? null : Long.valueOf(sysUserId));
        panel.setUnblindedAt(new Timestamp(System.currentTimeMillis()));
        return move(panel, EQAPanelStatus.UNBLINDED, sysUserId);
    }

    private void requireEdge(EQAPanel panel, EQAPanelStatus target) {
        Set<EQAPanelStatus> legal = EDGES.getOrDefault(panel.getStatus(), EnumSet.noneOf(EQAPanelStatus.class));
        if (!legal.contains(target)) {
            throw new IllegalStateException("Cannot move a panel from " + panel.getStatus() + " to " + target);
        }
    }

    private EQAPanel move(EQAPanel panel, EQAPanelStatus target, String sysUserId) {
        panel.setStatus(target);
        panel.setSysUserId(sysUserId);
        return eqaPanelDAO.update(panel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPanelDtos(Long cycleId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (EQAPanel panel : eqaPanelDAO.getAllMatching("cycle.id", cycleId)) {
            rows.add(toPanelDto(panel));
        }
        return rows;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> toPanelDto(EQAPanel panel) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", panel.getId());
        dto.put("panelName", panel.getPanelName());
        dto.put("panelType", panel.getPanelType());
        dto.put("status", panel.getStatus() == null ? null : panel.getStatus().name());
        dto.put("schemeId", panel.getScheme() == null ? null : panel.getScheme().getId());
        dto.put("cycleId", panel.getCycle() == null ? null : panel.getCycle().getId());
        dto.put("sourceType", panel.getSourceType() == null ? null : panel.getSourceType().name());
        dto.put("lotNumber", panel.getLotNumber());
        dto.put("unblindDate", panel.getUnblindDate() == null ? null : panel.getUnblindDate().toString());
        dto.put("aliquotsProduced", panel.getAliquotsProduced());
        dto.put("aliquotsReserved", panel.getAliquotsReserved());
        dto.put("aliquotsShipped", panel.getAliquotsShipped());
        dto.put("homogeneityQcPassed", panel.getHomogeneityQcPassed());
        dto.put("expirationDate", panel.getExpirationDate() == null ? null : panel.getExpirationDate().toString());
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSampleDtos(Long panelId, boolean callerCanUnblind) {
        EQAPanel panel = get(panelId);
        boolean revealTargets = TARGETS_REVEALED.contains(panel.getStatus()) || callerCanUnblind;

        List<Map<String, Object>> rows = new ArrayList<>();
        for (EQAPanelSample sample : samplesOf(panelId)) {
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("id", sample.getId());
            dto.put("panelId", panelId);
            dto.put("sampleCode", sample.getSampleCode());
            dto.put("blindCode", sample.getBlindCode());
            dto.put("analyteId", sample.getAnalyteId());
            // Names, not ids, so the pack list a courier reads is legible (T-25).
            dto.put("analyteName", analyteName(sample.getAnalyteId()));
            // The blinding guarantee: nulls, not omissions, so the shape is stable and a
            // client cannot infer anything from missing keys.
            dto.put("targetValue", revealTargets ? sample.getTargetValue() : null);
            dto.put("targetUnit", revealTargets ? sample.getTargetUnit() : null);
            dto.put("acceptanceRangeLow", revealTargets ? sample.getAcceptanceRangeLow() : null);
            dto.put("acceptanceRangeHigh", revealTargets ? sample.getAcceptanceRangeHigh() : null);
            dto.put("targetsRevealed", revealTargets);
            rows.add(dto);
        }
        return rows;
    }

    /** Null rather than an error for an analyte that no longer resolves. */
    private String analyteName(Long analyteId) {
        if (analyteId == null) {
            return null;
        }
        Analyte analyte = analyteService.get(String.valueOf(analyteId));
        return analyte == null ? null : analyte.getAnalyteName();
    }

    private List<EQAPanelSample> samplesOf(Long panelId) {
        return eqaPanelSampleDAO.getAllMatchingOrdered("panel.id", panelId, "sampleCode", false);
    }
}
