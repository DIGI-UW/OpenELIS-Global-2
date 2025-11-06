package org.openelisglobal.testconfiguration.validator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.openelisglobal.common.JSONUtils;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.testconfiguration.form.TestSectionOrderForm;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class TestSectionOrderFormValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return TestSectionOrderForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        TestSectionOrderForm form = (TestSectionOrderForm) target;

        try {
            JSONObject changeList = JSONUtils.getAsObject(form.getJsonChangeList());
            if (!JSONUtils.isEmpty(changeList)) {

                JSONArray testSections = JSONUtils.getAsArray(changeList.get("testSections"));
                for (int i = 0; i < testSections.size(); ++i) {
                    JSONObject testSection = JSONUtils.getAsObject(testSections.get(i));

                    ValidationHelper.validateIdField(StringUtil.nullSafeToString(testSection.get("id")),
                            "JsonChangeList", "testSection[" + i + "] id", errors, true);

                    ValidationHelper.validateFieldAndCharset(StringUtil.nullSafeToString(testSection.get("sortOrder")),
                            "JsonChangeList", "test section[" + i + "] sort order", errors, true, 3, "0-9");
                }
            }
        } catch (ParseException e) {
            LogEvent.logError(e);
            errors.rejectValue("jsonChangeList", "error.field.format.json");
        }
    }
}
