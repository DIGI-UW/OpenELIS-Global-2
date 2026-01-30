package org.openelisglobal.tb.service;

import java.time.LocalDate;
import java.util.Map;

/**
 * Service for generating TB laboratory workflow reporting metrics. Aggregates
 * data from both dedicated tables (tb_culture_reading, etc.) and JSONB fields
 * in notebook_page_sample.data.
 */
public interface TbReportingService {

    /**
     * Get sample intake metrics from Page 1 (Registration). Data source:
     * notebook_page_sample.data JSONB (specimenType, referringFacility,
     * treatmentHistory)
     *
     * @param notebookId the TB notebook ID to query
     * @param startDate  start of date range (inclusive)
     * @param endDate    end of date range (inclusive)
     * @return Map containing: - totalReceived: total samples registered -
     *         bySpecimenType: Map of specimen type to count - byReferringFacility:
     *         Map of facility to count - byTreatmentHistory: Map of treatment
     *         history to count
     */
    Map<String, Object> getSampleIntakeMetrics(Integer notebookId, LocalDate startDate, LocalDate endDate);

    /**
     * Get QC metrics from Page 2 (Quality Check). Data source:
     * notebook_page_sample.data JSONB (qcResult, rejectionReason)
     *
     * @param notebookId the TB notebook ID to query
     * @param startDate  start of date range (inclusive)
     * @param endDate    end of date range (inclusive)
     * @return Map containing: - totalChecked: total samples QC'd - passCount:
     *         samples with PASS result - passRate: percentage of PASS results -
     *         failDiscardCount: samples with FAIL_DISCARD result -
     *         failProceedCount: samples with FAIL_PROCEED result -
     *         byRejectionReason: Map of rejection reason to count
     */
    Map<String, Object> getQcMetrics(Integer notebookId, LocalDate startDate, LocalDate endDate);

    /**
     * Get culture metrics from Page 4 (Incubation & Monitoring). Data source:
     * tb_culture_reading dedicated table, filtered by notebook.
     *
     * @param notebookId the TB notebook ID to query
     * @param startDate  start of date range (inclusive)
     * @param endDate    end of date range (inclusive)
     * @return Map containing: - totalWithResults: cultures with final result -
     *         positiveCount: cultures with POSITIVE result - positivityRate:
     *         percentage of POSITIVE results - negativeCount: cultures with
     *         NEGATIVE result - contaminatedCount: cultures with CONTAMINATED
     *         result - contaminationRate: percentage of CONTAMINATED results -
     *         avgPositiveWeek: average week when growth detected (for positive
     *         cultures)
     */
    Map<String, Object> getCultureMetrics(Integer notebookId, LocalDate startDate, LocalDate endDate);

    /**
     * Get smear microscopy metrics from Page 5 (Test Execution). Data source:
     * notebook_page_sample.data JSONB (smearMethod, afbResult)
     *
     * @param notebookId the TB notebook ID to query
     * @param startDate  start of date range (inclusive)
     * @param endDate    end of date range (inclusive)
     * @return Map containing: - totalSmears: total smear tests performed -
     *         positiveCount: smears with positive result (SCANTY, PLUS1, PLUS2,
     *         PLUS3) - positivityRate: percentage of positive smears - byGrading:
     *         Map of AFB grading to count (NEGATIVE, SCANTY, PLUS1, PLUS2, PLUS3) -
     *         byMethod: Map of smear method to count (ZN, CONCENTRATED,
     *         FLUORESCENT)
     */
    Map<String, Object> getSmearMetrics(Integer notebookId, LocalDate startDate, LocalDate endDate);

    /**
     * Get GeneXpert metrics from Page 5 (Test Execution). Data source:
     * notebook_page_sample.data JSONB (geneXpertResult)
     *
     * @param notebookId the TB notebook ID to query
     * @param startDate  start of date range (inclusive)
     * @param endDate    end of date range (inclusive)
     * @return Map containing: - totalTests: total GeneXpert tests performed -
     *         mtbDetectedCount: tests with MTB detected - mtbDetectionRate:
     *         percentage of MTB detected - rifSensitiveCount: MTB detected, RIF
     *         sensitive - rifResistantCount: MTB detected, RIF resistant -
     *         rifResistanceRate: percentage of RIF resistance among MTB detected
     */
    Map<String, Object> getGeneXpertMetrics(Integer notebookId, LocalDate startDate, LocalDate endDate);

    /**
     * Get DST (Drug Susceptibility Testing) metrics from Page 5 (Test Execution).
     * Data source: notebook_page_sample.data JSONB (dstClassification, dstResults)
     *
     * @param notebookId the TB notebook ID to query
     * @param startDate  start of date range (inclusive)
     * @param endDate    end of date range (inclusive)
     * @return Map containing: - totalDst: total DST performed -
     *         fullySensitiveCount: fully drug-sensitive cases - mdrCount: MDR-TB
     *         cases (resistant to INH and RMP) - mdrRate: percentage of MDR-TB -
     *         xdrCount: XDR-TB cases (MDR + FLQ + injectable resistance) - xdrRate:
     *         percentage of XDR-TB - byClassification: Map of DST classification to
     *         count
     */
    Map<String, Object> getDstMetrics(Integer notebookId, LocalDate startDate, LocalDate endDate);

    /**
     * Get turnaround time (TAT) metrics across workflow stages. Data sources: -
     * Culture TAT: tb_culture_reading (inoculation_date to final_result_date) -
     * Smear TAT: notebook_page_sample completion timestamps
     *
     * @param notebookId the TB notebook ID to query
     * @param startDate  start of date range (inclusive)
     * @param endDate    end of date range (inclusive)
     * @return Map containing: - cultureTatDays: average culture TAT in days -
     *         minCultureTatDays: minimum culture TAT - maxCultureTatDays: maximum
     *         culture TAT
     */
    Map<String, Object> getTurnaroundTimeMetrics(Integer notebookId, LocalDate startDate, LocalDate endDate);

    /**
     * Get a comprehensive dashboard summary combining key metrics from all pages.
     *
     * @param notebookId the TB notebook ID to query
     * @param startDate  start of date range (inclusive)
     * @param endDate    end of date range (inclusive)
     * @return Map containing aggregated summary metrics
     */
    Map<String, Object> getDashboardSummary(Integer notebookId, LocalDate startDate, LocalDate endDate);

    /**
     * Get disposal and archiving metrics from Page 7 (Disposal & Archiving). Data
     * source: notebook_page_sample.data JSONB (disposalStatus, disposalReason,
     * disposalMethod, biorepositoryTransferId)
     *
     * @param notebookId the TB notebook ID to query
     * @param startDate  start of date range (inclusive)
     * @param endDate    end of date range (inclusive)
     * @return Map containing: - totalProcessed: total samples disposed or archived
     *         - disposedCount: samples with DISPOSED status - archivedCount:
     *         samples with ARCHIVED status (biorepository transfers) -
     *         byDisposalReason: Map of disposal reason to count - byDisposalMethod:
     *         Map of disposal method to count - biorepositoryTransferCount: count
     *         of samples transferred to biorepository
     */
    Map<String, Object> getDisposalMetrics(Integer notebookId, LocalDate startDate, LocalDate endDate);
}
