package org.openelisglobal.dashboard.management.surveillance.service;

import java.io.Reader;

public interface SurveillanceMappingService {

    // A row maps every active test it matches; an unreadable file throws
    // IllegalArgumentException.
    SurveillanceImportResult importCsv(Reader csv, String sourceFile, String sysUserId);
}
