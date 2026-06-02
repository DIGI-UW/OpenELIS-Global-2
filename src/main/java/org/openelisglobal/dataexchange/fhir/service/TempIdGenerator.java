package org.openelisglobal.dataexchange.fhir.service;

import org.openelisglobal.common.service.CrossDomainService;

@CrossDomainService(callers = "FHIR pipeline — internal infrastructure")
public interface TempIdGenerator {

    String getNextId();
}
