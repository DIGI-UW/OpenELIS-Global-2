package org.openelisglobal.qc.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.qc.dao.QCControlLotDAO;
import org.openelisglobal.qc.valueholder.QCControlLot;
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

    public QCControlLotServiceImpl() {
        super(QCControlLot.class);
    }

    @Override
    protected QCControlLotDAO getBaseObjectDAO() {
        return controlLotDAO;
    }

    @Override
    @Transactional
    public QCControlLot createControlLot(QCControlLot controlLot) throws IllegalArgumentException {
        validateControlLot(controlLot);
        assignInitialStatus(controlLot);
        return controlLotDAO.insert(controlLot);
    }

    @Override
    @Transactional
    public QCControlLot activateControlLot(String controlLotId) {
        QCControlLot controlLot = controlLotDAO.get(controlLotId);
        if (controlLot != null) {
            controlLot.setStatus("ACTIVE");
            controlLotDAO.update(controlLot);
        }
        return controlLot;
    }

    @Override
    @Transactional
    public QCControlLot deactivateControlLot(String controlLotId) {
        QCControlLot controlLot = controlLotDAO.get(controlLotId);
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

    /**
     * Validate control lot configuration based on calculation method.
     *
     * @param controlLot The control lot to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateControlLot(QCControlLot controlLot) throws IllegalArgumentException {
        String calculationMethod = controlLot.getCalculationMethod();

        if ("MANUFACTURER_FIXED".equals(calculationMethod)) {
            if (controlLot.getManufacturerMean() == null || controlLot.getManufacturerStdDev() == null) {
                throw new IllegalArgumentException(
                        "Manufacturer fixed method requires both mean and standard deviation");
            }
        }

        if ("INITIAL_RUNS".equals(calculationMethod)) {
            Integer initialRunsCount = controlLot.getInitialRunsCount();
            if (initialRunsCount == null || initialRunsCount <= 0) {
                throw new IllegalArgumentException("Initial runs method requires a positive initial runs count");
            }
        }
    }

    /**
     * Assign initial status based on calculation method. Per US6: -
     * MANUFACTURER_FIXED: Immediately ACTIVE (values known) - INITIAL_RUNS/ROLLING:
     * ESTABLISHMENT (need to collect data)
     *
     * @param controlLot The control lot to assign status
     */
    private void assignInitialStatus(QCControlLot controlLot) {
        if ("MANUFACTURER_FIXED".equals(controlLot.getCalculationMethod())) {
            controlLot.setStatus("ACTIVE");
        } else {
            controlLot.setStatus("ESTABLISHMENT");
        }
    }
}
