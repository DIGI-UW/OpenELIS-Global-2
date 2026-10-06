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

    /** An answer of a test; {@code componentId} names its component, if any. */
    record ResultOption(String id, String value, String label, String answerCode, String componentId) {
        public ResultOption(String id, String value, String label, String answerCode) {
            this(id, value, label, answerCode, null);
        }

        public ResultOption(String id, String value, String label) {
            this(id, value, label, null);
        }
    }

    record ComponentOption(String id, String code) {
    }
}
