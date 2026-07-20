package org.openelisglobal.testcatalog.service;

/**
 * OGC-1112 (FR-2..4) — create-in-place backing service for the unified Test
 * Catalog editor. Creates a minimal test in Inactive status (name + reporting
 * name localizations, code, lab unit, domain, one sample type) so it can then
 * be configured through the editor's sections.
 */
public interface TestCatalogCreationService {

    class CreateTestParams {
        public String name;
        public String reportingName;
        public String code;
        public String labUnitId;
        public String sampleTypeId;
        public String domain;
        public Boolean amr;
        public Boolean orderable;
        public String description;
    }

    /** True if an existing test already uses this local code (case-insensitive). */
    boolean codeInUse(String code);

    /**
     * Creates an Inactive test from the given params and returns its new id. Also
     * creates the name / reporting-name localizations and the single sample-type
     * link. Caller must have validated required fields and code uniqueness.
     */
    String createInactiveTest(CreateTestParams params, String sysUserId);
}
