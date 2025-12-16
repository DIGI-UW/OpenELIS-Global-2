package org.openelisglobal.notebook.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.SampleRouting;

/**
 * Service interface for end-of-project archiving operations (US8). Handles
 * transfer of parent and child samples to Biorepository Laboratory with
 * complete traceability verification.
 */
public interface ArchivingService {

    /**
     * Transfer samples to biorepository laboratory for permanent archival. Creates
     * SampleStorageMovement records and updates routing to BIOREPOSITORY
     * destination.
     *
     * @param notebookId    the notebook ID
     * @param sampleItemIds list of sample item IDs to transfer (both parent and
     *                      child)
     * @param locationId    biorepository storage location ID
     * @param locationType  type of location ('device', 'shelf', 'rack', 'box')
     * @param notes         transfer notes
     * @param userId        user performing the transfer
     * @return list of created SampleRouting records with BIOREPOSITORY destination
     */
    List<SampleRouting> transferToBiorepository(Integer notebookId, List<Integer> sampleItemIds, String locationId,
            String locationType, String notes, String userId);

    /**
     * Verify traceability for all samples in a notebook. Checks: - Parent-child
     * links are intact - Movement history is complete - Storage assignments are
     * verified
     *
     * @param notebookId the notebook ID
     * @return traceability verification result with details
     */
    TraceabilityResult verifyTraceability(Integer notebookId);

    /**
     * Finalize a notebook after archiving is complete. Transitions notebook status
     * to FINALIZED and prevents further modifications.
     *
     * @param notebookId the notebook ID to finalize
     * @param userId     user performing the finalization
     * @return the finalized notebook
     * @throws IllegalStateException if traceability verification fails or not all
     *                               samples are archived
     */
    NoteBook finalizeNotebook(Integer notebookId, String userId);

    /**
     * Check if a notebook can be finalized. Returns false if: - Traceability
     * verification fails - Not all samples are archived/transferred - Required
     * pages are incomplete
     *
     * @param notebookId the notebook ID
     * @return true if notebook can be finalized
     */
    boolean canFinalize(Integer notebookId);

    /**
     * Get archiving progress for a notebook.
     *
     * @param notebookId the notebook ID
     * @return archiving progress with counts
     */
    ArchivingProgress getArchivingProgress(Integer notebookId);

    /**
     * Get all samples (parent and child) eligible for archival transfer.
     *
     * @param notebookId the notebook ID
     * @return map with "parent" and "child" lists of sample item IDs
     */
    Map<String, List<Integer>> getArchivableSamples(Integer notebookId);

    /**
     * Generate traceability report for all sample lineages in a notebook.
     *
     * @param notebookId the notebook ID
     * @return traceability report data
     */
    TraceabilityReport generateTraceabilityReport(Integer notebookId);

    /** Result of traceability verification. */
    record TraceabilityResult(boolean passed, List<TraceabilityCheck> checks, String summary) {
        public boolean hasCriticalFailures() {
            return checks.stream().anyMatch(c -> c.critical() && !c.passed());
        }
    }

    /** Individual traceability check result. */
    record TraceabilityCheck(String checkName, String description, boolean passed, boolean critical,
            List<String> issues) {
    }

    /** Archiving progress summary. */
    record ArchivingProgress(int totalSamples, int archivedSamples, int pendingSamples, int parentSamples,
            int childSamples, int archivedParents, int archivedChildren, boolean readyForFinalization) {
        public double percentComplete() {
            return totalSamples > 0 ? (archivedSamples * 100.0 / totalSamples) : 0;
        }
    }

    /** Traceability report for audit purposes. */
    record TraceabilityReport(Integer notebookId, String notebookTitle, List<SampleLineage> lineages,
            List<MovementRecord> movements, String generatedAt, String generatedBy) {
    }

    /** Sample lineage information. */
    record SampleLineage(Integer sampleItemId, String sampleId, String externalId, Integer parentSampleItemId,
            List<Integer> childSampleItemIds, String currentLocation, String archiveLocation) {
    }

    /** Movement record for audit trail. */
    record MovementRecord(Integer sampleItemId, String fromLocation, String toLocation, String movedAt, String movedBy,
            String reason) {
    }
}
