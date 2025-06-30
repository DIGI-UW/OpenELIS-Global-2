package org.openelisglobal.result.service;

import jakarta.annotation.PostConstruct;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.validator.GenericValidator;
import org.json.simple.JSONObject;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.result.dao.ResultDAO;
import org.openelisglobal.result.daoimpl.ResultDAOImpl;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.result.valueholder.ResultSignature;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@DependsOn({ "springContext" })
public class ResultServiceImpl extends AuditableBaseObjectServiceImpl<Result, String> implements ResultService {

    private static String TABLE_REFERENCE_ID;

    private static ResultDAO baseObjectDAO = SpringContext.getBean(ResultDAO.class);
    private static DictionaryService dictionaryService = SpringContext.getBean(DictionaryService.class);
    private static ResultSignatureService signatureService = SpringContext.getBean(ResultSignatureService.class);

    @Autowired
    private ReferenceTablesService referenceTablesService = SpringContext.getBean(ReferenceTablesService.class);

    @Autowired
    private TypeOfSampleService typeOfSampleService = SpringContext.getBean(TypeOfSampleService.class);

    @Autowired
    private ResultLimitService resultLimitService = SpringContext.getBean(ResultLimitService.class);

    @PostConstruct
    private void initializeGlobalVariables() {
        TABLE_REFERENCE_ID = referenceTablesService.getReferenceTableByName("RESULT").getId();
    }

    ResultServiceImpl() {
        super(Result.class);
        this.auditTrailLog = true;
    }

    @Override
    protected ResultDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    public String insert(Result result) {
        if (result.getFhirUuid() == null) {
            result.setFhirUuid(UUID.randomUUID());
        }
        return super.insert(result);
    }

    public static String getTableReferenceId() {
        return TABLE_REFERENCE_ID;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Result> getResultsByAnalysis(Analysis analysis) {
        return baseObjectDAO.getAllMatchingOrdered("analysis.id", analysis.getId(), "id", false);
    }

    @Transactional(readOnly = true)
    public String getLabSectionName(Result result) {
        return result.getAnalysis().getTestSection().getName();
    }

    @Override
    @Transactional(readOnly = true)
    public String getReportingTestName(Result result) {
        Test test = result.getAnalysis() != null ? result.getAnalysis().getTest() : null;
        return TestServiceImpl.getUserLocalizedReportingTestName(test);
    }

    @Override
    @Transactional(readOnly = true)
    public String getTestDescription(Result result) {
        Test test = result.getAnalysis() != null ? result.getAnalysis().getTest() : null;
        return TestServiceImpl.getLocalizedTestNameWithType(test);
    }

    @Transactional(readOnly = true)
    public String getSampleType(Result result) {
        return result.getAnalysis() != null
                ? typeOfSampleService.getNameForTypeOfSampleId(result.getAnalysis().getSampleItem().getTypeOfSampleId())
                : "";
    }

    @Override
    @Transactional(readOnly = true)
    public String getLOINCCode(Result result) {
        Test test = result.getAnalysis() != null ? result.getAnalysis().getTest() : null;
        return test != null ? test.getLoinc() : "";
    }

    @Override
    @Transactional(readOnly = true)
    public String getTestTime(Result result) {
        return result.getAnalysis().getCompletedDateForDisplay();
    }

    @Override
    @Transactional(readOnly = true)
    public String getTestType(Result result) {
        return result.getResultType();
    }

    /**
     * This gets the simple value of the result, it treats multiresults as single
     * dictionary values and does not try to get the complete set
     *
     * @return The String value
     */
    @Override
    @Transactional(readOnly = true)
    public String getSimpleResultValue(Result result) {
        if (GenericValidator.isBlankOrNull(result.getValue())) {
            return "";
        }

        if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(getTestType(result))) {
            return getDictEntry(result);
        } else if (TypeOfTestResultServiceImpl.ResultType.NUMERIC.matches(getTestType(result))) {
            int significantPlaces = result.getSignificantDigits();
            if (significantPlaces == 0) {
                return result.getValue().split("\\.")[0];
            }
            StringBuilder value = new StringBuilder();
            value.append(result.getValue());
            int startFill = 0;

            if (!result.getValue().contains(".")) {
                value.append(".");
            } else {
                startFill = result.getValue().length() - result.getValue().lastIndexOf(".") - 1;
            }

            for (int i = startFill; i < significantPlaces; i++) {
                value.append("0");
            }

            return value.toString();
        } else if (TypeOfTestResultServiceImpl.ResultType.ALPHA.matches(result.getResultType())
                && !GenericValidator.isBlankOrNull(result.getValue())) {
            return result.getValue().split("\\(")[0].trim();
        } else {
            return result.getValue();
        }
    }

