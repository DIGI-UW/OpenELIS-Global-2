package org.openelisglobal.analyzer.dao;

import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.AnalyzerBridgePairing;
import org.openelisglobal.common.dao.BaseDAO;

public interface AnalyzerBridgePairingDAO extends BaseDAO<AnalyzerBridgePairing, String> {

    /** The one pairing record, if OpenELIS has created its identity. */
    Optional<AnalyzerBridgePairing> findCurrent();
}
