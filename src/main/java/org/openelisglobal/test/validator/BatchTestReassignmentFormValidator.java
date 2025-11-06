package org.openelisglobal.test.validator;

import org.apache.commons.lang3.ObjectUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.openelisglobal.common.JSONUtils;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.test.form.BatchTestReassignmentForm;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class BatchTestReassignmentFormValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return BatchTestReassignmentForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        BatchTestReassignmentForm form = (BatchTestReassignmentForm) target;

        try {
            JSONObject batchTest = JSONUtils.getAsObject(form.getJsonWad());
            if (!JSONUtils.isEmpty(batchTest)) {

                // done by test name, not id
                // ValidationHelper.validateIdField(StringUtil.nullSafeToString(batchTest.get("current")),
                // "JsonWad", "current", errors,
                // true);

                // done by sampleType name, not id
                // ValidationHelper.validateIdField(StringUtil.nullSafeToString(batchTest.get("sampleType")),
                // "JsonWad", "sampleType", errors,
                // true);

                JSONArray replacements = JSONUtils.getAsArray(batchTest.get("replace"));
                if (ObjectUtils.isNotEmpty(replacements)) {
                    for (int i = 0; i < replacements.size(); ++i) {
                        ValidationHelper.validateIdField(StringUtil.nullSafeToString(replacements.get(i)), "JsonWad",
                                "replace[" + i + "]", errors, true);
                    }
                }

                JSONArray changesNotStarted = JSONUtils.getAsArray(batchTest.get("changeNotStarted"));
                for (int i = 0; i < changesNotStarted.size(); ++i) {
                    ValidationHelper.validateIdField(StringUtil.nullSafeToString(changesNotStarted.get(i)), "JsonWad",
                            "changeNotStarted[" + i + "]", errors, true);
                }

                JSONArray noChangesNotStarted = JSONUtils.getAsArray(batchTest.get("noChangeNotStarted"));
                for (int i = 0; i < noChangesNotStarted.size(); ++i) {
                    ValidationHelper.validateIdField(StringUtil.nullSafeToString(noChangesNotStarted.get(i)), "JsonWad",
                            "noChangeNotStarted[" + i + "]", errors, true);
                }

                JSONArray changesTechReject = JSONUtils.getAsArray(batchTest.get("changeTechReject"));
                for (int i = 0; i < changesTechReject.size(); ++i) {
                    ValidationHelper.validateIdField(StringUtil.nullSafeToString(changesTechReject.get(i)), "JsonWad",
                            "changeTechReject[" + i + "]", errors, true);
                }

                JSONArray noChangesTechReject = JSONUtils.getAsArray(batchTest.get("noChangeTechReject"));
                for (int i = 0; i < noChangesTechReject.size(); ++i) {
                    ValidationHelper.validateIdField(StringUtil.nullSafeToString(noChangesTechReject.get(i)), "JsonWad",
                            "noChangeTechReject[" + i + "]", errors, true);
                }

                JSONArray changesBioReject = JSONUtils.getAsArray(batchTest.get("changeBioReject"));
                for (int i = 0; i < changesBioReject.size(); ++i) {
                    ValidationHelper.validateIdField(StringUtil.nullSafeToString(changesBioReject.get(i)), "JsonWad",
                            "changeBioReject[" + i + "]", errors, true);
                }

                JSONArray noChangesBioReject = JSONUtils.getAsArray(batchTest.get("noChangeBioReject"));
                for (int i = 0; i < noChangesBioReject.size(); ++i) {
                    ValidationHelper.validateIdField(StringUtil.nullSafeToString(noChangesBioReject.get(i)), "JsonWad",
                            "noChangeBioReject[" + i + "]", errors, true);
                }

                JSONArray changesNotValidated = JSONUtils.getAsArray(batchTest.get("changeNotValidated"));
                for (int i = 0; i < changesNotValidated.size(); ++i) {
                    ValidationHelper.validateIdField(StringUtil.nullSafeToString(changesNotValidated.get(i)), "JsonWad",
                            "changeNotValidated[" + i + "]", errors, true);
                }

                JSONArray noChangesNotValidated = JSONUtils.getAsArray(batchTest.get("noChangeNotValidated"));
                for (int i = 0; i < noChangesNotValidated.size(); ++i) {
                    ValidationHelper.validateIdField(StringUtil.nullSafeToString(noChangesNotValidated.get(i)),
                            "JsonWad", "noChangeNotValidated[" + i + "]", errors, true);
                }
            }

        } catch (ParseException e) {
            errors.rejectValue("jsonWad", "error.field.json.format.invalid");
        }
    }
}
