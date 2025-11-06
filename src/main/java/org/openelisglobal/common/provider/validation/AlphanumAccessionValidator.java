package org.openelisglobal.common.provider.validation;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat;
import org.openelisglobal.common.service.AccessionService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.IntegerUtil;
import org.openelisglobal.common.util.validator.GenericValidator;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.util.AccessionNumberUtil;
import org.openelisglobal.spring.util.SpringContext;

public class AlphanumAccessionValidator implements IAccessionNumberGenerator {

    protected SampleService sampleService = SpringContext.getBean(SampleService.class);
    protected AccessionService accessionService = SpringContext.getBean(AccessionService.class);

    private String startingAt;
    private Set<String> localReservedNumbers = new HashSet<>();

    @Override
    public boolean needProgramCode() {
        return false;
    }

    @Override
    public int getInvarientLength() {
        return getSiteEndIndex();
    }

    @Override
    public String getPrefix() {
        return ConfigurationProperties.getInstance().getPropertyValue(Property.ALPHANUM_ACCESSION_PREFIX) == null ? ""
                : ConfigurationProperties.getInstance().getPropertyValue(Property.ALPHANUM_ACCESSION_PREFIX);
    }

    protected int getIncrementStartIndex() {
        return getSiteEndIndex();
    }

    protected int getYearEndIndex() {
        return 2;
    }

    protected int getYearStartIndex() {
        return 0;
    }

    protected int getSiteEndIndex() {
        return getSiteStartIndex() + getPrefix().length();
    }

    protected int getSiteStartIndex() {
        return getYearEndIndex();
    }

    @Override
    public int getMaxAccessionLength() {
        return getSiteEndIndex() + getChangeableLength();
    }

    @Override
    public int getMinAccessionLength() {
        return getMaxAccessionLength();
    }

    @Override
    public int getChangeableLength() {
        return 6;
    }

    public String getOverrideStartingAt() {
        return startingAt;
    }

    public void setOverrideStartingAt(String startingAt) {
        this.startingAt = startingAt;
    }

    @Override
    public ValidationResults validFormat(String accessionNumber, boolean checkDate) throws IllegalArgumentException {
        if (!Boolean
                .valueOf(ConfigurationProperties.getInstance().getPropertyValue(Property.ACCESSION_NUMBER_VALIDATE))) {
            return AccessionNumberUtil.containsBlackListCharacters(accessionNumber) ? ValidationResults.FORMAT_FAIL
                    : ValidationResults.SUCCESS;
        }
        if (accessionNumber.length() > getMaxAccessionLength()) {
            return ValidationResults.LENGTH_FAIL;
        }
        if (accessionNumber.length() < getMinAccessionLength()) {
            return ValidationResults.LENGTH_FAIL;
        }

        if (!accessionNumber.substring(getSiteStartIndex(), getSiteEndIndex()).equals(getPrefix())) {
            return ValidationResults.SITE_FAIL;
        }

        if (checkDate) {
            int year = new GregorianCalendar().get(Calendar.YEAR);
            try {
                if ((year - 2000) != Integer
                        .parseInt(accessionNumber.substring(getYearStartIndex(), getYearEndIndex()))) {
                    return ValidationResults.YEAR_FAIL;
                }
            } catch (NumberFormatException e) {
                return ValidationResults.YEAR_FAIL;
            }
        } else {
            try { // quick and dirty to make sure they are digits
                Integer.parseInt(accessionNumber.substring(getYearStartIndex(), getYearEndIndex()));
            } catch (NumberFormatException e) {
                return ValidationResults.YEAR_FAIL;
            }
        }

        try {
            IntegerUtil.parseIntBase27(accessionNumber.substring(getIncrementStartIndex()));
        } catch (NumberFormatException e) {
            return ValidationResults.FORMAT_FAIL;
        }

        return ValidationResults.SUCCESS;
    }

    @Override
    public String getInvalidFormatMessage(ValidationResults results) {
        return MessageUtil.getMessage("sample.entry.invalid.accession.number.format.corrected",
                new String[] { getFormatPattern(), getFormatExample() });
    }

    private String getFormatPattern() {
        StringBuilder format = new StringBuilder();
        format.append(MessageUtil.getMessage("date.two.digit.year")).append(getPrefix())
                .append(StringUtils.rightPad("", getChangeableLength(), "#"));
        return format.toString();
    }

    private String getFormatExample() {
        StringBuilder format = new StringBuilder();
        format.append(MessageUtil.getMessage("date.two.digit.year")).append(getPrefix())
                .append(StringUtils.rightPad("", getChangeableLength(), "1"));
        return format.toString();
    }

    @Override
    public boolean accessionNumberIsUsed(String accessionNumber, String recordType) {
        return sampleService.getSampleByAccessionNumber(accessionNumber) != null;
    }

    @Override
    public ValidationResults checkAccessionNumberValidity(String accessionNumber, String recordType, String isRequired,
            String projectFormName) {
        ValidationResults results = validFormat(accessionNumber, true);
        // TODO refactor accessionNumberIsUsed into two methods so the null isn't
        // needed. (Its only used for program accession number)
        if (results == ValidationResults.SUCCESS && accessionNumberIsUsed(accessionNumber, null)) {
            results = ValidationResults.USED_FAIL;
        }

        return results;
    }

