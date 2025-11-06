package org.openelisglobal.project.service;

import java.util.List;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.project.dao.ProjectDAO;
import org.openelisglobal.project.valueholder.Project;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectServiceImpl extends AuditableBaseObjectServiceImpl<Project, String> implements ProjectService {
    @Autowired
    protected ProjectDAO baseObjectDAO;

    ProjectServiceImpl() {
        super(Project.class);
    }

    @Override
    protected ProjectDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(Project project) {
        getBaseObjectDAO().getData(project);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> getPageOfProjects(int startingRecNo) {
        return getBaseObjectDAO().getPageOfProjects(startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalProjectCount() {
        return getBaseObjectDAO().getTotalProjectCount();
    }

    @Override
    @Transactional(readOnly = true)
    public Project getProjectByLocalAbbreviation(Project project, boolean activeOnly) {
        return getBaseObjectDAO().getProjectByLocalAbbreviation(project, activeOnly);
    }

    @Override
    @Transactional(readOnly = true)
    public Project getProjectById(String id) {
        return getBaseObjectDAO().getProjectById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> getProjects(String filter, boolean activeOnly) {
        return getBaseObjectDAO().getProjects(filter, activeOnly);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> getAllProjects() {
        return getBaseObjectDAO().getAllProjects();
    }

    @Override
    @Transactional(readOnly = true)
    public Project getProjectByName(Project project, boolean ignoreCase, boolean activeOnly) {
        return getBaseObjectDAO().getProjectByName(project, ignoreCase, activeOnly);
    }

    @Override
    public void delete(Project project) {
        Project oldData = get(project.getId());
        oldData.setIsActive(IActionConstants.NO);
        oldData.setSysUserId(project.getSysUserId());
        updateDelete(oldData);
    }

    @Override
    public String insert(Project project) {
        if (duplicateProjectExists(project)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + project.getProjectName());
        }
        return super.insert(project);
    }

    @Override
    public Project save(Project project) {
        if (duplicateProjectExists(project)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + project.getProjectName());
        }
        return super.save(project);
    }

    @Override
    public Project update(Project project) {
        if (duplicateProjectExists(project)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + project.getProjectName());
        }
        return super.update(project);
    }

    private boolean duplicateProjectExists(Project project) {
        return baseObjectDAO.duplicateProjectExists(project);
    }
}
