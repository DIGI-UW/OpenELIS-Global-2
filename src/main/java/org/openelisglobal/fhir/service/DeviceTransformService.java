package org.openelisglobal.fhir.service;

import org.hl7.fhir.r4.model.Device;
import org.openelisglobal.analyzer.valueholder.Analyzer;

/**
 * OpenELIS Analyzer to and from FHIR Device.
 */
public interface DeviceTransformService {

    Analyzer transformDeviceToAnalyzer(Device device);

    Device transformAnalyzerToDevice(Analyzer analyzer);
}
