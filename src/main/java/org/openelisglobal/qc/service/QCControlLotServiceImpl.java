package org.openelisglobal.qc.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.qc.dao.QCControlLotDAO;
import org.openelisglobal.qc.dao.QCStatisticsDAO;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.qc.valueholder.QCStatistics;
import org.openelisglobal.qc.valueholder.WestgardRuleConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for QC Control Lot management. Implements business
 * logic for control lot lifecycle per US6.
 */
@Service
public class QCControlLotServiceImpl extends AuditableBaseObjectServiceImpl<QCControlLot, String>
        implements QCControlLotService {

    @Autowired
    private QCControlLotDAO controlLotDAO;

    @Autowired
    private QCStatisticsDAO statisticsDAO;

    @Autowired
    private QCControlLotValidator validator;

    @Autowired
    private WestgardRuleConfigService ruleConfigService;

    public QCControlLotServiceImpl() {
        super(QCControlLot.class);
        this.auditTrailLog = true;
    }

    @Override
    protected QCControlLotDAO getBaseObjectDAO() {
        return controlLotDAO;
    }

    @Override
    @Transactional
    public QCControlLot createControlLot(QCControlLot controlLot) throws IllegalArgumentException {
        // All lots start as ESTABLISHMENT — validated before insert
        controlLot.setStatus("ESTABLISHMENT");
        validator.validate(controlLot);

        String id = controlLotDAO.insert(controlLot);
        QCControlLot persisted = controlLotDAO.get(id).orElse(null);

        // MANUFACTURER_FIXED: seed stats, then activate (validator runs again)
        if (persisted != null && "MANUFACTURER_FIXED".equals(persisted.getCalculationMethod())) {
            seedManufacturerStatistics(persisted);
            persisted.setStatus("ACTIVE");
            validator.validate(persisted);
            controlLotDAO.update(persisted);
        }

        // Seed default Westgard rule configs if none exist for this test+instrument
        if (persisted != null) {
            ensureRuleConfigsExist(persisted.getTestId(), persisted.getInstrumentId());
        }

        return persisted;
    }

    @Override
    @Transactional
    public QCControlLot update(QCControlLot controlLot) {
        // For MANUFACTURER_FIXED: seed stats before validation so ACTIVE check passes
        if ("MANUFACTURER_FIXED".equals(controlLot.getCalculationMethod()) && controlLot.getManufacturerMean() != null
                && controlLot.getManufacturerStdDev() != null) {
            invalidateExistingStatistics(controlLot.getId());
            seedManufacturerStatistics(controlLot);
        }

        validator.validate(controlLot);
        return super.update(controlLot);
    }

    @Override
    @Transactional
    public QCControlLot activateControlLot(String controlLotId) {
        QCControlLot controlLot = controlLotDAO.get(controlLotId).orElse(null);
        if (controlLot == null) {
            throw new IllegalArgumentException("Control lot not found: " + controlLotId);
        }

        controlLot.setStatus("ACTIVE");
        validator.validate(controlLot);
        controlLotDAO.update(controlLot);
        return controlLot;
    }

    @Override
    @Transactional
    public QCControlLot deactivateControlLot(String controlLotId) {
        QCControlLot controlLot = controlLotDAO.get(controlLotId).orElse(null);
        if (controlLot != null) {
            controlLot.setStatus("EXPIRED");
            controlLotDAO.update(controlLot);
        }
        return controlLot;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QCControlLot> getActiveControlLots(Integer testId, Integer instrumentId) {
        return controlLotDAO.getActiveByTestAndInstrument(testId, instrumentId);
    }

    @Override
    @Transactional(readOnly = true)
    public QCControlLot getControlLotByLotNumber(String lotNumber) {
        return controlLotDAO.getByLotNumber(lotNumber);
    }

    @Override
    @Transactional
    public void checkAndExpireLots(Integer testId, Integer instrumentId) {
        List<QCControlLot> activeLots = controlLotDAO.getActiveByTestAndInstrument(testId, instrumentId);
        Timestamp now = new Timestamp(System.currentTimeMillis());

        for (QCControlLot lot : activeLots) {
            if (lot.getExpirationDate() != null && lot.getExpirationDate().before(now)) {
                lot.setStatus("EXPIRED");
                controlLotDAO.update(lot);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<QCControlLot> getAllControlLots() {
        return controlLotDAO.getAll();
    }

    private void invalidateExistingStatistics(String controlLotId) {
        QCStatistics existing = statisticsDAO.findLatestByControlLot(controlLotId);
        if (existing != null && existing.getValidityEnd() == null) {
            existing.setValidityEnd(new Timestamp(System.currentTimeMillis()));
            statisticsDAO.update(existing);
        }
    }

    /**
     * Ensure Westgard rule configs exist for a test+instrument combination. Creates
     * STANDARD preset defaults if none exist, so that rule evaluation is active
     * from the first QC result.
     */
    private void ensureRuleConfigsExist(Integer testId, Integer instrumentId) {
        List<WestgardRuleConfig> existing = ruleConfigService.findByTestAndInstrument(testId, instrumentId);
        if (existing.isEmpty()) {
            ruleConfigService.createDefaultConfig(testId, instrumentId);
            LogEvent.logInfo(this.getClass().getName(), "ensureRuleConfigsExist",
                    "Seeded default Westgard rule configs for test " + testId + ", instrument " + instrumentId);
        }
    }

    private void seedManufacturerStatistics(QCControlLot controlLot) {
        Timestamp now = new Timestamp(System.currentTimeMillis());

        QCStatistics stats = new QCStatistics();
        stats.setId(UUID.randomUUID().toString());
        stats.setControlLotId(controlLot.getId());
        stats.setMean(BigDecimal.valueOf(controlLot.getManufacturerMean()));
        stats.setStandardDeviation(BigDecimal.valueOf(controlLot.getManufacturerStdDev()));
        stats.setNumValues(0);
        stats.setCalculationMethod("MANUFACTURER_FIXED");
        stats.setCalculationDate(now);
        stats.setValidityStart(now);
        stats.setSystemUserId(controlLot.getSystemUserId());

        statisticsDAO.insert(stats);
    }
}
