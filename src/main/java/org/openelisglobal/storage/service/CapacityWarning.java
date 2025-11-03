package org.openelisglobal.storage.service;

/**
 * Value object for rack capacity warnings
 */
public class CapacityWarning {
    private final int occupied;
    private final int totalCapacity;
    private final int percentage;
    private final String warningMessage;

    public CapacityWarning(int occupied, int totalCapacity, int percentage, String warningMessage) {
        this.occupied = occupied;
        this.totalCapacity = totalCapacity;
        this.percentage = percentage;
        this.warningMessage = warningMessage;
    }

    public int getOccupied() {
        return occupied;
    }

    public int getTotalCapacity() {
        return totalCapacity;
    }

    public int getPercentage() {
        return percentage;
    }

    public String getWarningMessage() {
        return warningMessage;
    }

    public boolean hasWarning() {
        return warningMessage != null && !warningMessage.isEmpty();
    }
}
