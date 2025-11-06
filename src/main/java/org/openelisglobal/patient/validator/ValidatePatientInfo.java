package org.openelisglobal.patient.validator;

import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.provider.query.PatientSearchResults;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.search.service.SearchResultsService;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.validation.Errors;

public class ValidatePatientInfo {

    private static final String AMBIGUOUS_DATE_CHAR = ConfigurationProperties.getInstance()
            .getPropertyValue(ConfigurationProperties.Property.AmbiguousDateHolder);
    private static final String AMBIGUOUS_DATE_HOLDER = AMBIGUOUS_DATE_CHAR + AMBIGUOUS_DATE_CHAR;

    public static void validatePatientInfo(Errors errors, PatientManagementInfo patientInfo) {
        boolean disallowDuplicateSubjectNumbers = ConfigurationProperties.getInstance()
                .isPropertyValueEqual(ConfigurationProperties.Property.ALLOW_DUPLICATE_SUBJECT_NUMBERS, "false");
        boolean disallowDuplicateNationalIds = ConfigurationProperties.getInstance()
                .isPropertyValueEqual(ConfigurationProperties.Property.ALLOW_DUPLICATE_NATIONAL_IDS, "false");
        if (disallowDuplicateSubjectNumbers || disallowDuplicateNationalIds) {
            String newSTNumber = GenericValidator.isBlankOrNull(patientInfo.getSTnumber()) ? null
                    : patientInfo.getSTnumber();
            String newSubjectNumber = GenericValidator.isBlankOrNull(patientInfo.getSubjectNumber()) ? null
                    : patientInfo.getSubjectNumber();
            String newNationalId = GenericValidator.isBlankOrNull(patientInfo.getNationalId()) ? null
                    : patientInfo.getNationalId();

            List<PatientSearchResults> results = SpringContext.getBean(SearchResultsService.class).getSearchResults(
                    null, null, newSTNumber, newSubjectNumber, newNationalId, null, null, null, null, null);

            PatientSearchResults existingResult = null;

            if (!GenericValidator.isBlankOrNull(patientInfo.getPatientPK())) {
                existingResult = SpringContext.getBean(SearchResultsService.class).getSearchResults(null, null, null,
                        null, null, null, patientInfo.getPatientPK(), null, null, null).get(0);
            }

            if (!results.isEmpty()) {

                for (PatientSearchResults result : results) {
                    if (!result.getPatientID().equals(patientInfo.getPatientPK())) {
                        if (disallowDuplicateSubjectNumbers && newSTNumber != null
                                && newSTNumber.equals(result.getSTNumber())) {
                            if (existingResult == null
                                    || (existingResult != null && !existingResult.getSTNumber().equals(newSTNumber))) {
                                errors.reject("error.duplicate.STNumber", null, null);
                            }
                        }
                        if (disallowDuplicateSubjectNumbers && newSubjectNumber != null
                                && newSubjectNumber.equals(result.getSubjectNumber())) {

                            if (existingResult == null || (existingResult != null
                                    && !existingResult.getSubjectNumber().equals(newSubjectNumber))) {
                                errors.reject("error.duplicate.subjectNumber", null, null);
                            }
                        }
                        if (disallowDuplicateNationalIds && newNationalId != null
                                && newNationalId.equals(result.getNationalId())) {

                            if (existingResult == null || (existingResult != null
                                    && !existingResult.getNationalId().equals(newNationalId))) {
                                errors.reject("error.duplicate.nationalId", null, null);
                            }
                        }
                    }
                }
            }
        }
        validateBirthdateFormat(patientInfo, errors);
    }

    private static void validateBirthdateFormat(PatientManagementInfo patientInfo, Errors errors) {
        String birthDate = patientInfo.getBirthDateForDisplay();
        boolean validBirthDateFormat = true;

        if (!org.apache.commons.validator.GenericValidator.isBlankOrNull(birthDate)) {
            validBirthDateFormat = birthDate.length() == 10;
            // the regex matches ambiguous day and month or ambiguous day or completely
            // formed date
            if (validBirthDateFormat) {
                validBirthDateFormat = birthDate.matches("(((" + AMBIGUOUS_DATE_HOLDER + "|\\d{2})/\\d{2})|"
                        + AMBIGUOUS_DATE_HOLDER + "/(" + AMBIGUOUS_DATE_HOLDER + "|\\d{2}))/\\d{4}");
            }

            if (!validBirthDateFormat) {
                errors.reject("error.birthdate.format", "error.birthdate.format");
            }
        }
    }
}
