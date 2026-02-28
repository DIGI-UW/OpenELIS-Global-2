package org.openelisglobal.analyzer.service;

import java.sql.Timestamp;
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
    public AnalyzerPendingCode track(String analyzerId, String analyzerTestName, String samplePayload, String sysUserId) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        AnalyzerPendingCode code = analyzerPendingCodeDAO.findByAnalyzerAndCode(analyzerId, analyzerTestName)
                .orElseGet(() -> {
                    AnalyzerPendingCode created = new AnalyzerPendingCode();
                    created.setAnalyzerId(analyzerId);
                    created.setAnalyzerTestName(analyzerTestName);
                    created.setFirstSeenAt(now);
                    created.setSeenCount(0);
                    return created;
                });
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
}
