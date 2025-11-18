package org.openelisglobal.locationreporting.service;

public interface LocationReportingService {

    void sendLocationReport();

    boolean isOptedIn();

    /**
     * Returns true if the opt-in setting has been explicitly persisted in site
     * information.
     */
    boolean isOptInConfigured();

    void setOptIn(boolean optIn);

    void sendInitialLocationReport();
}
