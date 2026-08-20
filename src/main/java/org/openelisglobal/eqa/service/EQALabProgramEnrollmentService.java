package org.openelisglobal.eqa.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.eqa.valueholder.EQALabProgramEnrollment;

public interface EQALabProgramEnrollmentService extends BaseObjectService<EQALabProgramEnrollment, Long> {

    List<EQALabProgramEnrollment> findAll();

    List<EQALabProgramEnrollment> findActiveEnrollments();

    /**
     * @param testAnalytes which analyte each mapped test reports for this scheme,
     *                     keyed by test id (qa/030). Optional, but a test with no
     *                     analyte cannot be submitted automatically — see
     *                     {@link EQACycleSubmissionService}.
     */
    EQALabProgramEnrollment createEnrollment(EQALabProgramEnrollment enrollment, List<Long> labUnitIds,
            List<Long> testIds, List<Long> panelIds, Map<Long, Long> testAnalytes);

    EQALabProgramEnrollment updateEnrollment(Long id, EQALabProgramEnrollment updated, List<Long> labUnitIds,
            List<Long> testIds, List<Long> panelIds, Map<Long, Long> testAnalytes);

    void softDelete(Long id);

    List<String> getDistinctProviders();
}
