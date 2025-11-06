package org.openelisglobal.program.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.program.controller.immunohistochemistry.ImmunohistochemistrySampleForm;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample.ImmunohistochemistryStatus;
import org.openelisglobal.systemuser.valueholder.SystemUser;

public interface ImmunohistochemistrySampleService extends BaseObjectService<ImmunohistochemistrySample, Integer> {

    List<ImmunohistochemistrySample> getWithStatus(List<ImmunohistochemistryStatus> statuses);

    void assignTechnician(Integer pathologySampleId, SystemUser systemUser);

    void assignPathologist(Integer pathologySampleId, SystemUser systemUser);

    Long getCountWithStatus(List<ImmunohistochemistryStatus> statuses);

    void updateWithFormValues(Integer immunohistochemistrySampleId, ImmunohistochemistrySampleForm form);

    List<ImmunohistochemistrySample> searchWithStatusAndTerm(List<ImmunohistochemistryStatus> statuses,
            String searchTerm);

    Long getCountWithStatusBetweenDates(List<ImmunohistochemistryStatus> statuses, Timestamp from, Timestamp to);

    ImmunohistochemistrySample getByPathologySampleId(Integer pathologySampleId);
}
