package org.openelisglobal.analyzerimport.service;

import ca.uhn.fhir.context.FhirContext;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
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
import org.openelisglobal.analyzer.service.AnalyzerMappingRowKey;
import org.openelisglobal.analyzer.service.AnalyzerMappingService;
import org.openelisglobal.analyzer.service.AnalyzerMappingSnapshot;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.service.QCResultProcessingService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingResult;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingTest;
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

        List<AnalyzerResults> staged = mapResults(contract, analyzer);
        if (!staged.isEmpty()) {
            analyzerResultsService.insertAnalyzerResults(staged, effectiveActor);
        }

        int controlsProcessed = processControls(staged, analyzer, effectiveActor);
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
        int recoveredCount = 0;
        // Retry the stored normalized observation using the current local mapping.
        for (AnalyzerResults held : analyzerResultsService.findHeldMappingResultsByAnalyzer(analyzerId)) {
            if (AnalyzerResults.IMPORT_ISSUE_SETUP_CAPTURE.equals(held.getImportIssueReason())) {
                continue;
            }
            if (held.getSourceProfileRevision() == null
                    || !analyzer.getBridgeConnectionId().equals(held.getSourceConnectionId())
                    || held.getSourcePayload() == null) {
                continue;
            }
            // The stored observation already contains the Bridge's interpretation. Recovery
            // applies only local mapping and placement; historical parser snapshots remain
            // audit.
            Observation observation = fhirContext.newJsonParser().parseResource(Observation.class,
                    held.getSourcePayload());
            // Early retained observations omitted the version; the accepted receipt still
            // has it.
            String version = receiptDAO.findByDelivery(held.getSourceConnectionId(), held.getSourceMessageId())
                    .map(receipt -> fhirContext.newJsonParser().parseResource(Bundle.class, receipt.getBundleJson()))
                    .map(AnalyzerNormalizedResultContract::versionOf).orElse(null);
            var source = version == null
                    ? AnalyzerNormalizedResultContract.parseResult(observation,
                            Map.of(observation.getSpecimen().getReference(), specimenIdOf(held)), Map.of(),
                            observation.getDevice().getReference(), fhirContext)
                    : AnalyzerNormalizedResultContract.parseResult(observation,
                            Map.of(observation.getSpecimen().getReference(), specimenIdOf(held)), Map.of(),
                            observation.getDevice().getReference(), fhirContext, version);
            var contract = new AnalyzerNormalizedResultContract(held.getSourceMessageId(), held.getSourceConnectionId(),
                    held.getSourceProfileId(), held.getSourceProfileRevision(), held.getSourceProtocol(),
                    List.of(source), List.of());
            List<AnalyzerResults> mapped = mapResults(contract, analyzer);
            // The lab now excludes this record or its value: a new delivery would not
            // stage it, so the held row leaves review. Its bundle stays on the receipt.
            if (mapped.isEmpty()) {
                analyzerResultsService.delete(held.getId(), effectiveActor);
                continue;
            }
            // A row held before its record mapped stands for the whole record, which
            // may now stage a number and a call; a row held on a target takes the
            // row for that same target.
            boolean wholeRecord = held.getTestId() == null;
            Optional<AnalyzerResults> match = wholeRecord ? mapped.stream().findFirst()
                    : mapped.stream().filter(row -> Objects.equals(held.getComponentId(), row.getComponentId()))
                            .findFirst();
            if (match.isEmpty()) {
                continue;
            }
            AnalyzerResults recovered = match.get();
            if (wholeRecord && mapped.size() > 1) {
                List<AnalyzerResults> others = mapped.stream().filter(row -> row != recovered).toList();
                others.forEach(row -> {
                    row.setInstrumentPatientId(held.getInstrumentPatientId());
                    row.setInstrumentPatientName(held.getInstrumentPatientName());
                    row.setInstrumentSampleFlags(held.getInstrumentSampleFlags());
                    row.setArrivalReading(held.getArrivalReading());
                });
                analyzerResultsService.insertAnalyzerResults(others, effectiveActor);
                processControls(others, analyzer, effectiveActor);
                recoveredCount += (int) others.stream().filter(row -> !row.isReadOnly()).count();
            }
            if (!recovered.isReadOnly() && isPrimaryControl(recovered)) {
                holdUnrecordedControl(recovered, processControl(recovered, analyzer));
            }
            if (recovered.isReadOnly() && Objects.equals(held.getImportIssueReason(), recovered.getImportIssueReason())
                    && Objects.equals(held.getTestId(), recovered.getTestId())) {
                continue;
            }
            recovered.setId(held.getId());
            recovered.setInstrumentPatientId(held.getInstrumentPatientId());
            recovered.setInstrumentPatientName(held.getInstrumentPatientName());
            recovered.setInstrumentSampleFlags(held.getInstrumentSampleFlags());
            recovered.setArrivalReading(held.getArrivalReading());
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
        if ("SETUP".equals(contract.deliveryPurpose()) || analyzer.getStatus() == Analyzer.AnalyzerStatus.VALIDATION
                || (analyzer.getStatus() == Analyzer.AnalyzerStatus.SETUP && !analyzer.isActive())) {
            return contract.results().stream()
                    .map(result -> held(contract, result, analyzer, AnalyzerResults.IMPORT_ISSUE_SETUP_CAPTURE))
                    .toList();
        }
        if (analyzer.getMapping() == null || analyzer.getMapping().getId() == null) {
            throw new AnalyzerNormalizedResultImportException("analyzer.fhirImport.error.missingMapping",
                    "Analyzer has no mapping in force");
        }
        AnalyzerMappingSnapshot binding = mappingService.findById(analyzer.getMapping().getId()).orElseThrow(
                () -> new AnalyzerNormalizedResultImportException("analyzer.fhirImport.error.missingMapping",
                        "Analyzer mapping does not exist"));
        // Canonical record codes are scoped to their profile ID, not its parser
        // revision.
        // An unrelated profile needs its own approved binding; retain that work for
        // mapping.
        if (!Objects.equals(binding.mapping().getProfileId(), contract.profileId())) {
            return contract.results().stream()
                    .map(result -> held(contract, result, analyzer, AnalyzerResults.IMPORT_ISSUE_UNKNOWN_TEST))
                    .toList();
        }
        Mapping mapping = new Mapping(
                binding.tests().stream().collect(Collectors.toMap(AnalyzerMappingRowKey::of, row -> row)),
                binding.results().stream().collect(Collectors.toMap(
                        row -> new ResultKey(AnalyzerMappingRowKey.of(row), row.getId().getRawValue()), row -> row)),
                binding.results().stream().map(AnalyzerMappingRowKey::of).collect(Collectors.toSet()),
                AnalyzerMappingCatalogState.load(mappingCatalogService).validate(binding));
        boolean confirmed = confirmationService.hasMatchingConfirmation(binding);
        return contract.results().stream()
                .flatMap(result -> toStagedResults(contract, result, analyzer, mapping, confirmed).stream()).toList();
    }

    /**
     * Stages one reported record. A record carrying a number and a call stages two
     * rows: the number on the record's target and the call on its call target.
     */
    private List<AnalyzerResults> toStagedResults(AnalyzerNormalizedResultContract contract,
            AnalyzerNormalizedResultContract.Result result, Analyzer analyzer, Mapping mapping,
            boolean mappingConfirmed) {
        AnalyzerMappingRowKey record = new AnalyzerMappingRowKey(result.rawTestCode(), result.subIdentity());
        AnalyzerMappingTest testMapping = mapping.tests().get(record);
        if (testMapping == null) {
            return List.of(held(contract, result, analyzer, AnalyzerResults.IMPORT_ISSUE_UNKNOWN_TEST));
        }
        AnalyzerMappingTest assay = mapping.tests().get(AnalyzerMappingRowKey.main(result.rawTestCode()));
        if (assay != null && !assay.isEnabled()) {
            return List.of(held(contract, result, analyzer, AnalyzerResults.IMPORT_ISSUE_ASSAY_NOT_ENABLED));
        }
        if (!mappingConfirmed) {
            return List.of(held(contract, result, analyzer, AnalyzerResults.IMPORT_ISSUE_TEST_MAPPING_NOT_READY));
        }
        if (testMapping.getMappingState() == AnalyzerMappingState.EXCLUDED) {
            return List.of();
        }
        if (testMapping.getMappingState() != AnalyzerMappingState.BOUND || !mapping.catalog().isCurrentBoundTest(record)
                || !mapping.catalog().acceptsTestMeaning(testMapping.getTestId(), result.testCodings())) {
            return List.of(held(contract, result, analyzer, AnalyzerResults.IMPORT_ISSUE_TEST_MAPPING_NOT_READY));
        }
        if (result.runFailed()) {
            AnalyzerResults row = staged(contract, result, analyzer, result.reportedValue());
            row.setTestId(testMapping.getTestId());
            row.setComponentId(testMapping.getComponentId());
            hold(row, AnalyzerResults.IMPORT_ISSUE_RUN_FAILED);
            return List.of(row);
        }

        // A number and a call need a place each; with no call target the record is held
        // whole, and recovers into both once the call has one.
        if (result.number() != null && result.call() != null && testMapping.getCallComponentId() == null) {
            return List.of(held(contract, result, analyzer, AnalyzerResults.IMPORT_ISSUE_TEST_MAPPING_NOT_READY));
        }
        String callTarget = testMapping.getCallComponentId() != null ? testMapping.getCallComponentId()
                : testMapping.getComponentId();
        if ((result.number() != null
                && !mapping.catalog().acceptsValue(testMapping.getTestId(), testMapping.getComponentId(), true))
                || (result.call() != null
                        && !mapping.catalog().acceptsValue(testMapping.getTestId(), callTarget, false))) {
            return List.of(held(contract, result, analyzer, AnalyzerResults.IMPORT_ISSUE_TEST_MAPPING_NOT_READY));
        }
        List<AnalyzerResults> rows = new ArrayList<>();
        if (result.number() != null) {
            AnalyzerResults number = staged(contract, result, analyzer, result.reportedNumber());
            number.setTestId(testMapping.getTestId());
            number.setComponentId(testMapping.getComponentId());
            number.setResultType("N");
            rows.add(number);
        }
        if (result.call() != null) {
            // A record whose mapping names a call target always sends its call there, so a
            // viral load's Not detected never lands on the number's place.
            boolean hasCallTarget = testMapping.getCallComponentId() != null;
            String target = hasCallTarget ? testMapping.getCallComponentId() : testMapping.getComponentId();
            AnalyzerResults call = staged(contract, result, analyzer, result.call());
            call.setTestId(testMapping.getTestId());
            call.setComponentId(target);
            call.setResultType("A");
            AnalyzerMappingResult answer = mapping.results().get(new ResultKey(record, result.call()));
            if (answer != null && !bindAnswer(call, answer, record, result.call(), mapping, result.answerCodings())) {
                call = null;
            } else if (answer == null && mapping.recordsWithAnswers().contains(record)) {
                hold(call, AnalyzerResults.IMPORT_ISSUE_UNKNOWN_RESULT_VALUE);
            }
            if (call != null) {
                rows.add(call);
            }
        }
        if (result.number() == null && result.call() == null) {
            AnalyzerResults row = staged(contract, result, analyzer, result.reportedValue());
            row.setTestId(testMapping.getTestId());
            row.setComponentId(testMapping.getComponentId());
            rows.add(row);
        }
        return rows;
    }

    /**
     * Applies an answer mapping to a staged row. False when the operator excluded
     * that value, so the row is not staged at all.
     */
    private boolean bindAnswer(AnalyzerResults row, AnalyzerMappingResult answer, AnalyzerMappingRowKey record,
            String value, Mapping mapping, List<org.openelisglobal.terminology.Coding> deliveredCodings) {
        if (answer.getMappingState() == AnalyzerMappingState.EXCLUDED) {
            return false;
        }
        if (answer.getMappingState() != AnalyzerMappingState.BOUND || answer.getTestResultId() == null) {
            hold(row, AnalyzerResults.IMPORT_ISSUE_RESULT_MAPPING_NOT_READY);
            return true;
        }
        if (!mapping.catalog().isCurrentBoundResult(record, value) || !mapping.catalog()
                .acceptsAnswerMeaning(row.getTestId(), answer.getTestResultId(), deliveredCodings)) {
            hold(row, AnalyzerResults.IMPORT_ISSUE_INVALID_RESULT_MAPPING);
            return true;
        }
        TestResult option = testResultService.get(answer.getTestResultId());
        if (option == null || option.getValue() == null || option.getTestResultType() == null) {
            hold(row, AnalyzerResults.IMPORT_ISSUE_INVALID_RESULT_MAPPING);
            return true;
        }
        row.setResult(option.getValue());
        row.setResultType(option.getTestResultType());
        return true;
    }

    private AnalyzerResults held(AnalyzerNormalizedResultContract contract,
            AnalyzerNormalizedResultContract.Result result, Analyzer analyzer, String reason) {
        AnalyzerResults row = staged(contract, result, analyzer, result.reportedValue());
        hold(row, reason);
        return row;
    }

    private AnalyzerResults staged(AnalyzerNormalizedResultContract contract,
            AnalyzerNormalizedResultContract.Result result, Analyzer analyzer, String value) {
        AnalyzerResults row = new AnalyzerResults();
        row.setAnalyzerId(analyzer.getId());
        row.setInstrumentSpecimenId(result.accessionNumber());
        row.setInstrumentPatientId(result.instrumentPatientId());
        row.setInstrumentPatientName(result.instrumentPatientName());
        row.setAccessionNumber(placementService.accessionFor(result.accessionNumber()));
        row.setTestName(result.rawTestCode());
        row.setResult(value);
        row.setUnits(result.units());
        row.setResultType(result.resultType());
        row.setCompleteDate(
                result.completeDate() != null ? result.completeDate() : new Timestamp(System.currentTimeMillis()));
        row.setIsControl("CONTROL".equals(result.classification()));
        row.setControlLevel(result.controlLevel());
        copySourceContext(row, contract, result);
        return row;
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
        row.setRawSubIdentity(result.subIdentity());
        row.setRawResultValue(result.rawValue());
        row.setResultClassification(result.classification());
        row.setRecognitionMode(result.recognitionMode());
        row.setRecognitionOutcome(result.recognitionOutcome());
        row.setRecognitionFingerprint(result.recognitionFingerprint());
        row.setSourcePayload(result.sourcePayload());
        row.setInstrumentNote(result.note());
        row.setInstrumentFlags(result.flags());
        row.setInstrumentSampleFlags(
                contract.sampleFlags().isEmpty() ? null : String.join(", ", contract.sampleFlags()));
        row.setAssayName(result.assayName());
        row.setAssayVersion(result.assayVersion());
        row.setInstrumentOperator(result.operator());
    }

    /** Record mapped controls against local QC targets. */
    private int processControls(List<AnalyzerResults> staged, Analyzer analyzer, String actor) {
        int recorded = 0;
        for (AnalyzerResults row : staged) {
            if (row.getId() == null || row.isReadOnly() || !isPrimaryControl(row)) {
                continue;
            }
            QCResultProcessingService.Outcome outcome = processControl(row, analyzer);
            if (outcome == QCResultProcessingService.Outcome.RECORDED) {
                recorded++;
            } else {
                holdUnrecordedControl(row, outcome);
                row.setSysUserId(actor);
                analyzerResultsService.update(row);
            }
        }
        return recorded;
    }

    private boolean isPrimaryControl(AnalyzerResults row) {
        if (!row.getIsControl() || row.getTestId() == null) {
            return false;
        }
        // The approved local target determines the measurement. Raw vendor record names
        // do not.
        return row.getComponentId() == null || mappingCatalogService.getActiveComponents(row.getTestId()).stream()
                .anyMatch(component -> component.primary() && component.id().equals(row.getComponentId()));
    }

    private void holdUnrecordedControl(AnalyzerResults row, QCResultProcessingService.Outcome outcome) {
        if (outcome == QCResultProcessingService.Outcome.RECORDED) {
            return;
        }
        hold(row,
                outcome == QCResultProcessingService.Outcome.NO_LOT ? AnalyzerResults.IMPORT_ISSUE_QC_LOT_MISSING
                        : outcome == QCResultProcessingService.Outcome.NO_TARGET
                                ? AnalyzerResults.IMPORT_ISSUE_QC_TARGET_MISSING
                                : AnalyzerResults.IMPORT_ISSUE_QC_VALUE_UNSUPPORTED);
    }

    private QCResultProcessingService.Outcome processControl(AnalyzerResults row, Analyzer analyzer) {
        LocalDateTime timestamp = row.getCompleteDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        if ("D".equals(row.getResultType())) {
            return qcResultProcessingService.processQualitativeQCResult(analyzer.getId(), row.getTestId(),
                    row.getComponentId(), specimenIdOf(row), row.getControlLevel(), row.getResult(), timestamp);
        }
        BigDecimal value;
        try {
            value = new BigDecimal(row.getResult());
        } catch (NumberFormatException exception) {
            LogEvent.logWarn(CLASS_NAME, "processControl",
                    "Control result is neither numeric nor a mapped answer and remains staged for review");
            return QCResultProcessingService.Outcome.UNSUPPORTED_VALUE;
        }
        return qcResultProcessingService.processQCResult(analyzer.getId(), row.getTestId(), specimenIdOf(row),
                row.getControlLevel(), value, row.getUnits(), timestamp);
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

    private record ResultKey(AnalyzerMappingRowKey record, String rawValue) {
    }

    private record Mapping(Map<AnalyzerMappingRowKey, AnalyzerMappingTest> tests,
            Map<ResultKey, AnalyzerMappingResult> results, Set<AnalyzerMappingRowKey> recordsWithAnswers,
            AnalyzerMappingCatalogState.Validation catalog) {
    }
}
