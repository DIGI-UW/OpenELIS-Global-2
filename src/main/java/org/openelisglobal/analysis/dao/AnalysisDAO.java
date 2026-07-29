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
package org.openelisglobal.analysis.dao;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.valueholder.Test;

/**
 * @author diane benz
 *         <p>
 *         To change this generated comment edit the template variable
 *         "typecomment": Window>Preferences>Java>Templates. To enable and
 *         disable the creation of type comments go to
 *         Window>Preferences>Java>Code Generation.
 */
public interface AnalysisDAO extends BaseDAO<Analysis, String> {

    // boolean insertData(Analysis analysis, boolean duplicateCheck) throws
    // LIMSRuntimeException;

    //
    // void deleteData(List analysiss) throws LIMSRuntimeException;

    //
    // List getAllAnalyses() throws LIMSRuntimeException;

    //
    // List getPageOfAnalyses(int startingRecNo) throws LIMSRuntimeException;

    void getData(Analysis analysis) throws LIMSRuntimeException;

    // void updateData(Analysis analysis) throws LIMSRuntimeException;

    //
    // List getAnalyses(String filter) throws LIMSRuntimeException;

    //

    //
    //

    //
    // List getAllAnalysesPerTest(Test test) throws LIMSRuntimeException;

    List<Analysis> getAllAnalysisByTestAndStatus(String testId, List<String> statusIdList) throws LIMSRuntimeException;

    List<Analysis> getAllAnalysisByTestsAndStatus(List<String> testIdList, List<String> statusIdList)
            throws LIMSRuntimeException;

    List<Analysis> getAllAnalysisByTestAndExcludedStatus(String testId, List<String> statusIdList)
            throws LIMSRuntimeException;

    List<Analysis> getAllAnalysisByTestSectionAndStatus(String testSectionId, List<String> statusIdList,
            boolean sortedByDateAndAccession) throws LIMSRuntimeException;

    List<Analysis> getAllAnalysisByTestSectionAndExcludedStatus(String testSectionId, List<String> statusIdList)
            throws LIMSRuntimeException;

    List<Analysis> getAnalysesBySampleItem(SampleItem sampleItem) throws LIMSRuntimeException;

    List<Analysis> getAnalysesBySampleItemsExcludingByStatusIds(SampleItem sampleItem, Set<String> statusIds)
            throws LIMSRuntimeException;

    List<Analysis> getAnalysesBySampleStatusId(String statusId) throws LIMSRuntimeException;

    List<Analysis> getAnalysesBySampleStatusIdExcludingByStatusId(String statusId, Set<String> statusIds)
            throws LIMSRuntimeException;

    List<Analysis> getAnalysesReadyToBeReported() throws LIMSRuntimeException;

    List<Analysis> getAllChildAnalysesByResult(Result result) throws LIMSRuntimeException;

    List<Analysis> getMaxRevisionAnalysesReadyToBeReported() throws LIMSRuntimeException;

    List<Analysis> getMaxRevisionAnalysesReadyForReportPreviewBySample(List<String> accessionNumbers)
            throws LIMSRuntimeException;

    List<Analysis> getAnalysesAlreadyReportedBySample(Sample sample) throws LIMSRuntimeException;

    List<Analysis> getMaxRevisionAnalysesBySample(SampleItem sampleItem) throws LIMSRuntimeException;

    List<Analysis> getMaxRevisionAnalysesBySampleIncludeCanceled(SampleItem sampleItem) throws LIMSRuntimeException;

    List<Analysis> getRevisionHistoryOfAnalysesBySample(SampleItem sampleItem) throws LIMSRuntimeException;

    List<Analysis> getRevisionHistoryOfAnalysesBySampleAndTest(SampleItem sampleItem, Test test,
            boolean includeLatestRevision) throws LIMSRuntimeException;

    List<Analysis> getAllMaxRevisionAnalysesPerTest(Test test) throws LIMSRuntimeException;

