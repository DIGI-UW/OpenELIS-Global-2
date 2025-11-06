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
package org.openelisglobal.common.formfields;

import java.util.Map;

public class AdminFormFields {

    public static enum Field {
        ActionMenu, AnalyteMenu, AnalyzerTestNameMenu, CodeElementXref, CodeElementTypeMenu, CountyMenu, DictionaryMenu,
        DictionaryCategoryMenu, ExternalConnections, LabelMenu, MethodMenu, OrganizationMenu, PanelMenu, PanelItemMenu,
        PatientTypeMenu, ProgramMenu, ProjectMenu, ProviderMenu, QaEventMenu, ReceiverCodeElementMenu, RegionMenu,
        ResultLimitsMenu, RoleMenu, SiteInformationMenu, SampleEntryMenu, ResultInformationMenu, SampleDomainMenu,
        ScriptletMenu, SourceOfSampleMenu, StatusOfSampleMenu, TestMenu, TestAnalyteMenu, TestReflexMenu,
        TestResultMenu, TestSectionMenu, TestTrailerMenu, TypeOfSampleMenu, TypeOfSamplePanelMenu, TypeOfSampleTestMenu,
        TypeOfTestResultMenu, UnitOfMeasureMenu, TestAnalyteTestResult, LoginUserMenu, SystemUserMenu, UserRoleMenu,
        SystemModuleMenu, SystemUserSectionMenu, SystemUserModuleMenu, UnifiedSystemUserMenu, TestUsageAggregatation,
        RESULT_REPORTING_CONFIGURATION, PRINTED_REPORTS_CONFIGURATION, WORKPLAN_CONFIGURATION,
        NON_CONFORMITY_CONFIGURATION, PATIENT_ENTRY_CONFIGURATION,
    }

    private static AdminFormFields instance = null;

    private Map<AdminFormFields.Field, Boolean> fields;

    private AdminFormFields() {
        fields = new DefaultAdminFormFields().getFieldFormSet();
    }

    public static AdminFormFields getInstance() {
        if (instance == null) {
            instance = new AdminFormFields();
        }

        return instance;
    }

    public boolean useField(AdminFormFields.Field field) {
        return fields.get(field);
    }
}
