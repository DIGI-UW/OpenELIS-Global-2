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

    /**
     * The order details an update of {@code sample} saves: everything stored on the
     * order, with priority, requester, referring site and request date taken from
     * the ServiceRequest only where it differs from what a read publishes. Saving
     * the order writes every order field, so building it from the ServiceRequest
     * alone erased the program, payment status and next visit date the resource has
     * no element for. The item is marked modified only when one of those four
     * changed, because saving it also rewrites the received time to the minute.
     */
    SampleOrderItem buildSampleOrderItemForUpdate(ServiceRequest serviceRequest,
            org.openelisglobal.sample.valueholder.Sample sample, String sysUserId) throws Exception;

    List<Test> resolveTestsFromCodeableConcept(CodeableConcept codeableConcept);
}
