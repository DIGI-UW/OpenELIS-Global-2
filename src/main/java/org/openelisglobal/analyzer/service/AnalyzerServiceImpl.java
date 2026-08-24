package org.openelisglobal.analyzer.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.openelisglobal.analyzer.dao.AnalyzerDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyzerServiceImpl extends AuditableBaseObjectServiceImpl<Analyzer, String> implements AnalyzerService {

    @Autowired
    protected AnalyzerDAO baseObjectDAO;

    AnalyzerServiceImpl() {
        super(Analyzer.class);
        this.auditTrailLog = true;
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
        // Return the most recent active analyzer with this name.
        // Multiple analyzers can share a name (e.g., two instruments of the same
        // model).
        // Prefer ACTIVE over other statuses; within same status, prefer highest ID
        // (newest).
        List<Analyzer> matches = getAllMatching("name", name);
        if (matches == null || matches.isEmpty()) {
            return null;
        }
        return matches.stream().max(java.util.Comparator.comparing(a -> Integer.parseInt(a.getId())))
                .orElse(matches.get(matches.size() - 1));
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
    public void delete(Analyzer analyzer) {
        throw hardDeleteUnsupported();
    }

    @Override
    public void delete(String id, String sysUserId) {
        throw hardDeleteUnsupported();
    }

    @Override
    public void deleteAll(List<Analyzer> analyzers) {
        throw hardDeleteUnsupported();
    }

    @Override
    public void deleteAll(List<String> ids, String sysUserId) {
        throw hardDeleteUnsupported();
    }

    private UnsupportedOperationException hardDeleteUnsupported() {
        return new UnsupportedOperationException("Analyzer hard deletion is not supported");
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Analyzer> findByDiscoveredSourceId(String discoveredSourceId) {
        return baseObjectDAO.findByDiscoveredSourceId(discoveredSourceId);
    }
}
