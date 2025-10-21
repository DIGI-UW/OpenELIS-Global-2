package org.openelisglobal.locationreporting.service;

public interface LocationReportingService {

    void sendLocationReport();

    boolean isOptedIn();

    void setOptIn(boolean optIn);

    void sendInitialLocationReport();
}
