package org.openelisglobal.program.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.program.controller.immunohistochemistry.ImmunohistochemistrySampleForm;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample.ImmunohistochemistryStatus;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ImmunohistochemistrySampleService extends BaseObjectService<ImmunohistochemistrySample, Integer> {

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<ImmunohistochemistrySample> getWithStatus(List<ImmunohistochemistryStatus> statuses);

    @PreAuthorize("hasAuthority('PRIV_RESULT_PATHOLOGY_SIGN_OFF')")
    void assignTechnician(Integer pathologySampleId, SystemUser systemUser);

    @PreAuthorize("hasAuthority('PRIV_RESULT_PATHOLOGY_SIGN_OFF')")
    void assignPathologist(Integer pathologySampleId, SystemUser systemUser);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    Long getCountWithStatus(List<ImmunohistochemistryStatus> statuses);

    @PreAuthorize("hasAuthority('PRIV_RESULT_PATHOLOGY_SIGN_OFF')")
    void updateWithFormValues(Integer immunohistochemistrySampleId, ImmunohistochemistrySampleForm form);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<ImmunohistochemistrySample> searchWithStatusAndTerm(List<ImmunohistochemistryStatus> statuses,
            String searchTerm);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    Long getCountWithStatusBetweenDates(List<ImmunohistochemistryStatus> statuses, Timestamp from, Timestamp to);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    ImmunohistochemistrySample getByPathologySampleId(Integer pathologySampleId);
}