    /**
     * This returns a textual representation of the result value. Multiselect
     * results are returned as a comma delimited string. If there is a qualified
     * value it is not included
     *
     * @param printable If true the results will be suitable for printing, otherwise
     *                  they will be suitable for a web form
     * @return A textual representation of the value
     */
    @Override
    @Transactional(readOnly = true)
    public String getResultValue(Result result, boolean printable) {
        return getResultValue(result, ",", printable, false);
    }

    @Override
    @Transactional(readOnly = true)
    public String getResultValue(Result result, String separator, boolean printable, boolean includeUOM) {
        if (GenericValidator.isBlankOrNull(result.getValue())) {
            return "";
        }

        if (TypeOfTestResultServiceImpl.ResultType.DICTIONARY.matches(getTestType(result))) {

            if (!printable) {
                return result.getValue();
            }
            String reportResult = "";
            List<Result> resultList = baseObjectDAO.getResultsByAnalysis(result.getAnalysis());
            if (!resultList.isEmpty()) {
                if (resultList.size() == 1) {
                    reportResult = getDictEntry(result);
                } else {
                    // If dictionary result it can also have a quantified result
                    List<Result> dictionaryResults = new ArrayList<>();
                    Result quantification = null;
                    for (Result sibResult : resultList) {
                        if (TypeOfTestResultServiceImpl.ResultType.DICTIONARY.matches(sibResult.getResultType())) {
                            dictionaryResults.add(sibResult);
                        } else if (TypeOfTestResultServiceImpl.ResultType.ALPHA.matches(sibResult.getResultType())
                                && sibResult.getParentResult() != null) {
                            quantification = sibResult;
                        }
                    }

                    for (Result sibResult : dictionaryResults) {
                        Dictionary dictionary = dictionaryService.getDictionaryById(sibResult.getValue());
                        reportResult = (dictionary != null && dictionary.getId() != null)
                                ? dictionary.getLocalizedName()
                                : "";
                        if (quantification != null
                                && quantification.getParentResult().getId().equals(sibResult.getId())) {
                            reportResult += separator + quantification.getValue();
                        }
                    }
                }
            }

            if (includeUOM && !GenericValidator.isBlankOrNull(reportResult)) {
                String uom = getUOM(result);
                if (!GenericValidator.isBlankOrNull(uom)) {
                    reportResult += " " + uom;
                }
            }

            return StringEscapeUtils.escapeHtml4(reportResult);
        } else if (TypeOfTestResultServiceImpl.ResultType.isMultiSelectVariant(getTestType(result))) {
            StringBuilder buffer = new StringBuilder();
            boolean firstPass = true;

            List<Result> results = baseObjectDAO.getResultsByAnalysis(result.getAnalysis());

            for (Result multiResult : results) {
                if (!GenericValidator.isBlankOrNull(multiResult.getValue())
                        && TypeOfTestResultServiceImpl.ResultType.isMultiSelectVariant(multiResult.getResultType())) {
                    if (firstPass) {
                        firstPass = false;
                    } else {
                        buffer.append(separator);
                    }
                    buffer.append(dictionaryService.getDataForId(multiResult.getValue()).getDictEntry());
                }
            }
            return buffer.toString();
        } else if (TypeOfTestResultServiceImpl.ResultType.NUMERIC.matches(getTestType(result))) {
            int significantPlaces = result.getSignificantDigits();
            if (significantPlaces == -1) {
                return result.getValue() + appendUOM(result, includeUOM);
            }
            if (significantPlaces == 0) {
                return result.getValue().split("\\.")[0] + appendUOM(result, includeUOM);
            }
            StringBuilder value = new StringBuilder();
            value.append(result.getValue());
            int startFill = 0;

            if (!result.getValue().contains(".")) {
                value.append(".");
            } else {
                startFill = result.getValue(true).length() - result.getValue(true).lastIndexOf(".") - 1;
            }

            for (int i = startFill; i < significantPlaces; i++) {
                value.append("0");
            }

            return value.toString() + appendUOM(result, includeUOM);
        } else if (TypeOfTestResultServiceImpl.ResultType.ALPHA.matches(result.getResultType())
                && !GenericValidator.isBlankOrNull(result.getValue())) {
            return result.getValue().split("\\(")[0].trim();
        } else {
            return result.getValue();
        }
    }

