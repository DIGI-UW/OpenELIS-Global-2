/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 */
package org.openelisglobal.projectorganization.valueholder;

import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.common.valueholder.ValueHolder;
import org.openelisglobal.common.valueholder.ValueHolderInterface;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.project.valueholder.Project;

public class ProjectOrganization extends BaseObject<String> {

    private String id;

    private String organizationId;

    private ValueHolderInterface organization;

    private String projectId;

    private ValueHolderInterface project;

    private String projectOrganizationType;

    public ProjectOrganization() {
        super();
        this.project = new ValueHolder();
        this.organization = new ValueHolder();
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setProjectOrganizationType(String projectOrganizationType) {
        this.projectOrganizationType = projectOrganizationType;
    }

    public String getProjectOrganizationType() {
        return projectOrganizationType;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getProjectId() {
        return projectId;
    }

    // SAMPLE
    public Project getProject() {
        return (Project) this.project.getValue();
    }

    public void setProject(ValueHolderInterface project) {
        this.project = project;
    }

    public void setProject(Project project) {
        this.project.setValue(project);
    }

    protected ValueHolderInterface getProjectHolder() {
        return this.project;
    }

    protected void setProjectHolder(ValueHolderInterface project) {
        this.project = project;
    }

    // ORGANIZATION
    public Organization getOrganization() {
        return (Organization) this.organization.getValue();
    }

    public void setOrganization(ValueHolderInterface organization) {
        this.organization = organization;
    }

    public void setOrganization(Organization organization) {
        this.organization.setValue(organization);
    }

    protected ValueHolderInterface getOrganizationHolder() {
        return this.organization;
    }

    protected void setOrganizationHolder(ValueHolderInterface organization) {
        this.organization = organization;
    }
}
