package org.openelisglobal.analyzer.service;

import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class DefaultQcStatusDataProvider implements QcStatusDataProvider {

    @Override
    public Optional<QcControlLotSnapshot> findControlLot(String testId, String instrumentId) {
        return Optional.empty();
    }

    @Override
    public Optional<QcResultSnapshot> findLatestResult(String controlLotId) {
        return Optional.empty();
    }

    @Override
    public Optional<QcResultSnapshot> findLatestAcceptedResult(String controlLotId) {
        return Optional.empty();
    }
}
