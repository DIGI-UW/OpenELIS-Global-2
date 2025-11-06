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
package org.openelisglobal.statusofsample.util;

import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.result.action.util.ResultsLoadUtility;
import org.openelisglobal.spring.util.SpringContext;

public class StatusRules {

    public boolean hasFailedValidation(String analysisStatusId) {
        return analysisStatusId
                .equals(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.BiologistRejected));
    }

    public void setAllowableStatusForLoadingResults(ResultsLoadUtility resultsLoadUtility) {
        resultsLoadUtility.addIncludedAnalysisStatus(AnalysisStatus.BiologistRejected);
        resultsLoadUtility.addIncludedAnalysisStatus(AnalysisStatus.NotStarted);
        resultsLoadUtility.addIncludedAnalysisStatus(AnalysisStatus.NonConforming_depricated);
        resultsLoadUtility.addIncludedAnalysisStatus(AnalysisStatus.TechnicalRejected);
        resultsLoadUtility.addIncludedAnalysisStatus(AnalysisStatus.TechnicalAcceptance);
        resultsLoadUtility.addIncludedSampleStatus(OrderStatus.Entered);
        resultsLoadUtility.addIncludedSampleStatus(OrderStatus.Started);
        resultsLoadUtility.addIncludedSampleStatus(OrderStatus.NonConforming_depricated);
    }

    public String getStartingAnalysisStatus() {
        return SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NotStarted);
    }

    public static boolean useRecordStatusForValidation() {
        String statusRules = ConfigurationProperties.getInstance().getPropertyValueUpperCase(Property.StatusRules);
        return statusRules.equals(IActionConstants.STATUS_RULES_RETROCI);
    }
}
