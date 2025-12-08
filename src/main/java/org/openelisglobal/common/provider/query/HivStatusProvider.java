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
package org.openelisglobal.common.provider.query;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.servlet.validation.AjaxServlet;
import org.openelisglobal.common.util.XMLUtil;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.observationhistorytype.ObservationHistoryTypeMap;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.spring.util.SpringContext;

public class HivStatusProvider extends BaseQueryProvider {

    protected ObservationHistoryService observationHistoryService = SpringContext
            .getBean(ObservationHistoryService.class);

    private static final String NOT_FOUND = "Not found";
    /** */
    private static final String HIV_STATUS_OH_TYPE = "hivStatus";

    public HivStatusProvider() {
        super();
    }

    public HivStatusProvider(AjaxServlet ajaxServlet) {
        this.ajaxServlet = ajaxServlet;
    }

    @Override
    public void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String patientId = request.getParameter("patientId");
        StringBuilder xml = new StringBuilder();
        String result = findHivStatus(patientId, xml);
        ajaxServlet.sendData(xml.toString(), result, request, response);
    }

    public String findHivStatus(String patientId, StringBuilder xml) throws LIMSRuntimeException {
        String retVal = VALID;

        try {
            String hivStatus = findHIVStatusValue(patientId);
            if (NOT_FOUND.equals(hivStatus)) {
                return INVALID;
            }
            XMLUtil.appendKeyValue(HIV_STATUS_OH_TYPE, hivStatus, xml);
        } catch (RuntimeException e) {
            LogEvent.logError(e.getMessage(), e);
            retVal = INVALID;
        }
        return retVal;
    }

    private String findHIVStatusValue(String patientId) {
        Patient patient = new Patient();
        patient.setId(patientId);
        String typeId = ObservationHistoryTypeMap.getInstance().getIDForType(HIV_STATUS_OH_TYPE);
        List<ObservationHistory> list = observationHistoryService.getAll(patient, null, typeId);

        return list.isEmpty() ? NOT_FOUND : list.get(list.size() - 1).getValue();
    }
}
