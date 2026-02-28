package org.openelisglobal.analyzer.service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.openelisglobal.analyzer.dao.AstmPendingCodeDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AstmPendingCode;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AstmPendingCodeServiceImpl implements AstmPendingCodeService {

    private static final int MAX_PENDING_PER_ANALYZER = 100;

    @Autowired
    private AstmPendingCodeDAO pendingCodeDAO;

    @Autowired
    private AnalyzerService analyzerService;

    @Override
    @Transactional(readOnly = true)
    public List<AstmPendingCode> findPendingByAnalyzerId(String analyzerId) {
        return pendingCodeDAO.findPendingByAnalyzerId(analyzerId);
    }

    @Override
    @Transactional
    public void recordSeen(String analyzerId, String analyzerTestName, String samplePayload) {
        if (analyzerTestName == null || analyzerTestName.trim().isEmpty()) {
            return;
        }
        Analyzer analyzer = analyzerService.get(analyzerId);
        if (analyzer == null) {
            return;
        }
        List<AstmPendingCode> pending = pendingCodeDAO.findPendingByAnalyzerId(analyzerId);
        AstmPendingCode existing = null;
        for (AstmPendingCode p : pending) {
            if (analyzerTestName.equals(p.getAnalyzerTestName())) {
                existing = p;
                break;
            }
        }
        if (existing != null) {
            existing.setLastSeenAt(new Date());
            existing.setSeenCount(existing.getSeenCount() + 1);
            if (samplePayload != null && samplePayload.length() <= 500) {
                existing.setSamplePayload(samplePayload);
            }
            pendingCodeDAO.update(existing);
        } else {
            int count = pendingCodeDAO.countPendingByAnalyzerId(analyzerId);
            if (count >= MAX_PENDING_PER_ANALYZER) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "recordSeen",
                        "Pending code queue full for analyzer " + analyzerId + ", skipping " + analyzerTestName);
                return;
            }
            AstmPendingCode pc = new AstmPendingCode();
            pc.setAnalyzer(analyzer);
            pc.setAnalyzerTestName(analyzerTestName);
            pc.setFirstSeenAt(new Date());
            pc.setLastSeenAt(new Date());
            pc.setSeenCount(1);
            pc.setStatus(AstmPendingCode.Status.PENDING.name());
            if (samplePayload != null && samplePayload.length() <= 500) {
                pc.setSamplePayload(samplePayload);
            }
            pendingCodeDAO.insert(pc);
        }
    }

    @Override
    @Transactional
    public void resolveByMapping(String pendingCodeId, String openelisTestId) {
        AstmPendingCode pc = pendingCodeDAO.get(pendingCodeId)
                .orElseThrow(() -> new LIMSRuntimeException("Pending code not found"));
        pc.setStatus(AstmPendingCode.Status.RESOLVED.name());
        pendingCodeDAO.update(pc);
    }

    @Override
    @Transactional
    public void updateStatus(String pendingCodeId, AstmPendingCode.Status status) {
        AstmPendingCode pc = pendingCodeDAO.get(pendingCodeId)
                .orElseThrow(() -> new LIMSRuntimeException("Pending code not found"));
        pc.setStatus(status.name());
        pendingCodeDAO.update(pc);
    }

    @Override
    @Transactional
    public void purgeOlderThanDays(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -days);
        Date cutoff = cal.getTime();
        List<AstmPendingCode> all = pendingCodeDAO.getAll();
        for (AstmPendingCode pc : all) {
            if (pc.getLastSeenAt() != null && pc.getLastSeenAt().before(cutoff)) {
                pendingCodeDAO.delete(pc);
            }
        }
    }
}
