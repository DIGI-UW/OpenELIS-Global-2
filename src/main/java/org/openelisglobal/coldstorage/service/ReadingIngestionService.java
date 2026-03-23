package org.openelisglobal.coldstorage.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.openelisglobal.coldstorage.valueholder.Freezer;

public interface ReadingIngestionService {

    void ingest(Freezer freezer, OffsetDateTime recordedAt, BigDecimal temperature, BigDecimal humidity,
            boolean transmissionOk, String errorMessage);
}
