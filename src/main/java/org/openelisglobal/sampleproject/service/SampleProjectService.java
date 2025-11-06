package org.openelisglobal.sampleproject.service;

import java.sql.Date;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.sampleproject.valueholder.SampleProject;

public interface SampleProjectService extends BaseObjectService<SampleProject, String> {
    void getData(SampleProject sampleProj);

    List<SampleProject> getSampleProjectsByProjId(String projId);

    SampleProject getSampleProjectBySampleId(String id);

    List<SampleProject> getByOrganizationProjectAndReceivedOnRange(String organizationId, String projectName,
            Date lowDate, Date highDate);
}
