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

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.medlab.valueholder.QualityCheck;

/** DAO interface for QualityCheck entity operations. */
public interface QualityCheckDAO extends BaseDAO<QualityCheck, Integer> {

    /**
     * Get all quality checks for a specific sample item.
     *
     * @param sampleItemId the sample item ID
     * @return list of quality checks
     */
    List<QualityCheck> getQualityChecksBySampleItemId(Integer sampleItemId);

    /**
     * Get quality checks by status.
     *
     * @param status ACCEPTED or REJECTED
     * @return list of quality checks
     */
    List<QualityCheck> getQualityChecksByStatus(QualityCheck.OverallStatus status);

    /**
     * Get quality checks within a date range.
     *
     * @param startDate start of date range
     * @param endDate   end of date range
     * @return list of quality checks
     */
    List<QualityCheck> getQualityChecksByDateRange(Timestamp startDate, Timestamp endDate);

    /**
     * Get rejected quality checks for a date range (for reporting).
     *
     * @param startDate start of date range
     * @param endDate   end of date range
     * @return list of rejected quality checks
     */
    List<QualityCheck> getRejectedQualityChecksByDateRange(Timestamp startDate, Timestamp endDate);

    /**
     * Get the latest quality check for a sample item.
     *
     * @param sampleItemId the sample item ID
     * @return the most recent quality check, or null if none exists
     */
    QualityCheck getLatestQualityCheckForSample(Integer sampleItemId);
}
