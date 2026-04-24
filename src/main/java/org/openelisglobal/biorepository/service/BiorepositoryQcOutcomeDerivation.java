package org.openelisglobal.biorepository.service;

import org.openelisglobal.biorepository.valueholder.BiorepositoryQCInspection;

/**
 * Derives QC status and lifecycle outcome from persisted inspection state.
 */
public final class BiorepositoryQcOutcomeDerivation {

    private BiorepositoryQcOutcomeDerivation() {
    }

    public static String deriveQcStatus(BiorepositoryQCInspection inspection) {
        if (inspection == null || inspection.getQcResult() == null) {
            return "UNKNOWN";
        }
        if (BiorepositoryQCInspection.QCResult.VERIFIED.equals(inspection.getQcResult())) {
            return "VALID";
        }
        if (BiorepositoryQCInspection.QCResult.DISCREPANCY_FOUND.equals(inspection.getQcResult())) {
            if (isMarkMissingCorrectionApplied(inspection)) {
                return "MISSING";
            }
            return "QC_FAILED";
        }
        return "UNKNOWN";
    }

    public static String deriveLifecycleOutcome(BiorepositoryQCInspection inspection) {
        if (inspection == null || inspection.getQcResult() == null) {
            return "UNKNOWN";
        }
        if (BiorepositoryQCInspection.QCResult.VERIFIED.equals(inspection.getQcResult())) {
            return "PASSED";
        }
        if (BiorepositoryQCInspection.QCResult.DISCREPANCY_FOUND.equals(inspection.getQcResult())) {
            if (isMarkMissingCorrectionApplied(inspection)) {
                return "FAILED_MARKED_MISSING";
            }
            if (hasAppliedCorrectionWorkflow(inspection)) {
                return "FAILED_CORRECTED";
            }
            return "FAILED_PENDING_CORRECTION";
        }
        return "UNKNOWN";
    }

    public static boolean hasAppliedCorrectionWorkflow(BiorepositoryQCInspection inspection) {
        String correctionActionType = trimToNull(inspection != null ? inspection.getCorrectionActionType() : null);
        return correctionActionType != null;
    }

    public static boolean isMarkMissingCorrectionApplied(BiorepositoryQCInspection inspection) {
        String correctionActionType = trimToNull(inspection != null ? inspection.getCorrectionActionType() : null);
        return correctionActionType != null && correctionActionType.equalsIgnoreCase("MARK_MISSING");
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
