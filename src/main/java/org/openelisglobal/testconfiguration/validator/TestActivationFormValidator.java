package org.openelisglobal.testconfiguration.validator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.openelisglobal.common.JSONUtils;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.testconfiguration.form.TestActivationForm;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class TestActivationFormValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return TestActivationForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        TestActivationForm form = (TestActivationForm) target;

        try {
            JSONObject changeList = JSONUtils.getAsObject(form.getJsonChangeList());
            if (!JSONUtils.isEmpty(changeList)) {

                JSONArray activateTests = JSONUtils.getAsArray(changeList.get("activateTest"));
                for (int i = 0; i < activateTests.size(); ++i) {
                    JSONObject activateTest = JSONUtils.getAsObject(activateTests.get(i));

                    ValidationHelper.validateIdField(StringUtil.nullSafeToString(activateTest.get("id")),
                            "JsonChangeList", "id", errors, true);

                    ValidationHelper.validateFieldAndCharset(StringUtil.nullSafeToString(activateTest.get("sortOrder")),
                            "JsonChangeList", "sort order[" + i + "]", errors, true, 3, "0-9");

                    ValidationHelper.validateField(StringUtil.nullSafeToString(activateTest.get("activated")),
                            "JsonChangeList", "activated[" + i + "]", errors, true, 5, "^$|^true$|^false$");
                }

                JSONArray deactivateTests = JSONUtils.getAsArray(changeList.get("deactivateTest"));
                for (int i = 0; i < deactivateTests.size(); ++i) {
                    JSONObject deactivateTest = JSONUtils.getAsObject(deactivateTests.get(i));

                    ValidationHelper.validateIdField(StringUtil.nullSafeToString(deactivateTest.get("id")),
                            "JsonChangeList", "id[" + i + "]", errors, true);
                }
            }
        } catch (ParseException e) {
            LogEvent.logError(e);
            errors.rejectValue("jsonChangeList", "error.field.format.json");
        }
    }
}
