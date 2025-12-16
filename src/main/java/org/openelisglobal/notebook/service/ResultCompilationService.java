package org.openelisglobal.notebook.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.notebook.valueholder.ValidationStatus;

/**
 * Service interface for result compilation and dissemination per US7
 * requirements.
 *
 * <p>
 * US7 Goal: Compile analysis outputs into structured result files or database
 * records, deliver results to Data Management Team or designated recipients,
 * and flag invalid or inconclusive results for review.
 *
 * <p>
 * Workflow Requirements: a. Compile outputs into structured result files or
 * database records b. Deliver results to the Data Management Team or designated
 * recipients c. Flag invalid or inconclusive results if applicable
 */
public interface ResultCompilationService {

    /**
     * Flag a sample with validation status and reason.
     *
     * @param pageId       the notebook page ID
     * @param sampleItemId the sample item ID
     * @param status       validation status (VALID, INVALID, INCONCLUSIVE)
     * @param reason       reason for flagging (required for INVALID/INCONCLUSIVE)
     * @param userId       user performing the action
     * @return true if successfully flagged
     */
    boolean flagSample(Integer pageId, String sampleItemId, ValidationStatus status, String reason, String userId);

    /**
     * Bulk flag samples with same status and reason.
     *
     * @param pageId        the notebook page ID
     * @param sampleItemIds list of sample item IDs
     * @param status        validation status
     * @param reason        reason for flagging
     * @param userId        user performing the action
     * @return count of successfully flagged samples
     */
    int bulkFlagSamples(Integer pageId, List<String> sampleItemIds, ValidationStatus status, String reason,
            String userId);

    /**
     * Get validation statistics for a notebook page.
     *
     * @param pageId the notebook page ID
     * @return validation summary
     */
    ValidationSummary getValidationSummary(Integer pageId);

    /**
     * Get validation statistics for an entire notebook.
     *
     * @param notebookId the notebook ID
     * @return validation summary across all pages
     */
    ValidationSummary getNotebookValidationSummary(Integer notebookId);

    /**
     * Compile results into Excel format.
     *
     * @param notebookId the notebook ID
     * @param options    export options (columns, filters, etc.)
     * @return byte array containing Excel file
     */
    byte[] compileToExcel(Integer notebookId, ExportOptions options);

    /**
     * Compile results into CSV format.
     *
     * @param notebookId the notebook ID
     * @param options    export options
     * @return byte array containing CSV file
     */
    byte[] compileToCsv(Integer notebookId, ExportOptions options);

    /**
     * Generate a summary report in PDF format.
     *
     * @param notebookId the notebook ID
     * @param options    export options
     * @return byte array containing PDF file
     */
    byte[] generatePdfReport(Integer notebookId, ExportOptions options);

    /**
     * Record result delivery to a recipient.
     *
     * @param notebookId     the notebook ID
     * @param recipientName  name of recipient (e.g., "Data Management Team")
     * @param recipientEmail email of recipient
     * @param fileId         ID of the delivered file (NoteBookFile)
     * @param deliveryType   type of delivery (e.g., "email", "regulatory",
     *                       "internal")
     * @param regulatoryBody regulatory body name (for regulatory submissions)
     * @param notes          additional notes about the delivery
     * @param userId         user performing delivery
     * @return delivery record ID
     */
    Integer recordDelivery(Integer notebookId, String recipientName, String recipientEmail, Integer fileId,
            String deliveryType, String regulatoryBody, String notes, String userId);

    /**
     * Get delivery history for a notebook.
     *
     * @param notebookId the notebook ID
     * @return list of delivery records
     */
    List<DeliveryRecord> getDeliveryHistory(Integer notebookId);

    /** Validation summary statistics. */
    record ValidationSummary(long total, long valid, long invalid, long inconclusive, long pending) {
        public double validPercentage() {
            return total > 0 ? (valid * 100.0 / total) : 0;
        }

        public double invalidPercentage() {
            return total > 0 ? (invalid * 100.0 / total) : 0;
        }

        public double inconclusivePercentage() {
            return total > 0 ? (inconclusive * 100.0 / total) : 0;
        }
    }

    /** Export options for result compilation. */
    record ExportOptions(boolean includeInvalid, boolean includeInconclusive, boolean includeRawData,
            List<String> columns, String dateFormat, String title) {

        public static ExportOptions defaultOptions() {
            return new ExportOptions(true, true, true, null, "yyyy-MM-dd", "Analysis Results");
        }
    }

    /**
     * Delivery record for audit trail.
     */
    record DeliveryRecord(Integer id, String recipientName, String recipientEmail, String fileName, String deliveryType,
            String regulatoryBody, String notes, java.time.LocalDateTime deliveredAt, String deliveredBy) {
    }

    /**
     * Get samples with their validation status for a page.
     *
     * @param pageId the notebook page ID
     * @return list of sample data with validation status
     */
    List<Map<String, Object>> getSamplesWithValidation(Integer pageId);

    /**
     * Attach a compiled report to a notebook for audit trail. Creates an Excel
     * report, attaches it to the NoteBookFile list, and returns the file ID.
     *
     * @param notebookId the notebook ID
     * @param options    export options for report generation
     * @param userId     user generating and attaching the report
     * @return the ID of the created NoteBookFile record
     */
    Integer attachReportToNotebook(Integer notebookId, ExportOptions options, String userId);
}
