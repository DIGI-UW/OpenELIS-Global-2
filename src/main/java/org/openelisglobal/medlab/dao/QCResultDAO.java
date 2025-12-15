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
package org.openelisglobal.medlab.dao;

import java.sql.Date;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.medlab.valueholder.QCResult;

/**
 * DAO interface for QCResult entity operations.
 */
public interface QCResultDAO extends BaseDAO<QCResult, Integer> {

    /**
     * Get QC results for a specific test within a date range. Used for
     * Levey-Jennings chart data.
     *
     * @param testId    the test ID
     * @param startDate start of date range
     * @param endDate   end of date range
     * @return list of QC results ordered by date/time
     */
    List<QCResult> getQCResultsByTestAndDateRange(Integer testId, Date startDate, Date endDate);

    /**
     * Get QC results for a specific test and QC level.
     *
     * @param testId  the test ID
     * @param qcLevel NORMAL or PATHOLOGIC
     * @return list of QC results
     */
    List<QCResult> getQCResultsByTestAndLevel(Integer testId, QCResult.QCLevel qcLevel);

    /**
     * Get QC results for a specific lot number.
     *
     * @param lotNumber the control lot number
     * @return list of QC results
     */
    List<QCResult> getQCResultsByLotNumber(String lotNumber);

    /**
     * Get failed QC results for a date range.
     *
     * @param startDate start of date range
     * @param endDate   end of date range
     * @return list of failed QC results
     */
    List<QCResult> getFailedQCResultsByDateRange(Date startDate, Date endDate);

    /**
     * Get the latest N QC results for a test (for trend analysis).
     *
     * @param testId  the test ID
     * @param qcLevel NORMAL or PATHOLOGIC
     * @param count   number of results to return
     * @return list of most recent QC results
     */
    List<QCResult> getLatestQCResultsForTest(Integer testId, QCResult.QCLevel qcLevel, int count);

    /**
     * Get QC results for a specific analyzer.
     *
     * @param analyzerId the analyzer ID
     * @param startDate  start of date range
     * @param endDate    end of date range
     * @return list of QC results
     */
    List<QCResult> getQCResultsByAnalyzerAndDateRange(Integer analyzerId, Date startDate, Date endDate);

    /**
     * Get calibration records for an analyzer.
     *
     * @param analyzerId the analyzer ID
     * @param startDate  start of date range
     * @param endDate    end of date range
     * @return list of calibration QC results
     */
    List<QCResult> getCalibrationRecordsByAnalyzer(Integer analyzerId, Date startDate, Date endDate);
}
