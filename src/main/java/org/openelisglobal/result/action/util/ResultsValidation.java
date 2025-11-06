package org.openelisglobal.result.action.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.formfields.FormFields;
import org.openelisglobal.common.formfields.FormFields.Field;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.common.util.validator.CustomDateValidator;
import org.openelisglobal.common.util.validator.CustomDateValidator.DateRelation;
import org.openelisglobal.common.validator.BaseErrors;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;

@Service
public class ResultsValidation {

    private static final String SPECIAL_CASE = "XXXX";

    @Autowired
    private ResultService resultService;
    @Autowired
    private AnalysisService analysisService;

    public Errors validateItem(TestResultItem item) {
        Errors errors = new BaseErrors();

        validateTestDate(item, errors);

        if (!item.isRejected()) {
            validateResult(item, errors);
        }

        if (ConfigurationProperties.getInstance().isPropertyValueEqual(Property.notesRequiredForModifyResults, "true")
                && !item.isRejected()) {
            validateRequiredNote(item, errors);
        }

        if (FormFields.getInstance().useField(Field.ResultsReferral)) {
            validateReferral(item, errors);
        }
        if (ConfigurationProperties.getInstance().isPropertyValueEqual(Property.resultTechnicianName, "true")) {
            validateTesterSignature(item, errors);
        }
        if (ConfigurationProperties.getInstance().isPropertyValueEqual(Property.allowResultRejection, "true")) {
            validateRejection(item, errors);
        }

        return errors;
    }

    public Errors validateModifiedItems(List<TestResultItem> modifiedItems) {
        Errors errors = new BaseErrors();

        for (TestResultItem item : modifiedItems) {

            Errors itemErrors = validateItem(item);

            if (itemErrors.hasErrors()) {
                StringBuilder augmentedAccession = new StringBuilder(item.getSequenceAccessionNumber());
                augmentedAccession.append(" : ");
                augmentedAccession.append(item.getTestName());
                // ActionError accessionError = new ActionError("errors.followingAccession",
                // augmentedAccession);

                errors.reject("errors.followingAccession");
                errors.addAllErrors(itemErrors);
            }
        }

        return errors;
    }

    private void validateTestDate(TestResultItem item, Errors errors) {

        Date date = CustomDateValidator.getInstance().getDate(item.getTestDate());

        if (date == null) {
            // errors.add(new ActionError("errors.date", new
            // StringBuilder(item.getTestDate())));
            errors.reject("errors.date");
        } else if (!IActionConstants.VALID
                .equals(CustomDateValidator.getInstance().validateDate(date, DateRelation.PAST))) {
            errors.reject("error.date.inFuture");
        }
    }

    private void validateResult(TestResultItem testResultItem, Errors errors) {
        String resultValue = testResultItem.getShadowResultValue();
        if (GenericValidator.isBlankOrNull(resultValue) && testResultItem.isRejected()) {
            return;
        }

        if (!(ResultUtil.areNotes(testResultItem)
                || (FormFields.getInstance().useField(Field.ResultsReferral) && ResultUtil.isReferred(testResultItem))
                || ResultUtil.areResults(testResultItem) || ResultUtil.isForcedToAcceptance(testResultItem))) {
            errors.reject("errors.result.required");
        }

        if (!GenericValidator.isBlankOrNull(resultValue) && "N".equals(testResultItem.getResultType())) {
            if (resultValue.equals(SPECIAL_CASE)) {
                return;
            }
            try {
                Double.parseDouble(StringUtil.getActualNumericValue(resultValue));
            } catch (NumberFormatException e) {
                // errors.add(new ActionError("errors.number.format", new
                // StringBuilder("Result")));
                errors.reject("errors.number.format");
            }
        }

        if (testResultItem.isHasQualifiedResult()
                && GenericValidator.isBlankOrNull(testResultItem.getQualifiedResultValue())) {
            // errors.add(new ActionError("errors.missing.result.details", new
            // StringBuilder("Result")));
            errors.reject("errors.missing.result.details");
        }
    }

    private void validateReferral(TestResultItem item, Errors errors) {
        if (item.isShadowReferredOut() && "0".equals(item.getReferralReasonId())) {
            errors.reject("error.referral.noReason");
        }
    }

    private void validateRequiredNote(TestResultItem item, Errors errors) {
        if (GenericValidator.isBlankOrNull(item.getNote()) && !GenericValidator.isBlankOrNull(item.getResultId())) {

            if (resultHasChanged(item)) {
                errors.reject("error.requiredNote.missing");
            }
        }
    }

    private boolean resultHasChanged(TestResultItem item) {

        if (TypeOfTestResultServiceImpl.ResultType.isMultiSelectVariant(item.getResultType())) {

            Analysis analysis = analysisService.get(item.getAnalysisId());
            List<Result> resultList = analysisService.getResults(analysis);
            ArrayList<String> dictionaryIds = new ArrayList<>(resultList.size());
            for (Result result : resultList) {
                dictionaryIds.add(result.getValue());
            }
            if (!GenericValidator.isBlankOrNull(item.getMultiSelectResultValues())
                    && !"{}".equals(item.getMultiSelectResultValues())) {
                JSONParser parser = new JSONParser();
                try {
                    int matchCount = 0;
                    JSONObject jsonResult = (JSONObject) parser.parse(item.getMultiSelectResultValues());
                    for (Object key : jsonResult.keySet()) {
                        String[] ids = ((String) jsonResult.get(key)).trim().split(",");

                        for (String id : ids) {
                            if (dictionaryIds.contains(id)) {
                                matchCount++;
                            } else {
                                return false;
                            }
                        }
                    }
                    return matchCount != dictionaryIds.size();
                } catch (ParseException e) {
                    LogEvent.logDebug(e);
                    return false;
                }
            }

        } else {
            Result dbResult = resultService.getResultById(item.getResultId());
            return !item.getShadowResultValue().equals(dbResult.getValue())
                    && !GenericValidator.isBlankOrNull(dbResult.getValue());
        }

        return false;
    }

    private void validateTesterSignature(TestResultItem item, Errors errors) {
        // Conclusions may not need a signature. If the user changed the value
        // and then changed it back it will be
        // marked as dirty but the signature may still be blank.
        if ("0".equals(item.getResultId())) {
            return;
        }

        Result result = resultService.getResultById(item.getResultId());

        if (result != null && result.getAnalyte() != null
                && "Conclusion".equals(result.getAnalyte().getAnalyteName())) {
            if (result.getValue().equals(item.getShadowResultValue())) {
                return;
            }
        }
    }

    private void validateRejection(TestResultItem item, Errors errors) {
        if (item.isRejected() && "0".equals(item.getRejectReasonId())) {
            errors.reject("error.reject.noReason");
        }
    }
}
