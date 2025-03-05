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
package org.openelisglobal.common.provider.query;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.sample.form.ProjectData;
import org.openelisglobal.sample.util.CI.BaseProjectFormMapper.TypeOfSampleTests;
import org.openelisglobal.sample.util.CI.IProjectFormMapper;
import org.openelisglobal.sample.util.CI.ProjectFormMapperFactory;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;

/**
 * AJAX provider for answering questions about sampleitems and tests
 *
 * @author Paul A. Hill (pahill@uw.edu)
 * @since Aug 16, 2010
 */
public class SampleItemTestProvider extends BaseQueryProvider {

    protected static AnalysisService analysisService = SpringContext.getBean(AnalysisService.class);
    protected SampleItemService sampleItemService = SpringContext.getBean(SampleItemService.class);

    /**
     * @see org.openelisglobal.common.provider.query.BaseQueryProvider#processRequest(jakarta.servlet.http.HttpServletRequest,
     *      jakarta.servlet.http.HttpServletResponse)
     */
    @Override
    public void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        /** The primary Key for a sample */
        String sampleKey = request.getParameter("sampleKey");
        /** the particular project form we are trying to fill validate. */
        String projectFormName = request.getParameter("projectFormName");

        /**
         * the name (something derived from html form id) of the check box for the
         * relevant sample item type
         */
        String sampleItemTypeTag = request.getParameter("sampleItemTypeTag");
        if (sampleItemTypeTag.endsWith("vl")) {
            sampleItemTypeTag = sampleItemTypeTag.substring(0, sampleItemTypeTag.length() - 2);
        }

        /** the name (something derived from html form id) */
        String testTag = request.getParameter("testTag");

        StringBuilder xml = new StringBuilder();
        String result = VALID;

        boolean isChecked;
        if (GenericValidator.isBlankOrNull(testTag)) {
            isChecked = wasSampleTypeSelected(sampleKey, projectFormName, sampleItemTypeTag);
        } else {
            try {
                isChecked = wasTestSelected(sampleKey, projectFormName, sampleItemTypeTag, testTag);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new ServletException(e);
            }
        }

        xml.append(isChecked);

        ajaxServlet.sendData(xml.toString(), result, request, response);
    }

    /**
     * Figure out if the given "test" (actually something of a test panel) Ask the
     * project form mapper
     */
    private boolean wasTestSelected(String sampleKey, String projectFormName, String sampleItemType, String testTag)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        ProjectData projectData = new ProjectData();
        Map<String, Boolean> miniForm = new HashMap<>();
        miniForm.put(sampleItemType + "Taken", Boolean.TRUE);
        miniForm.put(testTag, Boolean.TRUE);
        PropertyUtils.copyProperties(projectData, miniForm);

        List<Analysis> analysis = findAnalysis(sampleKey, projectFormName, projectData);
        return analysis.size() != 0;
    }

    /***
     * Fill in one (or more?) sampleType fields of project data and one (or more?)
     * *Test fields, e.g. transaminaseTest and you'll get back a list Analysis which
     * where submitted because of that selection.
     *
     * @param sampleKey       - PK of a sample
     * @param projectFormName particular study we are working on
     * @param projectData     the data with two flags set.
     * @return List of analysis
     */
    // TODO PAHill - refactor - needs to be moved to some type of a utility
    // class.
    public static List<Analysis> findAnalysis(String sampleKey, String projectFormName, ProjectData projectData) {
        IProjectFormMapper projectFormMapper = new ProjectFormMapperFactory().getProjectInitializer(projectFormName,
                null);
        projectFormMapper.setProjectData(projectData);
        List<TypeOfSampleTests> typeOfSampleTestsList = projectFormMapper.getTypeOfSampleTests();
        if (typeOfSampleTestsList.size() == 0) {
            throw new IllegalArgumentException("The combination of sampleItem type and test (panel) is not valid.");
        }
        TypeOfSampleTests sampleTests = typeOfSampleTestsList.get(0);
        List<Test> tests = sampleTests.tests;

        List<Integer> testIds = new ArrayList<>();
        for (Test test : tests) {
            testIds.add(Integer.valueOf(test.getId()));
        }
        return analysisService.getAnalysisBySampleAndTestIds(sampleKey, testIds);
    }

    private boolean wasSampleTypeSelected(String sampleId, String projectFormName, String sampleItemType) {
        try {
            String sampleItemDesc = changeUIIdToDescription(sampleItemType);
            IProjectFormMapper projectFormMapper = new ProjectFormMapperFactory().getProjectInitializer(projectFormName,
                    null);
            TypeOfSample typeOfSample = projectFormMapper.getTypeOfSample(sampleItemDesc);
            List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sampleId);
            for (SampleItem sampleItem : sampleItems) {
                if (sampleItem.getTypeOfSampleId().equals(typeOfSample.getId())) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            LogEvent.logError(e);
            return false;
        }
    }

    private String changeUIIdToDescription(String sampleTypeId) {
        String description = sampleTypeId;
        int i = sampleTypeId.indexOf("Tube");
        if (i != -1) {
            description = sampleTypeId.substring(0, i) + " Tube";
        }
        return description;
    }
}
