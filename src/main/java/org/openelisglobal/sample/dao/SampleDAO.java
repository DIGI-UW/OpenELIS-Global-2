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
package org.openelisglobal.sample.dao;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.sample.valueholder.Sample;

public interface SampleDAO extends BaseDAO<Sample, String> {

    // public boolean insertData(Sample sample) throws LIMSRuntimeException;

    // public void deleteData(List samples) throws LIMSRuntimeException;

    List<Sample> getPageOfSamples(int startingRecNo) throws LIMSRuntimeException;

    void getData(Sample sample) throws LIMSRuntimeException;

    // public void updateData(Sample sample) throws LIMSRuntimeException;

    void getSampleByAccessionNumber(Sample sample) throws LIMSRuntimeException;

    Sample getSampleByAccessionNumber(String accessionNumber) throws LIMSRuntimeException;

    // public boolean insertDataWithAccessionNumber(Sample sample) throws
    // LIMSRuntimeException;

    List<Sample> getSamplesByStatusAndDomain(List<String> statuses, String domain) throws LIMSRuntimeException;

    List<Sample> getSamplesWithPendingQaEvents(Sample sample, boolean filterByCategory, String qaEventCategoryId,
            boolean filterByDomain) throws LIMSRuntimeException;

    List<Sample> getSamplesReceivedOn(String recievedDate) throws LIMSRuntimeException;

    Sample getSampleByReferringId(String referringId) throws LIMSRuntimeException;

    /** Find a range of samples INCLUSIVE of the given dates. */
    List<Sample> getSamplesReceivedInDateRange(String receivedDateStart, String receivedDateEnd)
            throws LIMSRuntimeException;

    List<Sample> getSamplesCollectedOn(String collectionDate) throws LIMSRuntimeException;

    List<Sample> getSamplesByProjectAndStatusIDAndAccessionRange(String projectId, List<Integer> inclusiveStatusIdList,
            String minAccession, String maxAccession) throws LIMSRuntimeException;

    List<Sample> getSamplesByProjectAndStatusIDAndAccessionRange(List<Integer> inclusiveProjectIdList,
            List<Integer> inclusiveStatusIdList, String minAccession, String maxAccession) throws LIMSRuntimeException;

    List<Sample> getSamplesByAccessionRange(String minAccession, String maxAccession) throws LIMSRuntimeException;

    String getLargestAccessionNumber();

    String getLargestAccessionNumberWithPrefix(String prefix);

    String getLargestAccessionNumberMatchingPattern(String startingWith, int size) throws LIMSRuntimeException;

    List<Sample> getSamplesWithPendingQaEventsByService(String serviceId) throws LIMSRuntimeException;

    List<Sample> getConfirmationSamplesReceivedInDateRange(Date receivedDateStart, Date receivedDateEnd)
            throws LIMSRuntimeException;

    String getNextAccessionNumber();

    List<Sample> getAllMissingFhirUuid();

    List<Sample> getSamplesByAnalysisIds(List<String> analysisIds);

    List<Sample> getSamplesForSiteBetweenOrderDates(String referringSiteId, LocalDate lowerDate, LocalDate upperDate);

    List<Sample> getStudySamplesForSiteBetweenOrderDates(String referringSiteId, LocalDate lowerDate,
            LocalDate upperDate);

    List<Sample> getSamplesByPriority(OrderPriority priority) throws LIMSRuntimeException;
}