    @Override
    public String getResultValueForDisplay(Result result, String separator, boolean printable, boolean includeUOM) {
        if (GenericValidator.isBlankOrNull(result.getValue())) {
            return "";
        }

        if (TypeOfTestResultServiceImpl.ResultType.DICTIONARY.matches(getTestType(result))) {

            if (!printable) {
                return result.getValue();
            }
            String reportResult = "";
            List<Result> resultList = baseObjectDAO.getResultsByAnalysis(result.getAnalysis());
            if (!resultList.isEmpty()) {
                if (resultList.size() == 1) {
                    reportResult = getDictValueForDisplay(result);
                } else {
                    // If dictionary result it can also have a quantified result
                    List<Result> dictionaryResults = new ArrayList<>();
                    Result quantification = null;
                    for (Result sibResult : resultList) {
                        if (TypeOfTestResultServiceImpl.ResultType.DICTIONARY.matches(sibResult.getResultType())) {
                            dictionaryResults.add(sibResult);
                        } else if (TypeOfTestResultServiceImpl.ResultType.ALPHA.matches(sibResult.getResultType())
                                && sibResult.getParentResult() != null) {
                            quantification = sibResult;
                        }
                    }

                    for (Result sibResult : dictionaryResults) {
                        Dictionary dictionary = dictionaryService.getDictionaryById(sibResult.getValue());
                        reportResult = (dictionary != null && dictionary.getId() != null)
                                ? dictionary.getLocalizedName()
                                : "";
                        if (quantification != null
                                && quantification.getParentResult().getId().equals(sibResult.getId())) {
                            reportResult += separator + quantification.getValue();
                        }
                    }
                }
            }

            if (includeUOM && !GenericValidator.isBlankOrNull(reportResult)) {
                String uom = getUOM(result);
                if (!GenericValidator.isBlankOrNull(uom)) {
                    reportResult += " " + uom;
                }
            }

            return StringEscapeUtils.escapeHtml4(reportResult);
        } else if (TypeOfTestResultServiceImpl.ResultType.isMultiSelectVariant(getTestType(result))) {
            StringBuilder buffer = new StringBuilder();
            boolean firstPass = true;

            List<Result> results = new ResultDAOImpl().getResultsByAnalysis(result.getAnalysis());

            for (Result multiResult : results) {
                if (!GenericValidator.isBlankOrNull(multiResult.getValue())
                        && TypeOfTestResultServiceImpl.ResultType.isMultiSelectVariant(multiResult.getResultType())) {
                    if (firstPass) {
                        firstPass = false;
                    } else {
                        buffer.append(separator);
                    }
                    buffer.append(dictionaryService.getDataForId(multiResult.getValue()).getDictEntry());
                }
            }
            return buffer.toString();
        } else if (TypeOfTestResultServiceImpl.ResultType.NUMERIC.matches(getTestType(result))) {
            int significantPlaces = result.getSignificantDigits();
            if (significantPlaces == -1) {
                return result.getValue() + appendUOM(result, includeUOM);
            }
            if (significantPlaces == 0) {
                return result.getValue().split("\\.")[0] + appendUOM(result, includeUOM);
            }
            StringBuilder value = new StringBuilder();
            value.append(result.getValue());
            int startFill = 0;

            if (!result.getValue().contains(".")) {
                value.append(".");
            } else {
                startFill = result.getValue(true).length() - result.getValue(true).lastIndexOf(".") - 1;
            }

            for (int i = startFill; i < significantPlaces; i++) {
                value.append("0");
            }

            return value.toString() + appendUOM(result, includeUOM);
        } else if (TypeOfTestResultServiceImpl.ResultType.ALPHA.matches(result.getResultType())
                && !GenericValidator.isBlankOrNull(result.getValue())) {
            return result.getValue().split("\\(")[0].trim();
        } else {
            return result.getValue();
        }
    }

    private String getDictEntry(Result result) {
        Dictionary dictionary = dictionaryService.getDataForId(result.getValue());
        return dictionary != null ? dictionary.getDictEntry() : "";
    }

    private String getDictValueForDisplay(Result result) {
        Dictionary dictionary = dictionaryService.getDataForId(result.getValue());
        if (dictionary != null) {
            return dictionary.getLocalizedName() == null ? dictionary.getDictEntry() : dictionary.getLocalizedName();
        }
        return "";
    }

    private String appendUOM(Result result, boolean includeUOM) {
        if (includeUOM && result.getAnalysis().getTest().getUnitOfMeasure() != null) {
            return " " + result.getAnalysis().getTest().getUnitOfMeasure().getName();
        } else {
            return "";
        }
    }

