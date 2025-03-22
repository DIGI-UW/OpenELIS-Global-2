package org.openelisglobal.testconfiguration.validator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.openelisglobal.common.JSONUtils;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.testconfiguration.form.TestOrderabilityForm;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
@JsonIgnoreProperties(ignoreUnknown = true)
public class TestOrderabilityFormValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return TestOrderabilityForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        TestOrderabilityForm form = (TestOrderabilityForm) target;

        try {
            JSONObject changeList = JSONUtils.getAsObject(form.getJsonChangeList());
            if (!JSONUtils.isEmpty(changeList)) {

                JSONArray activateTests = JSONUtils.getAsArray(changeList.get("activateTest"));
                for (int i = 0; i < activateTests.size(); ++i) {
                    JSONObject activateTest = JSONUtils.getAsObject(activateTests.get(i));

                    ValidationHelper.validateIdField(StringUtil.nullSafeToString(activateTest.get("id")),
                            "JsonChangeList", "activateTests[" + i + "] id", errors, true);
                }

                JSONArray deactivateTests = JSONUtils.getAsArray(changeList.get("deactivateTest"));
                for (int i = 0; i < deactivateTests.size(); ++i) {
                    JSONObject deactivateTest = JSONUtils.getAsObject(deactivateTests.get(i));

                    ValidationHelper.validateIdField(String.valueOf(deactivateTest.get("id")), "JsonChangeList",
                            "deactivateTests[" + i + "] id", errors, true);
                }
            }
        } catch (ParseException e) {
            LogEvent.logError(e);
            errors.rejectValue("jsonChangeList", "error.field.format.json");
        }
    }
}
