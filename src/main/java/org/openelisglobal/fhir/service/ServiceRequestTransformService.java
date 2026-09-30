package org.openelisglobal.fhir.service;

import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.SampleAddService.SampleTestCollection;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.bean.SampleEditItem;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.test.valueholder.Test;

/**
 * OpenELIS Analysis to and from FHIR ServiceRequest, including order intake
 * from an external ServiceRequest.
 */
public interface ServiceRequestTransformService {

    void updateReferringServiceRequestWithSampleInfo(Sample sample, ServiceRequest serviceRequest);

    Optional<ServiceRequest> getReferringServiceRequestForSample(Sample sample);

    List<ServiceRequest> transformToServiceRequests(SamplePatientUpdateData updateData,
            SampleTestCollection sampleTestCollection);

    ServiceRequest transformToServiceRequest(String anlaysisId);

    ServiceRequest transformToServiceRequest(Analysis analysis);

    void preserveTerminalServiceRequestStatus(Analysis analysis, ServiceRequest serviceRequest);

    List<SampleEditItem> buildSampleEditItemsListFromServiceRequest(ServiceRequest serviceRequest, String sysUserId)
            throws Exception;

    SampleOrderItem buildSampleOrderItemFromServiceRequest(ServiceRequest serviceRequest, String sysUserId)
            throws Exception;

    List<Test> resolveTestsFromCodeableConcept(CodeableConcept codeableConcept);
}
