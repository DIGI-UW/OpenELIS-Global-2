package org.openelisglobal.analyzerimport.service;

import ca.uhn.fhir.context.FhirContext;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.openelisglobal.analyzer.service.AnalyzerMappingCatalogService;
import org.openelisglobal.analyzer.service.AnalyzerMappingCatalogState;
import org.openelisglobal.analyzer.service.AnalyzerMappingConfirmationService;
import org.openelisglobal.analyzer.service.AnalyzerMappingService;
import org.openelisglobal.analyzer.service.AnalyzerMappingSnapshot;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.service.QCResultProcessingService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingResult;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingTest;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfilePin;
import org.openelisglobal.analyzerimport.dao.AnalyzerDeliveryReceiptDAO;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerDeliveryReceipt;
import org.openelisglobal.analyzerresults.service.AnalyzerResultPlacementService;
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
    private final AnalyzerMappingService mappingService;
    private final AnalyzerMappingConfirmationService confirmationService;
    private final AnalyzerMappingCatalogService mappingCatalogService;
    private final AnalyzerResultsService analyzerResultsService;
    private final TestResultService testResultService;
    private final QCResultProcessingService qcResultProcessingService;
    private final FhirContext fhirContext;
    private final AnalyzerDeliveryReceiptDAO receiptDAO;
    private final AnalyzerResultPlacementService placementService;

    public AnalyzerNormalizedResultImportServiceImpl(AnalyzerService analyzerService,
            AnalyzerMappingService mappingService, AnalyzerResultsService analyzerResultsService,
            TestResultService testResultService, QCResultProcessingService qcResultProcessingService,
            FhirContext fhirContext, AnalyzerDeliveryReceiptDAO receiptDAO,
            AnalyzerMappingConfirmationService confirmationService, AnalyzerMappingCatalogService mappingCatalogService,
            AnalyzerResultPlacementService placementService) {
        this.analyzerService = analyzerService;
        this.mappingService = mappingService;
        this.analyzerResultsService = analyzerResultsService;
        this.testResultService = testResultService;
        this.qcResultProcessingService = qcResultProcessingService;
        this.fhirContext = fhirContext;
        this.receiptDAO = receiptDAO;
        this.confirmationService = confirmationService;
        this.mappingCatalogService = mappingCatalogService;
        this.placementService = placementService;
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
            // The same receipt the first acceptance returned, so a sender that
            // retried because it never saw the first answer gets an identical
            // response rather than a second acceptance.
            return new AnalyzerNormalizedResultImportSummary(receipt.getId(), receipt.getAnalyzerId(),
                    receipt.getResultsStaged(), receipt.getResultsHeld(), receipt.getControlsProcessed());
        }
        requireMatchingProfile(analyzer, contract);

        List<AnalyzerResults> staged = mapResults(contract, analyzer);
        if (!staged.isEmpty()) {
            analyzerResultsService.insertAnalyzerResults(staged, effectiveActor);
        }

        int controlsProcessed = 0;
        for (AnalyzerResults row : staged) {
            if (!row.getIsControl() || row.isReadOnly() || row.getTestId() == null) {
                continue;
            }
            QCResultProcessingService.Outcome outcome = processControl(row, analyzer);
            if (outcome == QCResultProcessingService.Outcome.RECORDED) {
                controlsProcessed++;
            } else if (outcome == QCResultProcessingService.Outcome.NO_TARGET) {
                hold(row, AnalyzerResults.IMPORT_ISSUE_QC_TARGET_MISSING);
                row.setSysUserId(effectiveActor);
                analyzerResultsService.update(row);
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
        receipt.setBundleJson(fhirContext.newJsonParser().encodeResourceToString(bundle));
        receiptDAO.insert(receipt);
        return new AnalyzerNormalizedResultImportSummary(receipt.getId(), analyzer.getId(), staged.size(), held,
                controlsProcessed);
    }

    @Override
    @Transactional
    public int recoverHeldMappingResults(String analyzerId, String actor) {
        String effectiveActor = requireText(actor, "Recovery actor is required");
        Analyzer candidate = analyzerService.getWithMapping(analyzerId)
                .orElseThrow(() -> new IllegalArgumentException("Analyzer not found"));
        if (candidate.getBridgeConnectionId() == null) {
            return 0;
        }
        Analyzer analyzer = analyzerService.findByBridgeConnectionIdForUpdate(candidate.getBridgeConnectionId())
                .orElseThrow(() -> new IllegalArgumentException("Analyzer connection not found"));
        AnalyzerProfilePin pin = analyzer.getPinnedProfile();
        int recoveredCount = 0;
        for (AnalyzerResults held : analyzerResultsService.findHeldMappingResultsByAnalyzer(analyzerId)) {
            if (pin == null || !pin.getProfileId().equals(held.getSourceProfileId())
                    || !Integer.valueOf(pin.getProfileRevision()).equals(held.getSourceProfileRevision())
                    || !analyzer.getBridgeConnectionId().equals(held.getSourceConnectionId())
                    || held.getSourcePayload() == null) {
                continue;
            }
            Observation observation = fhirContext.newJsonParser().parseResource(Observation.class,
                    held.getSourcePayload());
            var source = AnalyzerNormalizedResultContract.parseResult(observation,
                    Map.of(observation.getSpecimen().getReference(), specimenIdOf(held)), Map.of(),
                    observation.getDevice().getReference(), fhirContext);
            var contract = new AnalyzerNormalizedResultContract(held.getSourceMessageId(), held.getSourceConnectionId(),
                    held.getSourceProfileId(), held.getSourceProfileRevision(), held.getSourceProtocol(),
                    List.of(source));
            List<AnalyzerResults> mapped = mapResults(contract, analyzer);
            if (mapped.isEmpty()) {
                continue;
            }
            AnalyzerResults recovered = mapped.get(0);
            if (!recovered.isReadOnly() && recovered.getIsControl()
                    && processControl(recovered, analyzer) == QCResultProcessingService.Outcome.NO_TARGET) {
                hold(recovered, AnalyzerResults.IMPORT_ISSUE_QC_TARGET_MISSING);
            }
            if (recovered.isReadOnly() && Objects.equals(held.getImportIssueReason(), recovered.getImportIssueReason())
                    && Objects.equals(held.getTestId(), recovered.getTestId())) {
                continue;
            }
            recovered.setId(held.getId());
            recovered.setInstrumentPatientId(held.getInstrumentPatientId());
            recovered.setInstrumentPatientName(held.getInstrumentPatientName());
            recovered.setLastupdated(held.getLastupdated());
            recovered.setSysUserId(effectiveActor);
            analyzerResultsService.update(recovered);
            if (!recovered.isReadOnly()) {
                recoveredCount++;
            }
        }
        return recoveredCount;
    }

    /** A row staged before tube IDs were kept has only its accession. */
    private static String specimenIdOf(AnalyzerResults row) {
        return row.getInstrumentSpecimenId() != null ? row.getInstrumentSpecimenId() : row.getAccessionNumber();
    }

    private List<AnalyzerResults> mapResults(AnalyzerNormalizedResultContract contract, Analyzer analyzer) {
        if (analyzer.getMapping() == null || analyzer.getMapping().getId() == null) {
            throw new AnalyzerNormalizedResultImportException("analyzer.fhirImport.error.missingMapping",
                    "Analyzer has no mapping in force");
        }
        AnalyzerMappingSnapshot binding = mappingService.findById(analyzer.getMapping().getId()).orElseThrow(
                () -> new AnalyzerNormalizedResultImportException("analyzer.fhirImport.error.missingMapping",
                        "Analyzer mapping does not exist"));
        Map<String, AnalyzerMappingTest> testsBySource = binding.tests().stream()
                .collect(Collectors.toMap(row -> row.getId().getSourceRowKey(), row -> row));
        Map<ResultKey, AnalyzerMappingResult> resultsBySource = binding.results().stream().collect(Collectors
                .toMap(row -> new ResultKey(row.getId().getSourceRowKey(), row.getId().getRawValue()), row -> row));
        Set<String> sourcesWithResultMappings = binding.results().stream().map(row -> row.getId().getSourceRowKey())
                .collect(Collectors.toSet());
        AnalyzerMappingCatalogState.Validation catalog = AnalyzerMappingCatalogState.load(mappingCatalogService)
                .validate(binding);
        Map<String, Boolean> confirmedByRecognition = new HashMap<>();
        return contract.results().stream().map(result -> {
            boolean confirmed = confirmedByRecognition.computeIfAbsent(result.recognitionFingerprint(),
                    fingerprint -> confirmationService.hasMatchingConfirmation(binding, fingerprint));
            return toStagedResult(contract, result, analyzer, testsBySource, resultsBySource, sourcesWithResultMappings,
                    confirmed, catalog);
        }).flatMap(Optional::stream).toList();
    }

    private Optional<AnalyzerResults> toStagedResult(AnalyzerNormalizedResultContract contract,
            AnalyzerNormalizedResultContract.Result result, Analyzer analyzer,
            Map<String, AnalyzerMappingTest> testsBySource, Map<ResultKey, AnalyzerMappingResult> resultsBySource,
            Set<String> sourcesWithResultMappings, boolean mappingConfirmed,
            AnalyzerMappingCatalogState.Validation catalog) {
        AnalyzerResults row = new AnalyzerResults();
        row.setAnalyzerId(analyzer.getId());
        row.setInstrumentSpecimenId(result.accessionNumber());
        row.setInstrumentPatientId(result.instrumentPatientId());
        row.setInstrumentPatientName(result.instrumentPatientName());
        row.setAccessionNumber(placementService.accessionFor(result.accessionNumber()));
        row.setTestName(result.rawTestCode());
        row.setResult(result.reportedValue());
        row.setUnits(result.units());
        row.setResultType(result.resultType());
        row.setCompleteDate(
                result.completeDate() != null ? result.completeDate() : new Timestamp(System.currentTimeMillis()));
        row.setIsControl("CONTROL".equals(result.classification()));
        row.setLotNumber(result.lotNumber());
        row.setControlLevel(result.controlLevel());
        copySourceContext(row, contract, result);
        AnalyzerMappingTest testMapping = testsBySource.get(result.rawTestCode());
        if (testMapping == null) {
            hold(row, AnalyzerResults.IMPORT_ISSUE_UNKNOWN_TEST);
            return Optional.of(row);
        }
        if (!mappingConfirmed) {
            hold(row, AnalyzerResults.IMPORT_ISSUE_TEST_MAPPING_NOT_READY);
            return Optional.of(row);
        }

        if (testMapping.getMappingState() == AnalyzerMappingState.EXCLUDED) {
            return Optional.empty();
        }
        if (testMapping.getMappingState() != AnalyzerMappingState.BOUND
                || !catalog.isCurrentBoundTest(result.rawTestCode())) {
            hold(row, AnalyzerResults.IMPORT_ISSUE_TEST_MAPPING_NOT_READY);
            return Optional.of(row);
        }
        row.setTestId(testMapping.getTestId());
        row.setComponentId(testMapping.getComponentId());
        if (result.runFailed()) {
            hold(row, AnalyzerResults.IMPORT_ISSUE_RUN_FAILED);
            return Optional.of(row);
        }

        ResultKey resultKey = new ResultKey(result.rawTestCode(), result.rawValue());
        AnalyzerMappingResult resultMapping = resultsBySource.get(resultKey);
        if (resultMapping == null) {
            if (sourcesWithResultMappings.contains(result.rawTestCode())) {
                hold(row, AnalyzerResults.IMPORT_ISSUE_UNKNOWN_RESULT_VALUE);
            }
            return Optional.of(row);
        }
        if (resultMapping.getMappingState() == AnalyzerMappingState.EXCLUDED) {
            return Optional.empty();
        }
        if (resultMapping.getMappingState() != AnalyzerMappingState.BOUND || resultMapping.getTestResultId() == null) {
            hold(row, AnalyzerResults.IMPORT_ISSUE_RESULT_MAPPING_NOT_READY);
            return Optional.of(row);
        }

        if (!catalog.isCurrentBoundResult(result.rawTestCode(), result.rawValue())) {
            hold(row, AnalyzerResults.IMPORT_ISSUE_INVALID_RESULT_MAPPING);
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
        row.setInstrumentNote(result.note());
    }

    private void requireMatchingProfile(Analyzer analyzer, AnalyzerNormalizedResultContract contract) {
        AnalyzerProfilePin pinned = analyzer.getPinnedProfile();
        if (pinned == null || !contract.profileId().equals(pinned.getProfileId())
                || contract.profileRevision() != pinned.getProfileRevision()) {
            throw new AnalyzerNormalizedResultImportException("analyzer.fhirImport.error.profileMismatch",
                    "Normalized traffic profile does not match the analyzer pin");
        }
    }

    /**
     * Sends a control to operational QC: a number to the statistical path, an
     * answer the mapping resolved to a dictionary entry to be judged against its QC
     * target. Null when the control is neither and stays staged for review.
     */
    private QCResultProcessingService.Outcome processControl(AnalyzerResults row, Analyzer analyzer) {
        LocalDateTime timestamp = row.getCompleteDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        if ("D".equals(row.getResultType())) {
            return qcResultProcessingService.processQualitativeQCResult(analyzer.getId(), row.getTestId(),
                    row.getComponentId(), row.getAccessionNumber(), row.getLotNumber(), row.getControlLevel(),
                    row.getResult(), timestamp);
        }
        BigDecimal value;
        try {
            value = new BigDecimal(row.getResult());
        } catch (NumberFormatException exception) {
            LogEvent.logWarn(CLASS_NAME, "processControl",
                    "Control result is neither numeric nor a mapped answer and remains staged for review");
            return null;
        }
        qcResultProcessingService.processQCResult(analyzer.getId(), row.getTestId(), row.getAccessionNumber(),
                row.getLotNumber(), row.getControlLevel(), value, row.getUnits(), timestamp);
        return QCResultProcessingService.Outcome.RECORDED;
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
