package org.openelisglobal.dictionary.validator;

import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.dictionary.form.DictionaryForm;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class DictionaryFormValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return DictionaryForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        DictionaryForm form = (DictionaryForm) target;

        String[] dirtyFields = form.getDirtyFormFields()
                .split(ConfigurationProperties.getInstance().getPropertyValue("default.idSeparator"), -1);
        for (String dirtyField : dirtyFields) {
            ValidationHelper.validateFieldAndCharset(dirtyField, "dirtyFormField", errors, false, 255, "a-zA-Z0-9_");
            if (errors.hasErrors()) {
                break;
            }
        }
    }
}
