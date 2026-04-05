package org.openelisglobal.eqa.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.eqa.valueholder.EQALabProgramEnrollment;

public interface EQALabProgramEnrollmentService extends BaseObjectService<EQALabProgramEnrollment, Long> {

    List<EQALabProgramEnrollment> findAll();

    List<EQALabProgramEnrollment> findActiveEnrollments();

    EQALabProgramEnrollment createEnrollment(EQALabProgramEnrollment enrollment, List<Long> labUnitIds,
            List<Long> testIds, List<Long> panelIds);

    EQALabProgramEnrollment updateEnrollment(Long id, EQALabProgramEnrollment updated, List<Long> labUnitIds,
            List<Long> testIds, List<Long> panelIds);

    void softDelete(Long id);

    List<String> getDistinctProviders();
}
