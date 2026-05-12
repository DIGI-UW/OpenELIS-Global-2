package org.openelisglobal.biorepository.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openelisglobal.biorepository.valueholder.BiorepositoryQCInspection;
import org.openelisglobal.biorepository.valueholder.BiorepositoryQCInspection.QCResult;

public class BiorepositoryQcOutcomeDerivationTest {

    @Test
    public void discrepancyWithCorrectiveActionButNoCorrectionWorkflow_IsPendingCorrection() {
        BiorepositoryQCInspection inspection = new BiorepositoryQCInspection();
        inspection.setQcResult(QCResult.DISCREPANCY_FOUND);
        inspection.setCorrectiveAction("Investigate discrepancy and locate sample");
        inspection.setCorrectionActionType(null);

        assertEquals("QC_FAILED", BiorepositoryQcOutcomeDerivation.deriveQcStatus(inspection));
        assertEquals("FAILED_PENDING_CORRECTION", BiorepositoryQcOutcomeDerivation.deriveLifecycleOutcome(inspection));
    }

    @Test
    public void updateLocationCorrection_IsFailedCorrected() {
        BiorepositoryQCInspection inspection = new BiorepositoryQCInspection();
        inspection.setQcResult(QCResult.DISCREPANCY_FOUND);
        inspection.setCorrectiveAction("UPDATE_LOCATION: moved to expected rack");
        inspection.setCorrectionActionType("UPDATE_LOCATION");

        assertEquals("QC_FAILED", BiorepositoryQcOutcomeDerivation.deriveQcStatus(inspection));
        assertEquals("FAILED_CORRECTED", BiorepositoryQcOutcomeDerivation.deriveLifecycleOutcome(inspection));
    }

    @Test
    public void reassignPositionCorrection_IsFailedCorrected() {
        BiorepositoryQCInspection inspection = new BiorepositoryQCInspection();
        inspection.setQcResult(QCResult.DISCREPANCY_FOUND);
        inspection.setCorrectiveAction("REASSIGN_POSITION: set to B4");
        inspection.setCorrectionActionType("REASSIGN_POSITION");

        assertEquals("QC_FAILED", BiorepositoryQcOutcomeDerivation.deriveQcStatus(inspection));
        assertEquals("FAILED_CORRECTED", BiorepositoryQcOutcomeDerivation.deriveLifecycleOutcome(inspection));
    }

    @Test
    public void markMissingCorrection_IsMissingAndFailedMarkedMissing() {
        BiorepositoryQCInspection inspection = new BiorepositoryQCInspection();
        inspection.setQcResult(QCResult.DISCREPANCY_FOUND);
        inspection.setDiscrepancyType(BiorepositoryQCInspection.DiscrepancyType.SAMPLE_MISSING);
        inspection.setCorrectiveAction("MARK_MISSING: unable to locate sample");
        inspection.setCorrectionActionType("MARK_MISSING");

        assertEquals("MISSING", BiorepositoryQcOutcomeDerivation.deriveQcStatus(inspection));
        assertEquals("FAILED_MARKED_MISSING", BiorepositoryQcOutcomeDerivation.deriveLifecycleOutcome(inspection));
    }
}
