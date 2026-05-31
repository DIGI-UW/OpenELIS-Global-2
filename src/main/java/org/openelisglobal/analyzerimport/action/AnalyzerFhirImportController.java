package org.openelisglobal.analyzerimport.action;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;
import org.hibernate.ObjectNotFoundException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Specimen;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.service.QCResultProcessingService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.Analyzer.AnalyzerStatus;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.test.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * FHIR R4 Bundle import endpoint for analyzer results.
 *
 * <p>
 * Receives a FHIR R4 transaction Bundle containing DiagnosticReport,
 * Observation, and Specimen resources from the analyzer bridge. Maps
 * Observations to {@link AnalyzerResults} staging rows — the same table used by
 * HL7, ASTM, and FILE import paths.
 *
 * <p>
 * This is the unified entry point for all analyzer results regardless of source
 * protocol. The bridge handles all format-specific parsing (HL7 v2, ASTM
 * LIS2-A2, Excel, CSV) and normalizes to FHIR R4.
 *
 * <p>
 * FHIR → AnalyzerResults mapping:
 * <ul>
 * <li>Specimen.identifier → accessionNumber</li>
 * <li>Observation.code.coding[0].code → testName</li>
 * <li>Observation.value[x] → result</li>
 * <li>Observation.valueQuantity.unit → units</li>
 * <li>X-Analyzer-Id header → analyzerId</li>
 * </ul>
 */
@RestController
public class AnalyzerFhirImportController extends org.openelisglobal.common.rest.BaseRestController {

    private static final String CLASS_NAME = "AnalyzerFhirImportController";

    @Autowired
    private AnalyzerResultsService analyzerResultsService;

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private FhirContext fhirContext;

    // Not @Autowired — Spring doesn't expose an ObjectMapper bean in this
    // webapp's context (see AnalyzerRestController for the same pattern).
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String[] PROFILE_PROTOCOL_DIRS = { "astm", "hl7", "file" };
    private static final int MIN_PROFILE_MATCH_SCORE = 2;

    @Autowired
    private QCResultProcessingService qcResultProcessingService;

    @Autowired
    private TestService testService;

