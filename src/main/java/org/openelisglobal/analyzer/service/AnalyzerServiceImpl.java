package org.openelisglobal.analyzer.service;

import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.openelisglobal.analyzer.dao.AnalyzerDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.Analyzer.AnalyzerStatus;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyzerServiceImpl extends AuditableBaseObjectServiceImpl<Analyzer, String> implements AnalyzerService {

    public static final int SOFT_DELETE_WINDOW_DAYS = 90;

    private static final Set<AnalyzerStatus> MANUALLY_SETTABLE_STATUSES = EnumSet.of(AnalyzerStatus.INACTIVE,
            AnalyzerStatus.SETUP, AnalyzerStatus.VALIDATION);

    @Autowired
    protected AnalyzerDAO baseObjectDAO;

    @Autowired
    private AnalyzerTestMappingService analyzerMappingService;

    @Autowired
    private AnalyzerResultsService analyzerResultsService;

    AnalyzerServiceImpl() {
        super(Analyzer.class);
    }

    @Override
    protected AnalyzerDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analyzer> getAllWithTypes() {
        return baseObjectDAO.findAllWithTypes();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Analyzer> getWithType(String id) {
        return baseObjectDAO.findByIdWithType(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Analyzer getAnalyzerByName(String name) {
        return getMatch("name", name).orElse(null);
    }

    @Override
    @Transactional
    public void persistData(Analyzer analyzer, List<AnalyzerTestMapping> testMappings,
            List<AnalyzerTestMapping> existingMappings) {
        if (analyzer.getId() == null) {
            insert(analyzer);
        } else {
            update(analyzer);
        }

        for (AnalyzerTestMapping mapping : testMappings) {
            mapping.setAnalyzerId(analyzer.getId());
            if (newMapping(mapping, existingMappings)) {
                mapping.setSysUserId("1");
                analyzerMappingService.insert(mapping);
                existingMappings.add(mapping);
            } else {
                mapping.setLastupdated(analyzerMappingService.get(mapping.getId()).getLastupdated());
                mapping.setSysUserId("1");
                analyzerMappingService.update(mapping);
            }
        }
    }

    private boolean newMapping(AnalyzerTestMapping mapping, List<AnalyzerTestMapping> existingMappings) {
        for (AnalyzerTestMapping existingMap : existingMappings) {
            if (existingMap.getAnalyzerId().equals(mapping.getAnalyzerId())
                    && existingMap.getAnalyzerTestName().equals(mapping.getAnalyzerTestName())) {
                return false;
            }
        }
        return true;
    }

    // --- Methods migrated from AnalyzerConfigurationService ---

    @Override
    @Transactional(readOnly = true)
    public Optional<Analyzer> getByIpAddress(String ipAddress) {
        return baseObjectDAO.findByIpAddress(ipAddress);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Analyzer> getByName(String name) {
        return baseObjectDAO.findByName(name);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Analyzer> findByIdentifierPatternMatch(String analyzerIdentifier) {
        if (analyzerIdentifier == null || analyzerIdentifier.trim().isEmpty()) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "findByIdentifierPatternMatch",
                    "Empty analyzer identifier");
            return Optional.empty();
        }

        List<Analyzer> candidates = baseObjectDAO.findGenericAnalyzersWithPatterns();
        LogEvent.logDebug(this.getClass().getSimpleName(), "findByIdentifierPatternMatch",
                "Looking for match: identifier='" + analyzerIdentifier + "', candidates="
                        + (candidates != null ? candidates.size() : 0));
        if (candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }

        String identifier = analyzerIdentifier.trim();
        for (Analyzer analyzer : candidates) {
            if (analyzer.getIdentifierPattern() == null) {
                continue;
            }
            try {
                String pattern = analyzer.getIdentifierPattern();
                Pattern p = Pattern.compile(pattern);
                if (p.matcher(identifier).find()) {
                    LogEvent.logInfo(this.getClass().getSimpleName(), "findByIdentifierPatternMatch", "MATCHED: '"
                            + identifier + "' matched pattern '" + pattern + "' for analyzer " + analyzer.getName());
                    return Optional.of(analyzer);
                }
            } catch (PatternSyntaxException e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "findByIdentifierPatternMatch",
                        "Invalid identifier_pattern regex for analyzer id=" + analyzer.getId());
            }
        }

        LogEvent.logWarn(this.getClass().getSimpleName(), "findByIdentifierPatternMatch",
                "No match found for identifier '" + identifier + "' among " + candidates.size() + " candidates");
        return Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasRecentResults(String analyzerId) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -SOFT_DELETE_WINDOW_DAYS);
        Date cutoffDate = calendar.getTime();

        List<AnalyzerResults> results = analyzerResultsService.getResultsbyAnalyzer(analyzerId);
        for (AnalyzerResults result : results) {
            Date resultDate = null;
            if (result.getCompleteDate() != null) {
                resultDate = new Date(result.getCompleteDate().getTime());
            } else if (result.getLastupdated() != null) {
                resultDate = new Date(result.getLastupdated().getTime());
            }
            if (resultDate != null && resultDate.after(cutoffDate)) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canTransitionTo(String analyzerId, AnalyzerStatus newStatus) {
        Analyzer analyzer = get(analyzerId);
        if (analyzer == null) {
            return false;
        }
        AnalyzerStatus currentStatus = analyzer.getStatus();
        return validateStatusTransition(currentStatus, newStatus);
    }

    @Override
    public boolean validateStatusTransition(AnalyzerStatus currentStatus, AnalyzerStatus newStatus) {
        if (currentStatus == null || newStatus == null) {
            return false;
        }
        if (currentStatus == newStatus) {
            return true;
        }
        if (newStatus == AnalyzerStatus.INACTIVE) {
            return true;
        }
        if (newStatus == AnalyzerStatus.DELETED) {
            return currentStatus == AnalyzerStatus.INACTIVE;
        }

        switch (currentStatus) {
        case INACTIVE:
            return newStatus == AnalyzerStatus.SETUP;
        case SETUP:
            return newStatus == AnalyzerStatus.VALIDATION;
        case VALIDATION:
            return newStatus == AnalyzerStatus.ACTIVE || newStatus == AnalyzerStatus.SETUP;
        case ACTIVE:
            return newStatus == AnalyzerStatus.ERROR_PENDING || newStatus == AnalyzerStatus.OFFLINE;
        case ERROR_PENDING:
            return newStatus == AnalyzerStatus.ACTIVE || newStatus == AnalyzerStatus.OFFLINE;
        case OFFLINE:
            return newStatus == AnalyzerStatus.ACTIVE || newStatus == AnalyzerStatus.ERROR_PENDING;
        case DELETED:
            return newStatus == AnalyzerStatus.INACTIVE;
        default:
            return false;
        }
    }

    @Override
    @Transactional
    public Analyzer setStatusManually(String analyzerId, AnalyzerStatus status, String userId) {
        if (!MANUALLY_SETTABLE_STATUSES.contains(status)) {
            throw new LIMSRuntimeException(
                    "Status " + status + " cannot be set manually. Only INACTIVE, SETUP, and VALIDATION are allowed.");
        }

        Analyzer analyzer = get(analyzerId);
        if (analyzer == null) {
            throw new LIMSRuntimeException("Analyzer not found: " + analyzerId);
        }

        AnalyzerStatus oldStatus = analyzer.getStatus();
        if (!validateStatusTransition(oldStatus, status)) {
            throw new LIMSRuntimeException("Invalid status transition from " + oldStatus + " to " + status);
        }

        analyzer.setStatus(status);
        analyzer.setSysUserId(userId);
        analyzer.setLastupdatedFields();

        update(analyzer);

        LogEvent.logInfo(this.getClass().getSimpleName(), "setStatusManually", "Analyzer " + analyzerId
                + " status manually changed from " + oldStatus + " to " + status + " by user " + userId);

        return analyzer;
    }
}
