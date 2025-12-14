package org.openelisglobal.pharmaceutical.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.pharmaceutical.dao.PharmaceuticalSampleDAO;
import org.openelisglobal.pharmaceutical.dao.QCCheckDAO;
import org.openelisglobal.pharmaceutical.valueholder.PharmaceuticalSample;
import org.openelisglobal.pharmaceutical.valueholder.QCCheck;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class QCCheckServiceImpl implements QCCheckService {

    @Autowired
    private QCCheckDAO qcCheckDAO;

    @Autowired
    private PharmaceuticalSampleDAO pharmaceuticalSampleDAO;

    @Override
    @Transactional(readOnly = true)
    public QCCheck get(Integer id) {
        return qcCheckDAO.get(id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QCCheck> getAll() {
        return qcCheckDAO.getAll();
    }

    @Override
    public QCCheck save(QCCheck qcCheck) {
        Integer id = qcCheckDAO.insert(qcCheck);
        qcCheck.setId(id);
        return qcCheck;
    }

    @Override
    public QCCheck update(QCCheck qcCheck) {
        qcCheckDAO.update(qcCheck);
        return qcCheck;
    }

    @Override
    public void delete(Integer id) {
        QCCheck qcCheck = get(id);
        if (qcCheck != null) {
            qcCheckDAO.delete(qcCheck);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<QCCheck> findBySampleId(Integer sampleId) {
        return qcCheckDAO.findBySampleId(sampleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QCCheck> findByOutcome(QCCheck.QCOutcome outcome) {
        return qcCheckDAO.findByOutcome(outcome);
    }

    @Override
    @Transactional(readOnly = true)
    public QCCheck findLatestBySampleId(Integer sampleId) {
        return qcCheckDAO.findLatestBySampleId(sampleId);
    }

    @Override
    public QCCheck performQCCheck(Integer sampleId, QCCheck qcCheck, String userId) {
        PharmaceuticalSample sample = pharmaceuticalSampleDAO.get(sampleId).orElse(null);
        if (sample == null) {
            throw new LIMSRuntimeException("Sample not found: " + sampleId);
        }

        qcCheck.setSampleId(sampleId);
        qcCheck.setOutcome(QCCheck.QCOutcome.PENDING);
        qcCheck.setCheckedAt(new Timestamp(System.currentTimeMillis()));
        qcCheck.setCheckedBy(userId);
        qcCheck.setSysUserId(userId);

        Integer id = qcCheckDAO.insert(qcCheck);
        qcCheck.setId(id);

        return qcCheck;
    }

    @Override
    public QCCheck recordQCResult(Integer qcCheckId, QCCheck.QCOutcome outcome, String comments, String userId) {
        QCCheck qcCheck = get(qcCheckId);
        if (qcCheck == null) {
            throw new LIMSRuntimeException("QC Check not found: " + qcCheckId);
        }

        qcCheck.setOutcome(outcome);
        if (comments != null && !comments.isEmpty()) {
            String existingComments = qcCheck.getComments();
            if (existingComments != null && !existingComments.isEmpty()) {
                qcCheck.setComments(existingComments + "\n" + comments);
            } else {
                qcCheck.setComments(comments);
            }
        }
        qcCheck.setSysUserId(userId);
        qcCheckDAO.update(qcCheck);

        if (outcome == QCCheck.QCOutcome.PASS) {
            PharmaceuticalSample sample = pharmaceuticalSampleDAO.get(qcCheck.getSampleId()).orElse(null);
            if (sample != null && sample.getStatus() == PharmaceuticalSample.SampleStatus.REGISTERED) {
                sample.setStatus(PharmaceuticalSample.SampleStatus.IN_TESTING);
                sample.setSysUserId(userId);
                pharmaceuticalSampleDAO.update(sample);
            }
        }

        return qcCheck;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isQCPassed(Integer sampleId) {
        QCCheck latestQC = qcCheckDAO.findLatestBySampleId(sampleId);
        return latestQC != null && latestQC.getOutcome() == QCCheck.QCOutcome.PASS;
    }
}
