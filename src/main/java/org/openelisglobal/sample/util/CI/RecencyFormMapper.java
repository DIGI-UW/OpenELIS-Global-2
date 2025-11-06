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

public class RecencyFormMapper extends BaseProjectFormMapper implements IProjectFormMapper {

    private final String projectCode = MessageUtil.getMessage("sample.entry.project.RT");
    private final String projectName = "Recency Testing";

    public RecencyFormMapper(String projectFormId, IProjectForm form) {
        super(projectFormId, form);
    }

    public String getProjectName() {
        return projectName;
    }

    @Override
    public String getProjectCode() {
        return projectCode;
    }

    public List<Test> getPlasmaTests() {
        List<Test> testList = new ArrayList<>();

        if (projectData.isAsanteTest()) {
            CollectionUtils.addIgnoreNull(testList, createTest("Asante HIV-1 Rapid Recency", true));
        }
        return testList;
    }

    public List<Test> getSerumTests() {
        List<Test> testList = new ArrayList<>();

        if (projectData.isAsanteTest()) {
            CollectionUtils.addIgnoreNull(testList, createTest("Asante HIV-1 Rapid Recency", true));
        }
        return testList;
    }

    @Override
    public ArrayList<TypeOfSampleTests> getTypeOfSampleTests() {
        ArrayList<TypeOfSampleTests> sItemTests = new ArrayList<>();

        // Check for Plasma Tests
        if (projectData.isPlasmaTaken()) {
            sItemTests.add(new TypeOfSampleTests(getTypeOfSample("Plasma"), getPlasmaTests()));
        }

        if (projectData.isSerumTaken()) {
            sItemTests.add(new TypeOfSampleTests(getTypeOfSample("Serum"), getSerumTests()));
        }

        return sItemTests;
    }

    /**
     * @see org.openelisglobal.sample.util.CI.BaseProjectFormMapper#getSampleCenterCode()
     */
    @Override
    public String getSampleCenterCode() {
        return projectData.getARVcenterCode();
    }
}
