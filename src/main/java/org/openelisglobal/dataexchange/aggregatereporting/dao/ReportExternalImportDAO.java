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
package org.openelisglobal.dataexchange.aggregatereporting.dao;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.dataexchange.aggregatereporting.valueholder.ReportExternalImport;

public interface ReportExternalImportDAO extends BaseDAO<ReportExternalImport, String> {
    public List<ReportExternalImport> getReportsInDateRangeSorted(Timestamp lower, Timestamp upper)
            throws LIMSRuntimeException;

    // public void insertReportExternalImport(ReportExternalImport report) throws
    // LIMSRuntimeException;

    // public void updateReportExternalImport(ReportExternalImport report) throws
    // LIMSRuntimeException;

    public List<String> getUniqueSites() throws LIMSRuntimeException;

    public List<ReportExternalImport> getReportsInDateRangeSortedForSite(Timestamp beginning, Timestamp end,
            String site) throws LIMSRuntimeException;

    public ReportExternalImport getReportByEventDateSiteType(ReportExternalImport importReport)
            throws LIMSRuntimeException;
}
