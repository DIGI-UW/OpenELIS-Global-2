package org.openelisglobal.analyzer.service;

import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.openelisglobal.analyzer.dao.AnalyzerConfigurationDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerConfiguration;
import org.openelisglobal.analyzer.valueholder.AnalyzerConfiguration.AnalyzerStatus;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AnalyzerConfigurationServiceImpl extends BaseObjectServiceImpl<AnalyzerConfiguration, String>
        implements AnalyzerConfigurationService {

    /**
     * Soft delete window in days - analyzers with recent results within this window
     * cannot be hard deleted
     */
    public static final int SOFT_DELETE_WINDOW_DAYS = 90;

    @Autowired
    private AnalyzerConfigurationDAO analyzerConfigurationDAO;

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private AnalyzerResultsService analyzerResultsService;

    public AnalyzerConfigurationServiceImpl() {
        super(AnalyzerConfiguration.class);
    }

    @Override
    protected AnalyzerConfigurationDAO getBaseObjectDAO() {
        return analyzerConfigurationDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalyzerConfiguration> getByAnalyzerId(String analyzerId) {
        return analyzerConfigurationDAO.findByAnalyzerId(analyzerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalyzerConfiguration> getByIpAddress(String ipAddress) {
        return analyzerConfigurationDAO.findByIpAddress(ipAddress);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalyzerConfiguration> getByAnalyzerName(String name) {
        return analyzerConfigurationDAO.findByAnalyzerName(name);
    }

    @Override
    public String createConfiguration(Analyzer analyzer, String ipAddress, Integer port, List<String> testUnitIds) {
        // Check if configuration already exists
        Optional<AnalyzerConfiguration> existing = analyzerConfigurationDAO.findByAnalyzerId(analyzer.getId());
        if (existing.isPresent()) {
            throw new LIMSRuntimeException("AnalyzerConfiguration already exists for analyzer: " + analyzer.getId());
        }

        AnalyzerConfiguration config = new AnalyzerConfiguration();
        config.setAnalyzer(analyzer);
        config.setIpAddress(ipAddress);
        config.setPort(port);
        config.setProtocolVersion("ASTM LIS2-A2");
        config.setTestUnitIds(testUnitIds);
        config.setSysUserId("1"); // Default system user (should come from security context)

        return analyzerConfigurationDAO.insert(config);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerConfiguration> getAllWithAnalyzers() {
        return analyzerConfigurationDAO.getAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalyzerConfiguration> findByIdentifierPatternMatch(String analyzerIdentifier) {
        if (analyzerIdentifier == null || analyzerIdentifier.trim().isEmpty()) {
            return Optional.empty();
        }

        List<AnalyzerConfiguration> candidates = analyzerConfigurationDAO.findGenericPluginConfigsWithPatterns();
        if (candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }

        String identifier = analyzerIdentifier.trim();
        for (AnalyzerConfiguration config : candidates) {
            if (config == null || config.getIdentifierPattern() == null) {
                continue;
            }

            try {
                Pattern p = Pattern.compile(config.getIdentifierPattern());
                if (p.matcher(identifier).find()) {
                    return Optional.of(config);
                }
            } catch (PatternSyntaxException e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "findByIdentifierPatternMatch",
                        "Invalid identifier_pattern regex for analyzer_configuration id=" + config.getId());
            } catch (Exception e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "findByIdentifierPatternMatch",
                        "Error evaluating identifier_pattern for analyzer_configuration id=" + config.getId());
            }
        }

        return Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasRecentResults(String analyzerId) {
        // Calculate cutoff date (90 days ago)
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -SOFT_DELETE_WINDOW_DAYS);
        Date cutoffDate = calendar.getTime();

        // Get all results for this analyzer
        List<AnalyzerResults> results = analyzerResultsService.getResultsbyAnalyzer(analyzerId);

        // Check if any result has complete_date or lastupdated within the window
        for (AnalyzerResults result : results) {
            Date resultDate = null;
            if (result.getCompleteDate() != null) {
                resultDate = new Date(result.getCompleteDate().getTime());
            } else if (result.getLastupdated() != null) {
                resultDate = new Date(result.getLastupdated().getTime());
            }

            if (resultDate != null && resultDate.after(cutoffDate)) {
                return true; // Found a recent result
            }
        }

        return false; // No recent results found
    }

    // === Status Transition Validation Methods (T151c) ===

    /**
     * Valid status transitions map - defines what transitions are allowed.
     * 
     * Manual transitions (user-initiated): - Any → INACTIVE (always allowed -
     * manual deactivation) - INACTIVE → SETUP (reactivation) - SETUP → VALIDATION
     * (when mappings are being created) - VALIDATION → SETUP (rollback if mappings
     * need changes)
     * 
     * Automatic transitions (system-triggered): - SETUP → VALIDATION (first mapping
     * created) - VALIDATION → ACTIVE (all required mappings activated) - ACTIVE →
     * ERROR_PENDING (unacknowledged error created) - ACTIVE → OFFLINE (connection
     * test failed) - ERROR_PENDING → ACTIVE (all errors acknowledged) - OFFLINE →
     * ACTIVE (connection test succeeded)
     */
    private static final Set<AnalyzerStatus> MANUALLY_SETTABLE_STATUSES = EnumSet.of(AnalyzerStatus.INACTIVE,
            AnalyzerStatus.SETUP, AnalyzerStatus.VALIDATION);

    @Override
    @Transactional(readOnly = true)
    public boolean canTransitionTo(String analyzerId, AnalyzerStatus newStatus) {
        Optional<AnalyzerConfiguration> configOpt = getByAnalyzerId(analyzerId);
        if (configOpt.isEmpty()) {
            return false;
        }
        AnalyzerStatus currentStatus = configOpt.get().getStatus();
        return validateStatusTransition(currentStatus, newStatus);
    }

    @Override
    public boolean validateStatusTransition(AnalyzerStatus currentStatus, AnalyzerStatus newStatus) {
        if (currentStatus == null || newStatus == null) {
            return false;
        }

        // Same status is not a transition
        if (currentStatus == newStatus) {
            return true;
        }

        // Transition to INACTIVE is always allowed (manual override)
        if (newStatus == AnalyzerStatus.INACTIVE) {
            return true;
        }

        // Transition to DELETED requires INACTIVE first
        if (newStatus == AnalyzerStatus.DELETED) {
            return currentStatus == AnalyzerStatus.INACTIVE;
        }

        // Define valid transitions based on current status
        switch (currentStatus) {
        case INACTIVE:
            // From INACTIVE can go to SETUP (reactivation)
            return newStatus == AnalyzerStatus.SETUP;

        case SETUP:
            // From SETUP can go to VALIDATION (mappings being created)
            return newStatus == AnalyzerStatus.VALIDATION;

        case VALIDATION:
            // From VALIDATION can go to:
            // - ACTIVE (all required mappings activated)
            // - SETUP (rollback for changes)
            return newStatus == AnalyzerStatus.ACTIVE || newStatus == AnalyzerStatus.SETUP;

        case ACTIVE:
            // From ACTIVE can go to:
            // - ERROR_PENDING (unacknowledged error)
            // - OFFLINE (connection failed)
            return newStatus == AnalyzerStatus.ERROR_PENDING || newStatus == AnalyzerStatus.OFFLINE;

        case ERROR_PENDING:
            // From ERROR_PENDING can go to:
            // - ACTIVE (all errors acknowledged)
            // - OFFLINE (connection failed while in error state)
            return newStatus == AnalyzerStatus.ACTIVE || newStatus == AnalyzerStatus.OFFLINE;

        case OFFLINE:
            // From OFFLINE can go to:
            // - ACTIVE (connection restored)
            // - ERROR_PENDING (had errors when went offline, now back)
            return newStatus == AnalyzerStatus.ACTIVE || newStatus == AnalyzerStatus.ERROR_PENDING;

        case DELETED:
            // From DELETED, no transitions allowed (except to INACTIVE for recovery)
            return newStatus == AnalyzerStatus.INACTIVE;

        default:
            return false;
        }
    }

    @Override
    @Transactional
    public AnalyzerConfiguration setStatusManually(String analyzerId, AnalyzerStatus status, String userId) {
        // Validate the status is manually settable
        if (!MANUALLY_SETTABLE_STATUSES.contains(status)) {
            throw new LIMSRuntimeException(
                    "Status " + status + " cannot be set manually. Only INACTIVE, SETUP, and VALIDATION are allowed.");
        }

        Optional<AnalyzerConfiguration> configOpt = getByAnalyzerId(analyzerId);
        if (configOpt.isEmpty()) {
            throw new LIMSRuntimeException("AnalyzerConfiguration not found for analyzer: " + analyzerId);
        }

        AnalyzerConfiguration config = configOpt.get();
        AnalyzerStatus oldStatus = config.getStatus();

        // Validate the transition is allowed
        if (!validateStatusTransition(oldStatus, status)) {
            throw new LIMSRuntimeException("Invalid status transition from " + oldStatus + " to " + status);
        }

        // Update status
        config.setStatus(status);
        config.setSysUserId(userId);
        config.setLastupdatedFields();

        // If transitioning to ACTIVE, record activation date
        if (status == AnalyzerStatus.ACTIVE && oldStatus != AnalyzerStatus.ACTIVE) {
            config.setLastActivatedDate(new Date());
        }

        update(config);

        // Log the status change for audit trail
        LogEvent.logInfo(this.getClass().getSimpleName(), "setStatusManually", "Analyzer " + analyzerId
                + " status manually changed from " + oldStatus + " to " + status + " by user " + userId);

        return config;
    }
}
