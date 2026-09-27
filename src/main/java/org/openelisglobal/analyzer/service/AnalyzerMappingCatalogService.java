package org.openelisglobal.analyzer.service;

import java.util.List;

public interface AnalyzerMappingCatalogService {

    List<TestOption> searchActiveTests(String query);

    List<ResultOption> getActiveResultOptions(String testId);

    record TestOption(String id, String name, String code, List<String> loincCodes) {
        public TestOption {
            loincCodes = loincCodes == null ? List.of() : List.copyOf(loincCodes);
        }
    }

    record ResultOption(String id, String value, String label) {
    }
}