    @Transactional(readOnly = true)
    public String getMultiSelectSelectedIdValues(Result result) {
        if (GenericValidator.isBlankOrNull(result.getValue())) {
            return "";
        }

        if (TypeOfTestResultServiceImpl.ResultType.MULTISELECT.getCharacterValue().equals(getTestType(result))) {
            StringBuilder buffer = new StringBuilder();
            boolean firstPass = true;

            List<Result> results = baseObjectDAO.getResultsByAnalysis(result.getAnalysis());

            for (Result multiResult : results) {
                if (!GenericValidator.isBlankOrNull(multiResult.getValue())
                        && TypeOfTestResultServiceImpl.ResultType.isMultiSelectVariant(multiResult.getResultType())) {
                    if (firstPass) {
                        firstPass = false;
                    } else {
                        buffer.append(",");
                    }
                    buffer.append(multiResult.getValue());
                }
            }
            return buffer.toString();
        }

        return "";
    }

    @Override
    @Transactional(readOnly = true)
    public String getUOM(Result result) {
        Test test = result.getAnalysis() != null ? result.getAnalysis().getTest() : null;
        return test != null && test.getUnitOfMeasure() != null ? test.getUnitOfMeasure().getUnitOfMeasureName() : "";
    }

    @Transactional(readOnly = true)
    public double getlowNormalRange(Result result) {
        return result.getMinNormal();
    }

    @Transactional(readOnly = true)
    public double getHighNormalRange(Result result) {
        return result.getMaxNormal();
    }

    /**
     * @return true if any of the result limits for this test have a gender
     *         specification
     */
    public boolean ageInRangeCriteria(Result result) {
        List<ResultLimit> resultLimits = getResultLimits(result);

        for (ResultLimit limit : resultLimits) {
            if (limit.getMaxAge() != limit.getMinAge()) {
                return true;
            }
        }

        return false;
    }

