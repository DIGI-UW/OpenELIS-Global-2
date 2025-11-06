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

    // public void deleteAll(List<AnalyzerResults> deletableAnalyzerResults) throws
    // LIMSRuntimeException;
}
