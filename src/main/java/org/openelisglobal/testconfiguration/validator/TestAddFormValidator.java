package org.openelisglobal.testconfiguration.validator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.openelisglobal.common.JSONUtils;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.testconfiguration.form.TestAddForm;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class TestAddFormValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return TestAddForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        TestAddForm form = (TestAddForm) target;

        try {
            JSONObject newTest = JSONUtils.getAsObject(form.getJsonWad());
            if (!JSONUtils.isEmpty(newTest)) {

                ValidationHelper.validateField(StringUtil.nullSafeToString(newTest.get("testNameEnglish")), "JsonWad",
                        "testNameEnglish", errors, true, 255);

                ValidationHelper.validateField(StringUtil.nullSafeToString(newTest.get("testNameFrench")), "JsonWad",
                        "testNameFrench", errors, true, 255);

                ValidationHelper.validateField(StringUtil.nullSafeToString(newTest.get("testReportNameEnglish")),
                        "JsonWad", "testReportNameEnglish", errors, true, 255);

                ValidationHelper.validateField(StringUtil.nullSafeToString(newTest.get("testReportNameFrench")),
                        "JsonWad", "testReportNameFrench", errors, true, 255);

                ValidationHelper.validateIdField(StringUtil.nullSafeToString(newTest.get("testSection")), "JsonWad",
                        "testSection", errors, true);

                JSONArray panels = JSONUtils.getAsArray(newTest.get("panels"));
                for (int i = 0; i < panels.size(); ++i) {
                    JSONObject panel = JSONUtils.getAsObject(panels.get(i));
                    ValidationHelper.validateIdField(StringUtil.nullSafeToString(panel.get("id")), "JsonWad",
                            "panels[" + i + "]", errors, false);
                }

                ValidationHelper.validateIdField(StringUtil.nullSafeToString(newTest.get("uom")), "JsonWad", "uom",
                        errors, false);

                ValidationHelper.validateIdField(StringUtil.nullSafeToString(newTest.get("resultType")), "JsonWad",
                        "resultType", errors, true);

                ValidationHelper.validateYNField(StringUtil.nullSafeToString(newTest.get("antimicrobialResistance")),
                        "JsonWad", "antimicrobialResistance", errors);

                ValidationHelper.validateYNField(StringUtil.nullSafeToString(newTest.get("orderable")), "JsonWad",
                        "orderable", errors);

                ValidationHelper.validateYNField(StringUtil.nullSafeToString(newTest.get("active")), "JsonWad",
                        "active", errors);

                ValidationHelper.validateYNField(StringUtil.nullSafeToString(newTest.get("inLabOnly")), "JsonWad",
                        "inLabOnly", errors);

                JSONArray sampleTypes = JSONUtils.getAsArray(newTest.get("sampleTypes"));
                for (int i = 0; i < sampleTypes.size(); ++i) {
                    JSONObject sampleType = JSONUtils.getAsObject(sampleTypes.get(i));
                    ValidationHelper.validateIdField(StringUtil.nullSafeToString(sampleType.get("typeId")), "JsonWad",
                            errors, false);

                    JSONArray tests = JSONUtils.getAsArray(sampleType.get("tests"));
                    for (int j = 0; j < tests.size(); ++j) {
                        JSONObject test = JSONUtils.getAsObject(tests.get(j));
                        ValidationHelper.validateIdField(StringUtil.nullSafeToString(test.get("id")), "JsonWad", errors,
                                false);
                    }
                }

                if (newTest.containsKey("dictionary")) {
                    JSONArray dictionaries = JSONUtils.getAsArray(newTest.get("dictionary"));

                    for (int i = 0; i < dictionaries.size(); ++i) {
                        JSONObject dictionary = JSONUtils.getAsObject(dictionaries.get(i));

                        ValidationHelper.validateIdField(StringUtil.nullSafeToString(dictionary.get("value")),
                                "JsonWad", "dictionary value", errors, true);

                        ValidationHelper.validateYNField(StringUtil.nullSafeToString(dictionary.get("qualified")),
                                "JsonWad", "dictionary qualified", errors);
                    }
                }

                if (newTest.containsKey("lowValid")) {
                    ValidationHelper.validateField(StringUtil.nullSafeToString(newTest.get("lowValid")), "JsonWad",
                            "lowValid", errors, false, 255, ValidationHelper.FLOAT_REGEX);

                    ValidationHelper.validateField(StringUtil.nullSafeToString(newTest.get("highValid")), "JsonWad",
                            "highValid", errors, false, 255, ValidationHelper.FLOAT_REGEX);

                    ValidationHelper.validateFieldAndCharset(
                            StringUtil.nullSafeToString(newTest.get("significantDigits")), "JsonWad", errors, false, 2,
                            "0-9");

                    JSONArray resultLimits = JSONUtils.getAsArray(newTest.get("resultLimits"));
                    for (int i = 0; i < resultLimits.size(); ++i) {
                        JSONObject resultLimit = JSONUtils.getAsObject(resultLimits.get(i));
                        ValidationHelper.validateField(StringUtil.nullSafeToString(resultLimit.get("highAgeRange")),
                                "JsonWad", "result limit [" + i + "] highAgeRange", errors, false, 255,
                                "^Infinity$|^\\d*$");

                        ValidationHelper.validateTFField((Boolean) resultLimit.get("gender"), "JsonWad",
                                "result limit [" + i + "] gender", errors, true);

                        ValidationHelper.validateField(StringUtil.nullSafeToString(resultLimit.get("lowNormal")),
                                "JsonWad", "result limit [" + i + "] lowNormal", errors, false, 255,
                                ValidationHelper.FLOAT_REGEX);

                        ValidationHelper.validateField(StringUtil.nullSafeToString(resultLimit.get("highNormal")),
                                "JsonWad", "result limit [" + i + "] highNormal", errors, false, 255,
                                ValidationHelper.FLOAT_REGEX);

                        ValidationHelper.validateField(
                                StringUtil.nullSafeToString(resultLimit.get("lowReportingRange")), "JsonWad",
                                "result limit [" + i + "] lowReportingRange", errors, false, 255,
                                ValidationHelper.FLOAT_REGEX);

                        ValidationHelper.validateField(
                                StringUtil.nullSafeToString(resultLimit.get("highReportingRange")), "JsonWad",
                                "result limit [" + i + "] highReportingRange", errors, false, 255,
                                ValidationHelper.FLOAT_REGEX);
                        ValidationHelper.validateField(StringUtil.nullSafeToString(resultLimit.get("lowCritical")),
                                "JsonWad", "result limit [" + i + "] lowCritical", errors, false, 255,
                                ValidationHelper.FLOAT_REGEX);

                        ValidationHelper.validateField(StringUtil.nullSafeToString(resultLimit.get("highCritical")),
                                "JsonWad", "result limit [" + i + "] highCritical", errors, false, 255,
                                ValidationHelper.FLOAT_REGEX);

                        if ((Boolean) resultLimit.get("gender")) {
                            ValidationHelper.validateField(
                                    StringUtil.nullSafeToString(resultLimit.get("lowNormalFemale")), "JsonWad",
                                    "result limit [" + i + "] lowNormalFemale", errors, false, 255,
                                    ValidationHelper.FLOAT_REGEX);

                            ValidationHelper.validateField(
                                    StringUtil.nullSafeToString(resultLimit.get("highNormalFemale")), "JsonWad",
                                    "result limit [" + i + "] highNormalFemale", errors, false, 255,
                                    ValidationHelper.FLOAT_REGEX);
                        }
                    }
                }
            }
        } catch (ParseException e) {
            LogEvent.logError(e);
            errors.rejectValue("jsonWad", "error.field.format.json");
        }
    }
}
