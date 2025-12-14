package org.openelisglobal.pharmaceutical.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.pharmaceutical.dao.AssayRunDAO;
import org.openelisglobal.pharmaceutical.dao.DeviationCAPADAO;
import org.openelisglobal.pharmaceutical.dao.PharmaceuticalSampleDAO;
import org.openelisglobal.pharmaceutical.valueholder.AssayRun;
import org.openelisglobal.pharmaceutical.valueholder.DeviationCAPA;
import org.openelisglobal.pharmaceutical.valueholder.PharmaceuticalSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AssayRunServiceImpl implements AssayRunService {

    @Autowired
    private AssayRunDAO assayRunDAO;

    @Autowired
    private PharmaceuticalSampleDAO pharmaceuticalSampleDAO;

    @Autowired
    private DeviationCAPADAO deviationCAPADAO;

    @Override
    @Transactional(readOnly = true)
    public AssayRun get(Integer id) {
        return assayRunDAO.get(id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssayRun> getAll() {
        return assayRunDAO.getAll();
    }

    @Override
    public AssayRun save(AssayRun assayRun) {
        Integer id = assayRunDAO.insert(assayRun);
        assayRun.setId(id);
        return assayRun;
    }

    @Override
    public AssayRun update(AssayRun assayRun) {
        assayRunDAO.update(assayRun);
        return assayRun;
    }

    @Override
    public void delete(Integer id) {
        AssayRun assayRun = get(id);
        if (assayRun != null) {
            assayRunDAO.delete(assayRun);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssayRun> findBySampleId(Integer sampleId) {
        return assayRunDAO.findBySampleId(sampleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssayRun> findByStatus(AssayRun.AssayStatus status) {
        return assayRunDAO.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssayRun> findByOosFlag(Boolean oosFlag) {
        return assayRunDAO.findByOosFlag(oosFlag);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssayRun> findPendingReview() {
        return assayRunDAO.findPendingReview();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssayRun> findByNotebookPageId(Integer notebookPageId) {
        return assayRunDAO.findByNotebookPageId(notebookPageId);
    }

    @Override
    public AssayRun initiateAssayRun(Integer sampleId, AssayRun assayRun, String userId) {
        PharmaceuticalSample sample = pharmaceuticalSampleDAO.get(sampleId).orElse(null);
        if (sample == null) {
            throw new LIMSRuntimeException("Sample not found: " + sampleId);
        }

        assayRun.setSampleId(sampleId);
        assayRun.setStatus(AssayRun.AssayStatus.INITIATED);
        assayRun.setOosFlag(false);
        assayRun.setStartedAt(new Timestamp(System.currentTimeMillis()));
        assayRun.setAnalystId(userId);
        assayRun.setSysUserId(userId);

        Integer id = assayRunDAO.insert(assayRun);
        assayRun.setId(id);

        if (sample.getStatus() == PharmaceuticalSample.SampleStatus.REGISTERED
                || sample.getStatus() == PharmaceuticalSample.SampleStatus.IN_TESTING) {
            sample.setStatus(PharmaceuticalSample.SampleStatus.IN_TESTING);
            sample.setSysUserId(userId);
            pharmaceuticalSampleDAO.update(sample);
        }

        return assayRun;
    }

    @Override
    public AssayRun recordAssayResults(Integer assayRunId, String rawResults, String calculatedResults,
            Boolean oosFlag, String userId) {
        AssayRun assayRun = get(assayRunId);
        if (assayRun == null) {
            throw new LIMSRuntimeException("Assay run not found: " + assayRunId);
        }

        assayRun.setRawResults(rawResults);
        assayRun.setCalculatedResults(calculatedResults);
        assayRun.setOosFlag(oosFlag);
        assayRun.setStatus(AssayRun.AssayStatus.DATA_ENTRY);
        assayRun.setSysUserId(userId);

        assayRunDAO.update(assayRun);

        return assayRun;
    }

    @Override
    public AssayRun submitForReview(Integer assayRunId, String userId) {
        AssayRun assayRun = get(assayRunId);
        if (assayRun == null) {
            throw new LIMSRuntimeException("Assay run not found: " + assayRunId);
        }

        if (assayRun.getStatus() != AssayRun.AssayStatus.DATA_ENTRY) {
            throw new LIMSRuntimeException("Assay run must be in DATA_ENTRY status to submit for review");
        }

        assayRun.setStatus(AssayRun.AssayStatus.PENDING_REVIEW);
        assayRun.setSysUserId(userId);
        assayRunDAO.update(assayRun);

        return assayRun;
    }

    @Override
    public AssayRun approveAssayRun(Integer assayRunId, String approverId) {
        AssayRun assayRun = get(assayRunId);
        if (assayRun == null) {
            throw new LIMSRuntimeException("Assay run not found: " + assayRunId);
        }

        if (!canApprove(assayRunId)) {
            throw new LIMSRuntimeException(
                    "Cannot approve assay run. OOS results require linked CAPA before approval.");
        }

        assayRun.setStatus(AssayRun.AssayStatus.APPROVED);
        assayRun.setReviewedBy(approverId);
        assayRun.setReviewedAt(new Timestamp(System.currentTimeMillis()));
        assayRun.setSysUserId(approverId);
        assayRunDAO.update(assayRun);

        return assayRun;
    }

    @Override
    public AssayRun rejectAssayRun(Integer assayRunId, String rejectionReason, String approverId) {
        AssayRun assayRun = get(assayRunId);
        if (assayRun == null) {
            throw new LIMSRuntimeException("Assay run not found: " + assayRunId);
        }

        assayRun.setStatus(AssayRun.AssayStatus.REJECTED);
        assayRun.setReviewedBy(approverId);
        assayRun.setReviewedAt(new Timestamp(System.currentTimeMillis()));
        assayRun.setComments(rejectionReason);
        assayRun.setSysUserId(approverId);
        assayRunDAO.update(assayRun);

        return assayRun;
    }

    @Override
    public AssayRun linkToNotebook(Integer assayRunId, Integer notebookPageId, String userId) {
        AssayRun assayRun = get(assayRunId);
        if (assayRun == null) {
            throw new LIMSRuntimeException("Assay run not found: " + assayRunId);
        }

        assayRun.setNotebookPageId(notebookPageId);
        assayRun.setSysUserId(userId);
        assayRunDAO.update(assayRun);

        return assayRun;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canApprove(Integer assayRunId) {
        AssayRun assayRun = get(assayRunId);
        if (assayRun == null) {
            return false;
        }

        if (assayRun.getStatus() != AssayRun.AssayStatus.PENDING_REVIEW) {
            return false;
        }

        if (Boolean.TRUE.equals(assayRun.getOosFlag())) {
            List<DeviationCAPA> linkedCAPAs = deviationCAPADAO.findByAssayRunId(assayRunId);
            return !linkedCAPAs.isEmpty();
        }

        return true;
    }
}
