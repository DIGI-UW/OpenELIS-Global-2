package org.openelisglobal.biorepository.service;

import java.sql.Timestamp;
import java.util.Map;
import org.openelisglobal.biorepository.controller.rest.dto.SampleLifecycleResponseDTO;
import org.openelisglobal.biorepository.valueholder.ChainOfCustodyLog.CustodyAction;

public interface SampleLifecycleService {

    SampleLifecycleResponseDTO getBySampleItemId(Integer sampleItemId);

    SampleLifecycleResponseDTO getByBioSampleId(Integer bioSampleId);

    Map<String, Object> search(String sampleExternalId, CustodyAction action, Timestamp startDate, Timestamp endDate,
            int page, int pageSize);
}