    @Override
    public String getNextAvailableAccessionNumber(String programCode, boolean reserve) {
        String nextAccessionNumber;
        do {
            if (reserve) {
                nextAccessionNumber = incrementAccessionNumber();
            } else {
                nextAccessionNumber = incrementAccessionNumberNoReserve();
            }
        } while (accessionNumberIsUsed(nextAccessionNumber, null));
        return nextAccessionNumber;
    }

    @Override
    public String getNextAccessionNumber(String programCode, boolean reserve) {
        if (!reserve) {
            LogEvent.logWarn(BaseSiteYearAccessionValidator.class.getName(), "getNextAvailableAccessionNumber",
                    "this generator always reserves");
        }
        return incrementAccessionNumber();
    }

    @Override
    public String getInvalidMessage(ValidationResults results) {
        String suggestedAccessionNumber = getNextAvailableAccessionNumber(null, false);

        return MessageUtil.getMessage("sample.entry.invalid.accession.number.suggestion") + " "
                + suggestedAccessionNumber;
    }

    private String incrementAccessionNumber() throws IllegalArgumentException {
        String year = DateUtil.getTwoDigitYear();
        if (GenericValidator.isBlankOrNull(startingAt)) {
            long nextNum = accessionService.getNextNumberIncrement(this.getPrefix() + year, AccessionFormat.ALT_YEAR);

            if (nextNum <= 2147483647L) {
                String incrementAsString = StringUtils.leftPad(IntegerUtil.toStringBase27((int) nextNum), 6, "0");
                StringBuilder accessionBuilder = new StringBuilder();
                accessionBuilder.append(year).append(getPrefix()).append(incrementAsString);
                return accessionBuilder.toString();
            } else {
                throw new IllegalArgumentException("AccessionNumber has no next value - too large for int");
            }
        } else {
            String nextAccessionNumber = startingAt;
            while (localReservedNumbers.contains(nextAccessionNumber)) {
                nextAccessionNumber = incrementAccessionNumber(nextAccessionNumber);
            }
            localReservedNumbers.add(nextAccessionNumber);

            long increment = Long.parseLong(nextAccessionNumber.substring(getIncrementStartIndex()));
            long dbIncrement = accessionService.getNextNumberNoIncrement(year + getPrefix(), AccessionFormat.ALPHANUM);
            if (dbIncrement <= increment) {
                accessionService.setCurVal(year + getPrefix(), AccessionFormat.ALPHANUM, increment);
            }

            return nextAccessionNumber;
        }
    }

    private String incrementAccessionNumberNoReserve() throws IllegalArgumentException {
        String year = DateUtil.getTwoDigitYear();
        long nextNum = accessionService.getNextNumberNoIncrement(this.getPrefix() + year, AccessionFormat.ALT_YEAR);
        if (nextNum <= 2147483647L) {
            String incrementAsString = StringUtils.leftPad(IntegerUtil.toStringBase27((int) nextNum), 6, "0");
            StringBuilder accessionBuilder = new StringBuilder();
            accessionBuilder.append(year).append(getPrefix()).append(incrementAsString);
            return accessionBuilder.toString();
        } else {
            throw new IllegalArgumentException("AccessionNumber has no next value - too large for int");
        }
    }

    private String incrementAccessionNumber(String currentHighAccessionNumber) throws IllegalArgumentException {
        long increment = Long.parseLong(currentHighAccessionNumber.substring(getIncrementStartIndex()));
        String incrementAsString;

        if (increment <= 2147483647L) {
            incrementAsString = StringUtils.leftPad(IntegerUtil.toStringBase27((int) (++increment)), 6, "0");
            return currentHighAccessionNumber.substring(0, getIncrementStartIndex()) + incrementAsString;
        } else {
            throw new IllegalArgumentException("AccessionNumber has no next value - too large for int");
        }
    }

    public static String normalizeAccessionString(String humanReadableAccessionNumber) {
        if (GenericValidator.isBlankOrNull(humanReadableAccessionNumber)) {
            return humanReadableAccessionNumber;
        }
        return humanReadableAccessionNumber.replace("-", "");
    }

    // turns format from YYPrefix6AlphaNum to YY-Prefix-3AlphaNum-3AlphaNum
    // or YY6AlphaNum to YY-3AlphaNum-3AlphaNum
    public static String convertAlphaNumLabNumForDisplay(String accessionNumber) {
        if (GenericValidator.isBlankOrNull(accessionNumber)) {
            return accessionNumber;
        }
        StringBuilder builder = new StringBuilder();
        builder.append(accessionNumber.substring(0, 2)).append("-");
        if (accessionNumber.length() > 8) {
            builder.append(accessionNumber.substring(2, accessionNumber.length() - 6).toUpperCase()).append("-");
        }
        builder.append(accessionNumber.substring(accessionNumber.length() - 6, accessionNumber.length() - 3))
                .append("-");
        builder.append(accessionNumber.substring(accessionNumber.length() - 3));
        return builder.toString();
    }
}
