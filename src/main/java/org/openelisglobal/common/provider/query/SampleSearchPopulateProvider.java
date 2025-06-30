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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.exception.LIMSInvalidConfigurationException;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusSet;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.common.util.XMLUtil;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleorganization.service.SampleOrganizationService;
import org.openelisglobal.spring.util.SpringContext;

/**
 * @author Paul A. Hill (pahill@uw.edu)
 * @since Jul 14, 2010
 */
public class SampleSearchPopulateProvider extends BaseQueryProvider {

    protected SampleService sampleService = SpringContext.getBean(SampleService.class);
    protected AnalysisService analysisService = SpringContext.getBean(AnalysisService.class);
    protected IStatusService statusService = SpringContext.getBean(IStatusService.class);
    protected SampleHumanService sampleHumanService = SpringContext.getBean(SampleHumanService.class);
    protected SampleOrganizationService sampleOrganizationService = SpringContext
            .getBean(SampleOrganizationService.class);

    /**
     * @throws LIMSInvalidConfigurationException
     * @see org.openelisglobal.common.provider.query.BaseQueryProvider#processRequest(jakarta.servlet.http.HttpServletRequest,
     *      jakarta.servlet.http.HttpServletResponse)
     */
    @Override
    public void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String patientId = request.getParameter("patientKey");
        String accessionNo = request.getParameter("accessionNo");
        String testId = request.getParameter("testId");
        String loinc = request.getParameter("loinc");
        boolean unvalidatedTestOnly = GenericValidator.isBlankOrNull(request.getParameter("unvalidatedTestOnly"))
                ? false
                : Boolean.valueOf(request.getParameter("unvalidatedTestOnly"));

        StringBuilder xml = new StringBuilder();
        String result = VALID;
        Sample sample = null;
        if (!GenericValidator.isBlankOrNull(loinc) && !GenericValidator.isBlankOrNull(patientId)) {
            sample = getSampleForPatientIdAndLoinc(patientId, loinc, unvalidatedTestOnly);
        } else if (!GenericValidator.isBlankOrNull(accessionNo) && !GenericValidator.isBlankOrNull(loinc)) {
            sample = getSampleByAccessionNumberAndTestId(accessionNo, testId, unvalidatedTestOnly);

            StatusSet statusSet = SpringContext.getBean(IStatusService.class)
                    .getStatusSetForAccessionNumber(accessionNo);
            patientId = statusSet.getPatientId();
        } else if (!GenericValidator.isBlankOrNull(accessionNo)) {
            sample = this.getSampleByAccessionNumberAndTestLoinc(accessionNo, loinc, unvalidatedTestOnly);

            StatusSet statusSet = SpringContext.getBean(IStatusService.class)
                    .getStatusSetForAccessionNumber(accessionNo);
            patientId = statusSet.getPatientId();
        } else if (!GenericValidator.isBlankOrNull(testId) && !GenericValidator.isBlankOrNull(patientId)) {
            sample = getSampleForPatientIdAndTestId(patientId, testId, unvalidatedTestOnly);
        }

        if (sample == null) {
            xml.append("empty");
            // result = MessageUtil.getMessage("xxx");
        } else {
            createReturnXML(sample, patientId, xml);
        }

