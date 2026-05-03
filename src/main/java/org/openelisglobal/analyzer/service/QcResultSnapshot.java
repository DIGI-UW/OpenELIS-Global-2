package org.openelisglobal.analyzer.service;

import java.time.LocalDateTime;

public class QcResultSnapshot {
    private final QcResultStatus resultStatus;
    private final LocalDateTime resultDateTime;

    public QcResultSnapshot(QcResultStatus resultStatus, LocalDateTime resultDateTime) {
        this.resultStatus = resultStatus;
        this.resultDateTime = resultDateTime;
    }

    public QcResultStatus getResultStatus() {
        return resultStatus;
    }

    public LocalDateTime getResultDateTime() {
        return resultDateTime;
    }
}