    List<Analysis> getMaxRevisionPendingAnalysesReadyToBeReportedBySample(Sample sample) throws LIMSRuntimeException;

    List<Analysis> getMaxRevisionPendingAnalysesReadyForReportPreviewBySample(Sample sample)
            throws LIMSRuntimeException;

    Analysis getPreviousAnalysisForAmendedAnalysis(Analysis analysis) throws LIMSRuntimeException;

    void getMaxRevisionAnalysisBySampleAndTest(Analysis analysis) throws LIMSRuntimeException;

    List<Analysis> getMaxRevisionParentTestAnalysesBySample(SampleItem sampleItem) throws LIMSRuntimeException;

    List<Analysis> getAnalysesForStatusId(String statusId) throws LIMSRuntimeException;

    List<Analysis> getAnalysisStartedOnExcludedByStatusId(Date collectionDate, Set<String> statusIds)
            throws LIMSRuntimeException;

    List<Analysis> getAnalysisStartedOn(Date collectionDate) throws LIMSRuntimeException;

    List<Analysis> getAnalysisCollectedOnExcludedByStatusId(Date collectionDate, Set<String> statusIds)
            throws LIMSRuntimeException;

    List<Analysis> getAnalysisCollectedOn(Date collectionDate) throws LIMSRuntimeException;

    List<Analysis> getAnalysesBySampleId(String id) throws LIMSRuntimeException;

    List<Analysis> getAnalysesBySampleIdExcludedByStatusId(String id, Set<String> statusIds)
            throws LIMSRuntimeException;

    List<Analysis> getAnalysisBySampleAndTestIds(String sampleKey, List<String> testIds);

    List<Analysis> getAnalysesBySampleIdTestIdAndStatusId(List<String> sampleIdList, List<String> testIdList,
            List<String> statusIdList);

    // Analysis getPatientPreviousAnalysisForTestName(Patient patient, Sample
    // currentSample, String
    // testName);

    List<Analysis> getAnalysisByTestSectionAndCompletedDateRange(String sectionID, Date lowDate, Date highDate)
            throws LIMSRuntimeException;

    List<Analysis> getAnalysisStartedOrCompletedInDateRange(Date lowDate, Date highDate) throws LIMSRuntimeException;

    List<Analysis> getAnalysisByTestIdAndTestSectionIdsAndStartedInDateRange(Date lowDate, Date highDate, String testId,
            List<String> testSectionIds) throws LIMSRuntimeException;

    List<Analysis> getAllAnalysisByTestSectionAndStatus(String testSectionId, List<String> analysisStatusList,
            List<String> sampleStatusList) throws LIMSRuntimeException;

    List<Analysis> getAnalysisStartedOnRangeByStatusId(Date lowDate, Date highDate, String statusID)
            throws LIMSRuntimeException;

    List<Analysis> getAnalysisCompleteInRange(Timestamp lowDate, Timestamp highDate) throws LIMSRuntimeException;

    List<Analysis> getAnalysisEnteredAfterDate(Timestamp latestCollectionDate) throws LIMSRuntimeException;

    List<Analysis> getAnalysisByAccessionAndTestId(String accessionNumber, String testId) throws LIMSRuntimeException;

    List<Analysis> getAnalysesBySampleIdAndStatusId(String id, Set<String> analysisStatusIds)
            throws LIMSRuntimeException;

    List<Analysis> getAnalysisByTestNamesAndCompletedDateRange(List<String> testNames, Date lowDate, Date highDate)
            throws LIMSRuntimeException;

    List<Analysis> getAnalysesBySampleItemIdAndStatusId(String sampleItemId, String statusId)
            throws LIMSRuntimeException;

    List<Analysis> getAnalysisByTestDescriptionAndCompletedDateRange(List<String> descriptions, Date sqlDayOne,
            Date sqlDayTwo) throws LIMSRuntimeException;