    @PostMapping(value = "/analyzer/fhir", consumes = { "application/fhir+json", MediaType.APPLICATION_JSON_VALUE,
            MediaType.ALL_VALUE })
    public ResponseEntity<Map<String, Object>> importFhirBundle(HttpServletRequest request,
            @RequestHeader(value = "X-Analyzer-Id", required = false) String analyzerId) {

        Map<String, Object> response = new LinkedHashMap<>();

        try {
            String bundleJson = new String(request.getInputStream().readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8);
            Bundle bundle = fhirContext.newJsonParser().parseResource(Bundle.class, bundleJson);

            if (bundle.getType() != Bundle.BundleType.TRANSACTION && bundle.getType() != Bundle.BundleType.BATCH) {
                response.put("success", false);
                response.put("error", "analyzer.fhirImport.error.invalidBundleType");
                response.put("errorKey", "analyzer.fhirImport.error.invalidBundleType");
                response.put("errorArgs", Map.of("bundleType", String.valueOf(bundle.getType())));
                return ResponseEntity.badRequest().body(response);
            }

            // Resolve analyzer from bridge identifier (composite like "MINDRAY-BC-5380")
            Analyzer analyzer = null;
            if (analyzerId != null && !analyzerId.isBlank()) {
                // Try numeric ID first (direct DB lookup)
                if (isNumericId(analyzerId)) {
                    analyzer = tryGetAnalyzerById(analyzerId);
                }
                if (analyzer == null) {
                    analyzer = tryFindAnalyzerByIdentifier(analyzerId);
                }
                if (analyzer == null) {
                    // Try by name
                    analyzer = tryGetAnalyzerByName(analyzerId);
                }
            }

            // Fallback: try to resolve analyzer from Device resource in the bundle.
            // Prefer identifier-based resolution (stable) over device name (display-only).
            // Also capture the first Device for find-or-create-stub if no match.
            Device firstDevice = null;
            if (analyzer == null) {
                for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                    Resource resource = entry.getResource();
                    if (resource instanceof Device device) {
                        if (firstDevice == null) {
                            firstDevice = device;
                        }
                        // Try identifiers first (machineId, sourceId, pattern match)
                        if (device.hasIdentifier()) {
                            List<String> identifierValues = device.getIdentifier().stream()
                                    .map(org.hl7.fhir.r4.model.Identifier::getValue)
                                    .filter(v -> v != null && !v.isBlank()).toList();
                            analyzer = tryFindAnalyzerByIdentifier(identifierValues);
                        }
                        // Fall back to serial number (machineId)
                        if (analyzer == null && device.hasSerialNumber()) {
                            analyzer = tryFindAnalyzerByIdentifier(device.getSerialNumber());
                        }
                        // Last resort: device display name
                        if (analyzer == null && device.hasDeviceName()) {
                            String deviceName = device.getDeviceNameFirstRep().getName();
                            if (deviceName != null && !deviceName.isBlank()) {
                                analyzer = tryGetAnalyzerByName(deviceName);
                            }
                        }
                        if (analyzer != null) {
                            LogEvent.logInfo(CLASS_NAME, "importFhirBundle",
                                    "Resolved analyzer from bundle Device resource");
                            break;
                        }
                    }
                }
            }

            // Transparent-pipe principle (companion to bridge change in PR #34):
            // if we still don't have an analyzer but the bundle has a Device, create
            // a PENDING_REGISTRATION stub so the results can land in staging instead
            // of failing on the analyzer_results.analyzer_id NOT NULL constraint.
            // The bridge always forwards now (no more dead-letter on unknown source),
            // so this code path activates for first-message-from-new-analyzer.
            // Refs: feedback_bridge_transparent_fhir_pipe.md.
            if (analyzer == null && firstDevice != null) {
                analyzer = findOrCreateStubFromDevice(firstDevice, request);
            }

            // Collect Specimen identifiers for accession number lookup
            Map<String, String> specimenAccessions = new LinkedHashMap<>();
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                Resource resource = entry.getResource();
                if (resource instanceof Specimen specimen) {
                    String fullUrl = entry.getFullUrl();
                    String accession = null;
                    if (specimen.hasIdentifier()) {
                        accession = specimen.getIdentifierFirstRep().getValue();
                    }
                    if (accession != null && fullUrl != null) {
                        specimenAccessions.put(fullUrl, accession);
                    }
                }
            }

