package org.openelisglobal.fhir.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The orchestrating {@link FhirTransformService} must produce exactly the
 * resource its per-resource transform service produces, for every resource the
 * facade exposes. Guards the split against a delegation drifting from the
 * service it fronts.
 */
public class TransformServiceDelegationParityTest extends BaseWebContextSensitiveTest {

    private static final String PATIENT_ID = "1";
    private static final String ANALYSIS_ID = "1";
    private static final String RESULT_ID = "3";
    private static final String SAMPLE_ITEM_ID = "601";
    private static final String PROVIDER_ID = "1";
    private static final String ORGANIZATION_ID = "3";
    private static final String ANALYZER_ID = "1";

    @Autowired
    private FhirTransformService fhirTransformService;
    @Autowired
    private PatientTransformService patientTransformService;
    @Autowired
    private PractitionerTransformService practitionerTransformService;
    @Autowired
    private OrganizationTransformService organizationTransformService;
    @Autowired
    private ServiceRequestTransformService serviceRequestTransformService;
    @Autowired
    private SpecimenTransformService specimenTransformService;
    @Autowired
    private ObservationTransformService observationTransformService;
    @Autowired
    private DiagnosticReportTransformService diagnosticReportTransformService;
    @Autowired
    private DeviceTransformService deviceTransformService;

    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private ResultService resultService;
    @Autowired
    private SampleItemService sampleItemService;
    @Autowired
    private ProviderService providerService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private AnalyzerService analyzerService;

    private final IParser parser = FhirContext.forR4().newJsonParser();

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/facade-device.xml");
        executeDataSetWithStateManagement("testdata/facade-organization.xml");
        executeDataSetWithStateManagement("testdata/result-facade.xml");
    }

    @Test
    public void patient_orchestratorMatchesPatientService() throws Exception {
        assertSameResource(fhirTransformService.transformToFhirPatient(PATIENT_ID),
                patientTransformService.transformToFhirPatient(PATIENT_ID));
    }

    @Test
    public void practitioner_orchestratorMatchesPractitionerService() throws Exception {
        Provider provider = providerService.get(PROVIDER_ID);
        assertNotNull(provider);
        assertSameResource(fhirTransformService.transformProviderToPractitioner(provider),
                practitionerTransformService.transformProviderToPractitioner(provider));
    }

    @Test
    public void organization_orchestratorMatchesOrganizationService() throws Exception {
        Organization organization = organizationService.get(ORGANIZATION_ID);
        assertNotNull(organization);
        if (organization.getIsActive() == null) {
            organization.setIsActive(IActionConstants.YES);
        }
        assertSameResource(fhirTransformService.transformToFhirOrganization(organization),
                organizationTransformService.transformToFhirOrganization(organization));
    }

    @Test
    public void serviceRequest_orchestratorMatchesServiceRequestService() throws Exception {
        assertSameResource(fhirTransformService.transformToServiceRequest(ANALYSIS_ID),
                serviceRequestTransformService.transformToServiceRequest(ANALYSIS_ID));
    }

    @Test
    public void specimen_orchestratorMatchesSpecimenService() throws Exception {
        SampleItem sampleItem = sampleItemService.get(SAMPLE_ITEM_ID);
        assertNotNull(sampleItem);
        assertSameResource(fhirTransformService.transformToSpecimen(sampleItem),
                specimenTransformService.transformToSpecimen(sampleItem));
    }

    @Test
    public void observation_orchestratorMatchesObservationService() throws Exception {
        Result result = resultService.get(RESULT_ID);
        assertNotNull(result);
        assertSameResource(fhirTransformService.transformResultToObservation(result),
                observationTransformService.transformResultToObservation(result));
    }

    @Test
    public void diagnosticReport_orchestratorMatchesDiagnosticReportService() throws Exception {
        Analysis analysis = analysisService.get(ANALYSIS_ID);
        assertNotNull(analysis);
        assertSameResource(fhirTransformService.transformResultToDiagnosticReport(analysis),
                diagnosticReportTransformService.transformResultToDiagnosticReport(analysis));
    }

    @Test
    public void device_orchestratorMatchesDeviceService() throws Exception {
        Analyzer analyzer = analyzerService.get(ANALYZER_ID);
        assertNotNull(analyzer);
        assertSameResource(fhirTransformService.transformAnalyzerToDevice(analyzer),
                deviceTransformService.transformAnalyzerToDevice(analyzer));
    }

    private void assertSameResource(IBaseResource viaOrchestrator, IBaseResource viaService) {
        assertNotNull(viaOrchestrator);
        assertNotNull(viaService);
        assertNotSame(viaOrchestrator, viaService);
        assertEquals(parser.encodeResourceToString(viaService), parser.encodeResourceToString(viaOrchestrator));
    }
}
