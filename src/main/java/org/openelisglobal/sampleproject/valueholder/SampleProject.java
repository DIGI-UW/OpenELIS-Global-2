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
package org.openelisglobal.sampleproject.valueholder;

import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.common.valueholder.ValueHolder;
import org.openelisglobal.common.valueholder.ValueHolderInterface;
import org.openelisglobal.project.valueholder.Project;
import org.openelisglobal.sample.valueholder.Sample;

public class SampleProject extends BaseObject<String> {

    /** */
    private static final long serialVersionUID = 1L;

    private String id;

    private String projectId;

    private ValueHolderInterface project;

    private String sampleId;

    private ValueHolderInterface sample;

    private String isPermanent;

    public SampleProject() {
        super();
        this.project = new ValueHolder();
        this.sample = new ValueHolder();
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIsPermanent() {
        return isPermanent;
    }

    public void setIsPermanent(String isPermanent) {
        this.isPermanent = isPermanent;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getId() {
        return id;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public String getSampleId() {
        return sampleId;
    }

    // PROJECT
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

    // SAMPLE
    public Sample getSample() {
        return (Sample) this.sample.getValue();
    }

    public void setSample(ValueHolderInterface sample) {
        this.sample = sample;
    }

    public void setSample(Sample sample) {
        this.sample.setValue(sample);
    }

    protected ValueHolderInterface getSampleHolder() {
        return this.sample;
    }

    protected void setSampleHolder(ValueHolderInterface sample) {
        this.sample = sample;
    }
}
