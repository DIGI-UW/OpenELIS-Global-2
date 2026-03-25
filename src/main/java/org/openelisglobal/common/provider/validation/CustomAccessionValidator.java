package org.openelisglobal.common.provider.validation;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.spring.util.SpringContext;

public class CustomAccessionValidator implements IAccessionNumberValidator {

    private static final int MAX_REGEX_LENGTH = 200;
    private static final int MAX_ACCESSSION_LENGTH = 50;
    private static final int MIN_ACCESSSION_LENGTH = 1;

    private static final String DANGEROUS_REGEX_PATTERN = ".*(\\(\\?<?[=!]?|\\*|\\+|\\{[0-9]+,\\}|\\(\\?[midslspu]*\\)).*";

    private static Pattern cachedPattern = null;
    private static String cachedRegex = null;

    protected SampleService sampleService = SpringContext.getBean(SampleService.class);

    @Override
    public boolean needProgramCode() {
        return false;
    }

    @Override
    public ValidationResults validFormat(String accessionNumber, boolean checkDate) throws IllegalArgumentException {
        if (accessionNumber == null) {
            return ValidationResults.REQUIRED_FAIL;
        }

        if (accessionNumber.length() > getMaxAccessionLength()) {
            return ValidationResults.LENGTH_FAIL;
        }
        if (accessionNumber.length() < getMinAccessionLength()) {
            return ValidationResults.LENGTH_FAIL;
        }

        String regex = ConfigurationProperties.getInstance().getPropertyValue(Property.CUSTOM_ACCESSION_REGEX);
        if (StringUtils.isBlank(regex)) {
            return ValidationResults.FORMAT_FAIL;
        }

        try {
            Pattern pattern = getCompiledPattern(regex);
            if (pattern.matcher(accessionNumber).matches()) {
                return ValidationResults.SUCCESS;
            } else {
                return ValidationResults.FORMAT_FAIL;
            }
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "validFormat", "Invalid regex pattern: " + e.getMessage());
            return ValidationResults.FORMAT_FAIL;
        }
    }

    private Pattern getCompiledPattern(String regex) {
        if (regex.equals(cachedRegex) && cachedPattern != null) {
            return cachedPattern;
        }

        cachedRegex = regex;
        cachedPattern = Pattern.compile(regex);
        return cachedPattern;
    }

    public static boolean isValidRegexPattern(String regex) {
        if (StringUtils.isBlank(regex)) {
            return false;
        }

        if (regex.length() > MAX_REGEX_LENGTH) {
            return false;
        }

        try {
            Pattern.compile(regex);
            return true;
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

    public static boolean isComplexRegex(String regex) {
        return regex != null && regex.matches(DANGEROUS_REGEX_PATTERN);
    }

    @Override
    public String getInvalidMessage(ValidationResults results) {
        return MessageUtil.getMessage("sample.entry.invalid.accession.number.format");
    }

    @Override
    public String getInvalidFormatMessage(ValidationResults results) {
        String regex = ConfigurationProperties.getInstance().getPropertyValue(Property.CUSTOM_ACCESSION_REGEX);
        return MessageUtil.getMessage("sample.entry.invalid.accession.number.format.custom", new String[] { regex });
    }

    @Override
    public int getMaxAccessionLength() {
        return MAX_ACCESSSION_LENGTH;
    }

    @Override
    public int getMinAccessionLength() {
        return MIN_ACCESSSION_LENGTH;
    }

    @Override
    public boolean accessionNumberIsUsed(String accessionNumber, String recordType) {
        return sampleService.getSampleByAccessionNumber(accessionNumber) != null;
    }

    @Override
    public ValidationResults checkAccessionNumberValidity(String accessionNumber, String recordType, String isRequired,
            String projectFormName) {
        ValidationResults results = validFormat(accessionNumber, true);
        if (results == ValidationResults.SUCCESS && accessionNumberIsUsed(accessionNumber, null)) {
            results = ValidationResults.USED_FAIL;
        }
        return results;
    }

    @Override
    public int getInvarientLength() {
        return 0;
    }

    @Override
    public int getChangeableLength() {
        return MAX_ACCESSSION_LENGTH;
    }

    @Override
    public String getPrefix() {
        return "";
    }
}