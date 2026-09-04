package org.openelisglobal.analyzerimport.service;

import ca.uhn.fhir.context.FhirContext;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.hl7.fhir.r4.model.Specimen;

/** Consumer projection of the versioned Bridge normalized-result contract. */
public record AnalyzerNormalizedResultContract(String messageId, String bridgeConnectionId, String profileId,
        int profileRevision, String sourceProtocol, List<Result> results) {

    private static final String NORMALIZED_PROFILE = "https://openelis-global.org/fhir/StructureDefinition/analyzer-normalized-bundle-v1";
    private static final String MESSAGE_ID_SYSTEM = "https://openelis-global.org/fhir/analyzer-message-id";
    private static final String CONNECTION_ID_SYSTEM = "https://openelis-global.org/fhir/analyzer-connection-id";
    private static final String RAW_CODE_SYSTEM = "https://openelis-global.org/fhir/CodeSystem/analyzer-raw-code";
    private static final String EXTENSION_ROOT = "https://openelis-global.org/fhir/StructureDefinition/";
    private static final String PROFILE_ID = EXTENSION_ROOT + "analyzer-profile-id";
    private static final String PROFILE_REVISION = EXTENSION_ROOT + "analyzer-profile-revision";
    private static final String SOURCE_PROTOCOL = EXTENSION_ROOT + "analyzer-source-protocol";
    private static final String CLASSIFICATION = EXTENSION_ROOT + "analyzer-result-classification";
    private static final String CONTROL_RECOGNITION = EXTENSION_ROOT + "analyzer-control-recognition";
    private static final String RAW_VALUE = EXTENSION_ROOT + "analyzer-raw-value";
    private static final String SOURCE_TRANSPORT = EXTENSION_ROOT + "analyzer-source-transport";
    private static final String LOT_NUMBER = "http://openelis-global.org/fhir/qc/lot-number";
    private static final String CONTROL_LEVEL = "http://openelis-global.org/fhir/qc/control-level";

    public AnalyzerNormalizedResultContract {
        results = List.copyOf(results);
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

        String deviceReference = deviceEntries.get(0).getFullUrl();
        List<Result> results = bundle.getEntry().stream().map(Bundle.BundleEntryComponent::getResource)
                .filter(Observation.class::isInstance).map(Observation.class::cast)
                .map(observation -> parseResult(observation, specimens, deviceReference, fhirContext)).toList();
        if (results.isEmpty()) {
            throw new IllegalArgumentException("Normalized analyzer traffic requires at least one Observation");
        }
        return new AnalyzerNormalizedResultContract(messageId, connectionIds.get(0), profileId, profileRevision,
                sourceProtocol, results);
    }

    private static Result parseResult(Observation observation, Map<String, String> specimens, String deviceReference,
            FhirContext fhirContext) {
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

        String rawValue = requireExtensionText(observation, RAW_VALUE,
                "Every analyzer Observation requires its raw value");
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
        String recognitionFingerprint = requireNestedExtensionText(recognition, "recognitionFingerprint",
                "Control-recognition fingerprint is required");

        String units = observation.hasValueQuantity() ? observation.getValueQuantity().getUnit() : null;
        String resultType = observation.hasValueQuantity() ? "N" : "A";
        Timestamp completed = observation.hasEffectiveDateTimeType()
                ? new Timestamp(observation.getEffectiveDateTimeType().getValue().getTime())
                : null;
        String lotNumber = optionalExtensionText(observation, LOT_NUMBER);
        String controlLevel = optionalExtensionText(observation, CONTROL_LEVEL);
        String sourcePayload = fhirContext.newJsonParser().encodeResourceToString(observation);

        return new Result(accessionNumber, rawCodes.get(0), rawValue, units, resultType, classification,
                sourceTransport, recognitionMode, recognitionOutcome, recognitionFingerprint, lotNumber, controlLevel,
                completed, sourcePayload);
    }

    private static String requireExtensionText(DomainResource resource, String url, String message) {
        return requireText(optionalExtensionText(resource, url), message);
    }

    private static String optionalExtensionText(DomainResource resource, String url) {
        List<Extension> matches = resource.getExtension().stream().filter(extension -> url.equals(extension.getUrl()))
                .toList();
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

    public record Result(String accessionNumber, String rawTestCode, String rawValue, String units, String resultType,
            String classification, String sourceTransport, String recognitionMode, String recognitionOutcome,
            String recognitionFingerprint, String lotNumber, String controlLevel, Timestamp completeDate,
            String sourcePayload) {
    }
}
