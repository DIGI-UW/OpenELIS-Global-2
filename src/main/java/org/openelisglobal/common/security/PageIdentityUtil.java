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
package org.openelisglobal.common.security;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.exception.LIMSRuntimeException;

public class PageIdentityUtil {

    private static final String REPORT_PARAMETER = "report";
    private static final String TYPE_PARAMETER = "type";

    public static boolean isMainPage(HttpServletRequest request) {
        String actionName = (String) request.getAttribute(IActionConstants.ACTION_KEY);

        return (IActionConstants.MAIN_PAGE.equals(actionName));
    }

    /*
     * This is ripped and simplified from userModuleServiceImp. In it's most basic
     * form it gets the ACTION_KEY
     */
    public static String getActionName(HttpServletRequest request, boolean useParameterExtention)
            throws LIMSRuntimeException {

        String actionName = null;

        actionName = (String) request.getAttribute(IActionConstants.ACTION_KEY);

        // String localizedName =
        // MessageUtil.getContextualMessage("dictionary.result.Positif_VIH_2");
        // LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown", "gnrTest:
        // " +
        // localizedName);

        if (actionName == null) {
            // LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown",
            // "actionName is
            // null");
            actionName = "dummy";
        } else {
            // LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown",
            // "actionName is "
            // + Encode.forJava(actionName));
        }

        if (actionName.equals("QuickEntryAddTestPopup")) {
            actionName = "QuickEntry";
        } else if (actionName.equals("TestManagementAddTestPopup")) {
            actionName = "TestManagement";
        } else if (actionName.equals("TestAnalyteTestResultAddDictionaryRGPopup")
                || actionName.equals("TestAnalyteTestResultAddNonDictionaryRGPopup")
                || actionName.equals("TestAnalyteTestResultAddRGPopup")
                || actionName.equals("TestAnalyteTestResultAssignRGPopup")
                || actionName.equals("TestAnalyteTestResultEditDictionaryRGPopup")
                || actionName.equals("TestAnalyteTestResultEditDictionaryRGPopup")
                || actionName.equals("TestAnalyteTestResultEditNonDictionaryRGPopup")) {
            actionName = "TestAnalyteTestResult";
        } else if (actionName.equals("QaEventsEntryAddQaEventsToTestsPopup")
                || actionName.equals("QaEventsEntryAddActionsToQaEventsPopup")) {
            actionName = "QaEventsEntry";
        }

        actionName = actionName.endsWith("Menu") ? actionName.substring(0, actionName.length() - 4) : actionName;

        if (useParameterExtention) {
            String parameter = request.getParameter(TYPE_PARAMETER);

            if (GenericValidator.isBlankOrNull(parameter)) {
                parameter = request.getParameter(REPORT_PARAMETER);
            }

            if (!GenericValidator.isBlankOrNull(parameter)) {
                actionName += ":" + parameter;
            }
        }

        return actionName;
    }
}
