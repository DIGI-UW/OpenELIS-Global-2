package org.openelisglobal.qaevent.validator;

import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.qaevent.form.NonConformityForm;
import org.openelisglobal.qaevent.valueholder.retroCI.QaEventItem;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class NonConformityFormValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return NonConformityForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        NonConformityForm form = (NonConformityForm) target;

        // sampleItemsTypeOfSampleIds
        String[] sampleItemsTypeOfSampleIds = form.getSampleItemsTypeOfSampleIds().split(",", -1);
        for (String sampleItemsTypeOfSampleId : sampleItemsTypeOfSampleIds) {
            ValidationHelper.validateIdField(sampleItemsTypeOfSampleId, "sampleItemsTypeOfSampleIds", errors, false);
            if (errors.hasErrors()) {
                break;
            }
        }

        for (int i = 0; i < form.getQaEvents().size(); ++i) {
            QaEventItem qaEvent = form.getQaEvents().get(i);
            if (!GenericValidator.isBlankOrNull(qaEvent.getId())) {
                ValidationHelper.validateFieldRequired(qaEvent.getQaEvent(), "qaEvents[" + i + "].qaEvent", errors);
            }
        }
    }
}
