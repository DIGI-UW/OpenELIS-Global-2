package org.openelisglobal.analyzer.dao;

import java.util.Optional;
import org.hibernate.Session;
import org.openelisglobal.analyzer.valueholder.AnalyzerBridgePairing;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AnalyzerBridgePairingDAOImpl extends BaseDAOImpl<AnalyzerBridgePairing, String>
        implements AnalyzerBridgePairingDAO {

    public AnalyzerBridgePairingDAOImpl() {
        super(AnalyzerBridgePairing.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalyzerBridgePairing> findCurrent() {
        return entityManager.unwrap(Session.class)
                .createQuery("FROM AnalyzerBridgePairing p ORDER BY p.id", AnalyzerBridgePairing.class).setMaxResults(1)
                .getResultList().stream().findFirst();
    }
}
