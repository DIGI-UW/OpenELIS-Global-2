package org.openelisglobal.pharmaceutical.service;

import java.util.List;
import org.openelisglobal.pharmaceutical.valueholder.QCCheck;

public interface QCCheckService {

    QCCheck get(Integer id);

    List<QCCheck> getAll();

    QCCheck save(QCCheck qcCheck);

    QCCheck update(QCCheck qcCheck);

    void delete(Integer id);

    List<QCCheck> findBySampleId(Integer sampleId);

    List<QCCheck> findByOutcome(QCCheck.QCOutcome outcome);

    QCCheck findLatestBySampleId(Integer sampleId);

    QCCheck performQCCheck(Integer sampleId, QCCheck qcCheck, String userId);

    QCCheck recordQCResult(Integer qcCheckId, QCCheck.QCOutcome outcome, String comments, String userId);

    boolean isQCPassed(Integer sampleId);
}