            // Map Observations to AnalyzerResults
            List<AnalyzerResults> results = new ArrayList<>();
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                Resource resource = entry.getResource();
                if (resource instanceof Observation obs) {
                    AnalyzerResults ar = mapObservationToAnalyzerResult(obs, specimenAccessions, analyzer);
                    if (ar != null) {
                        results.add(ar);
                    }
                }
            }

            if (results.isEmpty()) {
                response.put("success", false);
                response.put("error", "analyzer.fhirImport.error.noObservations");
                response.put("errorKey", "analyzer.fhirImport.error.noObservations");
                return ResponseEntity.badRequest().body(response);
            }

            String userId = getSysUserId(request);
            if (userId == null) {
                LogEvent.logWarn(CLASS_NAME, "importFhirBundle",
                        "Could not resolve sysUserId from request — using system default");
                userId = "1";
            }
            analyzerResultsService.insertAnalyzerResults(results, userId);

            // Process QC results through Westgard pipeline
            int qcProcessed = 0;
            for (AnalyzerResults ar : results) {
                if (ar.getIsControl() && ar.getTestId() != null && analyzer != null) {
                    processQCAnalyzerResult(ar, analyzer);
                    qcProcessed++;
                }
            }

            response.put("success", true);
            response.put("resultsInserted", results.size());
            response.put("qcResultsProcessed", qcProcessed);
            response.put("analyzerId", analyzer != null ? analyzer.getId() : null);
            LogEvent.logInfo(CLASS_NAME, "importFhirBundle", "Inserted " + results.size() + " results from FHIR Bundle"
                    + " (" + qcProcessed + " QC)" + (analyzer != null ? " for analyzer " + analyzer.getName() : ""));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            LogEvent.logError(e);
            LogEvent.logError(CLASS_NAME, "importFhirBundle",
                    "FHIR Bundle import failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            response.put("success", false);
            response.put("error", "analyzer.fhirImport.error.importFailed");
            response.put("errorKey", "analyzer.fhirImport.error.importFailed");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    private boolean isNumericId(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return !value.isEmpty();
    }

    private Analyzer tryGetAnalyzerById(String analyzerId) {
        try {
            return analyzerService.get(analyzerId);
        } catch (ObjectNotFoundException e) {
            LogEvent.logWarn(CLASS_NAME, "tryGetAnalyzerById",
                    "Analyzer id from header no longer exists: " + analyzerId);
            return null;
        }
    }

    private Analyzer tryFindAnalyzerByIdentifier(String identifier) {
        return analyzerService.findByIdentifierPatternMatch(identifier).orElse(null);
    }

    private Analyzer tryFindAnalyzerByIdentifier(List<String> identifiers) {
        return analyzerService.findByIdentifierPatternMatch(identifiers).orElse(null);
    }

    private Analyzer tryGetAnalyzerByName(String name) {
        return analyzerService.getByName(name).orElse(null);
    }

    /**
     * Resolve a LOINC-coded Observation to an OE2 Test, reusing the mechanism OE2
     * has used to import external FHIR orders for years
     * ({@code TaskInterpreterImpl.createTestFromFHIR} →
     * {@code TestService.getTestsByLoincCode}). Returns null when the Observation
     * carries no LOINC coding or no test matches — the caller then falls back to
     * the legacy analyzer-code mapping. Package-private for test.
     */
    org.openelisglobal.test.valueholder.Test resolveLoincTest(Observation obs) {
        if (obs == null || !obs.hasCode() || !obs.getCode().hasCoding()) {
            return null;
        }
        for (org.hl7.fhir.r4.model.Coding coding : obs.getCode().getCoding()) {
            if ("http://loinc.org".equals(coding.getSystem()) && coding.hasCode()) {
                String loinc = coding.getCode();
                if (loinc != null && !loinc.isBlank()) {
                    List<org.openelisglobal.test.valueholder.Test> tests = testService.getTestsByLoincCode(loinc);
                    if (tests != null && !tests.isEmpty()) {
                        return tests.get(0);
                    }
                }
            }
        }
        return null;
    }

    private AnalyzerResults mapObservationToAnalyzerResult(Observation obs, Map<String, String> specimenAccessions,
            Analyzer analyzer) {

        AnalyzerResults ar = new AnalyzerResults();

        // Analyzer ID
        if (analyzer != null) {
            ar.setAnalyzerId(analyzer.getId());
        }

        // Accession number from Specimen reference
        if (obs.hasSpecimen()) {
            String specimenRef = obs.getSpecimen().getReference();
            String accession = specimenAccessions.get(specimenRef);
            if (accession != null) {
                ar.setAccessionNumber(accession);
            }
        }
        // Fallback: check Observation.subject.identifier
        if (ar.getAccessionNumber() == null && obs.hasSubject() && obs.getSubject().hasIdentifier()) {
            ar.setAccessionNumber(obs.getSubject().getIdentifier().getValue());
        }

        if (ar.getAccessionNumber() == null) {
            LogEvent.logWarn(CLASS_NAME, "mapObservationToAnalyzerResult",
                    "Observation has no accession number — skipping");
            return null;
        }

        // Test code from Observation.code (raw analyzer code, e.g., "WBC")
        String testCode = null;
        if (obs.hasCode() && obs.getCode().hasCoding()) {
            var coding = obs.getCode().getCodingFirstRep();
            testCode = coding.getCode() != null ? coding.getCode() : coding.getDisplay();
        } else if (obs.hasCode() && obs.getCode().hasText()) {
            testCode = obs.getCode().getText();
        }

        LogEvent.logInfo(CLASS_NAME, "mapObservationToAnalyzerResult", "accession=" + ar.getAccessionNumber()
                + " testCode=" + testCode + " analyzerId=" + (analyzer != null ? analyzer.getId() : "null"));

        // Prefer LOINC resolution: the bridge now emits LOINC-coded Observations
        // (bridge owns analyzer-code↔LOINC), and OE2 resolves LOINC→test via the
        // SAME path it has used for external FHIR orders for years
        // (TaskInterpreterImpl.createTestFromFHIR → TestService.getTestsByLoincCode).
        // OE2 is analyzer-agnostic: it binds inbound results by LOINC only. The
        // bridge owns analyzer-code↔LOINC translation, so an Observation that
        // doesn't carry a resolvable LOINC is staged unmapped (no analyzer-code
        // binding in OE2). The result still stages — a tech can resolve it.
        org.openelisglobal.test.valueholder.Test loincTest = resolveLoincTest(obs);
        if (loincTest != null) {
            ar.setTestId(loincTest.getId());
            ar.setTestName(loincTest.getLocalizedName() != null ? loincTest.getLocalizedName() : testCode);
        } else {
            // LOINC didn't resolve — the analyzer/test isn't LOINC-coded yet, or the
            // bridge passed a raw analyzer code. Fall back to the lab's per-analyzer
            // analyzer-code→test mapping (analyzer_test_map via AnalyzerTestNameCache),
            // the path OE2 used before the LOINC interlingua. Without this fallback,
            // HL7/FILE/QC results carrying raw analyzer codes stage read-only and never
            // resolve — which breaks QC processing and result import for any analyzer
            // whose tests aren't LOINC-coded.
            org.openelisglobal.analyzerimport.util.MappedTestName mapped = (analyzer != null && testCode != null
                    && !testCode.isBlank())
                            ? org.openelisglobal.analyzerimport.util.AnalyzerTestNameCache.getInstance()
                                    .getMappedTestByAnalyzerId(analyzer.getId(), testCode)
                            : null;
            String mappedTestId = mapped != null ? mapped.getTestId() : null;
            if (mappedTestId != null) {
                ar.setTestId(mappedTestId);
                ar.setTestName(mapped.getOpenElisTestName() != null ? mapped.getOpenElisTestName() : testCode);
            } else {
                ar.setTestName(testCode);
                ar.setReadOnly(true);
                ar.setImportIssueReason("unmapped_loinc:" + testCode);
            }
        }

        // Result value
        if (obs.hasValueQuantity()) {
            ar.setResult(obs.getValueQuantity().getValue().toPlainString());
            ar.setUnits(obs.getValueQuantity().getUnit());
            ar.setResultType("N");
        } else if (obs.hasValueStringType()) {
            ar.setResult(obs.getValueStringType().getValue());
            ar.setResultType("A");
        } else if (obs.hasValueCodeableConcept()) {
            ar.setResult(obs.getValueCodeableConcept().hasText() ? obs.getValueCodeableConcept().getText()
                    : obs.getValueCodeableConcept().getCodingFirstRep().getDisplay());
            ar.setResultType("A");
        }

        if (ar.getResult() == null) {
            LogEvent.logWarn(CLASS_NAME, "mapObservationToAnalyzerResult", "Observation has no value — skipping");
            return null;
        }

        // QC/control flag from bridge tag
        if (obs.hasMeta() && obs.getMeta().hasTag()) {
            boolean isQc = obs.getMeta().getTag().stream().anyMatch(t -> "QC".equals(t.getCode()));
            ar.setIsControl(isQc);
        }

        // QC metadata from bridge extensions — used by
        // QCResultProcessingService.findMatchingControlLot to resolve to
        // the right qc_control_lot row directly. Populated by the bridge
        // when it could extract them (ASTM Q-segment OR matched FILE
        // qcRule operand). Carried in-memory only (transient on AR), so
        // no schema migration required.
        if (obs.hasExtension("http://openelis-global.org/fhir/qc/lot-number")) {
            org.hl7.fhir.r4.model.Extension lotExt = obs
                    .getExtensionByUrl("http://openelis-global.org/fhir/qc/lot-number");
            if (lotExt != null && lotExt.getValue() instanceof org.hl7.fhir.r4.model.StringType) {
                ar.setLotNumber(((org.hl7.fhir.r4.model.StringType) lotExt.getValue()).getValue());
            }
        }
        if (obs.hasExtension("http://openelis-global.org/fhir/qc/control-level")) {
            org.hl7.fhir.r4.model.Extension levelExt = obs
                    .getExtensionByUrl("http://openelis-global.org/fhir/qc/control-level");
            if (levelExt != null && levelExt.getValue() instanceof org.hl7.fhir.r4.model.StringType) {
                ar.setControlLevel(((org.hl7.fhir.r4.model.StringType) levelExt.getValue()).getValue());
            }
        }

        // Completion timestamp from effectiveDateTime
        if (obs.hasEffectiveDateTimeType()) {
            try {
                ar.setCompleteDate(new Timestamp(obs.getEffectiveDateTimeType().getValue().getTime()));
            } catch (Exception e) {
                ar.setCompleteDate(new Timestamp(System.currentTimeMillis()));
            }
        } else {
            ar.setCompleteDate(new Timestamp(System.currentTimeMillis()));
        }

        // testId is set by the mapping lookup above; if unmapped, it stays null
        // and readOnly=true so the result shows as "configuration needed"
        if (ar.getTestId() == null) {
            ar.setReadOnly(true);
        }

        return ar;
    }

    /**
     * Transparent-pipe companion: find-or-create a PENDING_REGISTRATION stub
     * analyzer from the bundle's Device resource when no existing analyzer matched.
     * Called only after identifier/name/serial-number resolution has already
     * failed, so we're creating exactly for the "first message from a new analyzer"
     * path.
     *
     * <p>
     * The stable handle is the network sourceId the bridge emits as
     * {@code Device.identifier[system="https://openelis-global.org/fhir/source-ip"]}.
     * Without it we can't safely de-duplicate across subsequent bundles, so we
     * decline to stub (caller will surface the missing-analyzer error).
     *
     * <p>
     * Mirrors {@code AnalyzerRestController.reportDiscoveredSource} so the
     * side-channel endpoint and the FHIR path converge on identical stub shape.
     * Duplicate-key races are handled by re-lookup via discoveredSourceId.
     *
     * @param device  the first Device resource from the bundle
     * @param request the inbound HTTP request (for sysUserId resolution)
     * @return the existing or newly-created stub analyzer, or null if we could not
     *         derive a stable sourceId
     */
    private Analyzer findOrCreateStubFromDevice(Device device, HttpServletRequest request) {
        String sourceId = extractSourceIdFromDevice(device);
        if (sourceId == null || sourceId.isBlank()) {
            LogEvent.logWarn(CLASS_NAME, "findOrCreateStubFromDevice",
                    "Device has no identifier with system=source-ip — cannot create stub deterministically");
            return null;
        }

        // Fast path: stub already exists from a prior message.
        java.util.Optional<Analyzer> existing = analyzerService.findByDiscoveredSourceId(sourceId);
        if (existing.isPresent()) {
            return existing.get();
        }

        String displayName = deriveDisplayName(device, sourceId);

        Analyzer stub = new Analyzer();
        stub.ensureFhirUuid();
        stub.setName(displayName);
        stub.setStatus(AnalyzerStatus.PENDING_REGISTRATION);
        stub.setDiscoveredSourceId(sourceId);
        String userId = getSysUserId(request);
        stub.setSysUserId(userId != null ? userId : "1");

        try {
            String analyzerId = analyzerService.insert(stub);
            LogEvent.logInfo(CLASS_NAME, "findOrCreateStubFromDevice",
                    "Created PENDING_REGISTRATION stub analyzerId=" + analyzerId + " for sourceId=" + sourceId);
            applyMatchedProfile(analyzerId, device, stub.getSysUserId());
            return analyzerService.get(analyzerId);
        } catch (RuntimeException e) {
            if (isDuplicateKeyViolation(e)) {
                // Race: another bundle just created the same stub. Re-look it up.
                return analyzerService.findByDiscoveredSourceId(sourceId).orElse(null);
            }
            LogEvent.logError(CLASS_NAME, "findOrCreateStubFromDevice",
                    "Failed to insert stub for sourceId=" + sourceId + ": " + e.getMessage());
            throw e;
        }
    }

    /**
     * Match the Device against the profile registry and apply the winning profile's
     * {@code default_test_mappings} to the newly-created stub.
     *
     * <p>
     * Without this step, stubs land with zero analyzer_test_map rows — every result
     * stays {@code read_only=true} until an admin manually configures mappings.
     * With the match-and-apply, the first message from a known analyzer family
     * (GeneXpert, Mindray, etc.) arrives with mappings pre-populated from the
     * profile's UNION of device-emitted codes, so results land accept-ready.
     *
     * <p>
     * Best-effort: mapping failures don't roll back the stub (separate
     * 
     * @Transactional boundary inside autoCreateTestMappings). Unknown analyzers
     *                with no profile match leave the stub bare and log a clear WARN
     *                so an operator can add a profile.
     */
    private void applyMatchedProfile(String analyzerId, Device device, String sysUserId) {
        try {
            ProfileMatch match = matchProfileFromDevice(device);
            if (match != null) {
                LogEvent.logInfo(CLASS_NAME, "applyMatchedProfile", "Matched profile " + match.fileName + " (score="
                        + match.score + ") for analyzer " + analyzerId + " — applying default_test_mappings");
                analyzerService.autoCreateTestMappings(analyzerId, match.config, sysUserId);
            } else {
                LogEvent.logWarn(CLASS_NAME, "applyMatchedProfile",
                        "No matching profile found for analyzer " + analyzerId + " — staging will remain read-only "
                                + "until admin creates a matching profile in /data/analyzer-profiles/");
            }
        } catch (RuntimeException e) {
            LogEvent.logWarn(CLASS_NAME, "applyMatchedProfile", "Profile apply failed for analyzer " + analyzerId
                    + " (stub persists): " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /**
     * Iterate all profile JSONs under {@code $ANALYZER_PROFILES_DIR} (default
     * {@code /data/analyzer-profiles}) and return the highest-scoring match for the
     * Device, or null if no profile scores at least
     * {@link #MIN_PROFILE_MATCH_SCORE}. Matching is data-driven — drop a new
     * profile in the directory and it becomes selectable without code change.
     */
    private ProfileMatch matchProfileFromDevice(Device device) {
        Path baseDir = resolveProfilesBaseDir();
        if (!Files.isDirectory(baseDir)) {
            return null;
        }
        String mfr = device.hasManufacturer() ? device.getManufacturer() : null;
        String name = device.hasDeviceName() ? device.getDeviceNameFirstRep().getName() : null;
        String senderToken = extractSenderTokenFromDevice(device);

        ProfileMatch best = null;
        for (String protocol : PROFILE_PROTOCOL_DIRS) {
            Path protocolDir = baseDir.resolve(protocol);
            if (!Files.isDirectory(protocolDir)) {
                continue;
            }
            try (Stream<Path> stream = Files.list(protocolDir)) {
                List<Path> files = stream.filter(p -> p.toString().endsWith(".json")).sorted().toList();
                for (Path file : files) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> profile = objectMapper.readValue(file.toFile(), Map.class);
                        int score = scoreProfile(profile, mfr, name, senderToken);
                        if (score >= MIN_PROFILE_MATCH_SCORE && (best == null || score > best.score)) {
                            best = new ProfileMatch(file.getFileName().toString(), profile, score);
                        }
                    } catch (IOException ioe) {
                        LogEvent.logWarn(CLASS_NAME, "matchProfileFromDevice",
                                "Skipping unreadable profile " + file + ": " + ioe.getMessage());
                    }
                }
            } catch (IOException e) {
                LogEvent.logWarn(CLASS_NAME, "matchProfileFromDevice",
                        "Failed to list " + protocolDir + ": " + e.getMessage());
            }
        }
        return best;
    }

    private int scoreProfile(Map<String, Object> profile, String deviceMfr, String deviceName, String senderToken) {
        int score = 0;
        String profileMfr = asString(profile.get("manufacturer"));
        String profileName = asString(profile.get("analyzer_name"));
        String pattern = asString(profile.get("identifier_pattern"));

        if (deviceMfr != null && profileMfr != null && profileMfr.equalsIgnoreCase(deviceMfr)) {
            score += 2;
        }
        if (deviceName != null && profileName != null && profileName.toUpperCase().contains(deviceName.toUpperCase())) {
            score += 2;
        }
        if (pattern != null && !pattern.isBlank()) {
            try {
                Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
                if (deviceName != null && p.matcher(deviceName).find()) {
                    score += 1;
                }
                if (senderToken != null && p.matcher(senderToken).find()) {
                    score += 1;
                }
            } catch (PatternSyntaxException ignored) {
                // Malformed profile regex — score only via other signals.
            }
        }
        return score;
    }

    private String extractSenderTokenFromDevice(Device device) {
        if (!device.hasIdentifier()) {
            return null;
        }
        return device.getIdentifier().stream()
                .filter(id -> "https://openelis-global.org/fhir/sender-token".equals(id.getSystem()))
                .map(org.hl7.fhir.r4.model.Identifier::getValue).filter(v -> v != null && !v.isBlank()).findFirst()
                .orElse(null);
    }

    private Path resolveProfilesBaseDir() {
        String dir = System.getenv("ANALYZER_PROFILES_DIR");
        if (dir == null || dir.isBlank()) {
            dir = "/data/analyzer-profiles";
        }
        return Path.of(dir);
    }

    private static String asString(Object o) {
        return o instanceof String s ? s : null;
    }

    private record ProfileMatch(String fileName, Map<String, Object> config, int score) {
    }

    private String extractSourceIdFromDevice(Device device) {
        if (!device.hasIdentifier()) {
            return null;
        }
        return device.getIdentifier().stream()
                .filter(id -> "https://openelis-global.org/fhir/source-ip".equals(id.getSystem()))
                .map(org.hl7.fhir.r4.model.Identifier::getValue).filter(v -> v != null && !v.isBlank()).findFirst()
                .orElse(null);
    }

    private String deriveDisplayName(Device device, String sourceId) {
        // Prefer device name (e.g. "GeneXpert"), fall back to sender-token
        // identifier (e.g. "LA2M3^GeneXpert^6.2"), last resort "Unknown (ip)".
        String name = null;
        if (device.hasDeviceName()) {
            name = device.getDeviceNameFirstRep().getName();
        }
        if ((name == null || name.isBlank()) && device.hasIdentifier()) {
            name = device.getIdentifier().stream()
                    .filter(id -> "https://openelis-global.org/fhir/sender-token".equals(id.getSystem()))
                    .map(org.hl7.fhir.r4.model.Identifier::getValue).filter(v -> v != null && !v.isBlank()).findFirst()
                    .orElse(null);
        }
        if (name == null || name.isBlank()) {
            name = "Unknown (" + sourceId + ")";
        }
        // Analyzer.name is VARCHAR(100)
        if (name.length() > 100) {
            name = name.substring(0, 97) + "...";
        }
        return name;
    }

    private boolean isDuplicateKeyViolation(Throwable e) {
        while (e != null) {
            String msg = e.getMessage();
            if (msg != null && (msg.contains("duplicate key") || msg.contains("unique constraint"))) {
                return true;
            }
            e = e.getCause();
        }
        return false;
    }

    /**
     * Route a QC-flagged AnalyzerResult to the Westgard QC pipeline.
     *
     * <p>
     * The accession number for QC samples is typically the control lot number or
     * control ID assigned by the lab. The QCResultProcessingService looks up the
     * matching {@code QCControlLot} by lot number + test + instrument and creates a
     * {@code QCResult} that triggers Westgard rule evaluation.
     */
    private void processQCAnalyzerResult(AnalyzerResults ar, Analyzer analyzer) {
        try {
            BigDecimal resultValue = new BigDecimal(ar.getResult());
            LocalDateTime timestamp = ar.getCompleteDate() != null
                    ? ar.getCompleteDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                    : LocalDateTime.now();

            qcResultProcessingService.processQCResult(analyzer.getId(), ar.getTestId(), ar.getAccessionNumber(),
                    ar.getLotNumber(), ar.getControlLevel(), resultValue, ar.getUnits(), timestamp);
        } catch (NumberFormatException e) {
            LogEvent.logWarn(CLASS_NAME, "processQCAnalyzerResult",
                    "QC result value is not numeric — skipping QC processing: " + ar.getResult());
        } catch (Exception e) {
            LogEvent.logError(CLASS_NAME, "processQCAnalyzerResult", "QC processing failed for accession="
                    + ar.getAccessionNumber() + " test=" + ar.getTestId() + ": " + e.getMessage());
        }
    }
}
