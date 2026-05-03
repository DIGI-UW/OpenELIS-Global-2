package org.openelisglobal.analyzer.service;

import java.time.LocalTime;

public class QcControlLotSnapshot {
    private final String controlLotId;
    private final boolean active;
    private final QcFrequencyType frequencyType;
    private final Integer customHours;
    private final LocalTime shiftStart;
    private final LocalTime shiftEnd;

    public QcControlLotSnapshot(String controlLotId, boolean active, QcFrequencyType frequencyType, Integer customHours,
            LocalTime shiftStart, LocalTime shiftEnd) {
        this.controlLotId = controlLotId;
        this.active = active;
        this.frequencyType = frequencyType;
        this.customHours = customHours;
        this.shiftStart = shiftStart;
        this.shiftEnd = shiftEnd;
    }

    public String getControlLotId() {
        return controlLotId;
    }

    public boolean isActive() {
        return active;
    }

    public QcFrequencyType getFrequencyType() {
        return frequencyType;
    }

    public Integer getCustomHours() {
        return customHours;
    }

    public LocalTime getShiftStart() {
        return shiftStart;
    }

    public LocalTime getShiftEnd() {
        return shiftEnd;
    }
}
