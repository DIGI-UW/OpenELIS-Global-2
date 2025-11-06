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
package org.openelisglobal.sample.util;

import static org.openelisglobal.common.provider.validation.IAccessionNumberValidator.ValidationResults.PATIENT_STATUS_FAIL;
import static org.openelisglobal.common.provider.validation.IAccessionNumberValidator.ValidationResults.SAMPLE_FOUND;
import static org.openelisglobal.common.provider.validation.IAccessionNumberValidator.ValidationResults.SAMPLE_STATUS_FAIL;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.exception.LIMSInvalidConfigurationException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat;
import org.openelisglobal.common.provider.validation.IAccessionNumberGenerator;
import org.openelisglobal.common.provider.validation.IAccessionNumberValidator;
import org.openelisglobal.common.provider.validation.IAccessionNumberValidator.ValidationResults;
import org.openelisglobal.common.provider.validation.ProgramAccessionValidator;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.RecordStatus;
import org.openelisglobal.common.services.StatusSet;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.spring.util.SpringContext;

public class AccessionNumberUtil {

    private static AccessionNumberValidatorFactory accessionNumberValidatorFactory = SpringContext
            .getBean(AccessionNumberValidatorFactory.class);

    private static String blacklistCharacters = ".*['\"<>\\[\\](){};:/?!@#$%^&+=].*";

    public static boolean containsBlackListCharacters(String value) {
        return value.matches(blacklistCharacters);
    }

    public static IAccessionNumberValidator getGeneralAccessionNumberValidator() {
        try {
            return accessionNumberValidatorFactory.getValidator(AccessionFormat.GENERAL);
        } catch (LIMSInvalidConfigurationException e) {
            LogEvent.logError("AccessionNumberUtil", "getGeneralAccessionNumberValidator", e.toString());
        }

        return null;
    }

    public static IAccessionNumberGenerator getMainAccessionNumberGenerator() {
        try {
            return accessionNumberValidatorFactory.getGenerator(AccessionFormat.MAIN);
        } catch (LIMSInvalidConfigurationException e) {
            LogEvent.logError("AccessionNumberUtil", "getMainAccessionNumberGenerator", e.toString());
        }
        return null;
    }

    public static IAccessionNumberValidator getMainAccessionNumberValidator() {
        try {
            return accessionNumberValidatorFactory.getValidator(AccessionFormat.MAIN);
        } catch (LIMSInvalidConfigurationException e) {
            LogEvent.logError("AccessionNumberUtil", "getMainAccessionNumberValidator", e.toString());
        }
        return null;
    }

    public static IAccessionNumberGenerator getAccessionNumberGenerator(AccessionFormat format) {
        try {
            return accessionNumberValidatorFactory.getGenerator(format);
        } catch (LIMSInvalidConfigurationException e) {
            LogEvent.logError("AccessionNumberUtil", "getAccessionNumberGenerator", e.toString());
        }
        return null;
    }

    public static IAccessionNumberValidator getAccessionNumberValidator(AccessionFormat format) {
        try {
            return accessionNumberValidatorFactory.getValidator(format);
        } catch (LIMSInvalidConfigurationException e) {
            LogEvent.logError("AccessionNumberUtil", "getAccessionNumberValidator", e.toString());
        }
        return null;
    }

    public static IAccessionNumberGenerator getProgramAccessionNumberGenerator() {
        try {
            return accessionNumberValidatorFactory.getGenerator(AccessionFormat.PROGRAMNUM);
        } catch (LIMSInvalidConfigurationException e) {
            LogEvent.logError("AccessionNumberUtil", "getProgramAccessionNumberGenerator", e.toString());
        }
        return null;
    }

    public static IAccessionNumberValidator getProgramAccessionNumberValidator() {
        try {
            return accessionNumberValidatorFactory.getValidator(AccessionFormat.PROGRAMNUM);
        } catch (LIMSInvalidConfigurationException e) {
            LogEvent.logError("AccessionNumberUtil", "getProgramAccessionNumberValidator", e.toString());
        }
        return null;
    }

    public static IAccessionNumberGenerator getAltAccessionNumberGenerator() {
        try {
            return accessionNumberValidatorFactory.getGenerator(AccessionFormat.ALT_YEAR);
        } catch (LIMSInvalidConfigurationException e) {
            LogEvent.logError("AccessionNumberUtil", "getAltAccessionNumberGenerator", e.toString());
        }
        return null;
    }

