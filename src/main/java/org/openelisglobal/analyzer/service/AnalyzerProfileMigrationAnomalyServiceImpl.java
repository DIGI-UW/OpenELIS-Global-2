package org.openelisglobal.analyzer.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzer.dao.AnalyzerProfileMigrationAnomalyDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileMigrationAnomaly;
import org.openelisglobal.audittrail.dao.AuditTrailService;
import org.openelisglobal.common.action.IActionConstants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyzerProfileMigrationAnomalyServiceImpl implements AnalyzerProfileMigrationAnomalyService {

    private static final String AUDIT_TABLE = "analyzer_profile_migration_anomaly";

    private final AnalyzerProfileMigrationAnomalyDAO anomalyDAO;
    private final AuditTrailService auditTrailService;

    public AnalyzerProfileMigrationAnomalyServiceImpl(AnalyzerProfileMigrationAnomalyDAO anomalyDAO,
            AuditTrailService auditTrailService) {
        this.anomalyDAO = anomalyDAO;
        this.auditTrailService = auditTrailService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerProfileMigrationAnomaly> findOpen(String analyzerId) {
        return anomalyDAO.findOpenByAnalyzerId(requireText(analyzerId, "analyzerId is required"));
    }

    @Override
    @Transactional
    public List<AnalyzerProfileMigrationAnomaly> replaceOpen(Analyzer analyzer,
            List<AnalyzerProfileMigrationAnomalyDraft> findings, String actor) {
        if (analyzer == null) {
            throw new IllegalArgumentException("analyzer is required");
        }
        String analyzerId = requireText(analyzer.getId(), "analyzer id is required");
        String effectiveActor = requireText(actor, "actor is required");
        List<AnalyzerProfileMigrationAnomalyDraft> orderedFindings = orderedDistinct(findings);
        List<AnalyzerProfileMigrationAnomaly> current = anomalyDAO.findByAnalyzerIdForUpdate(analyzerId);
        Map<String, AnalyzerProfileMigrationAnomaly> currentByKey = new LinkedHashMap<>();
        current.forEach(anomaly -> currentByKey.put(anomaly.getEvidenceKey(), anomaly));

        List<AnalyzerProfileMigrationAnomaly> open = new ArrayList<>();
        for (AnalyzerProfileMigrationAnomalyDraft finding : orderedFindings) {
            AnalyzerProfileMigrationAnomaly existing = currentByKey.remove(finding.evidenceKey());
            if (existing != null) {
                open.add(existing);
                continue;
            }
            AnalyzerProfileMigrationAnomaly created = create(analyzer, finding, effectiveActor);
            anomalyDAO.insert(created);
            auditTrailService.saveNewHistory(created, effectiveActor, AUDIT_TABLE);
            open.add(created);
        }

        currentByKey.values().forEach(anomaly -> resolve(anomaly, effectiveActor));
        open.sort(Comparator.comparing(AnalyzerProfileMigrationAnomaly::getEvidenceKey));
        return List.copyOf(open);
    }

    @Override
    @Transactional
    public void resolveAll(String analyzerId, String actor) {
        String effectiveAnalyzerId = requireText(analyzerId, "analyzerId is required");
        String effectiveActor = requireText(actor, "actor is required");
        anomalyDAO.findByAnalyzerIdForUpdate(effectiveAnalyzerId).forEach(anomaly -> resolve(anomaly, effectiveActor));
    }

    private void resolve(AnalyzerProfileMigrationAnomaly anomaly, String actor) {
        AnalyzerProfileMigrationAnomaly previous = copy(anomaly);
        anomaly.setStatus(AnalyzerProfileMigrationAnomaly.Status.RESOLVED);
        anomaly.setResolvedBy(actor);
        anomaly.setResolvedAt(Timestamp.from(Instant.now()));
        anomaly.setSysUserId(actor);
        anomalyDAO.update(anomaly);
        auditTrailService.saveHistory(anomaly, previous, actor, IActionConstants.AUDIT_TRAIL_UPDATE, AUDIT_TABLE);
    }

    private static AnalyzerProfileMigrationAnomaly create(Analyzer analyzer,
            AnalyzerProfileMigrationAnomalyDraft finding, String actor) {
        AnalyzerProfileMigrationAnomaly anomaly = new AnalyzerProfileMigrationAnomaly();
        anomaly.setAnalyzer(analyzer);
        anomaly.setCode(finding.code());
        anomaly.setEvidenceKey(finding.evidenceKey());
        anomaly.setLegacySourceKey(finding.legacySourceKey());
        anomaly.setLegacyTestId(finding.legacyTestId());
        anomaly.setDetail(finding.detail());
        anomaly.setStatus(AnalyzerProfileMigrationAnomaly.Status.OPEN);
        anomaly.setDetectedBy(actor);
        anomaly.setDetectedAt(Timestamp.from(Instant.now()));
        anomaly.setSysUserId(actor);
        return anomaly;
    }

    private static List<AnalyzerProfileMigrationAnomalyDraft> orderedDistinct(
            List<AnalyzerProfileMigrationAnomalyDraft> findings) {
        if (findings == null) {
            throw new IllegalArgumentException("migration anomaly findings are required");
        }
        Map<String, AnalyzerProfileMigrationAnomalyDraft> byKey = new LinkedHashMap<>();
        for (AnalyzerProfileMigrationAnomalyDraft finding : findings) {
            if (finding == null) {
                throw new IllegalArgumentException("migration anomaly finding is required");
            }
            if (byKey.putIfAbsent(finding.evidenceKey(), finding) != null) {
                throw new IllegalArgumentException("Duplicate migration anomaly evidence: " + finding.evidenceKey());
            }
        }
        return byKey.values().stream().sorted(Comparator.comparing(AnalyzerProfileMigrationAnomalyDraft::evidenceKey))
                .toList();
    }

    private static AnalyzerProfileMigrationAnomaly copy(AnalyzerProfileMigrationAnomaly source) {
        AnalyzerProfileMigrationAnomaly copy = new AnalyzerProfileMigrationAnomaly();
        copy.setId(source.getId());
        copy.setAnalyzer(source.getAnalyzer());
        copy.setCode(source.getCode());
        copy.setEvidenceKey(source.getEvidenceKey());
        copy.setLegacySourceKey(source.getLegacySourceKey());
        copy.setLegacyTestId(source.getLegacyTestId());
        copy.setDetail(source.getDetail());
        copy.setStatus(source.getStatus());
        copy.setDetectedBy(source.getDetectedBy());
        copy.setDetectedAt(source.getDetectedAt());
        copy.setResolvedBy(source.getResolvedBy());
        copy.setResolvedAt(source.getResolvedAt());
        copy.setLastupdated(source.getLastupdated());
        return copy;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
