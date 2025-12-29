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
        TbCultureReading reading = get(cultureReadingId);
        if (reading == null) {
            throw new IllegalArgumentException("Culture reading not found: " + cultureReadingId);
        }

        reading.setWeekNumber(weekNumber);
        reading.setGrowthObservation(observation);
        reading.setReadingDate(new Timestamp(System.currentTimeMillis()));
        if (notes != null) {
            reading.setNotes(notes);
        }

        if (readById != null) {
            SystemUser readBy = systemUserService.get(readById);
            reading.setReadBy(readBy);
        }

        return update(reading);
    }

    @Override
    @Transactional
    public TbCultureReading determineFinalResult(Integer cultureReadingId, CultureResult result, Integer positiveWeek) {
        TbCultureReading reading = get(cultureReadingId);
        if (reading == null) {
            throw new IllegalArgumentException("Culture reading not found: " + cultureReadingId);
        }

        reading.setCultureResult(result);
        reading.setFinalResultDate(new Date(System.currentTimeMillis()));

        if (result == CultureResult.POSITIVE && positiveWeek != null) {
            reading.setPositiveWeek(positiveWeek);
        }

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
}
