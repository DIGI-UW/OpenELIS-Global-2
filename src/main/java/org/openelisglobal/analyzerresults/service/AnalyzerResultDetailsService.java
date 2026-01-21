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
package org.openelisglobal.analyzerresults.service;

import org.openelisglobal.analyzerresults.bean.AnalyzerResultDetailsDTO;

import java.util.List;

public interface AnalyzerResultDetailsService {

    AnalyzerResultDetailsDTO getResultDetails(String analyzerResultId);

    List<AnalyzerResultDetailsDTO.PreviousResult> getPreviousResults(String testId, String accessionNumber);

    List<AnalyzerResultDetailsDTO.QCResult> getQCData(String analyzerId, String runDate, String testName);

    List<AnalyzerResultDetailsDTO.ReagentLot> getReagentLots(String analyzerId, String testName);

    AnalyzerResultDetailsDTO.RunInfo getRunInfo(String analyzerResultId);

    AnalyzerResultDetailsDTO.DeltaCheck calculateDeltaCheck(String testId, String accessionNumber, String currentValue);
}
