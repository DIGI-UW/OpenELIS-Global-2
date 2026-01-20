package org.openelisglobal.test.service.middleware;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.common.ReportTransmission;
import org.openelisglobal.dataexchange.common.ReportTransmission.HTTP_TYPE;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl.ResultType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TestMiddlewareSyncServiceImpl implements TestMiddlewareSyncService {

    @Autowired
    private TestService testService;

    @Autowired
    private ResultLimitService resultLimitService;

    @Autowired
    private TestResultService testResultService;

    @Autowired
    private DictionaryService dictionaryService;

    @Value("${org.openelisglobal.middleware.test.sync.enabled:false}")
    private boolean syncEnabled;

    @Value("${org.openelisglobal.middleware.test.sync.url:}")
    private String middlewareUrl;

    @Value("${org.openelisglobal.middleware.test.sync.secret:change-me}")
    private String middlewareSecret;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void syncTestToMiddleware(Test test, boolean isUpdate) {
        if (!syncEnabled) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "syncTestToMiddleware",
                    "Middleware test sync is disabled (org.openelisglobal.middleware.test.sync.enabled=false), skipping");
            return;
        }

        if (test == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "syncTestToMiddleware", "Cannot sync null test");
            return;
        }

        if (GenericValidator.isBlankOrNull(middlewareUrl)) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "syncTestToMiddleware",
                    "Middleware test sync URL is not configured (org.openelisglobal.middleware.test.sync.url), skipping");
            return;
        }

        try {
            String resultType = determineResultType(test);
            TestDefinitionEventPayload payload = buildPayload(test, resultType, isUpdate);

            String json = serializePayload(payload);

            LogEvent.logInfo(this.getClass().getSimpleName(), "syncTestToMiddleware",
                    "Sending test definition to middleware: testId=" + test.getId() + ", guid=" + test.getGuid()
                            + ", isUpdate=" + isUpdate + ", url=" + middlewareUrl);

            ReportTransmission transmission = new ReportTransmission();
            transmission.sendRawReport(json, middlewareUrl, true, null, HTTP_TYPE.POST,
                    "X-Nidan-Webhook-Secret", middlewareSecret);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "syncTestToMiddleware",
                    "Error syncing test to middleware: " + e.getMessage());
            LogEvent.logError(this.getClass().getSimpleName(), "syncTestToMiddleware", "Full error: " + e.toString());
        }
    }

    private String determineResultType(Test test) {
        try {
            return testService.getResultType(test);
        } catch (Exception e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "determineResultType",
                    "Error determining result type for test " + test.getId() + ", defaulting to ALPHA: "
                            + e.getMessage());
            return ResultType.ALPHA.getCharacterValue();
        }
    }

    private TestDefinitionEventPayload buildPayload(Test test, String resultType, boolean isUpdate) {
        TestDefinitionEventPayload payload = new TestDefinitionEventPayload();

        payload.eventType = isUpdate ? "LabTestDefinitionUpdated" : "LabTestDefinitionCreated";
        payload.sourceSystem = "OPENELIS";
        payload.occurredAt = OffsetDateTime.now().toString();
        payload.eventId = UUID.randomUUID().toString();

        payload.changeType = isUpdate ? "UPDATED" : "CREATED";

        String testUuid = !GenericValidator.isBlankOrNull(test.getGuid()) ? test.getGuid() : test.getId();
        payload.testUuid = testUuid;
        payload.openelisTestId = test.getId();
        payload.loincCode = test.getLoinc();

        String testName = getTestName(test);
        payload.name = testName;
        payload.description = test.getDescription();

        payload.orderable = test.getOrderable();
        payload.price = test.getPrice();
        payload.defaultPrice = test.getPrice();

        // For atomic tests (non-panels), explicitly mark panel=false and leave
        // componentTestUuids empty. Panel definitions are synced separately using the
        // PanelCreatedOrUpdatedEvent pipeline.
        payload.panel = false;
        payload.componentTestUuids = new ArrayList<>();

        payload.resultType = resultType;

        if (ResultType.NUMERIC.matches(resultType)) {
            if (test.getUnitOfMeasure() != null) {
                payload.unit = test.getUnitOfMeasure().getName();
            }
            payload.ranges = buildRanges(test);
        } else if (ResultType.isDictionaryVariant(resultType)) {
            payload.codedAnswers = buildCodedAnswers(test);
        }

        return payload;
    }

    private List<RangePayload> buildRanges(Test test) {
        List<RangePayload> ranges = new ArrayList<>();

        List<ResultLimit> resultLimits = resultLimitService.getAllResultLimitsForTest(test.getId());
        if (resultLimits == null || resultLimits.isEmpty()) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "buildRanges",
                    "No result limits defined for test " + test.getId());
            return ranges;
        }

        ResultLimit limit = resultLimits.get(0);

        if (isValidRange(limit.getLowNormal(), limit.getHighNormal())) {
            ranges.add(createRange(limit.getLowNormal(), limit.getHighNormal(), "normal"));
        }

        if (isValidRange(limit.getLowCritical(), limit.getHighCritical())) {
            ranges.add(createRange(limit.getLowCritical(), limit.getHighCritical(), "treatment"));
        }

        if (isValidRange(limit.getLowValid(), limit.getHighValid())) {
            ranges.add(createRange(limit.getLowValid(), limit.getHighValid(), "absolute"));
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "buildRanges",
                "Added " + ranges.size() + " ranges for test " + test.getId());

        return ranges;
    }

    private boolean isValidRange(Double low, Double high) {
        return low != null && high != null && !Double.isInfinite(low) && !Double.isInfinite(high);
    }

    private RangePayload createRange(Double low, Double high, String type) {
        RangePayload range = new RangePayload();
        range.type = type;
        range.low = BigDecimal.valueOf(low);
        range.high = BigDecimal.valueOf(high);
        return range;
    }

    private List<CodedAnswerPayload> buildCodedAnswers(Test test) {
        List<CodedAnswerPayload> answers = new ArrayList<>();

        try {
            List<TestResult> testResults = testResultService.getActiveTestResultsByTest(test.getId());

            for (TestResult tr : testResults) {
                if ("D".equals(tr.getTestResultType()) && !GenericValidator.isBlankOrNull(tr.getValue())) {
                    Dictionary dict = dictionaryService.get(tr.getValue());
                    if (dict != null && "Y".equals(dict.getIsActive())) {
                        CodedAnswerPayload answer = new CodedAnswerPayload();
                        answer.dictionaryId = dict.getId();
                        answer.code = dict.getDictEntry();

                        Localization localizedName = dict.getLocalizedDictionaryName();
                        if (localizedName != null) {
                            String localized = localizedName.getLocalizedValue();
                            if (!GenericValidator.isBlankOrNull(localized)) {
                                answer.display = localized.trim();
                            } else {
                                answer.display = dict.getDictEntry();
                            }
                        } else {
                            answer.display = dict.getDictEntry();
                        }

                        if (!GenericValidator.isBlankOrNull(dict.getLoincCode())) {
                            answer.loincCode = dict.getLoincCode();
                        }

                        answer.stableGuid = generateStableGuidForDictionary(dict);

                        answers.add(answer);
                    }
                }
            }

            LogEvent.logDebug(this.getClass().getSimpleName(), "buildCodedAnswers",
                    "Found " + answers.size() + " coded answers for test " + test.getId());

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "buildCodedAnswers",
                    "Error building coded answers for test " + test.getId() + ": " + e.getMessage());
        }

        return answers;
    }

    private String getTestName(Test test) {
        Localization localizedName = test.getLocalizedTestName();
        if (localizedName != null) {
            String localized = localizedName.getLocalizedValue();
            if (!GenericValidator.isBlankOrNull(localized)) {
                return localized.trim();
            }
        }

        if (!GenericValidator.isBlankOrNull(test.getDescription())) {
            return test.getDescription().trim();
        }

        return "Test " + test.getId();
    }

    private String generateStableGuidForDictionary(Dictionary dict) {
        String namespace = "openelis-dictionary";
        String name = "dict-" + dict.getId();
        return UUID.nameUUIDFromBytes((namespace + ":" + name).getBytes()).toString();
    }

    private String serializePayload(TestDefinitionEventPayload payload) throws JsonProcessingException {
        return objectMapper.writeValueAsString(payload);
    }

    private static class TestDefinitionEventPayload {
        public String eventType;
        public String sourceSystem;
        public String occurredAt;
        public String eventId;

        public String changeType;

        public String testUuid;
        public String openelisTestId;
        public String loincCode;
        public String name;
        public String description;

        public Boolean orderable;
        public BigDecimal price;

        // Panel-related metadata to align with LabTestDefinitionEvent
        public boolean panel;
        public List<String> componentTestUuids;

        // Middleware-side default price / currency semantics
        public BigDecimal defaultPrice;
        public String currency;

        public String resultType;
        public String unit;
        public List<RangePayload> ranges;
        public List<CodedAnswerPayload> codedAnswers;
    }

    private static class RangePayload {
        public String type;
        public BigDecimal low;
        public BigDecimal high;
    }

    private static class CodedAnswerPayload {
        public String dictionaryId;
        public String code;
        public String display;
        public String loincCode;
        public String stableGuid;
    }
}
