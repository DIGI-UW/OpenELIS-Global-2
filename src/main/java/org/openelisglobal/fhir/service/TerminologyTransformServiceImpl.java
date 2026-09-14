package org.openelisglobal.fhir.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.validator.GenericValidator;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sampletypeterminology.service.SampleTypeTerminologyMappingService;
import org.openelisglobal.sampletypeterminology.valueholder.SampleTypeTerminologyMapping;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testterminology.service.TestTerminologyMappingService;
import org.openelisglobal.testterminology.valueholder.TestTerminologyMapping;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TerminologyTransformServiceImpl implements TerminologyTransformService {

    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private TestService testService;
    @Autowired
    private TestTerminologyMappingService testTerminologyMappingService;
    @Autowired
    private SampleTypeTerminologyMappingService sampleTypeTerminologyMappingService;
    @Autowired
    private org.openelisglobal.testresultcomponent.service.TestResultComponentService testResultComponentService;

    /**
     * A candidate code for one terminology system, flagged if it is a SAME_AS
     * mapping, carrying the display to emit on its Coding (the mapping's curated
     * display name when present, else the test/component name).
     */
    private static final class Candidate {
        private final String code;
        private final boolean sameAs;
        private final String display;

        private Candidate(String code, boolean sameAs, String display) {
            this.code = code;
            this.sameAs = sameAs;
            this.display = display;
        }
    }

    @Override
    public CodeableConcept transformTestToCodeableConcept(String testId, String sampleTypeId) {
        return transformTestToCodeableConcept(testService.get(testId), sampleTypeId);
    }

    /**
     * The test's codings for one specimen. {@code sampleTypeId} is the specimen the
     * resource describes: a mapping scoped to another sample type does not apply to
     * it and is left out, so an Observation on a DBS specimen never carries the
     * Urines terminology. A mapping with no sample type applies to every specimen.
     */

    @Override
    public CodeableConcept transformTestToCodeableConcept(Test test, String sampleTypeId) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformTestToCodeableConcept",
                "transformTestToCodeableConcept test called");

        String display = test.getLocalizedTestName() != null ? test.getLocalizedTestName().getEnglish()
                : test.getName();
        CodeableConcept codeableConcept = new CodeableConcept();

        // Group the configured terminology mappings (the editor's Terminology section
        // is the source of truth) by their FHIR system, keeping only those with a
        // recognized system and a code. The legacy test.loinc value participates as a
        // LOINC SAME_AS candidate — it is kept in sync with the LOINC SAME_AS mapping
        // and also covers tests not yet migrated to the terminology editor.
        Map<String, List<Candidate>> bySystem = new LinkedHashMap<>();
        for (TestTerminologyMapping mapping : testTerminologyMappingService.getActiveByTestId(test.getId())) {
            // Only test-level mappings identify the test itself; component-scoped
            // mappings (component_id != null) describe a sub-result, not this code.
            if (mapping.getComponentId() != null || !appliesToSpecimen(mapping.getSampleTypeId(), sampleTypeId)) {
                continue;
            }
            String system = terminologySystemUrl(mapping.getSource());
            if (system == null || GenericValidator.isBlankOrNull(mapping.getCode())) {
                continue;
            }
            // Coding.display: the mapping's curated display name (the standard
            // term's label, FR-69) when present; otherwise the test name.
            String codingDisplay = GenericValidator.isBlankOrNull(mapping.getDisplayName()) ? display
                    : mapping.getDisplayName();
            bySystem.computeIfAbsent(system, k -> new ArrayList<>()).add(new Candidate(mapping.getCode(),
                    "SAME_AS".equalsIgnoreCase(mapping.getRelationship()), codingDisplay));
        }
        if (!GenericValidator.isBlankOrNull(test.getLoinc())) {
            bySystem.computeIfAbsent("http://loinc.org", k -> new ArrayList<>())
                    .add(new Candidate(test.getLoinc(), true, display));
        }

        addPrioritizedCodings(codeableConcept, bySystem);
        if (display != null && !display.isBlank()) {
            codeableConcept.setText(display);
        }
        return codeableConcept;
    }

    /**
     * Whether a mapping scoped to {@code mappingSampleTypeId} applies to the
     * specimen {@code sampleTypeId}. A mapping with no sample type is shared and
     * applies to all of them.
     */
    private boolean appliesToSpecimen(String mappingSampleTypeId, String sampleTypeId) {
        return mappingSampleTypeId == null || mappingSampleTypeId.equals(sampleTypeId);
    }

    /**
     * Emit one system's codings at a time. Within a system the SAME_AS mapping is
     * the equivalent concept, so it wins; with no SAME_AS we keep the rest. A
     * subject thus maps to multiple terminology systems at once (LOINC + SNOMED +
     * ...).
     */
    private void addPrioritizedCodings(CodeableConcept codeableConcept, Map<String, List<Candidate>> bySystem) {
        for (Map.Entry<String, List<Candidate>> entry : bySystem.entrySet()) {
            String system = entry.getKey();
            List<Candidate> candidates = entry.getValue();
            boolean hasSameAs = candidates.stream().anyMatch(c -> c.sameAs);
            Set<String> seenCodes = new HashSet<>();
            for (Candidate candidate : candidates) {
                if (hasSameAs && !candidate.sameAs) {
                    continue;
                }
                if (seenCodes.add(candidate.code)) {
                    codeableConcept.addCoding(new Coding(system, candidate.code, candidate.display));
                }
            }
        }
    }

    /**
     * The active result component a result belongs to (via its test_result's
     * component_id), or null when the result is not component-scoped / legacy.
     */

    @Override
    public org.openelisglobal.testresultcomponent.valueholder.TestResultComponent resolveResultComponent(String testId,
            Result result) {
        String componentId = result == null || result.getTestResult() == null ? null
                : result.getTestResult().getComponentId();
        if (componentId == null) {
            return null;
        }
        for (org.openelisglobal.testresultcomponent.valueholder.TestResultComponent c : testResultComponentService
                .getActiveComponentsByTestId(testId)) {
            if (componentId.equals(c.getId())) {
                return c;
            }
        }
        return null;
    }

    /**
     * The CodeableConcept for a result's Observation. It always starts from the
     * whole-test codings (the "Applies to = whole test" mappings + legacy LOINC),
     * so every component Observation still carries the test's identity. When the
     * result belongs to a component it ALSO gets that component's own codings (the
     * "Applies to = this component" mappings) — including the primary — so a single
     * Observation can bear both the test and the component terminology. A
     * non-primary component additionally gets an OpenELIS coding for its stable
     * code and the component label as text, so it is individually identifiable.
     */

    @Override
    public CodeableConcept transformResultCodeableConcept(Test test,
            org.openelisglobal.testresultcomponent.valueholder.TestResultComponent component, String sampleTypeId) {
        // Base: the whole-test codings, applied to every result's Observation.
        CodeableConcept codeableConcept = transformTestToCodeableConcept(test, sampleTypeId);
        if (component == null) {
            return codeableConcept;
        }
        String label = GenericValidator.isBlankOrNull(component.getLabel()) ? component.getCode()
                : component.getLabel();
        for (TestTerminologyMapping mapping : testTerminologyMappingService.getActiveByTestId(test.getId())) {
            if (!component.getId().equals(mapping.getComponentId())
                    || !appliesToSpecimen(mapping.getSampleTypeId(), sampleTypeId)) {
                continue;
            }
            String system = terminologySystemUrl(mapping.getSource());
            if (system == null || GenericValidator.isBlankOrNull(mapping.getCode())) {
                continue;
            }
            // Coding.display: the mapping's curated display name (FR-69) when
            // present; otherwise the component's name.
            String codingDisplay = GenericValidator.isBlankOrNull(mapping.getDisplayName()) ? label
                    : mapping.getDisplayName();
            codeableConcept.addCoding(new Coding(system, mapping.getCode(), codingDisplay));
        }
        if (!component.getIsPrimary()) {
            codeableConcept.addCoding(
                    new Coding(fhirConfig.getOeFhirSystem() + "/test_result_component", component.getCode(), label));
            codeableConcept.setText(label);
        }
        return codeableConcept;
    }

    /**
     * Canonical FHIR system URI for a terminology mapping source. LOINC and SNOMED
     * use the HL7-registered URIs already used elsewhere in this service; CIEL and
     * OCL use their OpenConceptLab canonical URLs. Returns {@code null} for an
     * unrecognized source so it is skipped rather than emitting a bogus system.
     */
    private String terminologySystemUrl(String source) {
        if (source == null) {
            return null;
        }
        switch (source.toUpperCase()) {
        case "LOINC":
            return "http://loinc.org";
        case "SNOMED":
            return "http://snomed.info/sct";
        case "CIEL":
            return "https://openconceptlab.org/orgs/CIEL/sources/CIEL";
        case "OCL":
            return "https://openconceptlab.org";
        default:
            return null;
        }
    }

    @Override
    public CodeableConcept transformTypeOfSampleToCodeableConcept(String typeOfSampleId) {
        return transformTypeOfSampleToCodeableConcept(typeOfSampleService.get(typeOfSampleId));
    }

    @Override
    public CodeableConcept transformTypeOfSampleToCodeableConcept(TypeOfSample typeOfSample) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformTypeOfSampleToCodeableConcept",
                "transformTypeOfSampleToCodeableConcept called");

        CodeableConcept codeableConcept = new CodeableConcept();
        codeableConcept.addCoding(new Coding(fhirConfig.getOeFhirSystem() + "/sampleType",
                typeOfSample.getLocalAbbreviation(), typeOfSample.getLocalizedName()));

        // The standard terminology configured in the Sample Type Editor, carried
        // alongside the OpenELIS coding so a consumer can resolve the specimen
        // against SNOMED/LOINC rather than a local abbreviation. Same precedence as
        // the test codings: within one system the SAME_AS mapping wins.
        Map<String, List<Candidate>> bySystem = new LinkedHashMap<>();
        for (SampleTypeTerminologyMapping mapping : sampleTypeTerminologyMappingService
                .getActiveBySampleTypeId(typeOfSample.getId())) {
            String system = terminologySystemUrl(mapping.getSource());
            if (system == null || GenericValidator.isBlankOrNull(mapping.getCode())) {
                continue;
            }
            bySystem.computeIfAbsent(system, k -> new ArrayList<>()).add(new Candidate(mapping.getCode(),
                    "SAME_AS".equalsIgnoreCase(mapping.getRelationship()), typeOfSample.getLocalizedName()));
        }
        addPrioritizedCodings(codeableConcept, bySystem);
        return codeableConcept;
    }
}
