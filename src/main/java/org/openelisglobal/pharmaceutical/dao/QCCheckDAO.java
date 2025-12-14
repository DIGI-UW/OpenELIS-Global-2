package org.openelisglobal.pharmaceutical.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.pharmaceutical.valueholder.QCCheck;

public interface QCCheckDAO extends BaseDAO<QCCheck, Integer> {

    List<QCCheck> findBySampleId(Integer sampleId);

    List<QCCheck> findByOutcome(QCCheck.QCOutcome outcome);

    QCCheck findLatestBySampleId(Integer sampleId);
}
