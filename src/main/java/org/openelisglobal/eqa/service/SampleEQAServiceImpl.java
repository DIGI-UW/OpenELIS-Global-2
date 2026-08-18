package org.openelisglobal.eqa.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.eqa.dao.SampleEQADAO;
import org.openelisglobal.eqa.valueholder.SampleEQA;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SampleEQAServiceImpl extends BaseObjectServiceImpl<SampleEQA, Long> implements SampleEQAService {

    @Autowired
    private SampleEQADAO sampleEQADAO;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private IStatusService statusService;

    public SampleEQAServiceImpl() {
        super(SampleEQA.class);
    }

    @Override
    protected SampleEQADAO getBaseObjectDAO() {
        return sampleEQADAO;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SampleEQA> findBySampleId(Long sampleId) {
        return sampleEQADAO.findBySampleId(sampleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleEQA> findByDeadlineBefore(Timestamp deadline) {
        return sampleEQADAO.findByDeadlineBefore(deadline);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleEQA> findByProgramId(Long programId) {
        return sampleEQADAO.findByProgramId(programId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleEQA> findEqaSamples() {
        return sampleEQADAO.findByIsEqaSample(true);
    }

    @Override
    @Transactional(readOnly = true)
    public String deriveOrderStatus(SampleEQA sampleEQA) {
        boolean completed = false;
        boolean started = false;
        if (sampleEQA.getSampleId() != null) {
            // ponytail: one analysis query per order — fine at EQA volumes (tens); batch if
            // that ever changes
            List<Analysis> analyses = analysisService.getAnalysesBySampleIdExcludedByStatusId(
                    String.valueOf(sampleEQA.getSampleId()),
                    Set.of(statusService.getStatusID(AnalysisStatus.Canceled)));
            if (!analyses.isEmpty()) {
                String finalizedId = statusService.getStatusID(AnalysisStatus.Finalized);
                String notStartedId = statusService.getStatusID(AnalysisStatus.NotStarted);
                completed = analyses.stream().allMatch(a -> finalizedId.equals(a.getStatusId()));
                started = analyses.stream().anyMatch(a -> !notStartedId.equals(a.getStatusId()));
            }
        }
        if (completed) {
            return "COMPLETED";
        }
        if (sampleEQA.getEqaDeadline() != null
                && sampleEQA.getEqaDeadline().before(new Timestamp(System.currentTimeMillis()))) {
            return "OVERDUE";
        }
        return started ? "IN_PROGRESS" : "PENDING";
    }
}
