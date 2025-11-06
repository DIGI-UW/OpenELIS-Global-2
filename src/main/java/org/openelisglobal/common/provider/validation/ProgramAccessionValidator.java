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
package org.openelisglobal.common.provider.validation;

import static org.openelisglobal.common.provider.validation.IAccessionNumberValidator.ValidationResults.PATIENT_STATUS_FAIL;
import static org.openelisglobal.common.provider.validation.IAccessionNumberValidator.ValidationResults.SAMPLE_FOUND;
import static org.openelisglobal.common.provider.validation.IAccessionNumberValidator.ValidationResults.SAMPLE_STATUS_FAIL;

import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.RecordStatus;
import org.openelisglobal.common.services.StatusSet;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.observationhistorytype.ObservationHistoryTypeMap;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.project.service.ProjectService;
import org.openelisglobal.project.valueholder.Project;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.util.AccessionNumberUtil;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spring.util.SpringContext;

public class ProgramAccessionValidator implements IAccessionNumberGenerator {

    protected SampleService sampleService = SpringContext.getBean(SampleService.class);
    protected ProjectService projectService = SpringContext.getBean(ProjectService.class);
    protected static ObservationHistoryService observationHistoryService = SpringContext
            .getBean(ObservationHistoryService.class);

    private static final String INCREMENT_STARTING_VALUE = "00001";
    private static final int UPPER_INC_RANGE = 99999;
    private static final int INCREMENT_START = 4;
    private static final int PROGRAM_START = 0;
    private static final int PROGRAM_END = 4;
    private static final int LENGTH = 9;
    private static final boolean NEED_PROGRAM_CODE = true;

    @Override
    public boolean needProgramCode() {
        return NEED_PROGRAM_CODE;
    }

    public String createFirstAccessionNumber(String programCode) {
        return programCode + INCREMENT_STARTING_VALUE;
    }

    public String incrementAccessionNumber(String currentHighAccessionNumber) {

        int increment = Integer.parseInt(currentHighAccessionNumber.substring(INCREMENT_START));
        String incrementAsString = INCREMENT_STARTING_VALUE;

        if (increment < UPPER_INC_RANGE) {
            increment++;
            incrementAsString = String.format("%05d", increment);
        } else {
            throw new IllegalArgumentException("AccessionNumber has no next value");
        }

        StringBuilder builder = new StringBuilder(
                currentHighAccessionNumber.substring(PROGRAM_START, PROGRAM_END).toUpperCase());
        builder.append(incrementAsString);

        return builder.toString();
    }

    @Override
    public ValidationResults validFormat(String accessionNumber, boolean checkDate) {
        // The rule is 4 digit program code and 5 incremented numbers
        if (accessionNumber.length() != LENGTH) {

            return ValidationResults.LENGTH_FAIL;
        }

        String programCode = accessionNumber.substring(PROGRAM_START, PROGRAM_END).toUpperCase();

        // check program code validity
        List<Project> programCodes = projectService.getAllProjects();

        boolean found = false;
        for (Project code : programCodes) {
            if (programCode.equals(code.getProgramCode())) {
                found = true;
                break;
            }
        }

        if (!found) {
            return ValidationResults.PROGRAM_FAIL;
        }

        try {
            Integer.parseInt(accessionNumber.substring(INCREMENT_START));
        } catch (NumberFormatException e) {
            return ValidationResults.FORMAT_FAIL;
        }

        return ValidationResults.SUCCESS;
    }

    @Override
    public String getInvalidMessage(ValidationResults results) {

        switch (results) {
        case LENGTH_FAIL:
            return MessageUtil.getMessage("sample.entry.invalid.accession.number.length");
        case USED_FAIL:
            return MessageUtil.getMessage("sample.entry.invalid.accession.number.used");
        case PROGRAM_FAIL:
            return MessageUtil.getMessage("sample.entry.invalid.accession.number.program");
        case FORMAT_FAIL:
            return MessageUtil.getMessage("sample.entry.invalid.accession.number.format");
        case REQUIRED_FAIL:
            return MessageUtil.getMessage("sample.entry.invalid.accession.number.required");
        case PATIENT_STATUS_FAIL:
            return MessageUtil.getMessage("sample.entry.invalid.accession.number.patientRecordStatus");
        case SAMPLE_STATUS_FAIL:
            return MessageUtil.getMessage("sample.entry.invalid.accession.number.sampleRecordStatus");
        default:
            return MessageUtil.getMessage("sample.entry.invalid.accession.number");
        }
    }

