package org.openelisglobal.tb.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.tb.valueholder.TbCultureReading;
import org.openelisglobal.tb.valueholder.TbEnums.CultureResult;
import org.openelisglobal.tb.valueholder.TbEnums.GrowthObservation;

/**
 * Data access interface for TB culture readings.
 */
public interface TbCultureReadingDAO extends BaseDAO<TbCultureReading, Integer> {

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
     * Find all inoculated samples (samples with inoculation_date set).
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
     * Find culture-positive samples (for Stage 5 downstream processing).
     */
    List<TbCultureReading> findCulturePositiveSamples();

    /**
     * Count samples by incubation week range.
     */
    Long countByIncubationWeekRange(int startWeek, int endWeek);

    // ====== Notebook Entry-Filtered Methods ======

    /**
     * Find samples currently incubating for a specific notebook entry. Joins
     * through notebook_page_sample to filter by entry.
     */
    List<TbCultureReading> findIncubatingSamplesByEntry(Integer notebookEntryId);

    /**
     * Find samples with a specific culture result for a specific notebook entry.
     */
    List<TbCultureReading> findByCultureResultAndEntry(CultureResult result, Integer notebookEntryId);

    /**
     * Count samples by incubation week range for a specific notebook entry.
     */
    Long countByIncubationWeekRangeAndEntry(int startWeek, int endWeek, Integer notebookEntryId);
}
