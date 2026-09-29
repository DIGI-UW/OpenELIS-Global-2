package org.openelisglobal.project.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.project.valueholder.Project;
import org.springframework.security.access.prepost.PreAuthorize;

// Projects are a reference list the order-entry form offers in a dropdown, so
// the read side also accepts PRIV_CATALOGUE_VIEW alongside the EQA persona's
// own privilege. Write operations inherit CrudPrivileges as before.
@PreAuthorize("hasAnyAuthority('PRIV_EQA_VIEW','PRIV_CATALOGUE_VIEW')")
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
