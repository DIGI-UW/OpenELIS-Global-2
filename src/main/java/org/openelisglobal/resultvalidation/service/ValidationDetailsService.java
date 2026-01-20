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
package org.openelisglobal.resultvalidation.service;

import org.openelisglobal.resultvalidation.bean.ValidationDetailsDTO;

/**
 * Service for fetching detailed validation data on-demand Used for expandable
 * row details (History, QC, Method/Reagents, Order Info, Attachments)
 */
public interface ValidationDetailsService {

    ValidationDetailsDTO getValidationDetails(String analysisId);

    java.util.List<ValidationDetailsDTO.PreviousResult> getPreviousResults(String testId, String patientId);

    java.util.List<ValidationDetailsDTO.QCResult> getQCData(String analyzerId, String testId);

    java.util.List<ValidationDetailsDTO.ReagentLot> getReagentLots(String analysisId);

    ValidationDetailsDTO.OrderInfo getOrderInfo(String analysisId);

    java.util.List<ValidationDetailsDTO.Attachment> getAttachments(String analysisId);

    ValidationDetailsDTO.DeltaCheck calculateDeltaCheck(String analysisId, String currentValue);
}
