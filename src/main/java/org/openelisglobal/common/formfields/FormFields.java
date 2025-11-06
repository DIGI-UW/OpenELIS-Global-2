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
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.internationalization.MessageUtil;

/*
 * These are different fields on the forms which can be turned on and off by configuration.
 * Note that the administration menu is in it's own class because it is a big area confined to it a single page
 */
public class FormFields {
    // Note- these should all be upper case, change as you touch and add form name
    // to name
    public static enum Field {
        AKA, // Include AKA with patient info
        StNumber, // Include ST number with patient info
        MothersName, // Include Mothers name with patient info
        PatientType, // Include patient type with patient info
        InsuranceNumber, // Include patient insurance number will patient info
        CollectionDate, // Track collection date for samples, current date will be used if false
        CollectionTime, // Track collection time for samples
        RequesterSiteList, // Present list of referring sites
        SITE_DEPARTMENT, // list of departments under referring sites
        OrgLocalAbrev, // Use organization abbreviation. Should be standardized to FALSE
        OrgState, // Include state in organization info
        ZipCode, // Include zip code in organization info
        MLS, // Include indicator if organization is a sentinel lab
        InlineOrganizationTypes, // Should organization types be included when specifying organizations
        SubjectNumber, // Include subject number with patient info
        ProviderInfo, // Include provider information on order form
        NationalID, // Include national ID with patient info
        Occupation, // Include occupation with patient info
        MotherInitial, // Include mothers first initial with patient info
        SearchSampleStatus, // Can patients be searched for by status
        OrganizationAddressInfo, // Include address info with organization info
        OrganizationCLIA, // Include CLIA status with organization info
        OrganizationParent, // Include parent with organization info
        OrganizationShortName, // Include short name with organization info
        OrganizationMultiUnit, // Include multiunit status with organization address info
        OrganizationOrgId, // Include db id with organization info
        Project, // Include project (RETROCI) with non-conformity info
        SampleCondition, // Allow for collection of sample condition with non-conformity
        NON_CONFORMITY_SITE_LIST, // site (patient entry or nonconforming) is defined by a list of
        // sites.
        NON_CONFORMITY_SITE_LIST_USER_ADDABLE, // Should the user be able to add to the site list
        NON_CONFORMITY_PROVIDER_ADDRESS, // Should the providers address be collected on non-conformity
        // page
        QaEventsBySection, // Should sample non-conformity events regrouped by test section in report
        ADDRESS_CITY, // Is a city part of an address
        ADDRESS_DEPARTMENT, // Is department part of an address
        ADDRESS_COMMUNE, // Is a commune part of an address
        ADDRESS_VILLAGE, // Is a village part of an address
        DepersonalizedResults, // Should results entry have personal identifiers
        SEARCH_PATIENT_WITH_LAB_NO, // Should lab number be part of patient search
        ResultsReferral, // Can results be referred out
        ValueHozSpaceOnResults, // favors a layout which values horizontal space over vertical space
        InitialSampleCondition, // Allow for collection of sample condition with sample entry
        SampleNature, // Allow for collection of sample nature with sample entry
        PatientRequired, // By default, a (minimal) patient to go with a sample is required.
        PatientRequired_SampleConfirmation, // Is patient required for sample confirmation
        QA_FULL_PROVIDER_INFO, // Include provider information on non-conformity
        QASubjectNumber, // Include subject number be on non-conformity
        QATimeWithDate, // Include time in addition to date on non-conformity
        PatientIDRequired_SampleConfirmation, // Is patient ID required for patient on sample
        // conformation form
        PatientNameRequired, // Is patient name required
        SampleEntryUseReceptionHour, // Include reception time on sample entry
        SampleEntryUseRequestDate, // Include request date on sample entry
        SampleEntryNextVisitDate, // Include next visit date on sample entry
        SampleEntryRequestingSiteSampleId, // Include sample ID from requesting site
        SampleEntryReferralSiteNameRequired, // Is referral site required
        SampleEntryReferralSiteNameCapitialized, // Should referral site name be transformed to upper
        // case
        SampleEntryReferralSiteCode, // Include referral site code on sample entry
        SampleEntryProviderFax, // Include provider fax for sample entry
        SampleEntryProviderEmail, // Include provider email for sample entry
        SampleEntryHealthFacilityAddress, // Include referral address
        SampleEntrySampleCollector, // Include name of sample collector
        SampleEntryRequesterPersonRequired, // Is the requester person required
        SAMPLE_ENTRY_USE_REFFERING_PATIENT_NUMBER, // Include referral patient number
        PatientPhone, // Include patient phone with patient info
        PatientEmail, // Include patient email with patient info
        PatientHealthRegion, // Include patient health region with patient info
        PatientHealthDistrict, // Include patient health district with patient info
        PatientMarriageStatus, // Include patient marriage status with patient info
        PatientEducation, // Include patient education level with patient info
        SampleEntryPatientClinical, // Include patient clinical information on sample entry (request by
        // CI but not
        // currently implemented)
        QA_DOCUMENT_NUMBER, // Include document number on non-conformity
        TEST_LOCATION_CODE // Include test location code on order entry
    }

    private static FormFields instance = null;

    private Map<FormFields.Field, FormField> fields;

    private FormFields() {
        AFormFields defaultFields = new DefaultFormFields();
        fields = defaultFields.getFieldFormSet();
    }

    public static FormFields getInstance() {
        if (instance == null) {
            instance = new FormFields();
        }

        return instance;
    }

    public boolean useField(FormFields.Field field) {
        return fields.get(field).getInUse();
    }

    public boolean requireField(FormFields.Field field) {
        return fields.get(field).getRequired();
    }

    public String getLabel(FormFields.Field field) {
        FormField formField = fields.get(field);
        if (!GenericValidator.isBlankOrNull(formField.getLabel())) {
            return formField.getLabel();
        }
        return MessageUtil.getMessage(formField.getLabelKey());
    }
}
