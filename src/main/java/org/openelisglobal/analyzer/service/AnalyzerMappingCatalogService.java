package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.terminology.Coding;

public interface AnalyzerMappingCatalogService {

    List<TestOption> searchActiveTests(String query);

    List<ResultOption> getActiveResultOptions(String testId);

    /** The active result components of a test, each by its stable code. */
    List<ComponentOption> getActiveComponents(String testId);

    /** An active test with every code it carries, in any system. */
    record TestOption(String id, String name, String code, List<Coding> codings, List<String> specimenTypes) {
        public TestOption(String id, String name, String code, List<Coding> codings) {
            this(id, name, code, codings, List.of());
        }

        public TestOption {
            codings = codings == null ? List.of() : List.copyOf(codings);
            specimenTypes = specimenTypes == null ? List.of() : List.copyOf(specimenTypes);
        }
    }

    /**
     * An answer of a test with the codings it carries; {@code componentId} names
     * its component, if any.
     */
    record ResultOption(String id, String value, String label, List<Coding> codings, String componentId) {
        public ResultOption {
            codings = codings == null ? List.of() : List.copyOf(codings);
        }

        public ResultOption(String id, String value, String label, List<Coding> codings) {
            this(id, value, label, codings, null);
        }

        public ResultOption(String id, String value, String label) {
            this(id, value, label, List.of());
        }
    }

    record ComponentOption(String id, String code, String label, boolean primary, String resultType, String unit) {
        public ComponentOption(String id, String code, String label, boolean primary) {
            this(id, code, label, primary, null, null);
        }

        public ComponentOption(String id, String code, String label) {
            this(id, code, label, false);
        }
    }
}
