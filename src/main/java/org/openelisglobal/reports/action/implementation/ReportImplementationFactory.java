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
import org.openelisglobal.module.runtime.ModuleRegistry;
import org.openelisglobal.spring.util.SpringContext;

public class ReportImplementationFactory {
    private static final boolean isLNSP = true;

    public static IReportParameterSetter getParameterSetter(String report) {
        if (!GenericValidator.isBlankOrNull(report)) {
            ReportRegistry registry = SpringContext.getBean(ReportRegistry.class);
            return registry.getParameterSetter(report);
        }
        return null;
    }

    private static boolean isNonConformityByDateReport(String report) {
        return report.equals("retroCINonConformityByDate") || report.equals("haitiNonConformityByDate")
                || report.equals("haitiClinicalNonConformityByDate");
    }

    private static boolean isNonConformityBySectionReport(String report) {
        return report.equals("retroCInonConformityBySectionReason")
                || report.equals("haitiNonConformityBySectionReason")
                || report.equals("haitiClinicalNonConformityBySectionReason");
    }

    /**
     * A report answered by an installed module's {@code ReportProvider} (the
     * {@code report} extension point), or {@code null} when no module claims the
     * id, including in contexts without the module runtime.
     */
    static IReportCreator moduleReportCreator(String report) {
        if (GenericValidator.isBlankOrNull(report)) {
            return null;
        }
        ModuleRegistry registry;
        try {
            registry = SpringContext.getBean(ModuleRegistry.class);
        } catch (RuntimeException e) {
            return null;
        }
        return registry.extensions().reportProvider(report).map(ModuleReportCreator::new).orElse(null);
    }

    /**
     * A built-in report first, then a module's report provider; never a module
     * taking over a core report id.
     */
    public static IReportCreator getReportCreator(String report) {
        IReportCreator module = moduleReportCreator(report);
        return module != null ? module : builtInReportCreator(report);
    }

    /**
     * Whether the core itself ships a report with this id. Only a matching id
     * instantiates a creator, and a creator that fails to construct (no Spring
     * context, as in a unit test) still proves the id is taken.
     */
    public static boolean isBuiltInReport(String report) {
        try {
            return builtInReportCreator(report) != null;
        } catch (RuntimeException | LinkageError e) {
            return true;
        }
    }

    static IReportCreator builtInReportCreator(String report) {
        if (GenericValidator.isBlankOrNull(report)) {
            return null;
        }

        ReportRegistry registry = SpringContext.getBean(ReportRegistry.class);
        return registry.getReportCreator(report);
    }
}
