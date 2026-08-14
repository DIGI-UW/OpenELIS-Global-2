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

    /**
     * Projected [sampleItemId, analysisId] pairs for patient analyses of a test
     * completed on an analyzer within a window, newest first. Returns scalar ids —
     * not managed entities — so the caller can dedupe result revisions to distinct
     * samples and avoid loading version-locked rows that collide under concurrent
     * NCE creation (OGC-728). Callers dedupe/cap by sample.
     */
    List<Object[]> getAffectedSampleItemIdsByAnalyzerAndTestCompletedInRange(String analyzerId, String testId,
            Timestamp lowDate, Timestamp highDate) throws LIMSRuntimeException;

    /**
     * Whether any patient analysis of a test completed on an analyzer strictly
     * before the given time. Used to tell whether the 24h affected-samples floor
     * actually excluded samples (OGC-728 cap-reason accuracy).
     */
    /**
     * The lab-unit-keyed counterpart of
     * {@link #getAffectedSampleItemIdsByAnalyzerAndTestCompletedInRange}, for
     * controls run at the bench (OGC-1147 FR-C1). A manual or RDT control has no
     * analyzer, so its blast radius is every analysis of that test completed in the
     * same lab unit inside the window. Same contract otherwise: {sampleItemId,
     * analysisId} pairs, newest first, callers dedupe and cap by sample.
     */
    List<Object[]> getAffectedSampleItemIdsByTestSectionAndTestCompletedInRange(String testSectionId, String testId,
            Timestamp lowDate, Timestamp highDate) throws LIMSRuntimeException;

    /**
     * Lab-unit-keyed counterpart of
     * {@link #existsAnalysisCompletedBeforeByAnalyzerAndTest}, so a bench control's
     * cap reason is as accurate as an analyzer's.
     */
    boolean existsAnalysisCompletedBeforeByTestSectionAndTest(String testSectionId, String testId, Timestamp before)
            throws LIMSRuntimeException;

    boolean existsAnalysisCompletedBeforeByAnalyzerAndTest(String analyzerId, String testId, Timestamp before)
            throws LIMSRuntimeException;

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
