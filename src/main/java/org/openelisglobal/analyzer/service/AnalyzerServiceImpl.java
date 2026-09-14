package org.openelisglobal.analyzer.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.analyzer.dao.AnalyzerDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
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
    public List<Analyzer> getAllWithBindings() {
        return baseObjectDAO.findAllWithBindings();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Analyzer> getWithBinding(String id) {
        return baseObjectDAO.findByIdWithBinding(id);
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

    @Override
    @Transactional(readOnly = true)
    public Optional<Analyzer> getByName(String name) {
        return baseObjectDAO.findByName(name);
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
    public Optional<Analyzer> findByBridgeConnectionId(String bridgeConnectionId) {
        return baseObjectDAO.findByBridgeConnectionId(bridgeConnectionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerTestCapability> getCapabilitiesForTest(String testId) {
        return baseObjectDAO.findCapabilitiesByTestId(testId);
    }
}