        ajaxServlet.sendData(xml.toString(), result, request, response);
    }

    private Sample getSampleByAccessionNumberAndTestId(String accessionNo, String testId, boolean unvalidatedTestOnly) {
        Sample sample = sampleService.getSampleByAccessionNumber(accessionNo);
        if (GenericValidator.isBlankOrNull(testId) || (sampleService.sampleContainsTest(sample.getId(), testId)
                && (!unvalidatedTestOnly || testNotFinialized(testId, sample.getId())))) {
            return sample;
        }
        return null;
    }

    private Sample getSampleByAccessionNumberAndTestLoinc(String accessionNo, String loinc,
            boolean unvalidatedTestOnly) {
        Sample sample = sampleService.getSampleByAccessionNumber(accessionNo);
        if (GenericValidator.isBlankOrNull(loinc) || (sampleService.sampleContainsTestWithLoinc(sample.getId(), loinc)
                && (!unvalidatedTestOnly || testWithLoincNotFinialized(loinc, sample.getId())))) {
            return sample;
        }
        return null;
    }

    private boolean testNotFinialized(String testId, String sampleId) {
        List<Integer> statusList = new ArrayList<>();
        statusList.add(Integer
                .parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.BiologistRejected)));
        statusList.add(
                Integer.parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Canceled)));
        statusList.add(Integer.parseInt(
                SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NonConforming_depricated)));
        statusList.add(
                Integer.parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NotStarted)));
        statusList.add(Integer
                .parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalAcceptance)));
        statusList.add(Integer
                .parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalRejected)));
        List<Analysis> analysises = analysisService.getAnalysesBySampleIdTestIdAndStatusId(
                Arrays.asList(Integer.parseInt(sampleId)), Arrays.asList(Integer.parseInt(testId)), statusList);
        for (Analysis analysis : analysises) {
            if (analysis.getTest().getId().equals(testId)) {
                return true;
            }
        }
        return false;
    }

    private boolean testWithLoincNotFinialized(String loinc, String sampleId) {
        Set<Integer> statusList = new HashSet<>();
        statusList.add(Integer
                .parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.BiologistRejected)));
        statusList.add(
                Integer.parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Canceled)));
        statusList.add(Integer.parseInt(
                SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NonConforming_depricated)));
        statusList.add(
                Integer.parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NotStarted)));
        statusList.add(Integer
                .parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalAcceptance)));
        statusList.add(Integer
                .parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalRejected)));

        List<Analysis> analysises = analysisService.getAnalysesBySampleIdAndStatusId(sampleId, statusList);
        for (Analysis analysis : analysises) {
            if (analysis.getTest().getLoinc().equals(loinc)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param sample a sample found in the DB
     * @param xml
     */
    private void createReturnXML(Sample sample, String patientId, StringBuilder xml) {
        XMLUtil.appendKeyValue("patientPK", StringUtil.snipToMaxIdLength(patientId), xml);
        XMLUtil.appendKeyValue("samplePK", sample.getId(), xml);
        XMLUtil.appendKeyValue("labNo", sample.getAccessionNumber(), xml);
        XMLUtil.appendKeyValue("receivedDateForDisplay", sample.getReceivedDateForDisplay(), xml);
        XMLUtil.appendKeyValue("collectionDateForDisplay", sample.getCollectionDateForDisplay(), xml);
        XMLUtil.appendKeyValue("receivedTimeForDisplay", sample.getReceivedTimeForDisplay(), xml);
        XMLUtil.appendKeyValue("collectionTimeForDisplay", sample.getCollectionTimeForDisplay(), xml);

        Organization o = getOrganizationForSample(sample);
        if (o != null) {
            XMLUtil.appendKeyValue("centerName", o.getOrganizationName(), xml);
            XMLUtil.appendKeyValue("centerCode", o.getId(), xml);
        }
    }

    /**
     * @param sample
     * @return first organization, if any of this sample
     */
    private Organization getOrganizationForSample(Sample sample) {
        return sampleOrganizationService.getDataBySample(sample).getOrganization();
    }

    private Sample getSampleForPatientIdAndLoinc(String patientId, String loinc, boolean unvalidatedTestOnly) {
        List<Sample> samples = sampleHumanService.getSamplesForPatient(patientId);
        if (samples == null || samples.size() == 0) {
            return null;
        }
        Sample sample = findBestLoincMatch(samples, loinc, unvalidatedTestOnly);
        if (sample == null) {
            return null;
        }

        // Reread in order to fill in pretend columns (aka accessionNumber)
        sampleService.getData(sample);
        return sample;
    }

    private Sample getSampleForPatientIdAndTestId(String patientId, String testId, boolean unvalidatedTestOnly) {
        List<Sample> samples = sampleHumanService.getSamplesForPatient(patientId);
        if (samples == null || samples.size() == 0) {
            return null;
        }
        Sample sample = findBestMatch(samples, testId, unvalidatedTestOnly);
        if (sample == null) {
            return null;
        }

        // Reread in order to fill in pretend columns (aka accessionNumber)
        sampleService.getData(sample);
        return sample;
    }

    private Sample findBestMatch(List<Sample> samples, String testId, boolean unvalidatedTestOnly) {
        Sample best = null;
        for (Sample sample : samples) {
            if (GenericValidator.isBlankOrNull(testId) || (sampleService.sampleContainsTest(sample.getId(), testId)
                    && (!unvalidatedTestOnly || testNotFinialized(testId, sample.getId())))) {
                if ((best == null || best.getEnteredDate().getTime() <= sample.getEnteredDate().getTime())) {
                    // currently latest entered date is the criteria
                    best = sample;
                }
            }
        }
        return best;
    }

    private Sample findBestLoincMatch(List<Sample> samples, String loinc, boolean unvalidatedTestOnly) {
        Sample best = null;
        for (Sample sample : samples) {
            if (GenericValidator.isBlankOrNull(loinc)
                    || (sampleService.sampleContainsTestWithLoinc(sample.getId(), loinc)
                            && (!unvalidatedTestOnly || testWithLoincNotFinialized(loinc, sample.getId())))) {
                if ((best == null || best.getEnteredDate().getTime() <= sample.getEnteredDate().getTime())) {
                    // currently latest entered date is the criteria
                    best = sample;
                }
            }
        }
        return best;
    }
}
