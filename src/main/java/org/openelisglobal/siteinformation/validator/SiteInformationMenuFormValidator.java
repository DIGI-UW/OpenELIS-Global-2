package org.openelisglobal.siteinformation.validator;

import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.siteinformation.form.SiteInformationMenuForm;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class SiteInformationMenuFormValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return SiteInformationMenuForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        SiteInformationMenuForm form = (SiteInformationMenuForm) target;

        ValidationHelper.validateOptionField(form.getSiteInfoDomainName(), "siteInfoDomainName", errors,
                new String[] { "non_conformityConfiguration", "WorkplanConfiguration", "PrintedReportsConfiguration",
                        "sampleEntryConfig", "ResultConfiguration", "MenuStatementConfig", "PaitientConfiguration",
                        "SiteInformation" });
    }
}
