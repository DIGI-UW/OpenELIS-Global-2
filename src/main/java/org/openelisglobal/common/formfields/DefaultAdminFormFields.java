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

import java.util.HashMap;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.formfields.AdminFormFields.Field;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;

public class DefaultAdminFormFields extends AAdminFormFields {
    private HashMap<AdminFormFields.Field, Boolean> defaultAttributes = new HashMap<>();

    {
        defaultAttributes.put(Field.ActionMenu, Boolean.FALSE);
        defaultAttributes.put(Field.AnalyteMenu, Boolean.FALSE);
        defaultAttributes.put(Field.CodeElementXref, Boolean.FALSE);
        defaultAttributes.put(Field.CodeElementTypeMenu, Boolean.FALSE);
        defaultAttributes.put(Field.CountyMenu, Boolean.FALSE);
        defaultAttributes.put(Field.DictionaryMenu, Boolean.FALSE);
        defaultAttributes.put(Field.DictionaryCategoryMenu, Boolean.FALSE);
        defaultAttributes.put(Field.ExternalConnections, Boolean.TRUE);
        defaultAttributes.put(Field.LabelMenu, Boolean.FALSE);
        defaultAttributes.put(Field.MethodMenu, Boolean.FALSE);
        defaultAttributes.put(Field.OrganizationMenu, Boolean.FALSE);
        defaultAttributes.put(Field.PanelMenu, Boolean.FALSE);
        defaultAttributes.put(Field.PanelItemMenu, Boolean.FALSE);
        defaultAttributes.put(Field.PatientTypeMenu, Boolean.FALSE);
        defaultAttributes.put(Field.ProgramMenu, Boolean.FALSE);
        defaultAttributes.put(Field.ProjectMenu, Boolean.FALSE);
        defaultAttributes.put(Field.ProviderMenu, Boolean.FALSE);
        defaultAttributes.put(Field.QaEventMenu, Boolean.FALSE);
        defaultAttributes.put(Field.ReceiverCodeElementMenu, Boolean.FALSE);
        defaultAttributes.put(Field.RegionMenu, Boolean.FALSE);
        defaultAttributes.put(Field.ResultLimitsMenu, Boolean.FALSE);
        defaultAttributes.put(Field.RoleMenu, Boolean.FALSE);
        defaultAttributes.put(Field.SiteInformationMenu, Boolean.FALSE);
        defaultAttributes.put(Field.SampleEntryMenu, Boolean.TRUE);
        defaultAttributes.put(Field.ResultInformationMenu, Boolean.FALSE);
        defaultAttributes.put(Field.SampleDomainMenu, Boolean.FALSE);
        defaultAttributes.put(Field.ScriptletMenu, Boolean.FALSE);
        defaultAttributes.put(Field.SourceOfSampleMenu, Boolean.FALSE);
        defaultAttributes.put(Field.StatusOfSampleMenu, Boolean.FALSE);
        defaultAttributes.put(Field.TestMenu, Boolean.FALSE);
        defaultAttributes.put(Field.TestAnalyteMenu, Boolean.FALSE);
        defaultAttributes.put(Field.TestReflexMenu, Boolean.FALSE);
        defaultAttributes.put(Field.TestResultMenu, Boolean.FALSE);
        defaultAttributes.put(Field.TestSectionMenu, Boolean.FALSE);
        defaultAttributes.put(Field.TestTrailerMenu, Boolean.FALSE);
        defaultAttributes.put(Field.TypeOfSampleMenu, Boolean.FALSE);
        defaultAttributes.put(Field.TypeOfSamplePanelMenu, Boolean.FALSE);
        defaultAttributes.put(Field.TypeOfSampleTestMenu, Boolean.FALSE);
        defaultAttributes.put(Field.TypeOfTestResultMenu, Boolean.FALSE);
        defaultAttributes.put(Field.UnitOfMeasureMenu, Boolean.FALSE);
        defaultAttributes.put(Field.TestAnalyteTestResult, Boolean.FALSE);
        defaultAttributes.put(Field.LoginUserMenu, Boolean.FALSE);
        defaultAttributes.put(Field.SystemUserMenu, Boolean.FALSE);
        defaultAttributes.put(Field.UserRoleMenu, Boolean.FALSE);
        defaultAttributes.put(Field.SystemModuleMenu, Boolean.FALSE);
        defaultAttributes.put(Field.SystemUserSectionMenu, Boolean.FALSE);
        defaultAttributes.put(Field.SystemUserModuleMenu, Boolean.FALSE);
        defaultAttributes.put(Field.UnifiedSystemUserMenu, Boolean.FALSE);
        defaultAttributes.put(Field.TestUsageAggregatation, Boolean.FALSE);
        defaultAttributes.put(Field.RESULT_REPORTING_CONFIGURATION, Boolean.TRUE);
        defaultAttributes.put(Field.PRINTED_REPORTS_CONFIGURATION, Boolean.TRUE);
        defaultAttributes.put(Field.WORKPLAN_CONFIGURATION, Boolean.TRUE);
        defaultAttributes.put(Field.NON_CONFORMITY_CONFIGURATION, Boolean.TRUE);
        defaultAttributes.put(Field.PATIENT_ENTRY_CONFIGURATION, Boolean.TRUE);
        defaultAttributes.put(Field.AnalyzerTestNameMenu, Boolean.TRUE);
    }

    @Override
    protected HashMap<Field, Boolean> getDefaultAttributes() {
        return defaultAttributes;
    }

    @Override
    protected HashMap<Field, Boolean> getSetAttributes() {
        String fieldSet = ConfigurationProperties.getInstance().getPropertyValueUpperCase(Property.FormFieldSet);

        if (IActionConstants.FORM_FIELD_SET_LNSP_HAITI.equals(fieldSet)) {
            return new HT_LNSPAdministrationFormFields().getImplementationAttributes();
        } else if (IActionConstants.FORM_FIELD_SET_HAITI.equals(fieldSet)) {
            return new HT_AdminFormFields().getImplementationAttributes();
        } else if (IActionConstants.FORM_FIELD_SET_LNSP_CI.equals(fieldSet)) {
            return new CI_LNSPAdminFormFields().getImplementationAttributes();
        } else if (IActionConstants.FORM_FIELD_SET_CDI.equals(fieldSet)) {
            return new CI_RETROAdminFormFields().getImplementationAttributes();
        } else if (IActionConstants.FORM_FIELD_SET_CI_GENERAL.equals(fieldSet)) {
            return new CI_GeneralAdminFormFields().getImplementationAttributes();
        } else if (IActionConstants.FORM_FIELD_SET_KENYA.equals(fieldSet)) {
            return new KenyaAdminFormFields().getImplementationAttributes();
        } else if (IActionConstants.FORM_FIELD_SET_MAURITIUS.equals(fieldSet)) {
            return new MauritiusAdminFormFields().getImplementationAttributes();
        }

        return null;
    }
}
