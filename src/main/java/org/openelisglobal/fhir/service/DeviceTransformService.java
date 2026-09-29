package org.openelisglobal.fhir.service;

import org.hl7.fhir.r4.model.Device;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.common.service.CrossDomainService;

/**
 * OpenELIS Analyzer to and from FHIR Device.
 */
@CrossDomainService(callers = "FHIR transform pipeline — one of the per-resource transformers"
        + " FhirTransformService (itself @CrossDomainService) was decomposed into. Pure resource"
        + " mapping invoked by the import/export pipeline and by sibling transformers; no controller"
        + " references it. The caller's own endpoint carries the privilege gate.")
public interface DeviceTransformService {

    Analyzer transformDeviceToAnalyzer(Device device);

    Device transformAnalyzerToDevice(Analyzer analyzer);
}
