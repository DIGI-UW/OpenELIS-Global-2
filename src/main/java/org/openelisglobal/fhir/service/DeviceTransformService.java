package org.openelisglobal.fhir.service;

import org.hl7.fhir.r4.model.Device;
import org.openelisglobal.analyzer.valueholder.Analyzer;

public interface DeviceTransformService {

    Device transformAnalyzerToDevice(Analyzer analyzer);

}
