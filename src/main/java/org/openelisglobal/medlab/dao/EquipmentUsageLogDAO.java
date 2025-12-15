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
import org.openelisglobal.medlab.valueholder.EquipmentUsageLog;

/**
 * DAO interface for EquipmentUsageLog entity operations.
 */
public interface EquipmentUsageLogDAO extends BaseDAO<EquipmentUsageLog, Integer> {

    /**
     * Get usage logs for a specific analyzer within a date range.
     *
     * @param analyzerId the analyzer ID
     * @param startDate  start of date range
     * @param endDate    end of date range
     * @return list of usage logs ordered by date/time
     */
    List<EquipmentUsageLog> getUsageLogsByAnalyzerAndDateRange(Integer analyzerId, Date startDate, Date endDate);

    /**
     * Get usage logs for a specific operator.
     *
     * @param operatorId the operator/user ID
     * @param startDate  start of date range
     * @param endDate    end of date range
     * @return list of usage logs
     */
    List<EquipmentUsageLog> getUsageLogsByOperatorAndDateRange(Integer operatorId, Date startDate, Date endDate);

    /**
     * Get all active sessions (no end time set).
     *
     * @return list of active usage sessions
     */
    List<EquipmentUsageLog> getActiveSessions();

    /**
     * Get usage logs with errors for a date range.
     *
     * @param startDate start of date range
     * @param endDate   end of date range
     * @return list of usage logs with errors
     */
    List<EquipmentUsageLog> getErrorLogsByDateRange(Date startDate, Date endDate);

    /**
     * Get maintenance logs for an analyzer within a date range.
     *
     * @param analyzerId the analyzer ID
     * @param startDate  start of date range
     * @param endDate    end of date range
     * @return list of usage logs with maintenance performed
     */
    List<EquipmentUsageLog> getMaintenanceLogsByAnalyzer(Integer analyzerId, Date startDate, Date endDate);

    /**
     * Get total samples processed by analyzer within a date range.
     *
     * @param analyzerId the analyzer ID
     * @param startDate  start of date range
     * @param endDate    end of date range
     * @return total samples processed
     */
    Long getTotalSamplesProcessedByAnalyzer(Integer analyzerId, Date startDate, Date endDate);
}
