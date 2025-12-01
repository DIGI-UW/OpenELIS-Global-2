package org.openelisglobal.qc.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.qc.valueholder.QCControlLot;

/**
 * Service interface for QC Control Lot management. Supports User Story 6:
 * Manage QC Control Lots
 */
public interface QCControlLotService extends BaseObjectService<QCControlLot, String> {

    /**
     * Create a new control lot with validation and status assignment.
     *
     * @param controlLot The control lot to create
     * @return The created control lot with assigned status
     * @throws IllegalArgumentException if validation fails
     */
    QCControlLot createControlLot(QCControlLot controlLot) throws IllegalArgumentException;

    /**
     * Activate a control lot, transitioning from ESTABLISHMENT to ACTIVE status.
     *
     * @param controlLotId The ID of the control lot to activate
     * @return The activated control lot
     */
    QCControlLot activateControlLot(String controlLotId);

    /**
     * Deactivate a control lot, marking it as EXPIRED.
     *
     * @param controlLotId The ID of the control lot to deactivate
     * @return The deactivated control lot
     */
    QCControlLot deactivateControlLot(String controlLotId);

    /**
     * Get active control lots for a specific test and instrument.
     *
     * @param testId       The test ID
     * @param instrumentId The instrument ID
     * @return List of active control lots
     */
    List<QCControlLot> getActiveControlLots(Integer testId, Integer instrumentId);

    /**
     * Get a control lot by its unique lot number.
     *
     * @param lotNumber The lot number
     * @return The matching control lot, or null if not found
     */
    QCControlLot getControlLotByLotNumber(String lotNumber);

    /**
     * Check and automatically expire control lots that have passed their expiration
     * date.
     *
     * @param testId       The test ID to check
     * @param instrumentId The instrument ID to check
     */
    void checkAndExpireLots(Integer testId, Integer instrumentId);
}
