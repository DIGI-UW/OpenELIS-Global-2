package org.openelisglobal.program.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.security.access.prepost.PreAuthorize;

public interface PathologySampleService extends BaseObjectService<PathologySample, Integer> {

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<PathologySample> getWithStatus(List<PathologyStatus> statuses);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<PathologySample> searchWithStatusAndTerm(List<PathologyStatus> statuses, String searchTerm);

    @PreAuthorize("hasAuthority('PRIV_RESULT_PATHOLOGY_SIGN_OFF')")
    void assignTechnician(Integer pathologySampleId, SystemUser systemUser, String curUserId);

    @PreAuthorize("hasAuthority('PRIV_RESULT_PATHOLOGY_SIGN_OFF')")
    void assignPathologist(Integer pathologySampleId, SystemUser systemUser, String curUserId);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    Long getCountWithStatus(List<PathologyStatus> statuses);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    Long getCountWithStatusBetweenDates(List<PathologyStatus> statuses, Timestamp from, Timestamp to);

    @PreAuthorize("hasAuthority('PRIV_RESULT_PATHOLOGY_SIGN_OFF')")
    void updateWithFormValues(Integer pathologySampleId, PathologySampleForm form);
}
