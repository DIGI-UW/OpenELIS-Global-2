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

public class ARVFormMapper extends BaseProjectFormMapper implements IProjectFormMapper {

    private final String projectCode = MessageUtil.getMessage("sample.entry.project.LART");

    public ARVFormMapper(String projectFormId, IProjectForm form) {
        super(projectFormId, form);
    }

    @Override
    public String getProjectCode() {
        return projectCode;
    }

    public List<Test> getDryTubeTests() {
        List<Test> testList = new ArrayList<>();

        if (projectData.getSerologyHIVTest()) {
            CollectionUtils.addIgnoreNull(testList, createTest("Vironostika", true));
            // CollectionUtils.addIgnoreNull(testList, createTest("Murex", true));
            CollectionUtils.addIgnoreNull(testList, createTest("Murex Combinaison", true));
            CollectionUtils.addIgnoreNull(testList, createTest("Genscreen", true));
            // CollectionUtils.addIgnoreNull(testList, createTest("Innolia", true));
        }
        if (projectData.getCreatinineTest()) {
            CollectionUtils.addIgnoreNull(testList, createTest("Créatininémie", true));
        }
        if (projectData.getGlycemiaTest()) {
            CollectionUtils.addIgnoreNull(testList, createTest("Glycémie", true));
        }

        if (projectData.getTransaminaseTest()) {
            CollectionUtils.addIgnoreNull(testList, createTest("Transaminases ALTL", true));
            CollectionUtils.addIgnoreNull(testList, createTest("Transaminases ASTL", true));
        }
        return testList;
    }

    public List<Test> getEDTATubeTests(IProjectForm form) {
        List<Test> testList = new ArrayList<>();

        if (projectData.getNfsTest()) {
            CollectionUtils.addIgnoreNull(testList, createTest("GB", true));
            CollectionUtils.addIgnoreNull(testList, createTest("Neut %", true));
            CollectionUtils.addIgnoreNull(testList, createTest("Lymph %", true));
            CollectionUtils.addIgnoreNull(testList, createTest("Mono %", true));
            CollectionUtils.addIgnoreNull(testList, createTest("Eo %", true));
            CollectionUtils.addIgnoreNull(testList, createTest("Baso %", true));
            CollectionUtils.addIgnoreNull(testList, createTest("GR", true));
            CollectionUtils.addIgnoreNull(testList, createTest("Hb", true));
            CollectionUtils.addIgnoreNull(testList, createTest("HCT", true));
            CollectionUtils.addIgnoreNull(testList, createTest("VGM", true));
            CollectionUtils.addIgnoreNull(testList, createTest("TCMH", true));
            CollectionUtils.addIgnoreNull(testList, createTest("CCMH", true));
            CollectionUtils.addIgnoreNull(testList, createTest("PLQ", true));
        }
        if (projectData.getCd4cd8Test()) {
            CollectionUtils.addIgnoreNull(testList, createTest("CD3 percentage count", true));
            CollectionUtils.addIgnoreNull(testList, createTest("CD4 percentage count", true));
        }
        if (projectData.getViralLoadTest()) {
            CollectionUtils.addIgnoreNull(testList, createTest("Viral Load", true));
        }

        if (projectData.getGenotypingTest()) {
            CollectionUtils.addIgnoreNull(testList, createTest("Génotypage", true));
        }

        return testList;
    }

    public List<Test> getPSCTests(IProjectForm form) {
        List<Test> testList = new ArrayList<>();
        if (projectData.getViralLoadTest()) {
            CollectionUtils.addIgnoreNull(testList, createTest("Viral Load", true));
        }
        return testList;
    }

    @Override
    public ArrayList<TypeOfSampleTests> getTypeOfSampleTests() {
        ArrayList<TypeOfSampleTests> sItemTests = new ArrayList<>();

        // Check for Dry Tube Tests
        if (projectData.getDryTubeTaken()) {
            sItemTests.add(new TypeOfSampleTests(getTypeOfSample("Dry Tube"), getDryTubeTests()));
        }

        // Check for EDTA Tubes Tests
        if (projectData.getEdtaTubeTaken()) {
            sItemTests.add(new TypeOfSampleTests(getTypeOfSample("EDTA Tube"), getEDTATubeTests(form)));
        }

        if (projectData.getDbsTaken()) {
            if (projectData.getDnaPCR()) {
                sItemTests.add(new TypeOfSampleTests(getTypeOfSample("DBS"), getDBSTests()));
            }
        }

        // Check for DBS Tubes Tests for Viral Load
        if (projectData.getdbsvlTaken()) {
            sItemTests.add(new TypeOfSampleTests(getTypeOfSample("DBS"), getEDTATubeTests(form)));
        }

        // Check for PSC Tests for Viral Load
        if (projectData.isPscvlTaken()) {
            sItemTests.add(new TypeOfSampleTests(getTypeOfSample("PSC"), getPSCTests(form)));
        }

        return sItemTests;
    }

    public List<Test> getDBSTests() {
        List<Test> testList = new ArrayList<>();

        if (projectData.getDnaPCR()) {
            CollectionUtils.addIgnoreNull(testList, createTest("DNA PCR", true));
        }

        return testList;
    }

    /**
     * @see org.openelisglobal.sample.util.CI.BaseProjectFormMapper#getSampleCenterCode()
     */
    @Override
    public String getSampleCenterCode() {
        return projectData.getARVcenterCode();
    }
}
