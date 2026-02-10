package org.openelisglobal.eqa.dao;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.eqa.valueholder.SampleEQA;

public interface SampleEQADAO extends BaseDAO<SampleEQA, Long> {

    Optional<SampleEQA> findBySampleId(Long sampleId);

    List<SampleEQA> findByDeadlineBefore(Timestamp deadline);

    List<SampleEQA> findByProgramId(Long programId);

    List<SampleEQA> findByIsEqaSample(Boolean isEqaSample);
}
