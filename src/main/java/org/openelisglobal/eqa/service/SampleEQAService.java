package org.openelisglobal.eqa.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.eqa.valueholder.SampleEQA;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SampleEQAService extends BaseObjectService<SampleEQA, Long> {

    @PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
    Optional<SampleEQA> findBySampleId(Long sampleId);

    @PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
    List<SampleEQA> findByDeadlineBefore(Timestamp deadline);

    @PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
    List<SampleEQA> findByProgramId(Long programId);

    @PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
    List<SampleEQA> findEqaSamples();
}
