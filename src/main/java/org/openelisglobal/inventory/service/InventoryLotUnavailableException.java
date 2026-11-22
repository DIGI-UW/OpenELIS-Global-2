package org.openelisglobal.inventory.service;

public class InventoryLotUnavailableException extends IllegalStateException {

    private final String code;
    private final String lotNumber;

    public InventoryLotUnavailableException(String code, String lotNumber) {
        super(code + ": " + lotNumber);
        this.code = code;
        this.lotNumber = lotNumber;
    }

    public String getCode() {
        return code;
    }

    public String getLotNumber() {
        return lotNumber;
    }
}
