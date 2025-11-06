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
import org.openelisglobal.common.formfields.FormFields.Field;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;

public class DefaultFormFields extends AFormFields {
    private HashMap<FormFields.Field, FormField> defaultAttributes = new HashMap<>();

    {
        /*
         * The rules for setting the default true or false are: If you have been asked
         * to hide an existing field then the default should be true If you have been
         * asked to add a new field then the default should be false
         *
         * The goal is to not break any existing configurations
         */
        setFieldTrue(Field.StNumber);
        setFieldTrue(Field.AKA);
        setFieldTrue(Field.MothersName);
        setFieldTrue(Field.PatientType);
        setFieldTrue(Field.InsuranceNumber);
        setFieldTrue(Field.CollectionDate);
        setFieldTrue(Field.OrgLocalAbrev);
        setFieldTrue(Field.ProviderInfo);
        setFieldTrue(Field.NationalID);
        setFieldTrue(Field.Occupation);
        setFieldTrue(Field.OrganizationAddressInfo);
        setFieldTrue(Field.OrganizationShortName);
        setFieldTrue(Field.OrganizationMultiUnit);
        setFieldTrue(Field.OrganizationOrgId);
        setFieldTrue(Field.ADDRESS_CITY);
        setFieldTrue(Field.PatientRequired);
        setFieldTrue(Field.QA_FULL_PROVIDER_INFO);
        setFieldTrue(Field.PatientNameRequired);
        setFieldTrue(Field.SampleEntryRequestingSiteSampleId);
        setFieldTrue(Field.ADDRESS_COMMUNE);
        setFieldTrue(Field.ADDRESS_VILLAGE);
        setFieldTrue(Field.SITE_DEPARTMENT);

        setFieldFalse(Field.OrgState);
        setFieldFalse(Field.ZipCode);
        setFieldFalse(Field.MLS);
        setFieldFalse(Field.CollectionTime);
        setFieldFalse(Field.RequesterSiteList);
        setFieldFalse(Field.InlineOrganizationTypes);
        setFieldFalse(Field.SubjectNumber);
        setFieldFalse(Field.ADDRESS_DEPARTMENT);
        setFieldFalse(Field.MotherInitial);
        setFieldFalse(Field.SearchSampleStatus);
        setFieldFalse(Field.DepersonalizedResults);
        setFieldFalse(Field.SEARCH_PATIENT_WITH_LAB_NO);
        setFieldFalse(Field.ResultsReferral);
        setFieldFalse(Field.ValueHozSpaceOnResults);
        setFieldFalse(Field.InitialSampleCondition);
        setFieldFalse(Field.OrganizationCLIA);
        setFieldFalse(Field.OrganizationParent);
        setFieldFalse(Field.PatientRequired_SampleConfirmation);
        setFieldFalse(Field.SampleCondition);
        setFieldFalse(Field.Project);
        setFieldFalse(Field.NON_CONFORMITY_SITE_LIST);
        setFieldFalse(Field.NON_CONFORMITY_SITE_LIST_USER_ADDABLE);
        setFieldFalse(Field.QaEventsBySection);
        setFieldFalse(Field.PatientIDRequired_SampleConfirmation);
        setFieldFalse(Field.SampleEntryUseReceptionHour);
        setFieldFalse(Field.SampleEntryUseRequestDate);
        setFieldFalse(Field.SampleEntryNextVisitDate);
        setFieldFalse(Field.SampleEntryReferralSiteNameRequired);
        setFieldFalse(Field.SampleEntryReferralSiteNameCapitialized);
        setFieldFalse(Field.SampleEntryReferralSiteCode);
        setFieldFalse(Field.SampleEntryProviderFax);
        setFieldFalse(Field.SampleEntryProviderEmail);
        setFieldFalse(Field.SampleEntryHealthFacilityAddress);
        setFieldFalse(Field.SampleEntrySampleCollector);
        setFieldFalse(Field.PatientPhone);
        setFieldFalse(Field.PatientHealthRegion);
        setFieldFalse(Field.PatientHealthDistrict);
        setFieldFalse(Field.PatientMarriageStatus);
        setFieldFalse(Field.PatientEducation);
        setFieldFalse(Field.SampleEntryPatientClinical);
        setFieldFalse(Field.SampleEntryRequesterPersonRequired);
        setFieldFalse(Field.QA_DOCUMENT_NUMBER);
        setFieldFalse(Field.QASubjectNumber);
        setFieldFalse(Field.QATimeWithDate);
        setFieldFalse(Field.SAMPLE_ENTRY_USE_REFFERING_PATIENT_NUMBER);
        setFieldFalse(Field.NON_CONFORMITY_PROVIDER_ADDRESS);
        setFieldFalse(Field.TEST_LOCATION_CODE);
        setFieldFalse(Field.SampleNature);
        setFieldFalse(Field.PatientEmail);

        setFieldLabelKey(Field.PatientHealthRegion, "person.health.region");
        setFieldLabelKey(Field.PatientHealthDistrict, "person.health.district");
    }

    private void setFieldLabelKey(Field field, String labelKey) {
        if (!defaultAttributes.containsKey(field)) {
            defaultAttributes.put(field, new FormField());
        }
        defaultAttributes.get(field).setLabelKey(labelKey);
    }

    private void setFieldFalse(Field field) {
        if (!defaultAttributes.containsKey(field)) {
            defaultAttributes.put(field, new FormField());
        }
        defaultAttributes.get(field).setInUse(false);
    }

    private void setFieldTrue(Field field) {
        if (!defaultAttributes.containsKey(field)) {
            defaultAttributes.put(field, new FormField());
        }
        defaultAttributes.get(field).setInUse(true);
    }

    @Override
    protected HashMap<Field, FormField> getDefaultAttributes() {
        return defaultAttributes;
    }

    @Override
    protected HashMap<Field, FormField> getSetAttributes() {
        String fieldSet = ConfigurationProperties.getInstance().getPropertyValueUpperCase(Property.FormFieldSet);

        if (IActionConstants.FORM_FIELD_SET_LNSP_HAITI.equals(fieldSet)) {
            return new HT_LNSPFormFields().getImplementationAttributes();
        } else if (IActionConstants.FORM_FIELD_SET_HAITI.equals(fieldSet)) {
            return new HT_ClinicalFormFields().getImplementationAttributes();
        } else if (IActionConstants.FORM_FIELD_SET_LNSP_CI.equals(fieldSet)) {
            return new CI_LNSPFormFields().getImplementationAttributes();
        } else if (IActionConstants.FORM_FIELD_SET_CDI.equals(fieldSet)) {
            return new CI_RETROFormFields().getImplementationAttributes();
        } else if (IActionConstants.FORM_FIELD_SET_CI_GENERAL.equals(fieldSet)) {
            return new CI_GeneralFormFields().getImplementationAttributes();
        } else if (IActionConstants.FORM_FIELD_SET_KENYA.equals(fieldSet)) {
            return new KenyaFormFields().getImplementationAttributes();
        } else if (IActionConstants.FORM_FIELD_SET_MAURITIUS.equals(fieldSet)) {
            return new MauritiusFormFields().getImplementationAttributes();
        }
        return new HashMap<>();
    }
}
