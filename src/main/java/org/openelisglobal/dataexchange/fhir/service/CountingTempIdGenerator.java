package org.openelisglobal.dataexchange.fhir.service;

public class CountingTempIdGenerator implements TempIdGenerator {

    private int lastTempId = 0;

    @Override
    public String getNextId() {
        return Integer.toString(++lastTempId);
    }
}
