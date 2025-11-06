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

import java.util.HashSet;
import java.util.Set;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.util.AccessionNumberUtil;
import org.openelisglobal.spring.util.SpringContext;

public class YearNumAccessionValidator implements IAccessionNumberGenerator {

    protected SampleService sampleService = SpringContext.getBean(SampleService.class);

    private String incrementStartingValue = "000001";
    private static int upperIncrementValue = 999999;
    private static final int INCREMENT_START = 2;
    private static final int YEAR_START = 0;
    private static final int YEAR_END = 2;
    private int acccessionLength = 8;
    private static final boolean NEED_PROGRAM_CODE = false;
    private static Set<String> REQUESTED_NUMBERS = new HashSet<>();
    private final boolean useSeparator;
    private final String separator;
    private final int separatorLength;
    private String incrementFormat;

    public YearNumAccessionValidator(int length, Character separator) {
        useSeparator = separator != null;
        this.separator = useSeparator ? separator.toString() : "";
        separatorLength = useSeparator ? 1 : 0;
        incrementFormat = "%0" + String.valueOf(length) + "d";
        incrementStartingValue = String.format(incrementFormat, 1);
        String upper = incrementStartingValue.replace("0", "9").replace("1", "9");
        upperIncrementValue = Integer.parseInt(upper);
        acccessionLength = length + YEAR_END + (useSeparator ? 1 : 0);
    }

    @Override
    public boolean needProgramCode() {
        return NEED_PROGRAM_CODE;
    }

    public String createFirstAccessionNumber(String programCode) {
        return DateUtil.getTwoDigitYear() + separator + incrementStartingValue;
    }

    public String incrementAccessionNumber(String currentHighAccessionNumber) {

        int increment = Integer.parseInt(currentHighAccessionNumber.substring(INCREMENT_START + separatorLength));
        String incrementAsString = incrementStartingValue;
        String year = DateUtil.getTwoDigitYear();

        if (year.equals(currentHighAccessionNumber.substring(YEAR_START, YEAR_END))) {
            if (increment < upperIncrementValue) {
                increment++;
                incrementAsString = String.format(incrementFormat, increment);
            } else {
                throw new IllegalArgumentException("AccessionNumber has no next value");
            }
        }

        return year + separator + incrementAsString;
    }

    @Override
    public ValidationResults validFormat(String accessionNumber, boolean checkDate) {
        if (!Boolean
                .valueOf(ConfigurationProperties.getInstance().getPropertyValue(Property.ACCESSION_NUMBER_VALIDATE))) {
            return AccessionNumberUtil.containsBlackListCharacters(accessionNumber) ? ValidationResults.FORMAT_FAIL
                    : ValidationResults.SUCCESS;
        }
        // The rule is 2 digit year code and incremented numbers
        if (accessionNumber.length() != acccessionLength) {
            return ValidationResults.LENGTH_FAIL;
        }

        if (checkDate) {
            if (!DateUtil.getTwoDigitYear().equals(accessionNumber.substring(YEAR_START, YEAR_END))) {
                return ValidationResults.YEAR_FAIL;
            }
        }

        try {
            Integer.parseInt(accessionNumber.substring(INCREMENT_START + separatorLength));
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
            return MessageUtil.getMessage("sample.entry.invalid.accession.number.suggestion") + " "
                    + getNextAvailableAccessionNumber(null, true);
        case YEAR_FAIL:
        case FORMAT_FAIL:
            return getInvalidFormatMessage(results);
        default:
            return MessageUtil.getMessage("sample.entry.invalid.accession.number");
        }
    }

    @Override
    public String getNextAvailableAccessionNumber(String prefix, boolean reserve) {
        String nextAccessionNumber;

        String curLargestAccessionNumber = sampleService.getLargestAccessionNumberWithPrefix(prefix);

        if (curLargestAccessionNumber == null) {
            if (REQUESTED_NUMBERS.isEmpty()) {
                nextAccessionNumber = createFirstAccessionNumber(prefix);
            } else {
                nextAccessionNumber = REQUESTED_NUMBERS.iterator().next();
            }
        } else {
            nextAccessionNumber = incrementAccessionNumber(curLargestAccessionNumber);
        }

        while (REQUESTED_NUMBERS.contains(nextAccessionNumber)) {
            nextAccessionNumber = incrementAccessionNumber(nextAccessionNumber);
        }

        REQUESTED_NUMBERS.add(nextAccessionNumber);

        return nextAccessionNumber;
    }

    @Override
    public int getMaxAccessionLength() {
        return acccessionLength;
    }

    @Override
    public int getMinAccessionLength() {
        return getMaxAccessionLength();
    }

    // recordType parameter is not used in this case
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
    public String getInvalidFormatMessage(ValidationResults results) {
        return MessageUtil.getMessage("sample.entry.invalid.accession.number.format.corrected",
                new String[] { getFormatPattern(), getFormatExample() });
    }

    private String getFormatExample() {
        StringBuilder format = new StringBuilder(DateUtil.getTwoDigitYear());
        if (useSeparator) {
            format.append(separator);
        }
        for (int i = 0; i < getChangeableLength() - 1; i++) {
            format.append("0");
        }

        format.append("1");
        return format.toString();
    }

    private String getFormatPattern() {
        StringBuilder format = new StringBuilder(MessageUtil.getMessage("date.two.digit.year"));
        if (useSeparator) {
            format.append(separator);
        }
        for (int i = 0; i < getChangeableLength(); i++) {
            format.append("#");
        }
        return format.toString();
    }

    @Override
    public int getInvarientLength() {
        return 0;
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
