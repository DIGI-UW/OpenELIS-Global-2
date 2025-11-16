package org.openelisglobal.storage.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Value object representing a parsed storage location barcode. Supports 2-5
 * level hierarchical barcodes per FR-023
 *
 * Barcode format: ROOM-DEVICE[-SHELF[-RACK[-POSITION]]] Examples: - 2-level:
 * MAIN-FRZ01 - 3-level: MAIN-FRZ01-SHA - 4-level: MAIN-FRZ01-SHA-RKR1 -
 * 5-level: MAIN-FRZ01-SHA-RKR1-A5
 */
public class ParsedBarcode {

    private boolean valid;
    private String errorMessage;
    private String roomCode;
    private String deviceCode;
    private String shelfCode;
    private String rackCode;
    private String positionCode;
    private List<String> components;

    public ParsedBarcode() {
        this.valid = false;
        this.components = new ArrayList<>();
    }

    /**
     * Get the number of levels in the barcode (2-5)
     */
    public int getLevelCount() {
        int count = 0;
        if (roomCode != null)
            count++;
        if (deviceCode != null)
            count++;
        if (shelfCode != null)
            count++;
        if (rackCode != null)
            count++;
        if (positionCode != null)
            count++;
        return count;
    }

    // Getters and setters
    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public String getShelfCode() {
        return shelfCode;
    }

    public void setShelfCode(String shelfCode) {
        this.shelfCode = shelfCode;
    }

    public String getRackCode() {
        return rackCode;
    }

    public void setRackCode(String rackCode) {
        this.rackCode = rackCode;
    }

    public String getPositionCode() {
        return positionCode;
    }

    public void setPositionCode(String positionCode) {
        this.positionCode = positionCode;
    }

    public List<String> getComponents() {
        return components;
    }

    public void setComponents(List<String> components) {
        this.components = components;
    }
}
