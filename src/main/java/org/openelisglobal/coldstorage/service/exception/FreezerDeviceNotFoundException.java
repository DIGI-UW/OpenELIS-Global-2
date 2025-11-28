package org.openelisglobal.coldstorage.service.exception;

public class FreezerDeviceNotFoundException extends RuntimeException {

    public FreezerDeviceNotFoundException(String name) {
        super("Freezer device not configured: " + name);
    }
}
