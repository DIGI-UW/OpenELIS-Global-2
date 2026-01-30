package org.openelisglobal.tb.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.tb.valueholder.TbCultureReading;
import org.openelisglobal.tb.valueholder.TbEnums.CultureResult;
import org.openelisglobal.tb.valueholder.TbEnums.GrowthObservation;
import org.openelisglobal.tb.valueholder.TbMediaPreparation;
import org.openelisglobal.tb.valueholder.TbSampleProcessing;

/**
 * Service interface for TB culture reading operations.
 */
public interface TbCultureReadingService extends BaseObjectService<TbCultureReading, Integer> {

    /**
     * Find all culture readings for a sample item, ordered by week number.
     */
    List<TbCultureReading> findBySampleItemId(String sampleItemId);

    /**
     * Find a specific week's reading for a sample.
     */
    Optional<TbCultureReading> findBySampleItemIdAndWeek(String sampleItemId, Integer weekNumber);

    /**
     * Find samples with a specific growth observation.
     */
    List<TbCultureReading> findByGrowthObservation(GrowthObservation observation);

    /**
     * Find the latest reading for a sample.
     */
    Optional<TbCultureReading> findLatestBySampleItemId(String sampleItemId);

    /**
     * Count samples with growth detected.
     */
    Long countGrowthDetected();

    /**
     * Find samples pending reading for a specific week.
     */
    List<String> findSampleItemIdsWithoutReadingForWeek(Integer weekNumber);

    // ====== Stage 4: Inoculation & Incubation Monitoring Methods ======

    /**
     * Inoculate a sample to a prepared media batch.
     */
    TbCultureReading inoculate(String sampleItemId, TbMediaPreparation mediaBatch, TbSampleProcessing processing,
            String inoculatedById);

    /**
     * Find all inoculated samples.
     */
    List<TbCultureReading> findInoculatedSamples();

    /**
     * Find samples currently incubating (inoculated but no final result).
     */
    List<TbCultureReading> findIncubatingSamples();

    /**
     * Find samples with a specific culture result.
     */
    List<TbCultureReading> findByCultureResult(CultureResult result);

    /**
     * Find culture-positive samples.
     */
    List<TbCultureReading> findCulturePositiveSamples();

    /**
     * Record a weekly reading for an incubating sample.
     */
    TbCultureReading recordReading(Integer cultureReadingId, Integer weekNumber, GrowthObservation observation,
            String notes, String readById);

    /**
     * Determine and set the final culture result.
     */
    TbCultureReading determineFinalResult(Integer cultureReadingId, CultureResult result, Integer positiveWeek,
            String sysUserId);

    /**
     * Count samples by incubation week range.
     */
    Long countByIncubationWeekRange(int startWeek, int endWeek);

    /**
     * Get incubation monitoring summary statistics.
     */
    IncubationSummary getIncubationSummary();

    /**
     * Summary statistics for incubation monitoring.
     */
    record IncubationSummary(long totalIncubating, long week1to4, long week5to8, long positive, long negative) {
    }

    // ====== Notebook Entry-Filtered Methods ======

    /**
     * Find samples currently incubating for a specific notebook entry.
     */
    List<TbCultureReading> findIncubatingSamplesByEntry(Integer notebookEntryId);

    /**
     * Find samples with a specific culture result for a specific notebook entry.
     */
    List<TbCultureReading> findByCultureResultAndEntry(CultureResult result, Integer notebookEntryId);

    /**
     * Get incubation monitoring summary statistics for a specific notebook entry.
     */
    IncubationSummary getIncubationSummaryByEntry(Integer notebookEntryId);
}
