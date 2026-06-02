package org.openelisglobal.coldstorage.service;

import java.util.Optional;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_COLDSTORAGE_MANAGE')")
public interface ModbusClientService {

    Optional<ReadingResult> readCurrentValues(Freezer freezer);

    record ReadingResult(double temperatureCelsius, Double humidityPercentage) {
    }
}
