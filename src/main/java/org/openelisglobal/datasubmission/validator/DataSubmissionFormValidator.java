package org.openelisglobal.datasubmission.validator;

import java.util.Calendar;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.datasubmission.form.DataSubmissionForm;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class DataSubmissionFormValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return DataSubmissionForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        DataSubmissionForm form = (DataSubmissionForm) target;

        ValidationHelper.validateFieldMinMax(form.getYear(), "year", errors,
                Calendar.getInstance().get(Calendar.YEAR) - 25, Calendar.getInstance().get(Calendar.YEAR));

        ValidationHelper.validateOptionField(form.getSiteId(), "siteId", errors,
                new String[] { ConfigurationProperties.getInstance().getPropertyValue(Property.SiteCode) });
    }
}
