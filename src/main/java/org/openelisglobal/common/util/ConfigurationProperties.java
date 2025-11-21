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
package org.openelisglobal.common.util;

import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.util.DefaultConfigurationProperties.OEProperties;
import org.openelisglobal.spring.util.SpringContext;

/*
 * This is an abstract class which represents the configuration properties of the application.  The derived
 * classes will determine how the propertiesValueMap is populated
 */
public abstract class ConfigurationProperties {

    protected OEProperties finalProperties;

    // These should all be upper case. As you touch them change them
    public enum Property {
        AmbiguousDateValue("date.ambiguous.date.value", "text"), // Are ambiguous dates allowed for DOB. i.e. patient
                                                                 // knows age
        // but not actuall DOB
        AmbiguousDateHolder("date.ambiguous.date.holder", "text"), // What character should be used as a placeholder
                                                                   // when
        // displaying the date. i.e. if 'X' then XX/XX/2000
        ReferingLabParentOrg("organization.reference.lab.parent", "text"), // Should the parent organization of a lab be
        // entered. i.e. The hospital in which the lab is run
        FormFieldSet("setFieldForm", "text"), // internal only
        PasswordRequirments("passwordRequirements", "text"), // Indicator for what the password complexity requirements
                                                             // are.
        // Should be standardized
        StringContext("stringContext", "text"), // Determines which equivalent string should be used. i.e. label for
        // accession
        // number can be 'lab number' or 'accession number'
        StatusRules("statusRules", "text"), // Only used to separate RetroCI rules from others. May be obsolete
        SiteCode("siteNumber", "text"), // Code referring to site
        SiteName("SiteName", "text"), // Name for site
        Addressline1label("Address line 1 label", "text"), // The address lines 1 can be used to specify anything
        Addressline2label("Address line 2 label", "text"), // The address lines 2 can be used to specify anything
        Addressline3label("Address line 3 label", "text"), // The address lines 3 can be used to specify anything
        GeographicUnit1Label("Geographic Unit 1 Label", "text"), // The Geographical units for a particular region
        GeographicUnit2Label("Geographic Unit 2 Label", "text"), // The Geographical units for a particular district
        AccessionFormat("acessionFormat", "text"), // Format of accession number can be one of SITEYEARNUM, YEARNUM OR
        // PROGRAMNUM, ALPHANUM see AccessionNumberValidatorFactory
        ReflexAction("reflexAction", "text"), // In combination with flags in reflex table determines actual action,
        // should be
        // standardize
        TrainingInstallation("TrainingInstallation", "text"), // Flag to indicate if this is a training instance
        UseExternalPatientInfo("useExternalPatientSource", "text"), // If true patient information will be searched for
        // externally
        PatientSearchURL("patientSearchURL", "text"), // URL of where external patient information will be searched
        PatientSearchUserName("patientSearchLogOnUser", "text"), // User name for accesses to external patient search
        PatientSearchPassword("PatientSearchPassword", "text"), // User password for accesses to external patient search
        PatientSearchEnabled("PatientSearchEnabled", "text"), labDirectorName("lab director", "text"), // The name of
                                                                                                       // the lab
                                                                                                       // director
        languageSwitch("allowLanguageChange", "text"), // If true a user can switch between English and French (changes
                                                       // it
        // for
        // everybody)
        reportResults("resultReporting", "text"), // If true results will be reported electronically
        resultReportingURL("resultReportingURL", "text"), // URL for electronic result reporting
        malariaSurveillanceReport("malariaSurReport", "text"), // If true malaria surveillance will be reported
                                                               // electronically
        malariaSurveillanceReportURL("malariaSurURL", "text"), // URL for malaria surveillance reporting
        malariaCaseReport("malariaCaseReport", "text"), // If true send malaria case reports
        malariaCaseReportURL("malariaCaseURL", "text"), // URL for malaria case reports
        releaseNumber("releaseNumber", "text"), // The release number
        // buildNumber("buildNumber", "text"), //Repository identifier
        configurationName("configuration name", "text"), // Identifies the configuration i.e. HaitiLNSP or CI IPCI
        testUsageReportingURL("testUsageAggregationUrl", "text"), // URL for test summary reporting
        testUsageReporting("testUsageReporting", "text"), // If true send test summary report electronically
        roleRequiredForModifyResults("modify results role", "text"), // If true a separate role is needed to modify
                                                                     // results
        notesRequiredForModifyResults("modify results note required", "text"), // If true a note is required when a
                                                                               // result is
        // modified
        resultTechnicianName("ResultTechnicianName", "text"), // If true the technicians name is needed for results
        customCriticalMessage("customCriticalMessage", "text"), // Override the default message for a critical result
        allowResultRejection("allowResultRejection", "text"), // If true then a technician has the ability to reject an
        // individual test and select a reason for rejection
        restrictFreeTextRefSiteEntry("restrictFreeTextRefSiteEntry", "text"), // If true then a user cannot enter new
                                                                              // referring
        // sites during sample entry and must choose from
        // list provided
        restrictFreeTextMethodEntry("restrictFreeTextMethodEntry", "text"), // If true the user must choose an existing
                                                                            // method
        // and will not be able to create a new method
        // through result entry
        restrictFreeTextProviderEntry("restrictFreeTextProviderEntry", "text"), // If true then a user cannot enter new
        // providers during sample entry and must choose
        // from list provided
        autoFillTechNameBox("autoFillTechNameBox", "text"), // If true a box will be provided to auto-fill technicians
                                                            // name for
        // all results on page
        autoFillTechNameUser("autoFillTechNameUser", "text"), // If true the technicians name will be auto-filled with
                                                              // the name
        // of the logged in user
        AUTOFILL_COLLECTION_DATE("auto-fill collection date/time", "text"), // If true the collection date will be
                                                                            // auto-filled
        // with current date
        failedValidationMarker("showValidationFailureIcon", "text"), // If true results that failed validation will have
                                                                     // icon
        // next to them
        resultsResendTime("results.send.retry.time", "text"), // How much time between trying to resend results that
                                                              // failed to
        // reach their destination
        TRACK_PATIENT_PAYMENT("trackPayment", "text"), // If true then patient payment status can be entered
        ACCESSION_NUMBER_VALIDATE("validateAccessionNumber", "text"), // If true then validate the accession number
        ALERT_FOR_INVALID_RESULTS("alertWhenInvalidResult", "text"), // If true then technician will get an alert for
                                                                     // results
        // outside of the valid range
        DEFAULT_LANG_LOCALE("default language locale", "text"), // Default language locale
        DEFAULT_DATE_LOCALE("default date locale", "text"), // Date local
        CONDENSE_NFS_PANEL("condenseNFS", "text"), // Flag used for RetroCI to condense NFS panel tests
        PATIENT_DATA_ON_RESULTS_BY_ROLE("roleForPatientOnResults", "text"), // If true patient data will show on results
                                                                            // page
        // only if user has correct permissions
        USE_PAGE_NUMBERS_ON_REPORTS("reportPageNumbers", "text"), // If true page numbers will be used on reports
        QA_SORT_EVENT_LIST("sortQaEvents", "text"), // If true QA events will be sorted by name
        ALWAYS_VALIDATE_RESULTS("validate all results", "text"), // If true all results will be validated, otherwise
                                                                 // just those
        // outside of valid range and those flagged
        ADDITIONAL_SITE_INFO("additional site info", "text"), // Extra site info for report header
        SUBJECT_ON_WORKPLAN("subject on workplan", "text"), // If true the subject will be on the workplan
        NEXT_VISIT_DATE_ON_WORKPLAN("next visit on workplan", "text"), // If true the next visit date will be on
                                                                       // workplan
        RESULTS_ON_WORKPLAN("results on workplan", "text"), // If true a space will be left for results on the workplan
        ACCEPT_EXTERNAL_ORDERS("external orders", "text"), // If true EMR's can make electronic orders to lab
        SIGNATURES_ON_NONCONFORMITY_REPORTS("non-conformity signature", "text"), // If true a space should be left for
        // signatures on non-conformity reports
        NONCONFORMITY_RECEPTION_AS_UNIT("Reception as unit", "text"), // If true then reception will be an option for
                                                                      // where a
        // non-conformity was identified
        NONCONFORMITY_SAMPLE_COLLECTION_AS_UNIT("Collection as unit", "text"), // If true then sample collection will be
                                                                               // an
        // option for where a non-conformity was
        // identified
        ACCESSION_NUMBER_PREFIX("Accession number prefix", "text"), // If SITEYEARNUM is the format then this is the
                                                                    // prefix
        NOTE_EXTERNAL_ONLY_FOR_VALIDATION("validationOnlyNotesAreExternal", "text"), // If true then only validation
                                                                                     // notes will
        // be on patient report
        PHONE_FORMAT("phone format", "text"), // Format of phone number
        VALIDATE_PHONE_FORMAT("validate phone format", "text"), // If true then entered phone numbers will be validated
                                                                // against
        // format
        ALLOW_DUPLICATE_SUBJECT_NUMBERS("Allow duplicate subject number", "text"), // If true then duplicate subject
                                                                                   // numbers are
        // allowed
        ALLOW_DUPLICATE_NATIONAL_IDS("Allow duplicate national ids", "text"), // If true then duplicate national ids are
                                                                              // allowed
        VALIDATE_REJECTED_TESTS("validateTechnicalRejection", "text"), // If true then if the technician rejects a test
                                                                       // the next
        // step is validation
        TEST_NAME_AUGMENTED("augmentTestNameWithType", "text"), // If true then in some places the test name will be
                                                                // suffixed
        // with the sample type
        USE_BILLING_REFERENCE_NUMBER("billingRefNumber", "text"), // If true then the user can enter billing codes for
                                                                  // latter
        // reporting
        BILLING_REFERENCE_NUMBER_LABEL("billingRefNumberLocalization", "localization"), // The label being used for the
                                                                                        // billing
        // reference number
        ORDER_PROGRAM("Program", "text"), // Should program be part of an order
        BANNER_TEXT("bannerHeading", "localization"), // Text on Banner
        CLOCK_24("24 hour clock", "text"), // True for 24 hour clock, false for 12 hour clock
        PATIENT_NATIONALITY("supportPatientNationality", "text"), // True if patient nationality should be collected
                                                                  // with
        // patient information
        PATIENT_ID_REQUIRED("Patient ID required", "text"), // True if patient id is required for new patient
        PATIENT_SUBJECT_NUMBER_REQUIRED("Subject number required", "text"), // True if patient subject number is
                                                                            // required for
        // new patient
        PATIENT_NATIONAL_ID_REQUIRED("National ID required", "text"), // True if patient national id is required for new
                                                                      // patient
        QA_SAMPLE_ID_REQUIRED("sample id required", "text"), // True if sample id required from referring lab
        MAX_ORDER_PRINTED("numMaxOrderLabels", "text"), // Max number of order labels that can be printed
        MAX_SPECIMEN_PRINTED("numMaxSpecimenLabels", "text"), // Max number of specimen labels that can be printed
        MAX_ALIQUOT_PRINTED("numMaxAliquotLabels", "text"), // Max number of aliquots that can be printed
        DEFAULT_ORDER_PRINTED("numDefaultOrderLabels", "text"), // Max number of order labels that can be printed
        DEFAULT_SPECIMEN_PRINTED("numDefaultSpecimenLabels", "text"), // Max number of specimen labels that can be
                                                                      // printed
        DEFAULT_ALIQUOT_PRINTED("numDefaultAliquotLabels", "text"), // Max number of aliquots that can be printed
        ORDER_BARCODE_HEIGHT("heightOrderLabels", "text"), // Height of the order barcode
        ORDER_BARCODE_WIDTH("widthOrderLabels", "text"), // Width of the order barcode
        SPECIMEN_BARCODE_HEIGHT("heightSpecimenLabels", "text"), // Height of the specimen barcode
        SPECIMEN_BARCODE_WIDTH("widthSpecimenLabels", "text"), // Width of the specimen barcode
        SPECIMEN_FIELD_DATE("collectionDateCheck", "text"), //
        SPECIMEN_FIELD_SEX("patientSexCheck", "text"), //
        SPECIMEN_FIELD_COLLECTED_BY("collectedByCheck", "text"), SPECIMEN_FIELD_TESTS("testsCheck", "text"), //
        BLOCK_BARCODE_HEIGHT("heightBlockLabels", "text"), //
        BLOCK_BARCODE_WIDTH("widthBlockLabels", "text"), //
        SLIDE_BARCODE_HEIGHT("heightSlideLabels", "text"), //
        SLIDE_BARCODE_WIDTH("widthSlideLabels", "text"), //
        STORAGE_LOCATION_BARCODE_HEIGHT("heightStorageLocationLabels", "text"), // Height of storage location barcode
                                                                                // labels
        STORAGE_LOCATION_BARCODE_WIDTH("widthStorageLocationLabels", "text"), // Width of storage location barcode
                                                                              // labels
        ALT_ACCESSION_PREFIX("prePrintAltAccessionPrefix", "text"), //
        USE_ALT_ACCESSION_PREFIX("prePrintUseAltAccession", "text"), //
        USE_ALPHANUM_ACCESSION_PREFIX("useAlphanumAccessionPrefix", "text"), //
        ALPHANUM_ACCESSION_PREFIX("alphanumAccessionPrefix", "text"), //
        LAB_DIRECTOR_NAME("labDirectorName", "text"), //
        LAB_DIRECTOR_TITLE("labDirectorTitle", "text"), //
        INFO_HIGHWAY_USERNAME("infoHighway.username", "text"), //
        INFO_HIGHWAY_PASSWORD("infoHighway.password", "text"), //
        INFO_HIGHWAY_ADDRESS("infoHighway.uri", "text"), //
        INFO_HIGHWAY_ENABLED("infoHighway.enabled", "text"), //
        PATIENT_RESULTS_BMP_SMS_USERNAME("patientresultsbmpsms.username", "text"), //
        PATIENT_RESULTS_BMP_SMS_PASSWORD("patientresultsbmpsms.password", "text"), //
        PATIENT_RESULTS_BMP_SMS_ADDRESS("patientresultsbmpsms.uri", "text"), //
        PATIENT_RESULTS_BMP_SMS_ENABLED("patientresultsbmpsms.enabled", "text"), //
        PATIENT_RESULTS_SMPP_SMS_USERNAME("patientresultssmpp.username", "text"), //
        PATIENT_RESULTS_SMPP_SMS_PASSWORD("patientresultssmpp.password", "text"), //
        PATIENT_RESULTS_SMPP_SMS_ADDRESS("patientresultssmpp.uri", "text"), //
        PATIENT_RESULTS_SMPP_SMS_ENABLED("patientresultssmpp.enabled", "text"), //
        PATIENT_RESULTS_SMTP_USERNAME("patientresultssmtp.username", "text"), //
        PATIENT_RESULTS_SMTP_PASSWORD("patientresultssmtp.password", "text"), //
        PATIENT_RESULTS_SMTP_ADDRESS("patientresultssmtp.uri", "text"), //
        PATIENT_RESULTS_SMTP_ENABLED("patientresultssmtp.enabled", "text"), //
        CONTACT_TRACING("contactTracingEnabled", "text"), //
        REQUIRE_LAB_UNIT_AT_LOGIN("requireLabUnitAtLogin", "text"), //
        ENABLE_CLIENT_REGISTRY("enableClientRegistry", "text"), // if true, then client registry search option is
        BAR_CODE_TYPE("BarCodeType", "text");

