package org.openelisglobal.analyzer.service;

import java.util.List;

public interface AnalyzerMappingCatalogService {

    List<TestOption> searchActiveTests(String query);

    List<ResultOption> getActiveResultOptions(String testId);

    /** The active result components of a test, each by its stable code. */
    List<ComponentOption> getActiveComponents(String testId);

    record TestOption(String id, String name, String code, List<String> loincCodes, List<String> specimenTypes) {
        public TestOption(String id, String name, String code, List<String> loincCodes) {
            this(id, name, code, loincCodes, List.of());
        }

        public TestOption {
            loincCodes = loincCodes == null ? List.of() : List.copyOf(loincCodes);
            specimenTypes = specimenTypes == null ? List.of() : List.copyOf(specimenTypes);
        }
    }

    /**
     * An answer of a test with the standard codings it carries; {@code componentId}
     * names its component, if any.
     */
    record ResultOption(String id, String value, String label, List<AnswerCoding> codings, String componentId) {
        public ResultOption {
            codings = codings == null ? List.of() : List.copyOf(codings);
        }

        public ResultOption(String id, String value, String label, List<AnswerCoding> codings) {
            this(id, value, label, codings, null);
        }

        public ResultOption(String id, String value, String label) {
            this(id, value, label, List.of());
        }
    }

    /** One standard code of an answer, in a FHIR system URI. */
    record AnswerCoding(String system, String code) {
    }

    record ComponentOption(String id, String code, String label, boolean primary) {
        public ComponentOption(String id, String code, String label) {
            this(id, code, label, false);
        }
    }
}