    public boolean genderInRangeCritera(Result result) {
        List<ResultLimit> resultLimits = getResultLimits(result);

        for (ResultLimit limit : resultLimits) {
            if (!GenericValidator.isBlankOrNull(limit.getGender())) {
                return true;
            }
        }

        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public String getDisplayReferenceRange(Result result, boolean includeSelectList) {
        String range = "";
        if (TypeOfTestResultServiceImpl.ResultType.NUMERIC.matches(result.getResultType())) {
            if (result.getMinNormal() != null && result.getMaxNormal() != null
                    && !result.getMinNormal().equals(result.getMaxNormal())) {
                range = SpringContext.getBean(ResultLimitService.class).getDisplayNormalRange(result.getMinNormal(),
                        result.getMaxNormal(), String.valueOf(result.getSignificantDigits()), "-");
            }
        } else if (includeSelectList
                && TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(result.getResultType())) {
            List<ResultLimit> limits = getResultLimits(result);
            if (!limits.isEmpty() && !GenericValidator.isBlankOrNull(limits.get(0).getDictionaryNormalId())) {
                range = dictionaryService.getDataForId(limits.get(0).getDictionaryNormalId()).getLocalizedName();
            }
        }
        return range;
    }

    private List<ResultLimit> getResultLimits(Result result) {
        Test test = result.getAnalysis() != null ? result.getAnalysis().getTest() : null;
        List<ResultLimit> resultLimit = test != null ? resultLimitService.getAllResultLimitsForTest(test.getId())
                : new ArrayList<>();

        return resultLimit;
    }

    @Override
    public boolean isAbnormalDictionaryResult(Result result) {
        if (result.getValue() != null
                && TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(result.getResultType())) {
            List<ResultLimit> limits = getResultLimits(result);
            if (!limits.isEmpty()) {
                return !result.getValue().equals(limits.get(0).getDictionaryNormalId());
            }
        }

        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public String getLastUpdatedTime(Result result) {
        return DateUtil.convertTimestampToStringDate(result.getLastupdated());
    }

    @Override
    @Transactional(readOnly = true)
    public String getSignature(Result result) {
        List<ResultSignature> signatures = signatureService.getResultSignaturesByResult(result);
        return signatures.isEmpty() ? "" : signatures.get(0).getNonUserName();
    }

    public static List<Result> getResultsInTimePeriodWithTest(Date startDate, Date endDate, String testId) {
        return baseObjectDAO.getResultsForTestInDateRange(testId, startDate, DateUtil.addDaysToSQLDate(endDate, 1));
    }

    public static List<Result> getResultsInTimePeriodInPanel(Date lowDate, Date highDate, String panelId) {
        return baseObjectDAO.getResultsForPanelInDateRange(panelId, lowDate, DateUtil.addDaysToSQLDate(highDate, 1));
    }

    public static List<Result> getResultsInTimePeriodInTestSection(Date lowDate, Date highDate, String testSectionId) {
        return baseObjectDAO.getResultsForTestSectionInDateRange(testSectionId, lowDate,
                DateUtil.addDaysToSQLDate(highDate, 1));
    }

    public static String getJSONStringForMultiSelect(List<Result> resultList) {
        Collections.sort(resultList, new Comparator<Result>() {
            @Override
            public int compare(Result o1, Result o2) {
                return o1.getGrouping() - o2.getGrouping();
            }
        });

        JSONObject jsonRep = new JSONObject();

        int currentGrouping = -1;
        StringBuilder currentString = new StringBuilder();

        for (Result result : resultList) {
            if (TypeOfTestResultServiceImpl.ResultType.isMultiSelectVariant(result.getResultType())
                    && result.getValue() != null) {
                if (currentGrouping != result.getGrouping()) {
                    if (currentString.length() > 1) {
                        currentString.setLength(currentString.length() - 1);
                        jsonRep.put(String.valueOf(currentGrouping), currentString.toString());
                    }

                    currentGrouping = result.getGrouping();
                    currentString = new StringBuilder();
                }

                currentString.append(result.getValue());
                currentString.append(",");
            }
        }

        if (currentString.length() > 1) {
            currentString.setLength(currentString.length() - 1);
            jsonRep.put(String.valueOf(currentGrouping), currentString.toString());
        }

        return jsonRep.toJSONString();
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(Result result) {
        getBaseObjectDAO().getData(result);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Result> getResultsForTestSectionInDateRange(String testSectionId, Date lowDate, Date highDate) {
        return getBaseObjectDAO().getResultsForTestSectionInDateRange(testSectionId, lowDate, highDate);
    }

    @Override
    @Transactional(readOnly = true)
    public void getResultByAnalysisAndAnalyte(Result result, Analysis analysis, TestAnalyte ta) {
        getBaseObjectDAO().getResultByAnalysisAndAnalyte(result, analysis, ta);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Result> getResultsForAnalysisIdList(List<Integer> analysisIdList) {
        return getBaseObjectDAO().getResultsForAnalysisIdList(analysisIdList);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Result> getResultsForPanelInDateRange(String panelId, Date lowDate, Date highDate) {
        return getBaseObjectDAO().getResultsForPanelInDateRange(panelId, lowDate, highDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Result> getResultsForSample(Sample sample) {
        return getBaseObjectDAO().getResultsForSample(sample);
    }

    @Override
    @Transactional(readOnly = true)
    public Result getResultForAnalyteInAnalysisSet(String analyteId, List<Integer> analysisIDList) {
        return getBaseObjectDAO().getResultForAnalyteInAnalysisSet(analyteId, analysisIDList);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Result> getResultsForTestInDateRange(String testId, Date startDate, Date endDate) {
        return getBaseObjectDAO().getResultsForTestInDateRange(testId, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public void getResultByTestResult(Result result, TestResult testResult) {
        getBaseObjectDAO().getResultByTestResult(result, testResult);
    }

    @Override
    @Transactional(readOnly = true)
    public Result getResultForAnalyteAndSampleItem(String analyteId, String sampleItemId) {
        return getBaseObjectDAO().getResultForAnalyteAndSampleItem(analyteId, sampleItemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Result> getResultsForTestAndSample(String sampleId, String testId) {
        return getBaseObjectDAO().getResultsForTestAndSample(sampleId, testId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Result> getReportableResultsByAnalysis(Analysis analysis) {
        return getBaseObjectDAO().getReportableResultsByAnalysis(analysis);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Result> getChildResults(String resultId) {
        return getBaseObjectDAO().getChildResults(resultId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Result> getAllResults() {
        return getBaseObjectDAO().getAllResults();
    }

    @Override
    @Transactional(readOnly = true)
    public Result getResultById(Result result) {
        return getBaseObjectDAO().getResultById(result);
    }

    @Override
    @Transactional(readOnly = true)
    public Result getResultById(String resultId) {
        return getBaseObjectDAO().getResultById(resultId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Result> getPageOfResults(int startingRecNo) {
        return getBaseObjectDAO().getPageOfResults(startingRecNo);
    }
}
