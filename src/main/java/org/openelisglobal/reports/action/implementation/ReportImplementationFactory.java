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
package org.openelisglobal.reports.action.implementation;

import org.apache.commons.validator.GenericValidator;

public class ReportImplementationFactory {

    public static IReportParameterSetter getParameterSetter(String report) {
        if (!GenericValidator.isBlankOrNull(report)) {
            ReportRegistry registry = org.openelisglobal.spring.util.SpringContext.getBean(ReportRegistry.class);
            return registry.getParameterSetter(report);
        }

        return null;
    }

    public static IReportCreator getReportCreator(String report) {
        if (!GenericValidator.isBlankOrNull(report)) {
            ReportRegistry registry = org.openelisglobal.spring.util.SpringContext.getBean(ReportRegistry.class);
            return registry.getReportCreator(report);
        }

        return null;
    }
}
