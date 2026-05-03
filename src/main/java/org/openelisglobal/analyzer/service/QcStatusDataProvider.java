package org.openelisglobal.analyzer.service;

import java.util.Optional;

/**
 * Adapter boundary for QC status resolution.
 *
 * Implement this with existing QC DAOs/repositories so QcStatusService can reuse
 * current QC data model without duplicating QC business logic.
 */
public interface QcStatusDataProvider {

    Optional<QcControlLotSnapshot> findControlLot(String testId, String instrumentId);

    Optional<QcResultSnapshot> findLatestResult(String controlLotId);

    Optional<QcResultSnapshot> findLatestAcceptedResult(String controlLotId);
}
