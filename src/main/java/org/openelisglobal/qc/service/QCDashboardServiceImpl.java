package org.openelisglobal.qc.service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.openelisglobal.qc.dao.QCRuleViolationDAO;
import org.openelisglobal.qc.valueholder.QCRuleViolation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for QC Dashboard (T120).
 *
 * Following Constitution IV.5: @Transactional in services ONLY (NOT
 * controllers) Compiles all data within transaction to prevent
 * LazyInitializationException.
 */
@Service
public class QCDashboardServiceImpl implements QCDashboardService {

    private static final String COLOR_GREEN = "GREEN";
    private static final String COLOR_YELLOW = "YELLOW";
    private static final String COLOR_RED = "RED";

    private static final String SEVERITY_REJECTION = "REJECTION";
    private static final String SEVERITY_WARNING = "WARNING";

    private static final String STATUS_UNRESOLVED = "UNRESOLVED";

    @Autowired
    private QCRuleViolationDAO violationDAO;

    @Override
    @Transactional(readOnly = true)
    public List<InstrumentComplianceStatus> getAllInstrumentComplianceStatus() {
        // Get all unresolved violations and group by instrument
        List<QCRuleViolation> allUnresolved = violationDAO.findUnresolved();

        // Get unique instrument IDs
        Set<Integer> instrumentIds = allUnresolved.stream().map(QCRuleViolation::getInstrumentId)
                .collect(Collectors.toSet());

        List<InstrumentComplianceStatus> statuses = new ArrayList<>();

        for (Integer instrumentId : instrumentIds) {
            InstrumentComplianceStatus status = buildInstrumentStatus(instrumentId, allUnresolved);
            statuses.add(status);
        }

        // Sort by compliance color (RED first, then YELLOW, then GREEN)
        statuses.sort((a, b) -> {
            int colorOrder = getColorOrder(a.getComplianceColor()) - getColorOrder(b.getComplianceColor());
            if (colorOrder != 0) {
                return colorOrder;
            }
            return a.getInstrumentId().compareTo(b.getInstrumentId());
        });

        return statuses;
    }

    @Override
    @Transactional(readOnly = true)
    public InstrumentComplianceStatus getInstrumentComplianceStatus(Integer instrumentId) {
        List<QCRuleViolation> violations = violationDAO.findUnresolvedByInstrument(instrumentId);
        return buildInstrumentStatus(instrumentId, violations);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardSummary getDashboardSummary() {
        List<InstrumentComplianceStatus> allStatuses = getAllInstrumentComplianceStatus();

        DashboardSummary summary = new DashboardSummary();
        summary.setTotalInstruments(allStatuses.size());

        int compliant = 0;
        int warning = 0;
        int nonCompliant = 0;
        int totalRejections = 0;
        int totalWarnings = 0;

        for (InstrumentComplianceStatus status : allStatuses) {
            switch (status.getComplianceColor()) {
            case COLOR_GREEN:
                compliant++;
                break;
            case COLOR_YELLOW:
                warning++;
                break;
            case COLOR_RED:
                nonCompliant++;
                break;
            }
            totalRejections += status.getUnresolvedRejections();
            totalWarnings += status.getUnresolvedWarnings();
        }

        summary.setCompliantInstruments(compliant);
        summary.setWarningInstruments(warning);
        summary.setNonCompliantInstruments(nonCompliant);
        summary.setTotalRejections(totalRejections);
        summary.setTotalWarnings(totalWarnings);
        summary.setTotalUnresolvedViolations(totalRejections + totalWarnings);
        summary.setLastUpdateTime(DateTimeFormatter.ISO_INSTANT.format(Instant.now()));

        return summary;
    }

    /**
     * Build the compliance status for a specific instrument.
     */
    private InstrumentComplianceStatus buildInstrumentStatus(Integer instrumentId,
            List<QCRuleViolation> allViolations) {

        // Filter violations for this instrument
        List<QCRuleViolation> instrumentViolations = allViolations.stream()
                .filter(v -> instrumentId.equals(v.getInstrumentId()))
                .filter(v -> STATUS_UNRESOLVED.equals(v.getResolutionStatus())).collect(Collectors.toList());

        InstrumentComplianceStatus status = new InstrumentComplianceStatus();
        status.setInstrumentId(instrumentId);
        status.setInstrumentName("Instrument " + instrumentId); // Would lookup actual name

        // Count violations by severity
        int rejections = 0;
        int warnings = 0;
        Set<String> triggeredRules = new HashSet<>();
        String lastViolationTime = null;

        for (QCRuleViolation violation : instrumentViolations) {
            if (SEVERITY_REJECTION.equals(violation.getSeverity())) {
                rejections++;
            } else if (SEVERITY_WARNING.equals(violation.getSeverity())) {
                warnings++;
            }
            triggeredRules.add(violation.getRuleCode());

            if (violation.getViolationDateTime() != null) {
                String violationTime = violation.getViolationDateTime().toInstant().toString();
                if (lastViolationTime == null || violationTime.compareTo(lastViolationTime) > 0) {
                    lastViolationTime = violationTime;
                }
            }
        }

        status.setUnresolvedRejections(rejections);
        status.setUnresolvedWarnings(warnings);
        status.setTriggeredRules(new ArrayList<>(triggeredRules));
        status.setLastViolationTime(lastViolationTime);

        // Calculate compliance color
        status.setComplianceColor(calculateComplianceColor(rejections, warnings));

        return status;
    }

    /**
     * Calculate compliance color based on violation counts. RED: Any unresolved
     * REJECTION violations YELLOW: Only WARNING violations (no rejections) GREEN:
     * No unresolved violations
     */
    private String calculateComplianceColor(int rejections, int warnings) {
        if (rejections > 0) {
            return COLOR_RED;
        } else if (warnings > 0) {
            return COLOR_YELLOW;
        } else {
            return COLOR_GREEN;
        }
    }

    /**
     * Get sort order for compliance colors (lower = higher priority).
     */
    private int getColorOrder(String color) {
        switch (color) {
        case COLOR_RED:
            return 0;
        case COLOR_YELLOW:
            return 1;
        case COLOR_GREEN:
            return 2;
        default:
            return 3;
        }
    }
}