    public static IAccessionNumberValidator getAltAccessionNumberValidator() {
        try {
            return accessionNumberValidatorFactory.getValidator(AccessionFormat.ALT_YEAR);
        } catch (LIMSInvalidConfigurationException e) {
            LogEvent.logError("AccessionNumberUtil", "getAltAccessionNumberValidator", e.toString());
        }
        return null;
    }

    public static List<IAccessionNumberValidator> getAllActiveAccessionNumberValidators() {
        List<IAccessionNumberValidator> activeValidators = new ArrayList<>();
        activeValidators.add(AccessionNumberUtil.getMainAccessionNumberGenerator());
        activeValidators.add(AccessionNumberUtil.getProgramAccessionNumberValidator());
        if (Boolean
                .valueOf(ConfigurationProperties.getInstance().getPropertyValue(Property.USE_ALT_ACCESSION_PREFIX))) {

            try {
                activeValidators.add(accessionNumberValidatorFactory.getValidator(AccessionFormat.ALT_YEAR));
            } catch (LIMSInvalidConfigurationException e) {
                LogEvent.logError("AccessionNumberUtil", "getAccessionNumberValidator", e.toString());
            }
        }
        return activeValidators;
    }

    public static int getMaxAccessionLength() {
        return getGeneralAccessionNumberValidator().getMaxAccessionLength();
    }

    public static int getMinAccessionLength() {
        return getGeneralAccessionNumberValidator().getMinAccessionLength();
    }

    public static String getInvalidMessage(ValidationResults result) {
        return getGeneralAccessionNumberValidator().getInvalidMessage(result);
    }

    public static String getInvalidFormatMessage(ValidationResults result) {
        return getGeneralAccessionNumberValidator().getInvalidFormatMessage(result);
    }

    public static ValidationResults checkAccessionNumberValidity(String accessionNumber, String recordType,
            String isRequired, String projectFormName) {
        return getGeneralAccessionNumberValidator().checkAccessionNumberValidity(accessionNumber, recordType,
                isRequired, projectFormName);
    }

    public static ValidationResults isPatientStatusValid(String accessionNumber, RecordStatus validStatus) {
        StatusSet statusSet = SpringContext.getBean(IStatusService.class)
                .getStatusSetForAccessionNumber(accessionNumber);
        if (statusSet.getPatientRecordStatus() == validStatus) {
            return SAMPLE_FOUND;
        } else {
            return PATIENT_STATUS_FAIL;
        }
    }

    public static ValidationResults isSampleStatusValid(String accessionNumber, RecordStatus validStatus) {
        StatusSet statusSet = SpringContext.getBean(IStatusService.class)
                .getStatusSetForAccessionNumber(accessionNumber);
        RecordStatus sampleRecordStatus = statusSet.getSampleRecordStatus();
        if (sampleRecordStatus == validStatus) {
            return SAMPLE_FOUND;
        } else {
            return SAMPLE_STATUS_FAIL;
        }
    }

    public static int getInvarientLength() {
        return getMainAccessionNumberGenerator().getInvarientLength();
    }

    public static int getChangeableLength() {
        return getMainAccessionNumberGenerator().getChangeableLength();
    }

    public static int getMaxLength() {
        return getGeneralAccessionNumberValidator().getMaxAccessionLength();
    }

    public static ValidationResults correctFormat(String accessionNumber, boolean validateYear) {
        return getGeneralAccessionNumberValidator().validFormat(accessionNumber, validateYear);
    }

    public static boolean isUsed(String accessionNumber) {
        return getGeneralAccessionNumberValidator().accessionNumberIsUsed(accessionNumber, null);
    }

    public static String getAccessionNumberFromSampleItemAccessionNumber(String accessionNumber) {
        int lastDash = accessionNumber.lastIndexOf('-');
        return accessionNumber.substring(0, lastDash);
    }

    public static boolean isProjectAccessionNumber(String accessionNumber) {
        if (StringUtil.isNullorNill(ProgramAccessionValidator.findStudyFormName(accessionNumber))) {
            return false;
        }
        return true;
    }
}