    @Override
    public String getInvalidFormatMessage(ValidationResults results) {
        return MessageUtil.getMessage("sample.entry.invalid.accession.number.format");
    }

    @Override
    public String getNextAvailableAccessionNumber(String prefix, boolean reserve) {
        String nextAccessionNumber = null;

        String curLargestAccessionNumber = sampleService.getLargestAccessionNumberWithPrefix(prefix);

        if (curLargestAccessionNumber == null) {
            nextAccessionNumber = createFirstAccessionNumber(prefix);
        } else {
            nextAccessionNumber = incrementAccessionNumber(curLargestAccessionNumber);
        }

        return nextAccessionNumber;
    }

    @Override
    public boolean accessionNumberIsUsed(String accessionNumber, String recordType) {
        boolean accessionNumberUsed = sampleService.getSampleByAccessionNumber(accessionNumber) != null;

        if (recordType == null) {
            return accessionNumberUsed;
        }
        StatusSet statusSet = SpringContext.getBean(IStatusService.class)
                .getStatusSetForAccessionNumber(accessionNumber);
        String recordStatus = new String();
        boolean isSampleEntry = recordType.contains("Sample");
        boolean isPatientEntry = recordType.contains("Patient");
        boolean isInitial = recordType.contains("initial");
        boolean isDouble = recordType.contains("double");

        if (accessionNumberUsed) {

            // sample entry, get SampleRecordStatus
            if (isSampleEntry) {
                recordStatus = statusSet.getSampleRecordStatus().toString();
            }

            // patient entry, get PatientRecordStatus
            else if (isPatientEntry) {
                recordStatus = statusSet.getPatientRecordStatus().toString();
            }

            // initial entry, the status must be NotRegistered
            String notRegistered = RecordStatus.NotRegistered.toString();
            String initialReg = RecordStatus.InitialRegistration.toString();
            if (isInitial) {
                if (!notRegistered.equals(recordStatus)) {
                    return true;
                }
            }

            // double entry, the status must be InitialRegistration
            else if (isDouble) {
                if (!initialReg.equals(recordStatus)) {
                    return false;
                } else {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public int getMaxAccessionLength() {
        return LENGTH;
    }

    @Override
    public int getMinAccessionLength() {
        return getMaxAccessionLength();
    }

    /**
     * There are many possible samples with various status, only some of which are
     * valid during certain entry steps. This method provides validation results
     * identifying whether a given sample is appropriate given all the information.
     *
     * @param accessionNumber the number for the sample
     * @param recordType      initialPatient, initialSample, doublePatient (double
     *                        entry for patient), doubleSample
     * @param isRequired      the step being done expects the sample to exist. This
     *                        is used generate appropriate results, either
     *                        REQUIRED_FAIL vs SAMPLE_NOT_FOUND
     * @param studyFormName   - an additional
     * @return
     */
    @Override
    public ValidationResults checkAccessionNumberValidity(String accessionNumber, String recordType, String isRequired,
            String studyFormName) {

        ValidationResults results = validFormat(accessionNumber, true);
        boolean accessionUsed = (sampleService.getSampleByAccessionNumber(accessionNumber) != null);
        if (results == ValidationResults.SUCCESS) {

            if (IActionConstants.TRUE.equals(isRequired) && !accessionUsed) {
                results = ValidationResults.REQUIRED_FAIL;
                return results;
            } else {
                if (recordType == null) {
                    results = ValidationResults.USED_FAIL;
                    return results;
                }
                // record Type specified, so work out the detailed response to report
                if (accessionUsed) {
                    if (recordType.contains("initial")) {
                        if (recordType.contains("Patient")) {
                            results = AccessionNumberUtil.isPatientStatusValid(accessionNumber,
                                    RecordStatus.NotRegistered);
                            if (results != PATIENT_STATUS_FAIL) {
                                results = matchExistingStudyFormName(accessionNumber, studyFormName, false);
                            }
                        } else if (recordType.contains("Sample")) {
                            results = AccessionNumberUtil.isSampleStatusValid(accessionNumber,
                                    RecordStatus.NotRegistered);
                            if (results != SAMPLE_STATUS_FAIL) {
                                results = matchExistingStudyFormName(accessionNumber, studyFormName, false);
                            }
                        }
                    } else if (recordType.contains("double")) {
                        if (recordType.contains("Patient")) {
                            results = AccessionNumberUtil.isPatientStatusValid(accessionNumber,
                                    RecordStatus.InitialRegistration);
                            if (results != PATIENT_STATUS_FAIL) {
                                results = matchExistingStudyFormName(accessionNumber, studyFormName, true);
                            }
                        } else if (recordType.contains("Sample")) {
                            results = AccessionNumberUtil.isSampleStatusValid(accessionNumber,
                                    RecordStatus.InitialRegistration);
                            if (results != SAMPLE_STATUS_FAIL) {
                                results = matchExistingStudyFormName(accessionNumber, studyFormName, true);
                            }
                        }
                    } else if (recordType.contains("orderModify")) {
                        results = ValidationResults.USED_FAIL;
                    }
                } else {
                    if (recordType.contains("initial")) {
                        results = ValidationResults.SAMPLE_NOT_FOUND; // initial entry not used is good
                    } else if (recordType.contains("double")) {
                        results = ValidationResults.REQUIRED_FAIL; // double entry not existing is a
                        // problem
                    } else if (recordType.contains("orderModify")) {
                        results = ValidationResults.SAMPLE_NOT_FOUND; // modify order page
                    }
                }
            }
        }
        return results;
    }

    /**
     * Can the existing accession number be used in the given form? This method is
     * useful when we have an existing accessionNumber and want to ask the question.
     *
     * @param accessionNumber
     * @param existingRequired true => it is required that there is an existing
     *                         studyFormName?
     * @return
     */
    private ValidationResults matchExistingStudyFormName(String accessionNumber, String studyFormName,
            boolean existingRequired) {
        if (GenericValidator.isBlankOrNull(studyFormName)) {
            return SAMPLE_FOUND;
        }
        String existingName = findStudyFormName(accessionNumber);
        if (existingName.equals(studyFormName) || (!existingRequired && GenericValidator.isBlankOrNull(existingName))) {
            return SAMPLE_FOUND;
        }
        return SAMPLE_STATUS_FAIL; // the sample was entered on a different form!
    }

    public static String findStudyFormName(String accessionNumber) {
        StatusSet statusSet = SpringContext.getBean(IStatusService.class)
                .getStatusSetForAccessionNumber(accessionNumber);
        Patient p = new Patient();
        p.setId(statusSet.getPatientId());
        Sample s = new Sample();
        s.setId(statusSet.getSampleId());
        List<ObservationHistory> all = observationHistoryService.getAll(p, s,
                ObservationHistoryTypeMap.getInstance().getIDForType("projectFormName"));
        String existingName = "";
        if (all.size() > 0) {
            existingName = all.get(0).getValue();
        }
        return existingName;
    }

    @Override
    public int getInvarientLength() {
        return PROGRAM_END;
    }

    @Override
    public int getChangeableLength() {
        return getMaxAccessionLength() - getInvarientLength();
    }

    @Override
    public String getPrefix() {
        return null; // no single prefix
    }

    @Override
    public String getNextAccessionNumber(String programCode, boolean reserve) {
        return this.getNextAvailableAccessionNumber(programCode, reserve);
    }
}
