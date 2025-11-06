package org.openelisglobal.referral.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.referral.action.beanitems.IReferralResultTest;
import org.openelisglobal.referral.action.beanitems.ReferralItem;
import org.openelisglobal.referral.action.beanitems.ReferredTest;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.referral.valueholder.ReferralResult;
import org.openelisglobal.result.service.ResultServiceImpl;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReferralItemServiceImpl implements ReferralItemService {

    @Autowired
    DictionaryService dictionaryService;
    @Autowired
    ReferralService referralService;
    @Autowired
    ReferralResultService referralResultService;
    @Autowired
    AnalysisService analysisService;
    @Autowired
    TestService testService;

    @Override
    @Transactional(readOnly = true)
    public List<ReferralItem> getReferralItems() {
        List<ReferralItem> referralItems = new ArrayList<>();

        List<Referral> referralList = referralService.getUncanceledOpenReferrals();

        for (Referral referral : referralList) {
            ReferralItem referralItem = getReferralItem(referral);
            if (referralItem != null) {
                referralItems.add(referralItem);
            }
        }

        Collections.sort(referralItems, new ReferralComparator());

        return referralItems;
    }

    @Transactional(readOnly = true)
    public ReferralItem getReferralItem(Referral referral) {
        boolean allReferralResultsHaveResults = true;
        List<ReferralResult> referralResults = referralResultService.getReferralResultsForReferral(referral.getId());
        for (ReferralResult referralResult : referralResults) {
            if (referralResult.getResult() == null
                    || GenericValidator.isBlankOrNull(referralResult.getResult().getValue())) {
                allReferralResultsHaveResults = false;
                break;
            }
        }

        if (allReferralResultsHaveResults) {
            return null;
        }

        ReferralItem referralItem = new ReferralItem();

        Analysis analysis = referral.getAnalysis();

        referralItem.setReferralStatus(referral.getStatus());
        referralItem.setReferredResultType("N");
        referralItem.setAccessionNumber(analysisService.getOrderAccessionNumber(analysis));

        TypeOfSample typeOfSample = analysisService.getTypeOfSample(analysis);
        referralItem.setSampleType(typeOfSample.getLocalizedName());

        referralItem.setReferringTestName(TestServiceImpl.getUserLocalizedTestName(analysis.getTest()));
        List<Result> resultList = analysisService.getResults(analysis);
        String resultString = "";

        if (!resultList.isEmpty()) {
            Result result = resultList.get(0);
            resultString = getAppropriateResultValue(resultList);
            referralItem.setInLabResultId(result.getId());
        }

        referralItem.setReferralId(referral.getId());
        if (!referralResults.isEmpty()) {
            referralResults = setReferralItemForNextTest(referralItem, referralResults);
            if (!referralResults.isEmpty()) {
                referralItem.setAdditionalTests(getAdditionalReferralTests(referralResults));
            }
        }
        referralItem.setReferralResults(resultString);
        referralItem.setReferralDate(DateUtil.convertTimestampToStringDate(referral.getRequestDate()));
        referralItem.setReferredSendDate(getSendDateOrDefault(referral));
        referralItem.setReferrer(referral.getRequesterName());
        referralItem.setReferralReasonId(referral.getReferralReasonId());
        referralItem.setTestSelectionList(getTestsForTypeOfSample(typeOfSample));
        referralItem.setReferralId(referral.getId());
        if (referral.getOrganization() != null) {
            referralItem.setReferredInstituteId(referral.getOrganization().getId());
        }
        String notes = analysisService.getNotesAsString(analysis, true, true, "<br/>", false);
        if (notes != null) {
            referralItem.setPastNotes(notes);
        }

        return referralItem;
    }

    private String getSendDateOrDefault(Referral referral) {
        if (referral.getSentDate() == null) {
            return DateUtil.getCurrentDateAsText();
        } else {
            return DateUtil.convertTimestampToStringDate(referral.getSentDate());
        }
    }

    private List<ReferredTest> getAdditionalReferralTests(List<ReferralResult> referralResults) {
        List<ReferredTest> additionalTestList = new ArrayList<>();

        while (!referralResults.isEmpty()) {
            ReferralResult referralResult = referralResults.get(0); // use the top one to load various bits of
            // information.
            ReferredTest referralTest = new ReferredTest();
            referralTest.setReferralId(referralResult.getReferralId());
            referralResults = setReferralItemForNextTest(referralTest, referralResults); // remove one or more
            // referralResults from the
            // list as needed (for
            // multiResults).
            referralTest.setReferredReportDate(
                    DateUtil.convertTimestampToStringDate(referralResult.getReferralReportDate()));
            referralTest.setReferralResultId(referralResult.getId());
            additionalTestList.add(referralTest);
        }
        return additionalTestList;
    }

    /**
     * Move everything appropriate to the referralItem including one or more of the
     * referralResults from the given list. Note: This method removes an item from
     * the referralResults list.
     *
     * @param referralItem    The source item
     * @param referralResults The created list
     */
    private List<ReferralResult> setReferralItemForNextTest(IReferralResultTest referralItem,
            List<ReferralResult> referralResults) {

        ReferralResult nextTestFirstResult = referralResults.remove(0);
        List<ReferralResult> resultsForOtherTests = new ArrayList<>(referralResults);

        referralItem.setReferredTestId(nextTestFirstResult.getTestId());
        referralItem.setReferredTestIdShadow(referralItem.getReferredTestId());
        referralItem.setReferredReportDate(
                DateUtil.convertTimestampToStringDate(nextTestFirstResult.getReferralReportDate()));
        // We can not use ResultService because that assumes the result is for an
        // analysis, not a referral
        Result result = nextTestFirstResult.getResult();

        String resultType;
        if (!GenericValidator.isBlankOrNull(referralItem.getReferredTestId())) {
            Test test = testService.get(referralItem.getReferredTestId());
            resultType = testService.getResultType(test);
        } else {
            resultType = (result != null) ? result.getResultType() : "N";
        }
        referralItem.setReferredResultType(resultType);
        if (!TypeOfTestResultServiceImpl.ResultType.isMultiSelectVariant(resultType)) {
            if (result != null) {
                String resultValue = GenericValidator.isBlankOrNull(result.getValue()) ? "" : result.getValue();
                referralItem.setReferredResult(resultValue);
                referralItem.setReferredDictionaryResult(resultValue);
            }
        } else {
            ArrayList<Result> resultList = new ArrayList<>();
            resultList.add(nextTestFirstResult.getResult());

            for (ReferralResult referralResult : referralResults) {
                if (nextTestFirstResult.getTestId().equals(referralResult.getTestId())
                        && !GenericValidator.isBlankOrNull(referralResult.getResult().getValue())) {
                    resultList.add(referralResult.getResult());
                    resultsForOtherTests.remove(referralResult);
                }
            }

            referralItem.setMultiSelectResultValues(ResultServiceImpl.getJSONStringForMultiSelect(resultList));
        }

        return resultsForOtherTests;
    }

    private String getAppropriateResultValue(List<Result> results) {
        Result result = results.get(0);
        if (TypeOfTestResultServiceImpl.ResultType.DICTIONARY.matches(result.getResultType())) {
            Dictionary dictionary = dictionaryService.get(result.getValue());
            if (dictionary != null) {
                return dictionary.getLocalizedName();
            }
        } else if (TypeOfTestResultServiceImpl.ResultType.isMultiSelectVariant(result.getResultType())) {
            StringBuilder multiResult = new StringBuilder();

            for (Result subResult : results) {
                Dictionary dictionary = dictionaryService.get(subResult.getValue());

                if (dictionary.getId() != null) {
                    multiResult.append(dictionary.getLocalizedName());
                    multiResult.append(", ");
                }
            }

            if (multiResult.length() > 0) {
                multiResult.setLength(multiResult.length() - 2); // remove last ", "
            }

            return multiResult.toString();
        } else {
            String resultValue = GenericValidator.isBlankOrNull(result.getValue()) ? "" : result.getValue();

            if (!GenericValidator.isBlankOrNull(resultValue)
                    && result.getAnalysis().getTest().getUnitOfMeasure() != null) {
                resultValue += " " + result.getAnalysis().getTest().getUnitOfMeasure().getName();
            }

            return resultValue;
        }

        return "";
    }

    private List<IdValuePair> getTestsForTypeOfSample(TypeOfSample typeOfSample) {
        List<Test> testList = SpringContext.getBean(TypeOfSampleService.class)
                .getActiveTestsBySampleTypeId(typeOfSample.getId(), false);

        List<IdValuePair> valueList = new ArrayList<>();

        for (Test test : testList) {
            if (test.getOrderable()) {
                valueList.add(new IdValuePair(test.getId(), TestServiceImpl.getUserLocalizedTestName(test)));
            }
        }

        return valueList;
    }

    private static final class ReferralComparator implements Comparator<ReferralItem> {
        @Override
        public int compare(ReferralItem left, ReferralItem right) {
            int result = left.getAccessionNumber().compareTo(right.getAccessionNumber());
            if (result != 0) {
                return result;
            }
            result = left.getSampleType().compareTo(right.getSampleType());
            if (result != 0) {
                return result;
            }
            return left.getReferringTestName().compareTo(right.getReferringTestName());
        }
    }
}
