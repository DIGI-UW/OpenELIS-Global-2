package org.openelisglobal.fhir.service;

import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.sample.bean.SampleEditItem;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.valueholder.Sample;

public interface ServiceRequestTransformService {

    ServiceRequest transformToServiceRequest(Analysis analysis);

    List<SampleEditItem> buildSampleEditItemsListFromServiceRequest(ServiceRequest serviceRequest, String sysUserId)
            throws Exception;

    SampleOrderItem buildSampleOrderItemFromServiceRequest(ServiceRequest serviceRequest, String sysUserId)
            throws Exception;

    Optional<ServiceRequest> getReferringServiceRequestForSample(Sample sample);

}
