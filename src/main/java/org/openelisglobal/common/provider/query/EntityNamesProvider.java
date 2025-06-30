/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
 */
package org.openelisglobal.common.provider.query;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Locale;
import org.apache.commons.validator.GenericValidator;
import org.json.simple.JSONObject;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.servlet.validation.AjaxServlet;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.renamemethod.service.RenameMethodService;
import org.openelisglobal.renametestsection.service.RenameTestSectionService;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;
import org.owasp.encoder.Encode;

public class EntityNamesProvider extends BaseQueryProvider {

    protected PanelService panelService = SpringContext.getBean(PanelService.class);
    protected RenameTestSectionService renameTestSectionService = SpringContext.getBean(RenameTestSectionService.class);
    protected TypeOfSampleService typeOfSampleService = SpringContext.getBean(TypeOfSampleService.class);
    protected UnitOfMeasureService unitOfMeasureService = SpringContext.getBean(UnitOfMeasureService.class);
    protected RenameMethodService renameMethodService = SpringContext.getBean(RenameMethodService.class);

    public static final String PANEL = "panel";
    public static final String SAMPLE_TYPE = "sampleType";
    public static final String TEST_SECTION = "testSection";
    public static final String UNIT_OF_MEASURE = "unitOfMeasure";
    public static final String METHOD = "method";
    protected AjaxServlet ajaxServlet = null;

    @Override
    public void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String id = request.getParameter("entityId");
        String entityName = request.getParameter("entityName");

        String jResult;
        JSONObject jsonResult = new JSONObject();
        String jString;

        if (GenericValidator.isBlankOrNull(id) || GenericValidator.isBlankOrNull(entityName)) {
            jResult = INVALID;
            jString = "Internal error, please contact Admin and file bug report";
        } else {
            jResult = createJsonTestNames(id, entityName, jsonResult);
            StringWriter out = new StringWriter();
            try {
                jsonResult.writeJSONString(out);
                jString = out.toString();
            } catch (IOException e) {
                LogEvent.logDebug(e);
                jResult = INVALID;
                jString = "Internal error, please contact Admin and file bug report";
            } catch (IllegalStateException e) {
                LogEvent.logDebug(e);
                jResult = INVALID;
                jString = "Internal error, please contact Admin and file bug report";
            }
        }
        ajaxServlet.sendData(Encode.forXmlContent(jString), Encode.forXmlContent(jResult), request, response);
    }

    @SuppressWarnings("unchecked")
    private String createJsonTestNames(String id, String entityName, JSONObject jsonResult)
            throws IllegalStateException {

        Localization localization = null;

        if (PANEL.equals(entityName)) {
            localization = getLocalizationForPanel(id);
        } else if (SAMPLE_TYPE.equals(entityName)) {
            localization = getLocalizationForSampleType(id);
        } else if (TEST_SECTION.equals(entityName)) {
            localization = getLocalizationForRenameTestSection(id);
        } else if (UNIT_OF_MEASURE.equals(entityName)) {
            localization = getLocalizationForUnitOfMeasure(id);
        } else if (METHOD.equals(entityName)) {
            localization = getLocalizationForRenameMethod(id);
        }
        // add entity types as needed

        if (localization != null) {

            JSONObject nameObject = new JSONObject();
            addAllLocalizations(nameObject, localization);
            // nameObject.put("english", localization.getEnglish());
            // nameObject.put("french", localization.getFrench());
            jsonResult.put("name", nameObject);

            return VALID;
        }

        return INVALID;
    }

    @SuppressWarnings("unchecked")
    private void addAllLocalizations(JSONObject jsonObject, Localization localization) {
        for (Locale locale : localization.getLocalesWithValue()) {
            jsonObject.put(locale.getDisplayLanguage(Locale.ENGLISH).toLowerCase(),
                    localization.getLocalizedValue(locale));
        }
    }

    private Localization getLocalizationForPanel(String id) {
        return panelService.getLocalizationForPanel(id);
    }

    private Localization getLocalizationForSampleType(String id) {
        return typeOfSampleService.getLocalizationForSampleType(id);
    }

    private Localization getLocalizationForRenameTestSection(String id) {
        return renameTestSectionService.getLocalizationForRenameTestSection(id);
    }

    private Localization getLocalizationForUnitOfMeasure(String id) {
        return unitOfMeasureService.getLocalizationForUnitOfMeasure(id);
    }

    private Localization getLocalizationForRenameMethod(String id) {
        return renameMethodService.getLocalizationForRenameMethod(id);
    }

    @Override
    public void setServlet(AjaxServlet as) {
        this.ajaxServlet = as;
    }

    @Override
    public AjaxServlet getServlet() {
        return this.ajaxServlet;
    }
}
