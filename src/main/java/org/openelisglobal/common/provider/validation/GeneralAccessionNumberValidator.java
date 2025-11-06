package org.openelisglobal.common.provider.validation;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.exception.LIMSInvalidConfigurationException;
import org.openelisglobal.sample.util.AccessionNumberUtil;

public class GeneralAccessionNumberValidator implements IAccessionNumberValidator {

    private List<IAccessionNumberValidator> activeValidators;

    public GeneralAccessionNumberValidator() throws LIMSInvalidConfigurationException {
        activeValidators = AccessionNumberUtil.getAllActiveAccessionNumberValidators();
    }

    @Override
    public boolean needProgramCode() {
        for (IAccessionNumberValidator activeValidator : activeValidators) {
            if (!activeValidator.needProgramCode()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ValidationResults validFormat(String accessionNumber, boolean checkDate) throws IllegalArgumentException {
        if (activeValidators.size() == 1) {
            return activeValidators.get(0).validFormat(accessionNumber, checkDate);
        }
        for (IAccessionNumberValidator activeValidator : activeValidators) {
            if (ValidationResults.SUCCESS.equals(activeValidator.validFormat(accessionNumber, checkDate))) {
                return ValidationResults.SUCCESS;
            }
        }
        return ValidationResults.GENERIC_FAIL;
    }

    @Override
    public String getInvalidMessage(ValidationResults results) {
        StringBuilder totalMessage = new StringBuilder();
        for (IAccessionNumberValidator activeValidator : activeValidators) {
            String message = activeValidator.getInvalidMessage(results);
            if (!GenericValidator.isBlankOrNull(message)) {
                totalMessage.append(message);
                totalMessage.append("\n  \n");
            }
        }

        return totalMessage.toString();
    }

    @Override
    public String getInvalidFormatMessage(ValidationResults results) {
        StringBuilder totalMessage = new StringBuilder();
        for (IAccessionNumberValidator activeValidator : activeValidators) {
            String message = activeValidator.getInvalidFormatMessage(results);
            if (!GenericValidator.isBlankOrNull(message)) {
                totalMessage.append(message);
                totalMessage.append("\n  \n");
            }
        }

        return totalMessage.toString();
    }

    @Override
    public int getMaxAccessionLength() {
        return Collections
                .max(activeValidators.stream().map(e -> e.getMaxAccessionLength()).collect(Collectors.toList()));
    }

    @Override
    public int getMinAccessionLength() {
        return Collections
                .min(activeValidators.stream().map(e -> e.getMinAccessionLength()).collect(Collectors.toList()));
    }

    @Override
    public boolean accessionNumberIsUsed(String accessionNumber, String recordType) {
        for (IAccessionNumberValidator activeValidator : activeValidators) {
            if (activeValidator.accessionNumberIsUsed(accessionNumber, recordType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ValidationResults checkAccessionNumberValidity(String accessionNumber, String recordType, String isRequired,
            String projectFormName) {
        if (activeValidators.size() == 1) {
            return activeValidators.get(0).checkAccessionNumberValidity(accessionNumber, recordType, isRequired,
                    projectFormName);
        }
        for (IAccessionNumberValidator activeValidator : activeValidators) {
            if (ValidationResults.SUCCESS.equals(activeValidator.checkAccessionNumberValidity(accessionNumber,
                    recordType, isRequired, projectFormName))) {
                return ValidationResults.SUCCESS;
            }
        }
        return ValidationResults.GENERIC_FAIL;
    }

    @Override
    public int getInvarientLength() {
        if (activeValidators.size() == 1) {
            return activeValidators.get(0).getInvarientLength();
        }
        throw new UnsupportedOperationException(
                "GeneralAccessionNumberValidator cannot give an invariant length when multiple validators"
                        + " are active");
    }

    @Override
    public int getChangeableLength() {
        if (activeValidators.size() == 1) {
            return activeValidators.get(0).getChangeableLength();
        }
        throw new UnsupportedOperationException(
                "GeneralAccessionNumberValidator annot give a changeable length when multiple validators"
                        + " are active");
    }

    @Override
    public String getPrefix() {
        if (activeValidators.size() == 1) {
            return activeValidators.get(0).getPrefix();
        }
        throw new UnsupportedOperationException(
                "GeneralAccessionNumberValidator cannot give a prefix when multiple validators are active");
    }
}
