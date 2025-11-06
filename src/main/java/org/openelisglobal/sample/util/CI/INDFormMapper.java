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
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.sample.util.CI;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.sample.util.CI.form.IProjectForm;
import org.openelisglobal.test.valueholder.Test;

public class INDFormMapper extends BaseProjectFormMapper implements IProjectFormMapper {

    private String projectCode = MessageUtil.getMessage("sample.entry.project.LIND");
    private final String projectName = "Indeterminate Results";

    public INDFormMapper(String projectFormId, IProjectForm form) {
        super(projectFormId, form);
    }

    private List<Test> getTests(IProjectForm form) {
        List<Test> testList = new ArrayList<>();

        if (projectData.getSerologyHIVTest()) {
            CollectionUtils.addIgnoreNull(testList, createTest("Vironostika", true));
            CollectionUtils.addIgnoreNull(testList, createTest("Murex Combinaison", true));
            CollectionUtils.addIgnoreNull(testList, createTest("Murex", true));
            CollectionUtils.addIgnoreNull(testList, createTest("Genscreen", true));
            CollectionUtils.addIgnoreNull(testList, createTest("Innolia", true));
        }

        return testList;
    }

    public String getProjectName() {
        return projectName;
    }

    @Override
    public String getProjectCode() {
        return projectCode;
    }

    @Override
    public String getOrganizationId() {
        return projectData.getINDsiteName();
    }

    @Override
    public ArrayList<TypeOfSampleTests> getTypeOfSampleTests() {
        ArrayList<TypeOfSampleTests> sItemTests = new ArrayList<>();

        // Check for Dry Tube Tests
        if (projectData.getSerologyHIVTest()) {
            if (projectData.getDryTubeTaken()) {
                sItemTests.add(new TypeOfSampleTests(getTypeOfSample("Dry Tube"), getTests(form)));
            }
        }

        return sItemTests;
    }

    /**
     * @see org.openelisglobal.sample.util.CI.BaseProjectFormMapper#getSampleCenterCode()
     */
    @Override
    public String getSampleCenterCode() {
        return projectData.getINDsiteCode();
    }
}
