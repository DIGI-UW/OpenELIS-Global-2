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
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.patient.saving;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.exception.LIMSException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.provider.query.SampleItemTestProvider;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.RecordStatus;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.patient.saving.form.IAccessionerForm;
import org.openelisglobal.sample.form.ProjectData;
import org.openelisglobal.sample.util.CI.BaseProjectFormMapper;
import org.openelisglobal.sample.util.CI.BaseProjectFormMapper.TypeOfSampleTests;
import org.openelisglobal.sample.util.CI.ProjectForm;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope("prototype")
@Primary
public class SampleEntry extends Accessioner implements ISampleEntry {

    protected IAccessionerForm form;
    protected HttpServletRequest request;

    public SampleEntry(IAccessionerForm form, String sysUserId, HttpServletRequest request) {
        this();
        setFieldsFromForm(form);
        this.request = request;
        super.setSysUserId(sysUserId);
    }

    public SampleEntry() {
        super();
        newPatientStatus = RecordStatus.NotRegistered;
        newSampleStatus = RecordStatus.InitialRegistration;
    }

    @Override
    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public final void setFieldsFromForm(IAccessionerForm form) throws LIMSRuntimeException {
        setAccessionNumber(form.getLabNo());
        setPatientIdentifier(form.getSubjectNumber());
        setProjectFormMapperFromForm(form);
        this.form = form;
    }

    public void setProjectFormMapperFromForm(IAccessionerForm form) throws LIMSRuntimeException {
        projectFormMapper = getProjectFormMapper(form);
        projectFormMapper.setPatientForm(false);
        projectForm = projectFormMapper.getProjectForm();
        findStatusSet();
    }

    @Override
    public boolean canAccession() {
        return (statusSet.getPatientRecordStatus() == null && statusSet.getSampleRecordStatus() == null);
    }

    @Override
    protected void populateSampleData() throws LIMSException {
        Timestamp receivedDate = DateUtil.convertStringDateStringTimeToTimestamp(projectFormMapper.getReceivedDate(),
                projectFormMapper.getReceivedTime());
        Timestamp collectionDate = DateUtil.convertStringDateStringTimeToTimestamp(
                projectFormMapper.getCollectionDate(), projectFormMapper.getCollectionTime());
        populateSample(receivedDate, collectionDate);
        populateSampleProject();
        populateSampleOrganization(projectFormMapper.getOrganizationId());
        populateSampleItems();
    }

    protected void populateSampleItems() throws LIMSException {
        List<TypeOfSampleTests> typeofSampleTestList = projectFormMapper.getTypeOfSampleTests();

        boolean testSampleMismatch = false;
        testSampleMismatch = (null == typeofSampleTestList) || (typeofSampleTestList.isEmpty());

        if (!((null == typeofSampleTestList) || (typeofSampleTestList.isEmpty()))) {
            for (TypeOfSampleTests typeOfSampleTest : typeofSampleTestList) {
                if (typeOfSampleTest.tests.isEmpty()) {
                    testSampleMismatch = true;
                    break;
                }
            }
        }

        if (testSampleMismatch) {
            messages.reject("errors.no.sample");
            throw new LIMSException("Mis-match between tests and sample types.");
        }

        Timestamp collectionDate = DateUtil.convertStringDateStringTimeToTimestamp(
                projectFormMapper.getCollectionDate(), projectFormMapper.getCollectionTime());
        populateSampleItems(typeofSampleTestList, collectionDate);
        cleanupSampleItemsAndAnalysis();
    }

    /**
     * Looking at each sampleType which was NOT checked, we pretend that it was
     * checked and see what analysis WOULD be generated combined with any test. If
     * there are any of those, we then try to find some sampleItems to delete and
     * update along with appropriate analysis.
     *
     * @param projectFormMapper
     */
    // TODO PAHill this code relies on the correct spelling of the
    // typeOfSample.description. These magic names should be
    // moved to one central spot so they are easier to find if needed to change.
    // They also appear in the ProjectFormMapper, so
    // probably an enum there (BaseProjectFormMapper) could contain the known set
    // for use there and here.

    protected void cleanupSampleItemsAndAnalysis() {
        String sampleId = sample.getId();
        if (GenericValidator.isBlankOrNull(sampleId)) {
            return;
        }

        // try each type of sample tube and see what real test have gone away.
        ProjectData submittedProjectData = projectFormMapper.getProjectData();
        if (!submittedProjectData.getDryTubeTaken()) {
            cleanupSampleAndAnalysis(sampleId, "Dry Tube");
        } else {
            ProjectData tProjectData = buildProjectDataTestsReversed(submittedProjectData);
            tProjectData.setDryTubeTaken(true);
            cleanupExistingAnalysis(projectForm, sampleId, tProjectData);
        }
        if (!submittedProjectData.getEdtaTubeTaken()) {
            cleanupSampleAndAnalysis(sampleId, "EDTA Tube");
        } else {
            ProjectData tProjectData = buildProjectDataTestsReversed(submittedProjectData);
            tProjectData.setEdtaTubeTaken(true);
            cleanupExistingAnalysis(projectForm, sampleId, tProjectData);
        }
        if (!submittedProjectData.getDbsTaken()) {
            cleanupSampleAndAnalysis(sampleId, "DBS");
        } else {
            ProjectData tProjectData = buildProjectDataTestsReversed(submittedProjectData);
            tProjectData.setDbsTaken(true);
            cleanupExistingAnalysis(projectForm, sampleId, tProjectData);
        }
        return;
    }

