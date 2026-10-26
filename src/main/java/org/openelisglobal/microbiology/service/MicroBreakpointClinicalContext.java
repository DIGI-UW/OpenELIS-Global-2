package org.openelisglobal.microbiology.service;

/**
 * Only contexts explicitly represented by the receiving workflow are accepted.
 */
public final class MicroBreakpointClinicalContext {
    private MicroBreakpointClinicalContext() {
    }

    public static String validate(String value) {
        if (value == null || value.isBlank())
            return null;
        if (!"NON_MENINGITIS".equals(value))
            throw new IllegalArgumentException("AST_CLINICAL_CONTEXT_UNSUPPORTED");
        return value;
    }
}