        // visible on
        // the ui

        private String dbName;
        private String propertyType; // text, localization

        private Property(String dbName, String propertyType) {
            this.dbName = dbName;
            this.propertyType = propertyType;
        }

        public String getDBName() {
            return dbName;
        }

        public static Property fromDBName(String name) {
            for (Property e : Property.values()) {
                if (e.getDBName().equals(name))
                    return e;
            }
            return null;
        }

        public String getPropertyType() {
            return propertyType;
        }
    }

    public static ConfigurationProperties getInstance() {
        return SpringContext.getBean(DefaultConfigurationProperties.class);
    }

    public String getPropertyValue(String propertyName) {
        return GenericValidator.isBlankOrNull(finalProperties.getProperty(propertyName)) ? null
                : finalProperties.getProperty(propertyName).trim();
    }

    public String getPropertyValue(Property property) {
        return GenericValidator.isBlankOrNull(finalProperties.getProperty(property.name())) ? null
                : finalProperties.getProperty(property.name()).trim();
    }

    public String getPropertyValueUpperCase(Property property) {
        String value = getPropertyValue(property);
        return value == null ? null : value.toUpperCase();
    }

    public String getPropertyValueLowerCase(Property property) {
        String value = getPropertyValue(property);
        return value == null ? null : value.toLowerCase();
    }

    public static void forceReload() {
        SpringContext.getBean(DefaultConfigurationProperties.class).initialize();
        SpringContext.getBean(ConfigurationListenerService.class).refreshConfigurations();
    }

    public static void loadDBValuesIntoConfiguration() {
        SpringContext.getBean(DefaultConfigurationProperties.class).loadChangedValuesFromDatabaseIntoFinalProperties();
        SpringContext.getBean(ConfigurationListenerService.class).refreshConfigurations();
    }

    public boolean isPropertyValueEqual(Property property, String target) {

        if (target == null) {
            return getPropertyValue(property) == null;
        } else {
            return target.equals(getPropertyValue(property));
        }
    }

    public boolean isCaseInsensitivePropertyValueEqual(Property property, String target) {
        if (target == null) {
            return getPropertyValue(property) == null;
        } else {
            return target.toLowerCase().equals(getPropertyValueLowerCase(property));
        }
    }

    public void setPropertyValue(Property property, String value) {
        finalProperties.setPropertyValue(property.name(), value);
    }
}
