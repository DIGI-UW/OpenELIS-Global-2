package org.openelisglobal.analyzer.service;

import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.openelisglobal.analyzer.dao.AnalyzerDAO;
import org.openelisglobal.analyzer.dao.AnalyzerErrorDAO;
import org.openelisglobal.analyzer.dao.AnalyzerFileUploadDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.Analyzer.AnalyzerStatus;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.analyzerimport.util.AnalyzerTestNameCache;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.notebook.dao.NoteBookDAO;
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

    @Autowired
    private org.openelisglobal.test.service.TestService testService;

    @Autowired
    private AnalyzerPluginConfigService analyzerPluginConfigService;

    @Autowired
    PluginAnalyzerService pluginAnalyzerService;

    @Autowired
    private AnalyzerErrorDAO analyzerErrorDAO;

    @Autowired
    private AnalyzerFileUploadDAO analyzerFileUploadDAO;

    @Autowired
    private NoteBookDAO noteBookDAO;

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

        persistTestMappings(analyzer.getAnalyzerType() != null ? analyzer.getAnalyzerType().getId() : null,
                testMappings, existingMappings);
    }

    @Override
    @Transactional
    public void persistTestMappings(String analyzerTypeId, List<AnalyzerTestMapping> testMappings,
            List<AnalyzerTestMapping> existingMappings) {
        if (analyzerTypeId == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "persistTestMappings",
                    "analyzerTypeId is null — skipping " + testMappings.size() + " mapping(s)");
            return;
        }
        for (AnalyzerTestMapping mapping : testMappings) {
            mapping.setAnalyzerTypeId(analyzerTypeId);
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
            if (Objects.equals(existingMap.getAnalyzerTypeId(), mapping.getAnalyzerTypeId())
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
    public Optional<Analyzer> getByIpAddressAndPort(String ipAddress, Integer port) {
        return baseObjectDAO.findByIpAddressAndPort(ipAddress, port);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Analyzer> getByName(String name) {
        return baseObjectDAO.findByName(name);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Analyzer> findActiveByListenPort(Integer port) {
        return baseObjectDAO.findActiveByPort(port);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Analyzer> findByIdentifierPatternMatch(String analyzerIdentifier) {
        return findByIdentifierPatternMatch(analyzerIdentifier == null ? List.of() : List.of(analyzerIdentifier));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Analyzer> findByIdentifierPatternMatch(List<String> analyzerIdentifiers) {
        List<String> normalizedIdentifiers = normalizeAnalyzerIdentifiers(analyzerIdentifiers);
        if (normalizedIdentifiers.isEmpty()) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "findByIdentifierPatternMatch",
                    "Empty analyzer identifiers");
            return Optional.empty();
        }

        List<Analyzer> candidates = baseObjectDAO.findGenericAnalyzersWithPatterns();
        LogEvent.logDebug(this.getClass().getSimpleName(), "findByIdentifierPatternMatch",
                "Looking for match: identifiers=" + normalizedIdentifiers + ", candidates="
                        + (candidates != null ? candidates.size() : 0));
        if (candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }

        Analyzer bestAnalyzer = null;
        String bestIdentifier = null;
        String bestPattern = null;
        int bestScore = -1;

        for (Analyzer analyzer : candidates) {
            if (analyzer.getIdentifierPattern() == null || analyzer.getIdentifierPattern().trim().isEmpty()) {
                continue;
            }
            try {
                String pattern = analyzer.getIdentifierPattern();
                Pattern p = Pattern.compile(pattern);
                for (String identifier : normalizedIdentifiers) {
                    Matcher m = p.matcher(identifier);
                    if (m.find()) {
                        int score = m.group().length();
                        if (score > bestScore) {
                            bestAnalyzer = analyzer;
                            bestIdentifier = identifier;
                            bestPattern = pattern;
                            bestScore = score;
                        }
                    }
                }
            } catch (PatternSyntaxException e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "findByIdentifierPatternMatch",
                        "Invalid identifier_pattern regex for analyzer id=" + analyzer.getId());
            }
        }

        if (bestAnalyzer != null) {
            LogEvent.logInfo(this.getClass().getSimpleName(), "findByIdentifierPatternMatch",
                    "MATCHED: '" + bestIdentifier + "' matched pattern '" + bestPattern + "' for analyzer "
                            + bestAnalyzer.getName());
            return Optional.of(bestAnalyzer);
        }

        LogEvent.logWarn(this.getClass().getSimpleName(), "findByIdentifierPatternMatch",
                "No match found for identifiers " + normalizedIdentifiers + " among " + candidates.size()
                        + " candidates");
        return Optional.empty();
    }

    private List<String> normalizeAnalyzerIdentifiers(List<String> analyzerIdentifiers) {
        if (analyzerIdentifiers == null || analyzerIdentifiers.isEmpty()) {
            return List.of();
        }

        Set<String> normalized = new LinkedHashSet<>();
        for (String identifier : analyzerIdentifiers) {
            if (identifier == null) {
                continue;
            }

            String trimmed = identifier.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            normalized.add(trimmed);

            String upperCased = trimmed.toUpperCase();
            if (!upperCased.equals(trimmed)) {
                normalized.add(upperCased);
            }
        }

        return List.copyOf(normalized);
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

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public void autoCreateTestMappings(String analyzerId, Map<String, Object> config, String sysUserId) {
        analyzerPluginConfigService.applyConfigDefaults(analyzerId, config.get("configDefaults"), sysUserId);

        Object mappingsObj = config.get("default_test_mappings");
        if (!(mappingsObj instanceof List)) {
            return;
        }

        List<Map<String, Object>> mappings = (List<Map<String, Object>>) mappingsObj;
        int created = 0;
        Analyzer analyzer = get(analyzerId);
        List<AnalyzerTestMapping> dbTestMappings = analyzerMappingService.getAll();
        for (Map<String, Object> mapping : mappings) {
            String analyzerCode = (String) mapping.get("analyzer_code");
            String loinc = (String) mapping.get("loinc");

            if (analyzerCode == null || loinc == null || analyzerCode.isEmpty() || loinc.isEmpty()) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "autoCreateTestMappings",
                        "Skipping test mapping with missing analyzer_code or loinc");
                continue;
            }

            List<org.openelisglobal.test.valueholder.Test> tests = testService.getActiveTestsByLoinc(loinc);
            if (tests == null || tests.isEmpty()) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "autoCreateTestMappings",
                        "No active test found for LOINC '" + loinc + "' (analyzer_code '" + analyzerCode + "')");
                continue;
            }

            org.openelisglobal.test.valueholder.Test test = tests.get(0);

            String typeId = (analyzer != null && analyzer.getAnalyzerType() != null)
                    ? analyzer.getAnalyzerType().getId()
                    : null;

            AnalyzerTestMapping atm = new AnalyzerTestMapping();
            atm.setAnalyzerId(analyzerId);
            atm.setAnalyzerTypeId(typeId);
            atm.setAnalyzerTestName(analyzerCode);
            atm.setTestId(test.getId());
            atm.setSysUserId(sysUserId);

            try {
                if (newMapping(atm, dbTestMappings)) {
                    analyzerMappingService.insert(atm);
                    AnalyzerTestNameCache.getInstance().registerPluginAnalyzer(analyzer.getAnalyzerType().getName(),
                            typeId);
                    created++;
                } else {
                    for (AnalyzerTestMapping existing : dbTestMappings) {
                        if (Objects.equals(existing.getAnalyzerTypeId(), atm.getAnalyzerTypeId())
                                && existing.getAnalyzerTestName().equals(atm.getAnalyzerTestName())
                                && !Objects.equals(existing.getTestId(), atm.getTestId())) {
                            existing.setTestId(atm.getTestId());
                            existing.setAnalyzerId(atm.getAnalyzerId());
                            existing.setSysUserId(sysUserId);
                            analyzerMappingService.update(existing);
                            created++;
                            LogEvent.logInfo(this.getClass().getSimpleName(), "autoCreateTestMappings",
                                    "Updated stale test mapping for '" + analyzerCode + "' → test " + test.getId());
                            break;
                        }
                    }
                }

            } catch (Exception e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "autoCreateTestMappings",
                        "Failed to create test mapping for analyzer_code '" + analyzerCode + "': " + e.getMessage());
            }
        }
        AnalyzerTestNameCache.getInstance().reloadCache();

        if (created > 0) {
            LogEvent.logInfo(this.getClass().getSimpleName(), "autoCreateTestMappings",
                    "Auto-created " + created + " test mappings for analyzer " + analyzerId);
        }
    }

    @Override
    @Transactional
    public void deleteWithDependents(Analyzer analyzer) {
        String id = analyzer.getId();

        // Tiered FK strategy — matches Liquibase 004 constraints:
        //
        // RESTRICT tier: analyzer_results, notebook_analysers
        // → Must not exist; throw if they do (clinical/reference data).
        // SET NULL tier: analyzer_error, analyzer_file_upload
        // → Preserve audit trail by nulling the FK, not deleting rows.
        // CASCADE tier: config tables
        // → DB ON DELETE CASCADE handles these automatically.

        // 1. RESTRICT tier — block if clinical/reference data exists
        List<AnalyzerResults> results = analyzerResultsService.getResultsbyAnalyzer(id);
        if (!results.isEmpty()) {
            throw new LIMSRuntimeException("Cannot delete analyzer " + id + ": " + results.size()
                    + " analyzer_results rows exist. Remove or reassign results first.");
        }

        long notebookCount = noteBookDAO.countByAnalyzerId(id);
        if (notebookCount > 0) {
            throw new LIMSRuntimeException("Cannot delete analyzer " + id + ": " + notebookCount
                    + " notebook references exist. Remove references first.");
        }

        // 2. SET NULL tier — preserve audit trail
        int errorsNulled = analyzerErrorDAO.nullifyAnalyzerId(id);
        int uploadsNulled = analyzerFileUploadDAO.nullifyAnalyzerId(Integer.valueOf(id));
        if (errorsNulled > 0 || uploadsNulled > 0) {
            LogEvent.logInfo(this.getClass().getSimpleName(), "deleteWithDependents", "Set analyzer_id = NULL on "
                    + errorsNulled + " error(s) and " + uploadsNulled + " upload(s) for analyzer " + id);
        }

        // 3. CASCADE tier — DB ON DELETE CASCADE handles config tables:
        // analyzer_field, analyzer_field_mapping, serial_port_configuration,
        // file_import_configuration, analyzer_plugin_config,
        // analyzer_pending_code, analyzer_experiment

        // Delete the analyzer — DB cascades config tables automatically
        delete(analyzer);

        LogEvent.logInfo(this.getClass().getSimpleName(), "deleteWithDependents",
                "Deleted analyzer " + id + " (" + analyzer.getName() + ") with all dependents");
    }
}
