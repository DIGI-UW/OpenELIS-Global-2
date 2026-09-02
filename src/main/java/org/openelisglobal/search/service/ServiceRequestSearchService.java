package org.openelisglobal.search.service;

import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.search.dao.ServiceRequestSearchDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ServiceRequestSearchService {

    @Autowired
    private FhirTransformService fhirTransformService;
    @Autowired
    private ServiceRequestSearchDao serviceRequestSearchDao;

}
