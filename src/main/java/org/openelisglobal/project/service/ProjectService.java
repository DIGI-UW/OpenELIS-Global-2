package org.openelisglobal.project.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.project.valueholder.Project;

public interface ProjectService extends BaseObjectService<Project, String> {
    void getData(Project project);

    List<Project> getPageOfProjects(int startingRecNo);

    Integer getTotalProjectCount();

    Project getProjectByLocalAbbreviation(Project project, boolean activeOnly);

    Project getProjectById(String id);

    List<Project> getProjects(String filter, boolean activeOnly);

    List<Project> getAllProjects();

    Project getProjectByName(Project project, boolean ignoreCase, boolean activeOnly);
}
