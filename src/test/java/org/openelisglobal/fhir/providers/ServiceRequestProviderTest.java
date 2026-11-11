package org.openelisglobal.fhir.providers;

import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;

@RunWith(MockitoJUnitRunner.class)
public class ServiceRequestProviderTest {

    @Mock
    private AnalysisService analysisService;

    @Mock
    private FhirTransformService fhirTransformService;

    @InjectMocks
    private ServiceRequestProvider serviceRequestProvider;

    @Test(expected = InternalErrorException.class)
    public void readServiceRequest_whenDuplicateUuidMatches_shouldReturn500() {
        String uuid = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b01";
        IdType id = new IdType("ServiceRequest", uuid);

        Analysis first = new Analysis();
        Analysis second = new Analysis();

        when(analysisService.getAllMatching("fhirUuid", UUID.fromString(uuid)))
                .thenReturn(Arrays.asList(first, second));

        serviceRequestProvider.readServiceRequest(id);
    }

    @Test(expected = InternalErrorException.class)
    public void readServiceRequest_whenTransformReturnsNull_shouldReturn500() {
        String uuid = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b01";
        IdType id = new IdType("ServiceRequest", uuid);

        Analysis analysis = new Analysis();
        analysis.setId("1");

        when(analysisService.getAllMatching("fhirUuid", UUID.fromString(uuid)))
                .thenReturn(Collections.singletonList(analysis));
        when(fhirTransformService.transformToServiceRequest("1")).thenReturn(null);

        serviceRequestProvider.readServiceRequest(id);
    }

    @Test
    public void readServiceRequest_whenSingleMatchAndTransformSuccess_shouldReturnServiceRequest() {
        String uuid = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b01";
        IdType id = new IdType("ServiceRequest", uuid);

        Analysis analysis = new Analysis();
        analysis.setId("1");

        ServiceRequest expected = new ServiceRequest();
        expected.setId(uuid);

        when(analysisService.getAllMatching("fhirUuid", UUID.fromString(uuid)))
                .thenReturn(Collections.singletonList(analysis));
        when(fhirTransformService.transformToServiceRequest("1")).thenReturn(expected);

        ServiceRequest actual = serviceRequestProvider.readServiceRequest(id);

        org.junit.Assert.assertEquals(uuid, actual.getIdElement().getIdPart());
    }
}
