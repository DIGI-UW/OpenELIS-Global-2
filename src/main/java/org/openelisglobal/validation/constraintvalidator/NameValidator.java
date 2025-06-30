package org.openelisglobal.validation.constraintvalidator;

import jakarta.annotation.PostConstruct;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.openelisglobal.common.util.ConfigurationListener;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.validation.annotations.ValidName;
import org.springframework.stereotype.Component;

@Component
public class NameValidator implements ConstraintValidator<ValidName, String>, ConfigurationListener {
    public enum NameType {
        USERNAME, FIRST_NAME, LAST_NAME, FULL_NAME
    }

    private static final SiteInformation DEFAULT_SITE_INFORATION = new SiteInformation();
    private static final String DEFAULT_REGEX = "0-9a-z .'_@-";
    private static String FIRST_NAME_REGEX;
    private static String LAST_NAME_REGEX;
    private static String USER_NAME_REGEX;
    private static String FULL_NAME_REGEX;
    private NameType nameType;

    SiteInformationService siteInformationService = SpringContext.getBean(SiteInformationService.class);

    private String escapeRegexChars(String regex) {
        // TODO Auto-generated method stub
        return regex;
    }

    @PostConstruct
    public void initSpring() {
        refreshConfiguration();
    }

    @Override
    public void initialize(ValidName constraint) {
        nameType = constraint.nameType();
        DEFAULT_SITE_INFORATION.setValue(DEFAULT_REGEX);
    }

    private String getRegex(NameType nameType) {
        switch (nameType) {
        case FIRST_NAME:
            return FIRST_NAME_REGEX;
        case LAST_NAME:
            return LAST_NAME_REGEX;
        case FULL_NAME:
            return FULL_NAME_REGEX;
        case USERNAME:
            return USER_NAME_REGEX;
        default:
            return null;
        }
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (org.apache.commons.validator.GenericValidator.isBlankOrNull(value)) {
            return true;
        }
        switch (nameType) {
        case LAST_NAME:
            return "UNKNOWN_".equals(value) || value.matches(getRegex(nameType));
        case FIRST_NAME:
        case FULL_NAME:
        case USERNAME:
            return value.matches(getRegex(nameType));
        }
        return false;
    }

    @Override
    public void refreshConfiguration() {

        FIRST_NAME_REGEX = "(?iu)^[" + escapeRegexChars(
                siteInformationService.getMatch("name", "firstNameCharset").orElse(DEFAULT_SITE_INFORATION).getValue())
                + "]*$";
        LAST_NAME_REGEX = "(?iu)^[" + escapeRegexChars(
                siteInformationService.getMatch("name", "lastNameCharset").orElse(DEFAULT_SITE_INFORATION).getValue())
                + "]*$";
        FULL_NAME_REGEX = "(?iu)^["
                + escapeRegexChars(siteInformationService.getMatch("name", "firstNameCharset")
                        .orElse(new SiteInformation()).getValue())
                + "]*([ ]*[" + escapeRegexChars(siteInformationService.getMatch("name", "lastNameCharset")
                        .orElse(DEFAULT_SITE_INFORATION).getValue())
                + "])?$";
        USER_NAME_REGEX = "(?iu)^[" + escapeRegexChars(
                siteInformationService.getMatch("name", "userNameCharset").orElse(DEFAULT_SITE_INFORATION).getValue())
                + "]*$";
    }
}
