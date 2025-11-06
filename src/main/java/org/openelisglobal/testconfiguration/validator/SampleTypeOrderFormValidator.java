package org.openelisglobal.testconfiguration.validator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.openelisglobal.common.JSONUtils;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.testconfiguration.form.SampleTypeOrderForm;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class SampleTypeOrderFormValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return SampleTypeOrderForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        SampleTypeOrderForm form = (SampleTypeOrderForm) target;

        try {
            JSONObject changeList = JSONUtils.getAsObject(form.getJsonChangeList());
            if (!JSONUtils.isEmpty(changeList)) {

                JSONArray sampleTypes = JSONUtils.getAsArray(changeList.get("sampleTypes"));
                for (int i = 0; i < sampleTypes.size(); ++i) {
                    JSONObject sampleType = JSONUtils.getAsObject(sampleTypes.get(i));

                    ValidationHelper.validateIdField(StringUtil.nullSafeToString(sampleType.get("id")),
                            "JsonChangeList", "id[" + i + "]", errors, true);

                    ValidationHelper.validateFieldAndCharset(StringUtil.nullSafeToString(sampleType.get("sortOrder")),
                            "JsonChangeList", "sort order[" + i + "]", errors, true, 3, "0-9");
                }
            }
        } catch (ParseException e) {
            LogEvent.logError(e);
            errors.rejectValue("jsonChangeList", "error.field.format.json");
        }
    }
}
