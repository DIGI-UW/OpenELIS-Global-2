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

public class SPEFormMapper extends ARVFormMapper implements IProjectFormMapper {

    private final String projectCode = MessageUtil.getMessage("sample.entry.project.LSPE");

    public SPEFormMapper(String projectFormId, IProjectForm form) {
        super(projectFormId, form);
    }

    @Override
    public String getProjectCode() {
        return projectCode;
    }

    @Override
    public String getOrganizationId() {
        // no organization id for Special Request
        return null;
    }

    @Override
    public List<Test> getDryTubeTests() {
        List<Test> testList = new ArrayList<>();

        if (projectData.getSerologyHIVTest()) {
            CollectionUtils.addIgnoreNull(testList, createTest("Vironostika", true));
            CollectionUtils.addIgnoreNull(testList, createTest("Murex", true));
            CollectionUtils.addIgnoreNull(testList, createTest("Murex Combinaison", true));
            CollectionUtils.addIgnoreNull(testList, createTest("Genscreen", true));
            CollectionUtils.addIgnoreNull(testList, createTest("Innolia", true));
        }

        if (projectData.getMurexTest()) {
            CollectionUtils.addIgnoreNull(testList, createTest("Murex Combinaison", true));
        }
        if (projectData.getInnoliaTest()) {
            CollectionUtils.addIgnoreNull(testList, createTest("Innolia", true));
        }
        if (projectData.getIntegralTest()) {
            CollectionUtils.addIgnoreNull(testList, createTest("Integral", true));
        }
        if (projectData.getGenscreenTest()) {
            CollectionUtils.addIgnoreNull(testList, createTest("Genscreen", true));
        }
        if (projectData.getVironostikaTest()) {
            CollectionUtils.addIgnoreNull(testList, createTest("Vironostika", true));
        }
        if (projectData.getGenieIITest()) {
            CollectionUtils.addIgnoreNull(testList, createTest("Genie II", true));
        }
        if (projectData.getGenieII100Test()) {
            CollectionUtils.addIgnoreNull(testList, createTest("Genie II 100", true));
        }
        if (projectData.getGenieII10Test()) {
            CollectionUtils.addIgnoreNull(testList, createTest("Genie II 10", true));
        }
        if (projectData.getWB1Test()) {
            CollectionUtils.addIgnoreNull(testList, createTest("Western Blot 1", true));
        }
        if (projectData.getWB2Test()) {
            CollectionUtils.addIgnoreNull(testList, createTest("Western Blot 2", true));
        }
        if (projectData.getP24AgTest()) {
            CollectionUtils.addIgnoreNull(testList, createTest("p24 Ag", true));
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
        } else {
            if (projectData.getTransaminaseALTLTest()) {
                CollectionUtils.addIgnoreNull(testList, createTest("Transaminases ALTL", true));
            }
            if (projectData.getTransaminaseASTLTest()) {
                CollectionUtils.addIgnoreNull(testList, createTest("Transaminases ASTL", true));
            }
        }
        return testList;
    }

    @Override
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

        if (projectData.getGbTest()) {
            CollectionUtils.addIgnoreNull(testList, createTest("GB", true));
        }
        if (projectData.getNeutTest()) {
            CollectionUtils.addIgnoreNull(testList, createTest("Neut %", true));
        }
        if (projectData.getLymphTest()) {
            CollectionUtils.addIgnoreNull(testList, createTest("Lymph %", true));
        }
        if (projectData.getMonoTest()) {
            CollectionUtils.addIgnoreNull(testList, createTest("Mono %", true));
        }
        if (projectData.getEoTest()) {
            CollectionUtils.addIgnoreNull(testList, createTest("Eo %", true));
        }
        if (projectData.getBasoTest()) {
            CollectionUtils.addIgnoreNull(testList, createTest("Baso %", true));
        }
        if (projectData.getGrTest()) {
            CollectionUtils.addIgnoreNull(testList, createTest("GR", true));
        }
        if (projectData.getHbTest()) {
            CollectionUtils.addIgnoreNull(testList, createTest("Hb", true));
        }
        if (projectData.getHctTest()) {
            CollectionUtils.addIgnoreNull(testList, createTest("HCT", true));
        }
        if (projectData.getVgmTest()) {
            CollectionUtils.addIgnoreNull(testList, createTest("VGM", true));
        }
        if (projectData.getTcmhTest()) {
            CollectionUtils.addIgnoreNull(testList, createTest("TCMH", true));
        }
        if (projectData.getCcmhTest()) {
            CollectionUtils.addIgnoreNull(testList, createTest("CCMH", true));
        }
        if (projectData.getPlqTest()) {
            CollectionUtils.addIgnoreNull(testList, createTest("PLQ", true));
        }
        if (projectData.getCd4cd8Test()) {
            CollectionUtils.addIgnoreNull(testList, createTest("CD3 percentage count", true));
            CollectionUtils.addIgnoreNull(testList, createTest("CD4 percentage count", true));
        }
        if (projectData.getCd3CountTest()) {
            CollectionUtils.addIgnoreNull(testList, createTest("CD3 percentage count", true));
        }
        if (projectData.getCd4CountTest()) {
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
}
