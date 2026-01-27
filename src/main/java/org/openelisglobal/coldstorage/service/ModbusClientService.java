package org.openelisglobal.coldstorage.service;

import java.util.Optional;
import org.openelisglobal.coldstorage.valueholder.Freezer;

public interface ModbusClientService {

    Optional<ReadingResult> readCurrentValues(Freezer freezer);

    record ReadingResult(double temperatureCelsius, Double humidityPercentage) {
    }
}
