package org.openelisglobal.dashboard.management.surveillance.service;

import java.io.Reader;
import java.util.List;

public interface SurveillanceMappingService {

    List<SurveillanceMappingView> getAllViews();

    // A LOINC row maps every active test carrying it; an unreadable file throws
    // IllegalArgumentException.
    SurveillanceImportResult importCsv(Reader csv, String sourceFile, String sysUserId);
}
