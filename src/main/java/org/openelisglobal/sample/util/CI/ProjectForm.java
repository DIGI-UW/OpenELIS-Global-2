/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) The Minnesota Department of Health.  All Rights Reserved.
 *
 * Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.sample.util.CI;

import java.util.HashMap;
import java.util.Map;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.project.service.ProjectService;
import org.openelisglobal.project.valueholder.Project;
import org.openelisglobal.spring.util.SpringContext;

/**
 * A type-safe enumeration of the known studies in CI, so that code doesn't have
 * to hand around just another string.
 *
 * @author Paul A. Hill (pahill@uw.edu)
 * @since Oct 6, 2010
 */
public enum ProjectForm {
    ARV_INITIAL("InitialARV_Id", "Antiretroviral Study"),
    ARV_FOLLOWUP("FollowUpARV_Id", "Antiretroviral Followup Study"), ROUTINE_HIV("RTN_Id", "Routine HIV Testing"),
    EID("EID_Id", "Early Infant Diagnosis for HIV Study"), VL("VL_Id", "Viral Load Results"),
    INDETERMINATE("Indeterminate_Id", "Indeterminate Results"),
    SPECIAL_REQUEST("Special_Request_Id", "Special Request"), RECENCY_TESTING("Recency_Id", "Recency Testing"),
    HPV_TESTING("HPV_Id", "HPV Testing");

    private ProjectForm(String projectFormId, String projectName) {
        this.projectFormId = projectFormId;
        this.projectName = projectName;
    }

    /**
     * This is the name of some form as it is known in the UI (actual html ID
     * value).
     */
    private String projectFormId;

    /**
     * This is the name of the project for which this form is an input. The DB
     * entity's project.name.
     */
    private String projectName = null;

    public String getProjectFormId() {
        return projectFormId;
    }

    public String getProjectName() {
        return projectName;
    }

    /**
     * Find the database entity which corresponds to this ProjectForm
     *
     * @return
     */
    public Project getProject() {
        ProjectService projectService = SpringContext.getBean(ProjectService.class);
        Project p = new Project();
        p.setProjectName(projectName);
        p = projectService.getProjectByName(p, false, true);
        if (null == p) {
            throw new LIMSRuntimeException(
                    "Undefined Project name = " + projectName + ". Unable to find project for form.");
        }
        return p;
    }

    public static ProjectForm findProjectFormByFormId(String projectFormId) {
        initFormToProjectFormMap();
        return form2ProjectFormMap.get(projectFormId);
    }

    private static Map<String, ProjectForm> form2ProjectFormMap = new HashMap<>();

    /** */
    private static void initFormToProjectFormMap() {
        if (form2ProjectFormMap.size() == 0) {
            for (ProjectForm pf : ProjectForm.values()) {
                form2ProjectFormMap.put(pf.projectFormId, pf);
            }
        }
    }
}
