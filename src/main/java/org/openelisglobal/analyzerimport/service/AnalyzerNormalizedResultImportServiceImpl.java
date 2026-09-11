package org.openelisglobal.analyzerimport.service;

import ca.uhn.fhir.context.FhirContext;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Bundle;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.service.AnalyzerSiteBindingService;
import org.openelisglobal.analyzer.service.AnalyzerSiteBindingSnapshot;
import org.openelisglobal.analyzer.service.QCResultProcessingService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingMappingState;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingResult;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingTest;
import org.openelisglobal.analyzerimport.dao.AnalyzerDeliveryReceiptDAO;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerDeliveryReceipt;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyzerNormalizedResultImportServiceImpl implements AnalyzerNormalizedResultImportService {

    private static final String CLASS_NAME = "AnalyzerNormalizedResultImportServiceImpl";

    private final AnalyzerService analyzerService;
    private final AnalyzerSiteBindingService siteBindingService;
    private final AnalyzerResultsService analyzerResultsService;
    private final TestResultService testResultService;
    private final QCResultProcessingService qcResultProcessingService;
    private final FhirContext fhirContext;
    private final AnalyzerDeliveryReceiptDAO receiptDAO;

    public AnalyzerNormalizedResultImportServiceImpl(AnalyzerService analyzerService,
            AnalyzerSiteBindingService siteBindingService, AnalyzerResultsService analyzerResultsService,
            TestResultService testResultService, QCResultProcessingService qcResultProcessingService,
            FhirContext fhirContext, AnalyzerDeliveryReceiptDAO receiptDAO) {
        this.analyzerService = analyzerService;
        this.siteBindingService = siteBindingService;
        this.analyzerResultsService = analyzerResultsService;
        this.testResultService = testResultService;
        this.qcResultProcessingService = qcResultProcessingService;
        this.fhirContext = fhirContext;
        this.receiptDAO = receiptDAO;
    }

    @Override
    @Transactional
    public AnalyzerNormalizedResultImportSummary importBundle(Bundle bundle, String actor) {
        String effectiveActor = requireText(actor, "Import actor is required");
        AnalyzerNormalizedResultContract contract = AnalyzerNormalizedResultContract.parse(bundle, fhirContext);
        Analyzer analyzer = analyzerService.findByBridgeConnectionIdForUpdate(contract.bridgeConnectionId())
                .orElseThrow(
                        () -> new AnalyzerNormalizedResultImportException("analyzer.fhirImport.error.unknownConnection",
                                "No analyzer references Bridge connection " + contract.bridgeConnectionId()));
        // The database lock serializes simultaneous deliveries for this connection
        // until commit.
        // Check receipts before today's bindings/profile: an accepted retry must not
        // become new work.
        Optional<AnalyzerDeliveryReceipt> accepted = receiptDAO.findByDelivery(contract.bridgeConnectionId(),
                contract.messageId());
        if (accepted.isPresent()) {
            AnalyzerDeliveryReceipt receipt = accepted.orElseThrow();
            return new AnalyzerNormalizedResultImportSummary(receipt.getAnalyzerId(), receipt.getResultsStaged(),
                    receipt.getResultsHeld(), receipt.getControlsProcessed());
        }
        requireMatchingProfile(analyzer, contract);

        List<AnalyzerResults> staged = mapResults(contract, analyzer);
        if (!staged.isEmpty()) {
            analyzerResultsService.insertAnalyzerResults(staged, effectiveActor);
        }

        int controlsProcessed = 0;
        for (AnalyzerResults row : staged) {
            if (row.getIsControl() && !row.isReadOnly() && row.getTestId() != null && processControl(row, analyzer)) {
                controlsProcessed++;
            }
        }
        int held = (int) staged.stream().filter(AnalyzerResults::isReadOnly).count();
        AnalyzerDeliveryReceipt receipt = new AnalyzerDeliveryReceipt();
        receipt.setConnectionId(contract.bridgeConnectionId());
        receipt.setMessageId(contract.messageId());
        receipt.setAnalyzerId(analyzer.getId());
        receipt.setProfileId(contract.profileId());
        receipt.setProfileRevision(contract.profileRevision());
        receipt.setResultsStaged(staged.size());
        receipt.setResultsHeld(held);
        receipt.setControlsProcessed(controlsProcessed);
        receipt.setAcceptedBy(effectiveActor);
        receipt.setAcceptedAt(new Timestamp(System.currentTimeMillis()));
        receiptDAO.insert(receipt);
        return new AnalyzerNormalizedResultImportSummary(analyzer.getId(), staged.size(), held, controlsProcessed);
    }

    @Override
    @Transactional
    public AnalyzerNormalizedResultImportSummary reprocessHeldResult(String analyzerId, String resultId, String actor) {
        String effectiveActor = requireText(actor, "Reprocessing actor is required");
        Analyzer selected = analyzerService.get(requireText(analyzerId, "Analyzer ID is required"));
        if (selected == null || selected.getBridgeConnectionId() == null) {
            throw new IllegalArgumentException("Analyzer has no Bridge connection");
        }
        Analyzer analyzer = analyzerService.findByBridgeConnectionIdForUpdate(selected.getBridgeConnectionId())
                .orElseThrow(() -> new IllegalArgumentException("Analyzer connection is no longer available"));
        AnalyzerResults held = analyzerResultsService
                .readAnalyzerResultsForUpdate(requireText(resultId, "Result ID is required"));
        if (held == null || !analyzer.getId().equals(held.getAnalyzerId())
                || !analyzer.getBridgeConnectionId().equals(held.getSourceConnectionId())) {
            throw new IllegalArgumentException("Held result does not belong to this analyzer connection");
        }
        if (!held.isReadOnly() || held.getImportIssueReason() == null || held.getImportIssueReason().isBlank()) {
            throw new IllegalStateException("Only a held normalized result can be reprocessed");
        }
        AnalyzerNormalizedResultContract.Result original = AnalyzerNormalizedResultContract
                .parseStoredObservation(held.getSourcePayload(), held.getAccessionNumber(), fhirContext);
        AnalyzerNormalizedResultContract contract = new AnalyzerNormalizedResultContract(held.getSourceMessageId(),
                held.getSourceConnectionId(), held.getSourceProfileId(), held.getSourceProfileRevision(),
                held.getSourceProtocol(), List.of(original));
        requireMatchingProfile(analyzer, contract);
        List<AnalyzerResults> mapped = mapResults(contract, analyzer);
        if (mapped.isEmpty()) {
            held.setSysUserId(effectiveActor);
            analyzerResultsService.delete(held);
            return new AnalyzerNormalizedResultImportSummary(analyzerId, 0, 0, 0);
        }
        AnalyzerResults updated = mapped.get(0);
        updated.setId(held.getId());
        updated.setLastupdated(held.getLastupdated());
        updated.setSysUserId(effectiveActor);
        analyzerResultsService.update(updated);
        if (updated.isReadOnly()) {
            return new AnalyzerNormalizedResultImportSummary(analyzerId, 1, 1, 0);
        }
        int controls = updated.getIsControl() && processControl(updated, analyzer) ? 1 : 0;
        // This explicit, audited row transition never deletes or bypasses a delivery
        // receipt.
        return new AnalyzerNormalizedResultImportSummary(analyzerId, 1, 0, controls);
    }

    private List<AnalyzerResults> mapResults(AnalyzerNormalizedResultContract contract, Analyzer analyzer) {
        AnalyzerProfileBinding profileBinding = analyzer.getPinnedProfileBinding();
        if (profileBinding == null || profileBinding.getId() == null) {
            throw new AnalyzerNormalizedResultImportException("analyzer.fhirImport.error.missingSiteBinding",
                    "Analyzer has no profile-scoped site binding");
        }
        AnalyzerSiteBindingSnapshot binding = siteBindingService.findCurrentByProfileBindingId(profileBinding.getId())
                .orElseThrow(() -> new AnalyzerNormalizedResultImportException(
                        "analyzer.fhirImport.error.missingSiteBinding", "Analyzer site binding does not exist"));
        Map<String, AnalyzerSiteBindingTest> testsBySource = binding.tests().stream()
                .collect(Collectors.toMap(row -> row.getId().getSourceRowKey(), row -> row));
        Map<ResultKey, AnalyzerSiteBindingResult> resultsBySource = binding.results().stream().collect(Collectors
                .toMap(row -> new ResultKey(row.getId().getSourceRowKey(), row.getId().getRawValue()), row -> row));
        Set<String> sourcesWithResultMappings = binding.results().stream().map(row -> row.getId().getSourceRowKey())
                .collect(Collectors.toSet());
        return contract.results().stream().map(result -> toStagedResult(contract, result, analyzer, testsBySource,
                resultsBySource, sourcesWithResultMappings)).flatMap(Optional::stream).toList();
    }

    private Optional<AnalyzerResults> toStagedResult(AnalyzerNormalizedResultContract contract,
            AnalyzerNormalizedResultContract.Result result, Analyzer analyzer,
            Map<String, AnalyzerSiteBindingTest> testsBySource,
            Map<ResultKey, AnalyzerSiteBindingResult> resultsBySource, Set<String> sourcesWithResultMappings) {
        AnalyzerResults row = new AnalyzerResults();
        row.setAnalyzerId(analyzer.getId());
        row.setAccessionNumber(result.accessionNumber());
        row.setTestName(result.rawTestCode());
        row.setResult(result.rawValue());
        row.setUnits(result.units());
        row.setResultType(result.resultType());
        row.setCompleteDate(
                result.completeDate() != null ? result.completeDate() : new Timestamp(System.currentTimeMillis()));
        row.setIsControl("CONTROL".equals(result.classification()));
        row.setLotNumber(result.lotNumber());
        row.setControlLevel(result.controlLevel());
        copySourceContext(row, contract, result);

        AnalyzerSiteBindingTest testMapping = testsBySource.get(result.rawTestCode());
        if (testMapping == null) {
            hold(row, AnalyzerResults.IMPORT_ISSUE_UNKNOWN_TEST);
            return Optional.of(row);
        }
        if (testMapping.getMappingState() == AnalyzerSiteBindingMappingState.EXCLUDED) {
            return Optional.empty();
        }
        if (testMapping.getMappingState() != AnalyzerSiteBindingMappingState.BOUND || testMapping.getTestId() == null) {
            hold(row, AnalyzerResults.IMPORT_ISSUE_TEST_MAPPING_NOT_READY);
            return Optional.of(row);
        }
        row.setTestId(testMapping.getTestId());

        ResultKey resultKey = new ResultKey(result.rawTestCode(), result.rawValue());
        AnalyzerSiteBindingResult resultMapping = resultsBySource.get(resultKey);
        if (resultMapping == null) {
            if (sourcesWithResultMappings.contains(result.rawTestCode())) {
                hold(row, AnalyzerResults.IMPORT_ISSUE_UNKNOWN_RESULT_VALUE);
            }
            return Optional.of(row);
        }
        if (resultMapping.getMappingState() == AnalyzerSiteBindingMappingState.EXCLUDED) {
            return Optional.empty();
        }
        if (resultMapping.getMappingState() != AnalyzerSiteBindingMappingState.BOUND
                || resultMapping.getTestResultId() == null) {
            hold(row, AnalyzerResults.IMPORT_ISSUE_RESULT_MAPPING_NOT_READY);
            return Optional.of(row);
        }

        TestResult option = testResultService.get(resultMapping.getTestResultId());
        if (option == null || option.getValue() == null || option.getTestResultType() == null) {
            hold(row, AnalyzerResults.IMPORT_ISSUE_INVALID_RESULT_MAPPING);
            return Optional.of(row);
        }
        row.setResult(option.getValue());
        row.setResultType(option.getTestResultType());
        return Optional.of(row);
    }

    private void copySourceContext(AnalyzerResults row, AnalyzerNormalizedResultContract contract,
            AnalyzerNormalizedResultContract.Result result) {
        row.setSourceMessageId(contract.messageId());
        row.setSourceConnectionId(contract.bridgeConnectionId());
        row.setSourceProfileId(contract.profileId());
        row.setSourceProfileRevision(contract.profileRevision());
        row.setSourceProtocol(contract.sourceProtocol());
        row.setSourceTransport(result.sourceTransport());
        row.setRawTestCode(result.rawTestCode());
        row.setRawResultValue(result.rawValue());
        row.setResultClassification(result.classification());
        row.setRecognitionMode(result.recognitionMode());
        row.setRecognitionOutcome(result.recognitionOutcome());
        row.setRecognitionFingerprint(result.recognitionFingerprint());
        row.setSourcePayload(result.sourcePayload());
    }

    private void requireMatchingProfile(Analyzer analyzer, AnalyzerNormalizedResultContract contract) {
        AnalyzerProfileBinding pinned = analyzer.getPinnedProfileBinding();
        if (pinned == null || !contract.profileId().equals(pinned.getProfileId())
                || contract.profileRevision() != pinned.getProfileRevision()) {
            throw new AnalyzerNormalizedResultImportException("analyzer.fhirImport.error.profileMismatch",
                    "Normalized traffic profile does not match the analyzer pin");
        }
    }

    private boolean processControl(AnalyzerResults row, Analyzer analyzer) {
        BigDecimal value;
        try {
            value = new BigDecimal(row.getResult());
        } catch (NumberFormatException exception) {
            LogEvent.logWarn(CLASS_NAME, "processControl",
                    "Control result is not numeric and remains staged for review");
            return false;
        }
        LocalDateTime timestamp = row.getCompleteDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        qcResultProcessingService.processQCResult(analyzer.getId(), row.getTestId(), row.getAccessionNumber(),
                row.getLotNumber(), row.getControlLevel(), value, row.getUnits(), timestamp);
        return true;
    }

    private void hold(AnalyzerResults row, String reason) {
        row.setReadOnly(true);
        row.setImportIssueReason(reason);
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private record ResultKey(String sourceRowKey, String rawValue) {
    }
}
