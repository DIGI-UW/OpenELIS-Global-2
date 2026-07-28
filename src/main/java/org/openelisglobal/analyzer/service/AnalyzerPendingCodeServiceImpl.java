package org.openelisglobal.analyzer.service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.openelisglobal.analyzer.dao.AnalyzerPendingCodeDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerPendingCode;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.analyzerimport.util.AnalyzerTestNameCache;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@Transactional
public class AnalyzerPendingCodeServiceImpl extends BaseObjectServiceImpl<AnalyzerPendingCode, String>
        implements AnalyzerPendingCodeService {

    private static final int MAX_PENDING_CODES_PER_ANALYZER = 100;
    private static final Duration PENDING_CODE_RETENTION = Duration.ofDays(30);

    @Autowired
    private AnalyzerPendingCodeDAO analyzerPendingCodeDAO;

    @Autowired
    private AnalyzerTestMappingService analyzerTestMappingService;

    @Autowired
    private TestService testService;

    @Autowired
    private AnalyzerBridgeSyncService analyzerBridgeSyncService;

    public AnalyzerPendingCodeServiceImpl() {
        super(AnalyzerPendingCode.class);
    }

    @Override
    protected AnalyzerPendingCodeDAO getBaseObjectDAO() {
        return analyzerPendingCodeDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerPendingCode> findByAnalyzerId(String analyzerId) {
        return analyzerPendingCodeDAO.findByAnalyzerId(analyzerId);
    }

    @Override
    public AnalyzerPendingCode track(String analyzerId, String analyzerTestName, String samplePayload,
            String sysUserId) {
        purgeExpired(analyzerId);

        Timestamp now = new Timestamp(System.currentTimeMillis());
        AnalyzerPendingCode code = analyzerPendingCodeDAO.findByAnalyzerAndCode(analyzerId, analyzerTestName)
                .orElseGet(() -> {
                    long pendingCount = analyzerPendingCodeDAO.countByAnalyzerIdAndStatus(analyzerId,
                            AnalyzerPendingCode.Status.PENDING);
                    if (pendingCount >= MAX_PENDING_CODES_PER_ANALYZER) {
                        return null;
                    }
                    AnalyzerPendingCode created = new AnalyzerPendingCode();
                    created.setAnalyzerId(analyzerId);
                    created.setAnalyzerTestName(analyzerTestName);
                    created.setFirstSeenAt(now);
                    created.setSeenCount(0);
                    return created;
                });
        if (code == null) {
            return null;
        }
        code.setLastSeenAt(now);
        code.setSeenCount((code.getSeenCount() == null ? 0 : code.getSeenCount()) + 1);
        code.setSamplePayload(samplePayload);
        code.setStatus(AnalyzerPendingCode.Status.PENDING);
        code.setSysUserId(sysUserId);

        if (code.getId() == null || code.getId().trim().isEmpty()) {
            insert(code);
            return code;
        }
        return update(code);
    }

    @Override
    public AnalyzerPendingCode updateStatus(String analyzerId, String pendingCodeId, AnalyzerPendingCode.Status status,
            String sysUserId) {
        AnalyzerPendingCode code = get(pendingCodeId);
        if (code == null || !analyzerId.equals(code.getAnalyzerId())) {
            throw new IllegalArgumentException("Pending analyzer code does not belong to analyzer " + analyzerId);
        }
        if (status == AnalyzerPendingCode.Status.MAPPED) {
            throw new IllegalArgumentException("Mapped status requires an OpenELIS test resolution");
        }
        code.setStatus(status);
        code.setLastSeenAt(new Timestamp(System.currentTimeMillis()));
        code.setSysUserId(sysUserId);
        return update(code);
    }

    @Override
    public AnalyzerPendingCode resolve(String analyzerId, String pendingCodeId, String openelisTestId,
            String sysUserId) {
        if (openelisTestId == null || openelisTestId.isBlank()) {
            throw new IllegalArgumentException("openelisTestId is required");
        }
        AnalyzerPendingCode code = get(pendingCodeId);
        if (code == null || !analyzerId.equals(code.getAnalyzerId())) {
            throw new IllegalArgumentException("Pending analyzer code does not belong to analyzer " + analyzerId);
        }
        Test selectedTest = testService.get(openelisTestId);
        if (selectedTest == null || !selectedTest.isActive() || !testService.isTestFullySetup(selectedTest)) {
            throw new IllegalArgumentException("OpenELIS test is not active and fully configured: " + openelisTestId);
        }

        AnalyzerTestMapping mapping = analyzerTestMappingService.getAllForAnalyzer(analyzerId).stream()
                .filter(existing -> Objects.equals(code.getAnalyzerTestName(), existing.getAnalyzerTestName()))
                .findFirst().orElse(null);
        if (mapping == null) {
            mapping = new AnalyzerTestMapping();
            mapping.setAnalyzerId(analyzerId);
            mapping.setAnalyzerTestName(code.getAnalyzerTestName());
            mapping.setTestId(selectedTest.getId());
            mapping.setSysUserId(sysUserId);
            analyzerTestMappingService.insert(mapping);
        } else if (!Objects.equals(mapping.getTestId(), selectedTest.getId())) {
            mapping.setTestId(selectedTest.getId());
            mapping.setComponentId(null);
            mapping.setSysUserId(sysUserId);
            analyzerTestMappingService.update(mapping);
        }

        code.setStatus(AnalyzerPendingCode.Status.MAPPED);
        code.setLastSeenAt(new Timestamp(System.currentTimeMillis()));
        code.setSysUserId(sysUserId);
        AnalyzerPendingCode resolved = update(code);
        refreshAfterCommit(analyzerId);
        return resolved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMappingOptions() {
        return testService.getAllActiveTests(true).stream().map(test -> {
            Map<String, Object> option = new LinkedHashMap<>();
            option.put("id", test.getId());
            option.put("name", test.getName());
            option.put("loinc", test.getLoinc());
            option.put("localCode", test.getLocalCode());
            return option;
        }).sorted(Comparator.comparing(option -> String.valueOf(option.get("name")), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> getMappedTestIds(String analyzerId) {
        return analyzerTestMappingService.getAllForAnalyzer(analyzerId).stream()
                .collect(Collectors.toMap(AnalyzerTestMapping::getAnalyzerTestName, AnalyzerTestMapping::getTestId,
                        (first, replacement) -> replacement, LinkedHashMap::new));
    }

    private void refreshAfterCommit(String analyzerId) {
        Runnable refresh = () -> {
            try {
                AnalyzerTestNameCache.getInstance().reloadCache();
            } finally {
                analyzerBridgeSyncService.pushAnalyzer(analyzerId);
            }
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            analyzerBridgeSyncService.pushAnalyzer(analyzerId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                refresh.run();
            }
        });
    }

    @Override
    public int purgeExpired(String analyzerId) {
        if (analyzerId == null || analyzerId.trim().isEmpty()) {
            return 0;
        }
        Timestamp cutoff = Timestamp.from(Instant.now().minus(PENDING_CODE_RETENTION));
        return analyzerPendingCodeDAO.deletePendingOlderThan(analyzerId, cutoff);
    }
}
