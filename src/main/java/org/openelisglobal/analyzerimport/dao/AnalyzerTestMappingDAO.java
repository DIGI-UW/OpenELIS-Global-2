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
package org.openelisglobal.analyzerimport.dao;

import java.util.List;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMappingPK;
import org.openelisglobal.common.dao.BaseDAO;

public interface AnalyzerTestMappingDAO extends BaseDAO<AnalyzerTestMapping, AnalyzerTestMappingPK> {

    List<AnalyzerTestMapping> getAllForAnalyzer(String analyzerId);

    // List<AnalyzerTestMapping> getAllAnalyzerTestMappings() throws
    // LIMSRuntimeException;

    // void deleteData(List<AnalyzerTestMapping> testMappingList, String
    // currentUserId) throws
    // LIMSRuntimeException;

    // void insertData(AnalyzerTestMapping analyzerTestMapping, String
    // currentUserId) throws
    // LIMSRuntimeException;

    // void updateMapping(AnalyzerTestMapping analyzerTestNameMapping, String
    // currentUserId) throws
    // LIMSRuntimeException;
}