    Analysis getAnalysisById(String analysisId) throws LIMSRuntimeException;

    List<Analysis> getAllAnalysisByTestsAndStatus(List<String> testIds, List<String> analysisStatusList,
            List<String> sampleStatusList);

    @Override
    List<Analysis> get(List<String> value);

    List<Analysis> getAllAnalysisByTestsAndStatusAndCompletedDateRange(List<String> testIdList,
            List<String> analysisStatusList, List<String> sampleStatusList, Date lowDate, Date highDate);

    List<Analysis> getAllAnalysisByTestsAndStatusAndCompletedDateRange(List<String> nfsTestIdList,
            List<String> statusList, Date lowDate, Date highDate);

    List<Analysis> getPageAnalysisByTestSectionAndStatus(String testSectionId, List<String> analysisStatusList,
            List<String> sampleStatusList);

    int getCountAnalysisByTestSectionAndStatus(String testSectionId, List<String> analysisStatusList,
            List<String> sampleStatusList);
    // void updateData(Analysis analysis, boolean skipAuditTrail) throws
    // LIMSRuntimeException;

    List<Analysis> getPageAnalysisByTestSectionAndStatus(String testSectionId, List<String> statusIdList,
            boolean sortedByDateAndAccession) throws LIMSRuntimeException;

    List<Analysis> getPageAnalysisAtAccessionNumberAndStatus(String accessionNumber, List<String> statusIdList,
            boolean sortedByDateAndAccession) throws LIMSRuntimeException;

    int getCountAnalysisByTestSectionAndStatus(String testSectionId, List<String> analysisStatusList);

    int getCountAnalysisByStatusFromAccession(List<String> analysisStatusList, List<String> sampleStatusList,
            String accessionNumber);

    List<Analysis> getPageAnalysisByStatusFromAccession(List<String> analysisStatusList, List<String> sampleStatusList,
            String accessionNumber);

    List<Analysis> getPageAnalysisByStatusFromAccession(List<String> analysisStatusList, List<String> sampleStatusList,
            String accessionNumber, String upperRangeAccessionNumber, boolean doRange, boolean finished);

    List<Analysis> getAnalysisForSiteBetweenResultDates(String referringSiteId, LocalDate lowerDate,
            LocalDate upperDate);

    List<Analysis> getAnalysesByPriorityAndStatusId(OrderPriority priority, List<String> analysisStatusIds);

    List<Analysis> getStudyAnalysisForSiteBetweenResultDates(String referringSiteId, LocalDate lowerDate,
            LocalDate upperDate);

    List<Analysis> getAnalysesCompletedOnByStatusId(Date completedDate, String statusId) throws LIMSRuntimeException;

    List<Analysis> getAnalysesResultEnteredOnExcludedByStatusId(Date completedDate, Set<String> statusIds)
            throws LIMSRuntimeException;

    int getCountOfAnalysesForStatusIds(List<String> statusIdList);

    int getCountOfAnalysisCompletedOnByStatusId(Date completedDate, List<String> statusIds);

    int getCountOfAnalysisStartedOnExcludedByStatusId(Date collectionDate, Set<String> statusIds);

    int getCountOfAnalysisStartedOnByStatusId(Date startedDate, List<String> statusIds);

    List<Analysis> getAnalysisStartedOnByStatusId(Date startedDate, List<String> statusIds);

    /**
     * Find an analysis by sample item ID and test ID.
     *
     * <p>
     * Used for duplicate detection when adding tests to sample items. Returns the
     * analysis if a matching test already exists for the sample item, or null if no
     * such analysis exists.
     *
     * <p>
     * Related: Feature 001-sample-management, User Story 2 (Add Tests)
     *
     * @param sampleItemId the sample item ID
     * @param testId       the test ID
     * @return the existing Analysis or null if not found
     */
    Analysis getAnalysisBySampleItemAndTest(String sampleItemId, String testId);
}
