package org.openelisglobal.batchworkplan.form;

/**
 * Release a batch. {@code override} is the user's confirmation that a batch
 * whose QC is not Pass should be released anyway, raising an NCE, and
 * {@code reason} is why; an override is refused without one.
 */
public record BatchGenerateRequest(boolean override, String reason) {
}
