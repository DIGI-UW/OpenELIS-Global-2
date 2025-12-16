package org.openelisglobal.sampleproject.service;

import java.sql.Date;
import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.sampleproject.dao.SampleProjectDAO;
import org.openelisglobal.sampleproject.valueholder.SampleProject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SampleProjectServiceImpl extends AuditableBaseObjectServiceImpl<SampleProject, String>
        implements SampleProjectService {
    @Autowired
    protected SampleProjectDAO baseObjectDAO;

    SampleProjectServiceImpl() {
        super(SampleProject.class);
    }

    @Override
    protected SampleProjectDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public SampleProject getSampleProjectBySampleId(String id) {
        return getMatch("sample.id", id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(SampleProject sampleProj) {
        getBaseObjectDAO().getData(sampleProj);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleProject> getSampleProjectsByProjId(String projId) {
        return getBaseObjectDAO().getSampleProjectsByProjId(projId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleProject> getByOrganizationProjectAndReceivedOnRange(String organizationId, String projectName,
            Date lowDate, Date highDate) {
        return getBaseObjectDAO().getByOrganizationProjectAndReceivedOnRange(organizationId, projectName, lowDate,
                highDate);
    }
}
