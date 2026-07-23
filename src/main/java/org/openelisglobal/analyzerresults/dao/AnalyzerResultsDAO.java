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
package org.openelisglobal.analyzerresults.dao;

import java.util.List;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.dao.BaseDAO;

public interface AnalyzerResultsDAO extends BaseDAO<AnalyzerResults, String> {

    // public List<AnalyzerResults> getResultsbyAnalyzer(String analyzerId) throws
    // LIMSRuntimeException;

    // public void insertAnalyzerResults(List<AnalyzerResults> results, String
    // sysUserId) throws
    // LIMSRuntimeException;

    // public void updateData(AnalyzerResults results) throws LIMSRuntimeException;

    // public void getData(AnalyzerResults results) throws LIMSRuntimeException;

    public AnalyzerResults readAnalyzerResults(String idString);

    public List<AnalyzerResults> getDuplicateResultByAccessionAndTest(AnalyzerResults result);

    /**
     * Staging rows the accept pipeline flagged as un-actionable: a Host Test Code
     * arrived that no {@code analyzer_test_map} row resolves, a cartridge code
     * slipped through as a result code, or a dict value didn't match any
     * {@code dict_entry}. These are the rows the Import Issues admin panel surfaces
     * so operators can add a mapping or alias instead of the row silently draining
     * out of the screen.
     * <p>
     * Capped to prevent unbounded result sets if a misconfiguration floods the
     * staging table overnight. Ordered newest first.
     */
    public List<AnalyzerResults> findWithImportIssues(int limit);

    // public void deleteAll(List<AnalyzerResults> deletableAnalyzerResults) throws
    // LIMSRuntimeException;
}
