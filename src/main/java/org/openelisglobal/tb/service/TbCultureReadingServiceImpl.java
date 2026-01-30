package org.openelisglobal.tb.service;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.tb.dao.TbCultureReadingDAO;
import org.openelisglobal.tb.valueholder.TbCultureReading;
import org.openelisglobal.tb.valueholder.TbEnums.CultureResult;
import org.openelisglobal.tb.valueholder.TbEnums.GrowthObservation;
import org.openelisglobal.tb.valueholder.TbMediaPreparation;
import org.openelisglobal.tb.valueholder.TbSampleProcessing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for TB culture reading operations.
 */
@Service
public class TbCultureReadingServiceImpl extends AuditableBaseObjectServiceImpl<TbCultureReading, Integer>
        implements TbCultureReadingService {

    @Autowired
    private TbCultureReadingDAO tbCultureReadingDAO;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private SystemUserService systemUserService;

    public TbCultureReadingServiceImpl() {
        super(TbCultureReading.class);
        this.auditTrailLog = true;
    }

    @Override
    protected BaseDAO<TbCultureReading, Integer> getBaseObjectDAO() {
        return tbCultureReadingDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbCultureReading> findBySampleItemId(String sampleItemId) {
        return tbCultureReadingDAO.findBySampleItemId(sampleItemId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TbCultureReading> findBySampleItemIdAndWeek(String sampleItemId, Integer weekNumber) {
        return tbCultureReadingDAO.findBySampleItemIdAndWeek(sampleItemId, weekNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbCultureReading> findByGrowthObservation(GrowthObservation observation) {
        return tbCultureReadingDAO.findByGrowthObservation(observation);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TbCultureReading> findLatestBySampleItemId(String sampleItemId) {
        return tbCultureReadingDAO.findLatestBySampleItemId(sampleItemId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countGrowthDetected() {
        return tbCultureReadingDAO.countGrowthDetected();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findSampleItemIdsWithoutReadingForWeek(Integer weekNumber) {
        return tbCultureReadingDAO.findSampleItemIdsWithoutReadingForWeek(weekNumber);
    }

    // ====== Stage 4: Inoculation & Incubation Monitoring Methods ======

    @Override
    @Transactional
    public TbCultureReading inoculate(String sampleItemId, TbMediaPreparation mediaBatch, TbSampleProcessing processing,
            String inoculatedById) {
        SampleItem sampleItem = sampleItemService.get(sampleItemId);
        if (sampleItem == null) {
            throw new IllegalArgumentException("Sample item not found: " + sampleItemId);
        }

        // Create a new culture reading for week 1 with inoculation details
        TbCultureReading reading = new TbCultureReading();
        reading.setSampleItem(sampleItem);
        reading.setWeekNumber(1);
        reading.setReadingDate(new Timestamp(System.currentTimeMillis()));
        reading.setCultureMethod(mediaBatch.getMediaType() == org.openelisglobal.tb.valueholder.TbEnums.MediaType.LJ
                ? org.openelisglobal.tb.valueholder.TbEnums.CultureMethod.LJ
                : org.openelisglobal.tb.valueholder.TbEnums.CultureMethod.MGIT);
        reading.setGrowthObservation(GrowthObservation.NO_GROWTH);
        reading.setInoculationDate(new Date(System.currentTimeMillis()));
        reading.setMediaBatch(mediaBatch);
        reading.setSampleProcessing(processing);

        if (inoculatedById != null) {
            SystemUser inoculatedBy = systemUserService.get(inoculatedById);
            reading.setInoculatedBy(inoculatedBy);
        }

        // CRITICAL: Set sys user ID for audit trail before insert
        reading.setSysUserId(inoculatedById);

        Integer id = insert(reading);
        return get(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbCultureReading> findInoculatedSamples() {
        return tbCultureReadingDAO.findInoculatedSamples();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbCultureReading> findIncubatingSamples() {
        return tbCultureReadingDAO.findIncubatingSamples();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbCultureReading> findByCultureResult(CultureResult result) {
        return tbCultureReadingDAO.findByCultureResult(result);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbCultureReading> findCulturePositiveSamples() {
        return tbCultureReadingDAO.findCulturePositiveSamples();
    }

    @Override
    @Transactional
    public TbCultureReading recordReading(Integer cultureReadingId, Integer weekNumber, GrowthObservation observation,
            String notes, String readById) {
        TbCultureReading existingReading = get(cultureReadingId);
        if (existingReading == null) {
            throw new IllegalArgumentException("Culture reading not found: " + cultureReadingId);
        }

        String sampleItemId = existingReading.getSampleItem().getId();

        // Check if we're updating the same week or recording a new week
        if (existingReading.getWeekNumber() != null && existingReading.getWeekNumber().equals(weekNumber)) {
            // Same week - update existing record
            existingReading.setGrowthObservation(observation);
            existingReading.setReadingDate(new Timestamp(System.currentTimeMillis()));
            if (notes != null) {
                existingReading.setNotes(notes);
            }
            if (readById != null) {
                SystemUser readBy = systemUserService.get(readById);
                existingReading.setReadBy(readBy);
            }
            existingReading.setSysUserId(readById);
            return update(existingReading);
        }

        // Different week - check if a record for this (sample, week) already exists
        Optional<TbCultureReading> existingWeekReading = tbCultureReadingDAO.findBySampleItemIdAndWeek(sampleItemId,
                weekNumber);
        if (existingWeekReading.isPresent()) {
            // Update existing week record
            TbCultureReading weekReading = existingWeekReading.get();
            weekReading.setGrowthObservation(observation);
            weekReading.setReadingDate(new Timestamp(System.currentTimeMillis()));
            if (notes != null) {
                weekReading.setNotes(notes);
            }
            if (readById != null) {
                SystemUser readBy = systemUserService.get(readById);
                weekReading.setReadBy(readBy);
            }
            weekReading.setSysUserId(readById);
            return update(weekReading);
        }

        // Create new record for this week
        TbCultureReading newReading = new TbCultureReading();
        newReading.setSampleItem(existingReading.getSampleItem());
        newReading.setWeekNumber(weekNumber);
        newReading.setGrowthObservation(observation);
        newReading.setReadingDate(new Timestamp(System.currentTimeMillis()));
        newReading.setCultureMethod(existingReading.getCultureMethod());
        newReading.setInoculationDate(existingReading.getInoculationDate());
        newReading.setMediaBatch(existingReading.getMediaBatch());
        newReading.setSampleProcessing(existingReading.getSampleProcessing());
        newReading.setInoculatedBy(existingReading.getInoculatedBy());
        if (notes != null) {
            newReading.setNotes(notes);
        }
        if (readById != null) {
            SystemUser readBy = systemUserService.get(readById);
            newReading.setReadBy(readBy);
        }
        newReading.setSysUserId(readById);

        Integer id = insert(newReading);
        return get(id);
    }

    @Override
    @Transactional
    public TbCultureReading determineFinalResult(Integer cultureReadingId, CultureResult result, Integer positiveWeek,
            String sysUserId) {
        TbCultureReading reading = get(cultureReadingId);
        if (reading == null) {
            throw new IllegalArgumentException("Culture reading not found: " + cultureReadingId);
        }

        reading.setCultureResult(result);
        reading.setFinalResultDate(new Date(System.currentTimeMillis()));

        if (result == CultureResult.POSITIVE && positiveWeek != null) {
            reading.setPositiveWeek(positiveWeek);
        }

        // CRITICAL: Set sys user ID for audit trail before update
        reading.setSysUserId(sysUserId);

        return update(reading);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countByIncubationWeekRange(int startWeek, int endWeek) {
        return tbCultureReadingDAO.countByIncubationWeekRange(startWeek, endWeek);
    }

    @Override
    @Transactional(readOnly = true)
    public IncubationSummary getIncubationSummary() {
        long totalIncubating = findIncubatingSamples().size();
        long week1to4 = countByIncubationWeekRange(1, 4);
        long week5to8 = countByIncubationWeekRange(5, 8);
        long positive = findByCultureResult(CultureResult.POSITIVE).size();
        long negative = findByCultureResult(CultureResult.NEGATIVE).size();

        return new IncubationSummary(totalIncubating, week1to4, week5to8, positive, negative);
    }

    // ====== Notebook Entry-Filtered Methods ======

    @Override
    @Transactional(readOnly = true)
    public List<TbCultureReading> findIncubatingSamplesByEntry(Integer notebookEntryId) {
        return tbCultureReadingDAO.findIncubatingSamplesByEntry(notebookEntryId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbCultureReading> findByCultureResultAndEntry(CultureResult result, Integer notebookEntryId) {
        return tbCultureReadingDAO.findByCultureResultAndEntry(result, notebookEntryId);
    }

    @Override
    @Transactional(readOnly = true)
    public IncubationSummary getIncubationSummaryByEntry(Integer notebookEntryId) {
        long totalIncubating = findIncubatingSamplesByEntry(notebookEntryId).size();
        long week1to4 = tbCultureReadingDAO.countByIncubationWeekRangeAndEntry(1, 4, notebookEntryId);
        long week5to8 = tbCultureReadingDAO.countByIncubationWeekRangeAndEntry(5, 8, notebookEntryId);
        long positive = findByCultureResultAndEntry(CultureResult.POSITIVE, notebookEntryId).size();
        long negative = findByCultureResultAndEntry(CultureResult.NEGATIVE, notebookEntryId).size();

        return new IncubationSummary(totalIncubating, week1to4, week5to8, positive, negative);
    }
}
