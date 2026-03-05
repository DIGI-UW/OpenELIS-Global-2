package org.openelisglobal.analyzer.service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.openelisglobal.analyzer.dao.AnalyzerPendingCodeDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerPendingCode;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AnalyzerPendingCodeServiceImpl extends BaseObjectServiceImpl<AnalyzerPendingCode, String>
        implements AnalyzerPendingCodeService {

    private static final int MAX_PENDING_CODES_PER_ANALYZER = 100;
    private static final Duration PENDING_CODE_RETENTION = Duration.ofDays(30);

    @Autowired
    private AnalyzerPendingCodeDAO analyzerPendingCodeDAO;

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
    public AnalyzerPendingCode updateStatus(String pendingCodeId, AnalyzerPendingCode.Status status, String sysUserId) {
        AnalyzerPendingCode code = get(pendingCodeId);
        code.setStatus(status);
        code.setLastSeenAt(new Timestamp(System.currentTimeMillis()));
        code.setSysUserId(sysUserId);
        return update(code);
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
