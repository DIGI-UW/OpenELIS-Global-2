package org.openelisglobal.analyzerimport.service;

import ca.uhn.fhir.context.FhirContext;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.openelisglobal.analyzer.service.AnalyzerMappingCatalogService;
import org.openelisglobal.analyzer.service.AnalyzerMappingCatalogState;
import org.openelisglobal.analyzer.service.AnalyzerMappingConfirmationService;
import org.openelisglobal.analyzer.service.AnalyzerMappingRowKey;
import org.openelisglobal.analyzer.service.AnalyzerMappingService;
import org.openelisglobal.analyzer.service.AnalyzerMappingSnapshot;
import org.openelisglobal.analyzer.service.AnalyzerRecordReading;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.service.BridgeAnalyzerProfile;
import org.openelisglobal.analyzer.service.BridgeProfileCatalog;
import org.openelisglobal.analyzer.service.BridgeProfileCatalogException;
import org.openelisglobal.analyzer.service.BridgeProfileCatalogService;
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
    private final BridgeProfileCatalogService profileCatalogService;

    public AnalyzerNormalizedResultImportServiceImpl(AnalyzerService analyzerService,
            AnalyzerMappingService mappingService, AnalyzerResultsService analyzerResultsService,
            TestResultService testResultService, QCResultProcessingService qcResultProcessingService,
            FhirContext fhirContext, AnalyzerDeliveryReceiptDAO receiptDAO,
            AnalyzerMappingConfirmationService confirmationService, AnalyzerMappingCatalogService mappingCatalogService,
            AnalyzerResultPlacementService placementService, BridgeProfileCatalogService profileCatalogService) {
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
        this.profileCatalogService = profileCatalogService;
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
        // A row held under another revision of this profile is retried like a new
        // delivery from that revision: it maps only where both revisions read it alike.
        for (AnalyzerResults held : analyzerResultsService.findHeldMappingResultsByAnalyzer(analyzerId)) {
            if (pin == null || !pin.getProfileId().equals(held.getSourceProfileId())
                    || held.getSourceProfileRevision() == null
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
                });
                analyzerResultsService.insertAnalyzerResults(others, effectiveActor);
                recoveredCount += (int) others.stream().filter(row -> !row.isReadOnly()).count();
            }
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
        Mapping mapping = new Mapping(
                binding.tests().stream().collect(Collectors.toMap(AnalyzerMappingRowKey::of, row -> row)),
                binding.results().stream().collect(Collectors.toMap(
                        row -> new ResultKey(AnalyzerMappingRowKey.of(row), row.getId().getRawValue()), row -> row)),
                binding.results().stream().map(AnalyzerMappingRowKey::of).collect(Collectors.toSet()),
                AnalyzerMappingCatalogState.load(mappingCatalogService).validate(binding),
                readsAlike(contract, binding.mapping().getProfilePin()));
        Map<String, Boolean> confirmedByRecognition = new HashMap<>();
        return contract.results().stream().flatMap(result -> {
            boolean confirmed = confirmedByRecognition.computeIfAbsent(result.recognitionFingerprint(),
                    fingerprint -> confirmationService.hasMatchingConfirmation(binding, fingerprint));
            return toStagedResults(contract, result, analyzer, mapping, confirmed).stream();
        }).toList();
    }

    /**
     * Stages one reported record. A record carrying a number and a call stages two
     * rows: the number on the record's target and the call on its call target.
     */
    private List<AnalyzerResults> toStagedResults(AnalyzerNormalizedResultContract contract,
            AnalyzerNormalizedResultContract.Result result, Analyzer analyzer, Mapping mapping,
            boolean mappingConfirmed) {
        AnalyzerMappingRowKey record = new AnalyzerMappingRowKey(result.rawTestCode(), result.subIdentity());
        if (!mapping.readsAlike().test(record)) {
            return List.of(held(contract, result, analyzer, AnalyzerResults.IMPORT_ISSUE_OTHER_REVISION));
        }
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
        if (testMapping.getMappingState() != AnalyzerMappingState.BOUND
                || !mapping.catalog().isCurrentBoundTest(record)) {
            return List.of(held(contract, result, analyzer, AnalyzerResults.IMPORT_ISSUE_TEST_MAPPING_NOT_READY));
        }
        if (result.runFailed()) {
            AnalyzerResults row = staged(contract, result, analyzer, result.reportedValue());
            row.setTestId(testMapping.getTestId());
            row.setComponentId(testMapping.getComponentId());
            hold(row, AnalyzerResults.IMPORT_ISSUE_RUN_FAILED);
            return List.of(row);
        }

        List<AnalyzerResults> rows = new ArrayList<>();
        if (result.number() != null) {
            AnalyzerResults number = staged(contract, result, analyzer, result.reportedNumber());
            number.setTestId(testMapping.getTestId());
            number.setComponentId(testMapping.getComponentId());
            number.setResultType("N");
            // With a call, the raw value is that call and is mapped on the call's row
            // below.
            AnalyzerMappingResult answer = result.call() != null ? null
                    : mapping.results().get(new ResultKey(record, result.rawValue()));
            if (answer == null || bindAnswer(number, answer, record, result.rawValue(), mapping)) {
                rows.add(number);
            }
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
            if (result.number() != null && !hasCallTarget) {
                hold(call, AnalyzerResults.IMPORT_ISSUE_TEST_MAPPING_NOT_READY);
            } else {
                AnalyzerMappingResult answer = mapping.results().get(new ResultKey(record, result.call()));
                if (answer != null && !bindAnswer(call, answer, record, result.call(), mapping)) {
                    call = null;
                } else if (answer == null && mapping.recordsWithAnswers().contains(record)) {
                    hold(call, AnalyzerResults.IMPORT_ISSUE_UNKNOWN_RESULT_VALUE);
                }
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
            String value, Mapping mapping) {
        if (answer.getMappingState() == AnalyzerMappingState.EXCLUDED) {
            return false;
        }
        if (answer.getMappingState() != AnalyzerMappingState.BOUND || answer.getTestResultId() == null) {
            hold(row, AnalyzerResults.IMPORT_ISSUE_RESULT_MAPPING_NOT_READY);
            return true;
        }
        if (!mapping.catalog().isCurrentBoundResult(record, value)) {
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
        row.setLotNumber(result.lotNumber());
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
        row.setAssayName(result.assayName());
        row.setAssayVersion(result.assayVersion());
        row.setInstrumentOperator(result.operator());
    }

    /**
     * Traffic from another revision of the analyzer's own profile is never refused
     * for its revision alone; {@link #readsAlike} decides record by record.
     */
    private void requireMatchingProfile(Analyzer analyzer, AnalyzerNormalizedResultContract contract) {
        AnalyzerProfilePin pinned = analyzer.getPinnedProfile();
        if (pinned == null || !contract.profileId().equals(pinned.getProfileId())) {
            throw new AnalyzerNormalizedResultImportException("analyzer.fhirImport.error.profileMismatch",
                    "Normalized traffic profile does not match the analyzer pin");
        }
    }

    /**
     * The records a delivery stamped with another revision can map through the
     * revision in force: those both revisions read alike. When either revision
     * cannot be read, none can.
     */
    private Predicate<AnalyzerMappingRowKey> readsAlike(AnalyzerNormalizedResultContract contract,
            AnalyzerProfilePin pinned) {
        if (contract.profileRevision() == pinned.getProfileRevision()) {
            return record -> true;
        }
        try {
            BridgeProfileCatalog.ProfileRevision sent = profileCatalogService.getProfile(pinned.getProfileId(),
                    contract.profileRevision());
            BridgeProfileCatalog.ProfileRevision inForce = profileCatalogService.getProfile(pinned.getProfileId(),
                    pinned.getProfileRevision());
            if (sent == null || inForce == null) {
                return record -> false;
            }
            return AnalyzerRecordReading.readAlike(BridgeAnalyzerProfile.from(sent.profile()),
                    BridgeAnalyzerProfile.from(inForce.profile()))::contains;
        } catch (BridgeProfileCatalogException | IllegalArgumentException exception) {
            LogEvent.logWarn(CLASS_NAME, "readsAlike", "Profile revision " + contract.profileRevision() + " of "
                    + pinned.getProfileId() + " could not be read: " + exception.getMessage());
            return record -> false;
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

    private record ResultKey(AnalyzerMappingRowKey record, String rawValue) {
    }

    private record Mapping(Map<AnalyzerMappingRowKey, AnalyzerMappingTest> tests,
            Map<ResultKey, AnalyzerMappingResult> results, Set<AnalyzerMappingRowKey> recordsWithAnswers,
            AnalyzerMappingCatalogState.Validation catalog, Predicate<AnalyzerMappingRowKey> readsAlike) {
    }
}
