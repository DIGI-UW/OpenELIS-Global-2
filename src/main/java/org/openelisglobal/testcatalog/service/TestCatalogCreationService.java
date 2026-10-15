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
        // OGC-1145 FR-2: all sample types to link; wins over the scalar when set.
        public java.util.List<String> sampleTypeIds;
        public String domain;
        public Boolean amr;
        public Boolean orderable;
        public String description;
    }

    /** Thrown by a create whose code another test took meanwhile (OGC-1464). */
    class CodeInUseException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public CodeInUseException(String code) {
            super("Test code already in use: " + code);
        }
    }

    /**
     * True if an existing test already uses this local code, compared trimmed and
     * case-insensitive against the stored codes (not a cached list).
     */
    boolean codeInUse(String code);

    /** As {@link #codeInUse(String)}, ignoring the test {@code excludeTestId}. */
    boolean codeInUse(String code, String excludeTestId);

    /**
     * Creates an Inactive test from the given params and returns its new id. Also
     * creates the name / reporting-name localizations and the single sample-type
     * link. Creates are serialized and the code is checked again under that lock,
     * so two creates with one code cannot both succeed: the second throws
     * {@link CodeInUseException} (OGC-1464).
     */
    String createInactiveTest(CreateTestParams params, String sysUserId);
}
