package org.openelisglobal.fhir.service;

import java.util.List;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.sample.bean.SampleEditItem;
import org.openelisglobal.sample.bean.SampleOrderItem;

public interface ServiceRequestTransformService {

    ServiceRequest transformToServiceRequest(Analysis analysis);

    List<SampleEditItem> buildSampleEditItemsListFromServiceRequest(ServiceRequest serviceRequest, String sysUserId)
            throws Exception;

    SampleOrderItem buildSampleOrderItemFromServiceRequest(ServiceRequest serviceRequest, String sysUserId)
            throws Exception;

}
