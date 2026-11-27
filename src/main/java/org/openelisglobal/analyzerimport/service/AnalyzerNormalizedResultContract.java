package org.openelisglobal.analyzerimport.service;

import ca.uhn.fhir.context.FhirContext;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.hl7.fhir.r4.model.Specimen;
import org.openelisglobal.common.util.BoundedDecimal;

/**
 * Consumer projection of the versioned Bridge normalized-result contract.
 *
 * @param sampleFlags the warnings the instrument raised about the sample, by
 *                    the names it sent; they go with the results and are never
 *                    results themselves
 */
public record AnalyzerNormalizedResultContract(String messageId, String bridgeConnectionId, String profileId,
        int profileRevision, String sourceProtocol, List<Result> results, List<String> sampleFlags,
        String profileFingerprint, String deliveryPurpose) {

    public AnalyzerNormalizedResultContract(String messageId, String bridgeConnectionId, String profileId,
            int profileRevision, String sourceProtocol, List<Result> results, List<String> sampleFlags,
            String profileFingerprint) {
        this(messageId, bridgeConnectionId, profileId, profileRevision, sourceProtocol, results, sampleFlags,
                profileFingerprint, "ROUTINE");
    }

    /** A delivery whose profile content the Bridge did not state. */
    public AnalyzerNormalizedResultContract(String messageId, String bridgeConnectionId, String profileId,
            int profileRevision, String sourceProtocol, List<Result> results, List<String> sampleFlags) {
        this(messageId, bridgeConnectionId, profileId, profileRevision, sourceProtocol, results, sampleFlags, null);
    }

    private static final String NORMALIZED_PROFILE = "https://openelis-global.org/fhir/StructureDefinition/analyzer-normalized-bundle-v1";
    private static final String MESSAGE_ID_SYSTEM = "https://openelis-global.org/fhir/analyzer-message-id";
    private static final String CONNECTION_ID_SYSTEM = "https://openelis-global.org/fhir/analyzer-connection-id";
    private static final String RAW_CODE_SYSTEM = "https://openelis-global.org/fhir/CodeSystem/analyzer-raw-code";
    private static final String EXTENSION_ROOT = "https://openelis-global.org/fhir/StructureDefinition/";
    private static final String PROFILE_ID = EXTENSION_ROOT + "analyzer-profile-id";
    private static final String PROFILE_REVISION = EXTENSION_ROOT + "analyzer-profile-revision";
    private static final String PROFILE_FINGERPRINT = EXTENSION_ROOT + "analyzer-profile-fingerprint";
    private static final String CONTRACT_VERSION = EXTENSION_ROOT + "analyzer-contract-version";
    private static final String DELIVERY_PURPOSE = EXTENSION_ROOT + "analyzer-delivery-purpose";
    private static final String RESULT_PART = EXTENSION_ROOT + "analyzer-result-part";
    private static final String CANONICAL_ANSWER = EXTENSION_ROOT + "analyzer-canonical-answer";
    private static final String SOURCE_PROTOCOL = EXTENSION_ROOT + "analyzer-source-protocol";
    private static final String CLASSIFICATION = EXTENSION_ROOT + "analyzer-result-classification";
    private static final String CONTROL_RECOGNITION = EXTENSION_ROOT + "analyzer-control-recognition";
    private static final String RAW_VALUE = EXTENSION_ROOT + "analyzer-raw-value";
    private static final String SOURCE_TRANSPORT = EXTENSION_ROOT + "analyzer-source-transport";
    private static final String PATIENT_SOURCE = EXTENSION_ROOT + "analyzer-patient-source";
    private static final String CONTROL_LEVEL = "http://openelis-global.org/fhir/qc/control-level";
    private static final String V2_SUBID = "http://hl7.org/fhir/StructureDefinition/observation-v2-subid";
    private static final String ORIGINAL_SUB_IDENTIFIER = "original-sub-identifier";
    private static final String INTERPRETATION = "http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation";
    private static final String ASSAY_VERSION = EXTENSION_ROOT + "analyzer-assay-version";
    /**
     * Interpretation codes that state a call (positive, negative, detected, not
     * detected, indeterminate); any other interpretation is a flag such as N, A or
     * H.
     */
    private static final Set<String> CALL_CODES = Set.of("POS", "NEG", "DET", "ND", "IND");

    private static final String SAMPLE_FLAG = "SAMPLE_FLAG";

    public AnalyzerNormalizedResultContract {
        results = List.copyOf(results);
        sampleFlags = sampleFlags == null ? List.of() : List.copyOf(sampleFlags);
    }

    public static AnalyzerNormalizedResultContract parse(Bundle bundle, FhirContext fhirContext) {
        if (bundle == null || bundle.getType() != Bundle.BundleType.TRANSACTION || bundle.getMeta().getProfile()
                .stream().noneMatch(profile -> NORMALIZED_PROFILE.equals(profile.getValue()))) {
            throw new IllegalArgumentException("Request is not a normalized analyzer result bundle");
        }

        String messageId = requireText(
                bundle.hasIdentifier() && MESSAGE_ID_SYSTEM.equals(bundle.getIdentifier().getSystem())
                        ? bundle.getIdentifier().getValue()
                        : null,
                "Normalized analyzer traffic requires one message ID");

        List<Bundle.BundleEntryComponent> deviceEntries = bundle.getEntry().stream()
                .filter(entry -> entry.getResource() instanceof Device).toList();
        if (deviceEntries.size() != 1) {
            throw new IllegalArgumentException("Normalized analyzer traffic requires one Device");
        }
        Device device = (Device) deviceEntries.get(0).getResource();
        List<String> connectionIds = device.getIdentifier().stream()
                .filter(identifier -> CONNECTION_ID_SYSTEM.equals(identifier.getSystem()))
                .map(identifier -> identifier.getValue()).filter(value -> value != null && !value.isBlank()).toList();
        if (connectionIds.size() != 1) {
            throw new IllegalArgumentException("Normalized analyzer traffic requires one Bridge connection ID");
        }

        String version = optionalExtensionText(device, CONTRACT_VERSION);
        if (version != null && !"1.0".equals(version) && !"1.1".equals(version) && !"1.2".equals(version)) {
            throw new IllegalArgumentException("Unsupported analyzer result contract version: " + version);
        }
        String purpose = optionalExtensionText(device, DELIVERY_PURPOSE);
        if ("1.2".equals(version) && purpose == null) {
            throw new IllegalArgumentException("Version 1.2 requires the receipt-time delivery purpose");
        }
        if (purpose != null && !Set.of("SETUP", "ROUTINE").contains(purpose)) {
            throw new IllegalArgumentException("Analyzer delivery purpose must be SETUP or ROUTINE");
        }
        String profileId = requireExtensionText(device, PROFILE_ID,
                "Normalized analyzer traffic requires a profile ID");
        int profileRevision = requirePositiveIntegerExtension(device, PROFILE_REVISION,
                "Normalized analyzer traffic requires a positive profile revision");
        String sourceProtocol = requireExtensionText(device, SOURCE_PROTOCOL,
                "Normalized analyzer traffic requires a source protocol");

        Map<String, String> specimens = new LinkedHashMap<>();
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.getResource() instanceof Specimen specimen && entry.getFullUrl() != null
                    && specimen.hasIdentifier() && specimen.getIdentifierFirstRep().hasValue()) {
                specimens.put(entry.getFullUrl(), specimen.getIdentifierFirstRep().getValue());
            }
        }

        Map<String, InstrumentPatient> patients = new LinkedHashMap<>();
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.getResource() instanceof Patient patient && entry.getFullUrl() != null
                    && "instrument".equals(optionalExtensionText(patient, PATIENT_SOURCE))
                    && patient.getIdentifierFirstRep().hasValue()) {
                patients.put(entry.getFullUrl(),
                        new InstrumentPatient(patient.getIdentifierFirstRep().getValue(), nameOf(patient)));
            }
        }

        String deviceReference = deviceEntries.get(0).getFullUrl();
        List<Observation> observations = bundle.getEntry().stream().map(Bundle.BundleEntryComponent::getResource)
                .filter(Observation.class::isInstance).map(Observation.class::cast).toList();
        List<String> sampleFlags = observations.stream()
                .filter(observation -> SAMPLE_FLAG.equals(optionalExtensionText(observation, CLASSIFICATION)))
                .map(AnalyzerNormalizedResultContract::flagName).toList();
        List<Result> results = observations.stream()
                .filter(observation -> !SAMPLE_FLAG.equals(optionalExtensionText(observation, CLASSIFICATION)))
                .map(observation -> parseResult(observation, specimens, patients, deviceReference, fhirContext,
                        version))
                .toList();
        if (results.isEmpty()) {
            throw new IllegalArgumentException("Normalized analyzer traffic requires at least one Observation");
        }
        return new AnalyzerNormalizedResultContract(messageId, connectionIds.get(0), profileId, profileRevision,
                sourceProtocol, results, sampleFlags, optionalExtensionText(device, PROFILE_FINGERPRINT),
                purpose == null ? "ROUTINE" : purpose);
    }

    /**
     * A sample flag by the name the instrument gave it, or its code when it gave
     * none.
     */
    private static String flagName(Observation flag) {
        return flag.getCode().getCoding().stream().filter(coding -> RAW_CODE_SYSTEM.equals(coding.getSystem()))
                .map(coding -> coding.hasDisplay() ? coding.getDisplay() : coding.getCode())
                .filter(name -> name != null && !name.isBlank()).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Every sample flag requires the instrument's code"));
    }

    static String versionOf(Bundle bundle) {
        return bundle.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).filter(Device.class::isInstance)
                .map(Device.class::cast).map(device -> optionalExtensionText(device, CONTRACT_VERSION))
                .filter(java.util.Objects::nonNull).findFirst().orElse(null);
    }

    static Result parseResult(Observation observation, Map<String, String> specimens,
            Map<String, InstrumentPatient> patients, String deviceReference, FhirContext fhirContext) {
        return parseResult(observation, specimens, patients, deviceReference, fhirContext,
                optionalExtensionText(observation, CONTRACT_VERSION));
    }

    static Result parseResult(Observation observation, Map<String, String> specimens,
            Map<String, InstrumentPatient> patients, String deviceReference, FhirContext fhirContext, String version) {
        if ("1.1".equals(version) || "1.2".equals(version)) {
            for (var interpretation : observation.getInterpretation()) {
                requireText(optionalExtensionText(interpretation, RESULT_PART),
                        "Version 1.1 interpretations require an explicit call or flag role");
            }
        }
        if (!observation.hasDevice() || !deviceReference.equals(observation.getDevice().getReference())) {
            throw new IllegalArgumentException("Every analyzer Observation must reference the bundle Device");
        }
        String accessionNumber = observation.hasSpecimen() ? specimens.get(observation.getSpecimen().getReference())
                : null;
        accessionNumber = requireText(accessionNumber,
                "Every analyzer Observation must reference an identified Specimen");

        List<String> rawCodes = observation.getCode().getCoding().stream()
                .filter(coding -> RAW_CODE_SYSTEM.equals(coding.getSystem())).map(coding -> coding.getCode())
                .filter(value -> value != null && !value.isBlank()).toList();
        if (rawCodes.size() != 1) {
            throw new IllegalArgumentException("Every analyzer Observation requires one raw analyzer code");
        }

        String rawValue = optionalExtensionText(observation, RAW_VALUE);
        if ((rawValue == null || rawValue.isBlank()) && !observation.hasValue()) {
            throw new IllegalArgumentException("Every analyzer Observation requires its raw value");
        }
        rawValue = rawValue == null ? "" : rawValue.trim();
        String sourceTransport = requireExtensionText(observation, SOURCE_TRANSPORT,
                "Every analyzer Observation requires its source transport");
        String classification = requireExtensionText(observation, CLASSIFICATION,
                "Every analyzer Observation requires a patient/control classification");
        if (!"PATIENT".equals(classification) && !"CONTROL".equals(classification)) {
            throw new IllegalArgumentException("Analyzer result classification must be PATIENT or CONTROL");
        }

        Extension recognition = requireSingleExtension(observation, CONTROL_RECOGNITION,
                "Every analyzer Observation requires control-recognition evidence");
        String recognitionMode = requireNestedExtensionText(recognition, "mode",
                "Control-recognition mode is required");
        String recognitionOutcome = requireNestedExtensionText(recognition, "outcome",
                "Control-recognition outcome is required");
        String recognitionFingerprint = optionalExtensionText(recognition, "recognitionFingerprint");
        if (!Set.of("NONE", "RULES").contains(recognitionMode)
                || !Set.of("MATCH", "NO_MATCH", "NOT_EVALUATED").contains(recognitionOutcome)
                || ("NONE".equals(recognitionMode) && !"NOT_EVALUATED".equals(recognitionOutcome))
                || "CONTROL".equals(classification) != "MATCH".equals(recognitionOutcome)) {
            throw new IllegalArgumentException("Result classification contradicts its delivered recognition evidence");
        }
        if (observation.hasDataAbsentReason() && observation.getDataAbsentReason().getCoding().stream().noneMatch(
                coding -> "http://terminology.hl7.org/CodeSystem/data-absent-reason".equals(coding.getSystem())
                        && "error".equals(coding.getCode()))) {
            throw new IllegalArgumentException(
                    "Unsupported absent-value reason; only an explicit failed run is supported");
        }
        if (observation.getInterpretation().stream().filter(AnalyzerNormalizedResultContract::isCall).count() > 1) {
            throw new IllegalArgumentException("An analyzer result has at most one qualitative call");
        }

        // The quantity becomes the stored number through toPlainString, which writes
        // out every digit an exponent implies; stored numbers are also read back
        // into calculations.
        if (observation.hasValueQuantity() && observation.getValueQuantity().hasValue()
                && !BoundedDecimal.isBounded(observation.getValueQuantity().getValue())) {
            throw new IllegalArgumentException("A numeric analyzer result must be a bounded decimal");
        }
        String units = observation.hasValueQuantity() ? observation.getValueQuantity().getUnit() : null;
        String comparator = observation.hasValueQuantity() && observation.getValueQuantity().hasComparator()
                ? observation.getValueQuantity().getComparator().toCode()
                : null;
        String number = observation.hasValueQuantity() && observation.getValueQuantity().hasValue()
                ? observation.getValueQuantity().getValue().toPlainString()
                : null;
        if (observation.hasDataAbsentReason() && observation.hasValue()) {
            throw new IllegalArgumentException("An analyzer result cannot contain a value and an absent-value reason");
        }
        String canonicalAnswer = optionalExtensionText(observation, CANONICAL_ANSWER);
        String call = observation.hasDataAbsentReason() ? null
                : canonicalAnswer != null ? canonicalAnswer
                        : number != null ? callInterpretation(observation) : callValue(observation);
        if (canonicalAnswer != null && (observation.hasDataAbsentReason() || !canonicalAnswer
                .equals(number != null ? callInterpretation(observation) : callValue(observation)))) {
            throw new IllegalArgumentException("Canonical answer contradicts the delivered result value");
        }
        String resultType = observation.hasValueQuantity() ? "N" : "A";
        Timestamp completed = observation.hasEffectiveDateTimeType()
                ? new Timestamp(observation.getEffectiveDateTimeType().getValue().getTime())
                : null;
        String controlLevel = optionalExtensionText(observation, CONTROL_LEVEL);
        Observation retained = observation.copy();
        if (version != null) {
            retained.getExtension().removeIf(extension -> CONTRACT_VERSION.equals(extension.getUrl()));
            retained.addExtension(CONTRACT_VERSION, new org.hl7.fhir.r4.model.StringType(version));
        }
        String sourcePayload = fhirContext.newJsonParser().encodeResourceToString(retained);
        InstrumentPatient patient = observation.hasSubject() ? patients.get(observation.getSubject().getReference())
                : null;
        String note = observation.getNote().stream().map(annotation -> annotation.getText())
                .filter(text -> text != null && !text.isBlank()).map(String::trim)
                .collect(java.util.stream.Collectors.joining("\n"));

        return new Result(accessionNumber, rawCodes.get(0), rawValue, units, resultType, classification,
                sourceTransport, recognitionMode, recognitionOutcome, recognitionFingerprint, controlLevel, completed,
                sourcePayload, patient == null ? null : patient.identifier(), patient == null ? null : patient.name(),
                note.isEmpty() ? null : note,
                observation.hasDataAbsentReason() && observation.getDataAbsentReason().getCoding().stream()
                        .anyMatch(coding -> "http://terminology.hl7.org/CodeSystem/data-absent-reason"
                                .equals(coding.getSystem()) && "error".equals(coding.getCode())),
                comparator, subIdentityOf(observation), number, call, flagsOf(observation), assayNameOf(observation),
                observation.hasMethod() ? optionalExtensionText(observation.getMethod(), ASSAY_VERSION) : null,
                operatorOf(observation),
                observation.getCode().getCoding().stream().filter(
                        coding -> !RAW_CODE_SYSTEM.equals(coding.getSystem()) && coding.hasSystem() && coding.hasCode())
                        .map(coding -> new org.openelisglobal.terminology.Coding(coding.getSystem(), coding.getCode()))
                        .toList(),
                answerCodings(observation));
    }

    /**
     * Read the delivered answer for local mapping; vendor raw text is never a
     * mapping key.
     */
    public static String deliveredAnswer(Observation observation) {
        if (observation.hasDataAbsentReason())
            return null;
        String canonical = optionalExtensionText(observation, CANONICAL_ANSWER);
        return canonical != null ? canonical
                : observation.hasValueQuantity() ? callInterpretation(observation) : callValue(observation);
    }

    private static List<org.openelisglobal.terminology.Coding> answerCodings(Observation observation) {
        var concepts = observation.hasValueCodeableConcept()
                ? java.util.stream.Stream.of(observation.getValueCodeableConcept())
                : observation.getInterpretation().stream().filter(AnalyzerNormalizedResultContract::isCall);
        return concepts.flatMap(concept -> concept.getCoding().stream()
                // In the old combined-result representation these codes identify the
                // call role, not a clinical answer identity. Explicit roles and
                // qualitative answer codings retain their delivered identities.
                .filter(coding -> !observation.hasValueQuantity() || optionalExtensionText(concept, RESULT_PART) != null
                        || !INTERPRETATION.equals(coding.getSystem()) || !CALL_CODES.contains(coding.getCode())))
                .filter(coding -> coding.hasSystem() && coding.hasCode())
                .map(coding -> new org.openelisglobal.terminology.Coding(coding.getSystem(), coding.getCode()))
                .distinct().toList();
    }

    /**
     * The instrument's flags as sent: every interpretation that does not state a
     * call. OpenELIS shows them and does not interpret them.
     */
    private static String flagsOf(Observation observation) {
        return joined(observation.getInterpretation().stream().filter(concept -> !isCall(concept))
                .map(concept -> concept.hasText() ? concept.getText()
                        : concept.getCoding().stream().findFirst()
                                .map(coding -> coding.hasDisplay() ? coding.getDisplay() : coding.getCode())
                                .orElse(null)),
                ", ");
    }

    private static boolean isCall(org.hl7.fhir.r4.model.CodeableConcept concept) {
        String role = optionalExtensionText(concept, RESULT_PART);
        if (role != null) {
            if (!"call".equals(role) && !"flag".equals(role)) {
                throw new IllegalArgumentException("Unknown analyzer result part: " + role);
            }
            return "call".equals(role);
        }
        // Version 1.0 encoded the role in these standard codes. Decode only that old
        // wire representation; never consult a profile or classify vendor spellings.
        return concept.getCoding().stream()
                .anyMatch(coding -> INTERPRETATION.equals(coding.getSystem()) && CALL_CODES.contains(coding.getCode()));
    }

    /** The assay that produced the result ({@code Observation.method}). */
    private static String assayNameOf(Observation observation) {
        if (!observation.hasMethod()) {
            return null;
        }
        var method = observation.getMethod();
        return method.hasText() ? method.getText().trim()
                : joined(method.getCoding().stream()
                        .map(coding -> coding.hasDisplay() ? coding.getDisplay() : coding.getCode()), ", ");
    }

    private static String operatorOf(Observation observation) {
        return joined(observation.getPerformer().stream().map(performer -> performer.getDisplay()), ", ");
    }

    private static String joined(java.util.stream.Stream<String> texts, String separator) {
        String text = texts.filter(value -> value != null && !value.isBlank()).map(String::trim)
                .collect(java.util.stream.Collectors.joining(separator));
        return text.isEmpty() ? null : text;
    }

    /** The record's sub-identity under its code; empty for the main result. */
    private static String subIdentityOf(Observation observation) {
        List<Extension> subIds = observation.getExtension().stream()
                .filter(extension -> V2_SUBID.equals(extension.getUrl())).toList();
        if (subIds.isEmpty()) {
            return "";
        }
        if (subIds.size() != 1) {
            throw new IllegalArgumentException("An analyzer Observation has at most one sub-identity");
        }
        Extension original = subIds.get(0).getExtensionByUrl(ORIGINAL_SUB_IDENTIFIER);
        String value = original != null && original.getValue() instanceof PrimitiveType<?> primitive
                ? primitive.getValueAsString()
                : null;
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Analyzer sub-identity requires a nonblank original sub-identifier");
        }
        return value.trim();
    }

    /**
     * The call of a record that also carries a number: its call interpretation, as
     * the instrument wrote it.
     */
    private static String callInterpretation(Observation observation) {
        return observation.getInterpretation().stream().filter(AnalyzerNormalizedResultContract::isCall)
                .map(concept -> concept.hasText() ? concept.getText()
                        : concept.getCoding().stream().filter(coding -> INTERPRETATION.equals(coding.getSystem()))
                                .findFirst().map(coding -> coding.hasDisplay() ? coding.getDisplay() : coding.getCode())
                                .orElse(null))
                .filter(text -> text != null && !text.isBlank()).map(String::trim).findFirst().orElse(null);
    }

    /**
     * The explicit typed call; raw text is audit evidence and never supplies a
     * value.
     */
    private static String callValue(Observation observation) {
        if (observation.hasValueStringType() && observation.getValueStringType().hasValue()) {
            return observation.getValueStringType().getValue().trim();
        }
        if (observation.hasValueCodeableConcept() && observation.getValueCodeableConcept().hasText()) {
            return observation.getValueCodeableConcept().getText().trim();
        }
        throw new IllegalArgumentException(
                "Analyzer result requires a quantity, text, coded answer or failed-run reason");
    }

    private static String nameOf(Patient patient) {
        var name = patient.getNameFirstRep();
        if (name.hasText()) {
            return name.getText();
        }
        String given = name.getGivenAsSingleString();
        String family = name.getFamily();
        if (family != null && !given.isBlank()) {
            return family + ", " + given;
        }
        return family != null ? family : given.isBlank() ? null : given;
    }

    private static String requireExtensionText(DomainResource resource, String url, String message) {
        return requireText(optionalExtensionText(resource, url), message);
    }

    private static String optionalExtensionText(DomainResource resource, String url) {
        return optionalExtensionText(resource.getExtension(), url);
    }

    private static String optionalExtensionText(org.hl7.fhir.r4.model.Element element, String url) {
        return optionalExtensionText(element.getExtension(), url);
    }

    private static String optionalExtensionText(List<Extension> extensions, String url) {
        List<Extension> matches = extensions.stream().filter(extension -> url.equals(extension.getUrl())).toList();
        if (matches.isEmpty()) {
            return null;
        }
        if (matches.size() != 1 || !(matches.get(0).getValue() instanceof PrimitiveType<?> primitive)) {
            throw new IllegalArgumentException("Expected one primitive extension " + url);
        }
        return primitive.getValueAsString();
    }

    private static int requirePositiveIntegerExtension(DomainResource resource, String url, String message) {
        String value = requireExtensionText(resource, url, message);
        try {
            int parsed = Integer.parseInt(value);
            if (parsed > 0) {
                return parsed;
            }
        } catch (NumberFormatException ignored) {
            // The shared validation message below is more useful than the parser detail.
        }
        throw new IllegalArgumentException(message);
    }

    private static Extension requireSingleExtension(DomainResource resource, String url, String message) {
        List<Extension> matches = resource.getExtension().stream().filter(extension -> url.equals(extension.getUrl()))
                .toList();
        if (matches.size() != 1) {
            throw new IllegalArgumentException(message);
        }
        return matches.get(0);
    }

    private static String requireNestedExtensionText(Extension parent, String url, String message) {
        List<Extension> matches = parent.getExtension().stream().filter(extension -> url.equals(extension.getUrl()))
                .toList();
        if (matches.size() != 1 || !(matches.get(0).getValue() instanceof PrimitiveType<?> primitive)) {
            throw new IllegalArgumentException(message);
        }
        return requireText(primitive.getValueAsString(), message);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    /**
     * One analyzer result. {@code note} is the instrument's own comments on the run
     * ({@code Observation.note}); {@code runFailed} means the run produced no value
     * ({@code Observation.dataAbsentReason}), as for an ERROR or NO RESULT.
     * {@code flags}, {@code assayName}, {@code assayVersion} and {@code operator}
     * are instrument-reported and shown as sent.
     */
    public record Result(String accessionNumber, String rawTestCode, String rawValue, String units, String resultType,
            String classification, String sourceTransport, String recognitionMode, String recognitionOutcome,
            String recognitionFingerprint, String controlLevel, Timestamp completeDate, String sourcePayload,
            String instrumentPatientId, String instrumentPatientName, String note, boolean runFailed, String comparator,
            String subIdentity, String number, String call, String flags, String assayName, String assayVersion,
            String operator, List<org.openelisglobal.terminology.Coding> testCodings,
            List<org.openelisglobal.terminology.Coding> answerCodings) {

        /**
         * The number as OpenELIS records it: a comparator the instrument reported leads
         * it, as in "<40", so a result beyond the measuring range is never stored as
         * the bare limit. Null when the record carries no number.
         */
        public String reportedNumber() {
            return number == null ? null : comparator == null ? number : comparator + number;
        }

        /**
         * The value staged when the record is held whole: its number, else its call,
         * else the raw text.
         */
        public String reportedValue() {
            return number != null ? reportedNumber() : call != null ? call : rawValue;
        }
    }

    /**
     * The patient the instrument reported, kept as reported; never an OpenELIS
     * patient.
     */
    record InstrumentPatient(String identifier, String name) {
    }
}
