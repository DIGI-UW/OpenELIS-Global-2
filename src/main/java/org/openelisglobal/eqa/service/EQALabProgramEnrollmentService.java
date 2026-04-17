package org.openelisglobal.eqa.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.eqa.valueholder.EQALabProgramEnrollment;
import org.springframework.security.access.prepost.PreAuthorize;

public interface EQALabProgramEnrollmentService extends BaseObjectService<EQALabProgramEnrollment, Long> {

    @PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
    List<EQALabProgramEnrollment> findAll();

    @PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
    List<EQALabProgramEnrollment> findActiveEnrollments();

    @PreAuthorize("hasAuthority('PRIV_EQA_MANAGE')")
    EQALabProgramEnrollment createEnrollment(EQALabProgramEnrollment enrollment, List<Long> labUnitIds,
            List<Long> testIds, List<Long> panelIds);

    @PreAuthorize("hasAuthority('PRIV_EQA_MANAGE')")
    EQALabProgramEnrollment updateEnrollment(Long id, EQALabProgramEnrollment updated, List<Long> labUnitIds,
            List<Long> testIds, List<Long> panelIds);

    @PreAuthorize("hasAuthority('PRIV_EQA_MANAGE')")
    void softDelete(Long id);

    @PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
    List<String> getDistinctProviders();
}