    /**
     * Check if particular
     *
     * @param sampleId
     * @param typeName
     */
    private void cleanupSampleAndAnalysis(String sampleId, String typeName) {
        TypeOfSample typeOfSample = BaseProjectFormMapper.getTypeOfSampleByDescription(typeName);
        List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleIdAndType(sampleId, typeOfSample);
        for (SampleItem sampleItem : sampleItems) {
            List<Analysis> analyses = analysisService.getAnalysesBySampleItem(sampleItem);
            if (cleanupExistingAnalysis(analyses).size() != 0) {
                sampleItem.setSysUserId(sysUserId);
                sampleItemsToDelete.add(sampleItem);
            }
        }
    }

    /**
     * Find the analysis which are defined to be associated with checked sampleType
     * in the projectdata. update list of analysisToDelete, analysisToSave Called
     * with tests checked that are NOT wanted, so we can find those analysis and
     * check to delete them
     *
     * @param projectFormId - the name of form from the UI (typically the unique DIV
     *                      id)
     * @param sampleId      some sample
     * @param tProjectData  a mocked up set data of data, representing a set of
     *                      choices
     * @return
     */
    private Map<String, SampleItem> cleanupExistingAnalysis(ProjectForm projectForm, String sampleId,
            ProjectData tProjectData) {
        try {
            List<Analysis> analysisList = SampleItemTestProvider.findAnalysis(sampleId, projectForm.getProjectFormId(),
                    tProjectData);
            return cleanupExistingAnalysis(analysisList);
        } catch (IllegalArgumentException e) {
            return null; // reversing the test boxes resulted in NO valid resuest, so we can move on.
        }
    }

    private Map<String, SampleItem> cleanupExistingAnalysis(List<Analysis> analysisList) {
        String canceledId = SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Canceled);
        Map<String, SampleItem> sampleItemsToDelete = new HashMap<>();
        // first we assume we'll delete them all
        for (Analysis analysis : analysisList) {
            SampleItem sampleItem = analysis.getSampleItem();
            sampleItemsToDelete.put(sampleItem.getId(), sampleItem);
        }
        for (Analysis analysis : analysisList) {
            AnalysisStatus analysisStatus = SpringContext.getBean(IStatusService.class)
                    .getAnalysisStatusForID(analysis.getStatusId());
            switch (analysisStatus) {
            case NotStarted:
                // deletable => leave sampleItem in the Delete list
                analysis.setSysUserId(sysUserId);
                analysisToDelete.add(analysis);
                break;
            case NonConforming_depricated:
                // not deletable => remove the sampleItem from the delete list
                SampleItem sampleItem = analysis.getSampleItem();
                sampleItemsToDelete.remove(sampleItem.getId());
                break;
            default:
                // not deletable => remove the sampleItem from the delete list
                sampleItem = analysis.getSampleItem();
                sampleItemsToDelete.remove(sampleItem.getId());
                // save this analysis for later updating
                analysis.setStatusId(canceledId);
                analysis.setSysUserId(sysUserId);
                analysisToUpdate.add(analysis);
                break;
            }
        }
        return sampleItemsToDelete;
    }

    /**
     * Creates a new projectData object with all Test which are NOT checked in the
     * submitted projectData now checked in the object returned
     *
     * @param ProjectData - nothing but some Test properties are set in this object.
     */
    private ProjectData buildProjectDataTestsReversed(ProjectData submitted) {
        ProjectData projectData = new ProjectData();
        if (!submitted.getCreatinineTest()) {
            projectData.setCreatinineTest(true);
        }
        if (!submitted.getGlycemiaTest()) {
            projectData.setGlycemiaTest(true);
        }
        if (!submitted.getGenotypingTest()) {
            projectData.setGenotypingTest(true);
        }
        if (!submitted.getNfsTest()) {
            projectData.setNfsTest(true);
        }
        if (!submitted.getSerologyHIVTest()) {
            projectData.setSerologyHIVTest(true);
        }
        if (!submitted.getTransaminaseTest()) {
            projectData.setTransaminaseTest(true);
        }
        if (!submitted.getCd4cd8Test()) {
            projectData.setCd4cd8Test(true);
        }
        return projectData;
    }

    /**
     * @see org.openelisglobal.patient.saving.Accessioner#persistSampleData()
     */
    @Override
    protected void persistSampleData() {
        // TODO Auto-generated method stub
        super.persistSampleData();
    }

    /**
     * There are no lists in Sample forms, so no repeating OHs to update. If there
     * where the base class returning empty lists for all those without values and
     * then the persist coding deleting all sublists blindly and then rewriting them
     * would be broken, so be careful if it comes to that.
     *
     * @see org.openelisglobal.patient.saving.Accessioner#persistObservationHistoryLists()
     */
    @Override
    protected void persistObservationHistoryLists() {
        // do nothing. See note above. Do not delete this method.
    }

    @Override
    protected String getActionLabel() {
        return MessageUtil.getMessage("banner.menu.createSample.Initial");
    }
}
