package org.openelisglobal.eqa.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.eqa.valueholder.SampleEQA;

public interface SampleEQAService extends BaseObjectService<SampleEQA, Long> {

    Optional<SampleEQA> findBySampleId(Long sampleId);

    List<SampleEQA> findByDeadlineBefore(Timestamp deadline);

    List<SampleEQA> findByProgramId(Long programId);

    List<SampleEQA> findEqaSamples();
}
