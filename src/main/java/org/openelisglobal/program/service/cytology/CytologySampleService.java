package org.openelisglobal.program.service.cytology;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.program.controller.cytology.CytologySampleForm;
import org.openelisglobal.program.valueholder.cytology.CytologySample;
import org.openelisglobal.program.valueholder.cytology.CytologySample.CytologyStatus;
import org.openelisglobal.systemuser.valueholder.SystemUser;

public interface CytologySampleService extends BaseObjectService<CytologySample, Integer> {
    List<CytologySample> getWithStatus(List<CytologyStatus> statuses);

    List<CytologySample> searchWithStatusAndTerm(List<CytologyStatus> statuses, String searchTerm);

    void assignTechnician(Integer cytologySampleId, SystemUser systemUser);

    void assignCytoPathologist(Integer cytologySampleId, SystemUser systemUser);

    Long getCountWithStatus(List<CytologyStatus> statuses);

    Long getCountWithStatusBetweenDates(List<CytologyStatus> statuses, Timestamp from, Timestamp to);

    void updateWithFormValues(Integer cytologySampleId, CytologySampleForm form);
}
