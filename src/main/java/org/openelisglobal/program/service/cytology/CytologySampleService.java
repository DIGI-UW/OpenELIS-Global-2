package org.openelisglobal.program.service.cytology;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.program.controller.cytology.CytologySampleForm;
import org.openelisglobal.program.valueholder.cytology.CytologySample;
import org.openelisglobal.program.valueholder.cytology.CytologySample.CytologyStatus;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CytologySampleService extends BaseObjectService<CytologySample, Integer> {
    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<CytologySample> getWithStatus(List<CytologyStatus> statuses);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<CytologySample> searchWithStatusAndTerm(List<CytologyStatus> statuses, String searchTerm);

    @PreAuthorize("hasAuthority('PRIV_RESULT_PATHOLOGY_SIGN_OFF')")
    void assignTechnician(Integer cytologySampleId, SystemUser systemUser);

    @PreAuthorize("hasAuthority('PRIV_RESULT_PATHOLOGY_SIGN_OFF')")
    void assignCytoPathologist(Integer cytologySampleId, SystemUser systemUser);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    Long getCountWithStatus(List<CytologyStatus> statuses);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    Long getCountWithStatusBetweenDates(List<CytologyStatus> statuses, Timestamp from, Timestamp to);

    @PreAuthorize("hasAuthority('PRIV_RESULT_PATHOLOGY_SIGN_OFF')")
    void updateWithFormValues(Integer cytologySampleId, CytologySampleForm form);
}
