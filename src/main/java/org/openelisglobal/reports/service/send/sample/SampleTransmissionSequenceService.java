package org.openelisglobal.reports.service.send.sample;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.reports.send.sample.valueholder.SampleTransmissionSequence;

@CrossDomainService(callers = "sample transmission sequencing — HL7 outbound pipeline, cross-domain")
public interface SampleTransmissionSequenceService extends BaseObjectService<SampleTransmissionSequence, String> {
}
