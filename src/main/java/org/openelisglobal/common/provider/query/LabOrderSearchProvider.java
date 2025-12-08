/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.common.provider.query;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Specimen;
import org.hl7.fhir.r4.model.Task;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.ExternalOrderStatus;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.XMLUtil;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;

public class LabOrderSearchProvider extends BaseQueryProvider {
    // private TestDAO testDAO = new TestDAOImpl();
    // private PanelDAO panelDAO = new PanelDAOImpl();
    // private PanelItemDAO panelItemDAO = new PanelItemDAOImpl();
    // private TypeOfSampleTestDAO typeOfSampleTest = new TypeOfSampleTestDAOImpl();

    // @Value("${org.openelisglobal.fhirstore.uri.from.eclipse_common.properties}")
    // private String localFhirStorePath;
    // @Value("${org.openelisglobal.task.useBasedOn}")
    // private Boolean useBasedOn;

    private FhirConfig fhirConfig = SpringContext.getBean(FhirConfig.class);
    private FhirUtil fhirUtil = SpringContext.getBean(FhirUtil.class);

    private TestService testService = SpringContext.getBean(TestService.class);
    private PanelService panelService = SpringContext.getBean(PanelService.class);
    private PanelItemService panelItemService = SpringContext.getBean(PanelItemService.class);
    private ProviderService providerService = SpringContext.getBean(ProviderService.class);
    private TypeOfSampleTestService typeOfSampleTestService = SpringContext.getBean(TypeOfSampleTestService.class);
    private ElectronicOrderService electronicOrderService = SpringContext.getBean(ElectronicOrderService.class);
    private TypeOfSampleService typeOfSampleService = SpringContext.getBean(TypeOfSampleService.class);
    private FhirPersistanceService fhirPersistanceService = SpringContext.getBean(FhirPersistanceService.class);
    private OrganizationService organizationService = SpringContext.getBean(OrganizationService.class);

    private Map<TypeOfSample, PanelTestLists> typeOfSampleMap;
    private Map<Panel, List<TypeOfSample>> panelSampleTypesMap;
    private Map<String, List<TestSampleType>> testNameTestSampleTypeMap;

    private Task task = null;
    private Practitioner requesterPerson = null;
    private Practitioner collector = null;
    private Organization referringOrganization = null;
    private Location location = null;
    private ServiceRequest serviceRequest = null;
    private Specimen specimen = null;
    private Patient patient = null;
    private String patientGuid;

    private List<ElectronicOrder> eOrders = null;
    private ElectronicOrder eOrder = null;
    private ExternalOrderStatus eOrderStatus = null;

    private static final String NOT_FOUND = "Not Found";
    private static final String CANCELED = "Canceled";
    private static final String REALIZED = "Realized";
    private static final String PROVIDER_ID = "id";
    private static final String PROVIDER_PERSON_ID = "personId";
    private static final String PROVIDER_FIRST_NAME = "firstName";
    private static final String PROVIDER_LAST_NAME = "lastName";
    private static final String PROVIDER_PHONE = "phone";
    private static final String PROVIDER_EMAIL = "email";
    private static final String PROVIDER_FAX = "fax";

    @Override
    public void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String orderNumber = request.getParameter("orderNumber");
        eOrders = electronicOrderService.getElectronicOrdersByExternalId(orderNumber);

        // if (eOrders.isEmpty()) {
        // eOrders =
        // fhirTransformService.getElectronicOrdersOrdersByServiceRequestId(orderNumber);
        // }
        if (!eOrders.isEmpty()) {
            eOrder = eOrders.get(eOrders.size() - 1);
            eOrderStatus = SpringContext.getBean(IStatusService.class)
                    .getExternalOrderStatusForID(eOrder.getStatusId());

            IGenericClient localFhirClient = fhirUtil.getFhirClient(fhirConfig.getLocalFhirStorePath());

            for (String remotePath : fhirConfig.getRemoteStorePaths()) {
                Bundle srBundle = (Bundle) localFhirClient.search().forResource(ServiceRequest.class)
                        .where(ServiceRequest.RES_ID.exactly().code(orderNumber))
                        .include(ServiceRequest.INCLUDE_SPECIMEN).execute();
                for (BundleEntryComponent bundleComponent : srBundle.getEntry()) {
                    if (bundleComponent.hasResource()
                            && ResourceType.ServiceRequest.equals(bundleComponent.getResource().getResourceType())) {
                        serviceRequest = (ServiceRequest) bundleComponent.getResource();
                    }
                    if (bundleComponent.hasResource()
                            && ResourceType.Specimen.equals(bundleComponent.getResource().getResourceType())) {
                        specimen = (Specimen) bundleComponent.getResource();
                    }
                }
                srBundle = (Bundle) localFhirClient.search().forResource(ServiceRequest.class)
                        .where(ServiceRequest.IDENTIFIER.exactly().systemAndIdentifier(remotePath, orderNumber))
                        .include(ServiceRequest.INCLUDE_SPECIMEN).execute();
                for (BundleEntryComponent bundleComponent : srBundle.getEntry()) {
                    if (bundleComponent.hasResource()
                            && ResourceType.ServiceRequest.equals(bundleComponent.getResource().getResourceType())) {
                        serviceRequest = (ServiceRequest) bundleComponent.getResource();
                    }
                    if (bundleComponent.hasResource()
                            && ResourceType.Specimen.equals(bundleComponent.getResource().getResourceType())) {
                        specimen = (Specimen) bundleComponent.getResource();
                    }
                }
            }
            if (serviceRequest != null) {
                LogEvent.logDebug(this.getClass().getSimpleName(), "processRequest",
                        "found matching serviceRequest " + serviceRequest.getIdElement().getIdPart());
            } else {
                LogEvent.logDebug(this.getClass().getSimpleName(), "processRequest", "no matching serviceRequest");
            }

            patient = localFhirClient.read() //
                    .resource(Patient.class) //
                    .withId(serviceRequest.getSubject().getReferenceElement().getIdPart()) //
                    .execute();

            if (patient != null) {
                LogEvent.logDebug(this.getClass().getSimpleName(), "processRequest",
                        "found matching patient " + patient.getIdElement().getIdPart());
            } else {
                LogEvent.logDebug(this.getClass().getSimpleName(), "processRequest", "no matching patient");
            }

            // task = fhirUtil.getFhirParser().parseResource(Task.class, eOrder.getData());

            // task = (Task) localFhirClient.search()//
            // .forResource(Task.class)//
            // .where(Task.BASED_ON.hasAnyOfIds(serviceRequest.getId()))//
            // .returnBundle(Bundle.class)//
            // .execute().getEntryFirstRep().getResource();
            task = fhirPersistanceService.getTaskBasedOnServiceRequest(orderNumber).orElseThrow();

            if (task != null) {
                LogEvent.logDebug(this.getClass().getSimpleName(), "processRequest",
                        "found matching task " + task.getIdElement().getIdPart());
            } else {
                LogEvent.logDebug(this.getClass().getSimpleName(), "processRequest", "no matching task");
            }

            if (!GenericValidator
                    .isBlankOrNull(task.getRestriction().getRecipientFirstRep().getReferenceElement().getIdPart())) {
                try {
                    referringOrganization = localFhirClient.read() //
                            .resource(Organization.class) //
                            .withId(task.getRestriction().getRecipientFirstRep().getReferenceElement().getIdPart()) //
                            .execute();
                    LogEvent.logDebug(this.getClass().getSimpleName(), "processRequest",
                            "found matching organization " + referringOrganization.getIdElement().getIdPart());
                } catch (ResourceNotFoundException e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "processRequest", "no matching organization");
                }
            }

            if (!GenericValidator
                    .isBlankOrNull(serviceRequest.getLocationReferenceFirstRep().getReferenceElement().getIdPart())) {
                try {
                    location = localFhirClient.read() //
                            .resource(Location.class) //
                            .withId(serviceRequest.getLocationReferenceFirstRep().getReferenceElement().getIdPart()) //
                            .execute();
                    LogEvent.logDebug(this.getClass().getSimpleName(), "processRequest",
                            "found matching location " + referringOrganization.getIdElement().getIdPart());
                } catch (ResourceNotFoundException e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "processRequest", "no matching location");
                }
            }

            if (!GenericValidator.isBlankOrNull(task.getOwner().getReferenceElement().getIdPart())
                    && task.getOwner().getReference().contains(ResourceType.Practitioner.toString())) {
                try {
                    requesterPerson = localFhirClient.read() //
                            .resource(Practitioner.class) //
                            .withId(task.getOwner().getReferenceElement().getIdPart()) //
                            .execute();
                    LogEvent.logDebug(this.getClass().getSimpleName(), "processRequest",
                            "found matching requester " + requesterPerson.getIdElement().getIdPart());
                } catch (ResourceNotFoundException e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "processRequest", "no matching requester");
                }
            }

            if (requesterPerson == null) {
                if (!GenericValidator.isBlankOrNull(serviceRequest.getRequester().getReferenceElement().getIdPart())
                        && serviceRequest.getRequester().getReference()
                                .contains(ResourceType.Practitioner.toString())) {
                    try {
                        requesterPerson = localFhirClient.read() //
                                .resource(Practitioner.class) //
                                .withId(serviceRequest.getRequester().getReferenceElement().getIdPart()) //
                                .execute();
                        LogEvent.logDebug(this.getClass().getSimpleName(), "processRequest",
                                "found matching requester " + requesterPerson.getIdElement().getIdPart());
                    } catch (ResourceNotFoundException e) {
                        LogEvent.logWarn(this.getClass().getSimpleName(), "processRequest", "no matching requester");
                    }
                }

            }

            if (specimen != null && !GenericValidator
                    .isBlankOrNull(specimen.getCollection().getCollector().getReferenceElement().getIdPart())) {
                try {
                    collector = localFhirClient.read() //
                            .resource(Practitioner.class) //
                            .withId(specimen.getCollection().getCollector().getReferenceElement().getIdPart()) //
                            .execute();
                    LogEvent.logDebug(this.getClass().getSimpleName(), "processRequest",
                            "found matching collector " + requesterPerson.getIdElement().getIdPart());
                } catch (ResourceNotFoundException e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "processRequest", "no matching collector");
                }
            }
        }

        StringBuilder xml = new StringBuilder();

        String result = createSearchResultXML(orderNumber, xml);

        if (!result.equals(VALID)) {
            if (result.equals(NOT_FOUND)) {
                result = MessageUtil.getMessage("electronic.order.message.orderNotFound");
            } else if (result.equals(CANCELED)) {
                result = MessageUtil.getMessage("electronic.order.message.canceled");
            } else if (result.equals(REALIZED)) {
                result = MessageUtil.getMessage("electronic.order.message.realized");
            }
            result += "\n\n" + MessageUtil.getMessage("electronic.order.message.suggestion");
            xml.append("empty");
        }
        ajaxServlet.sendData(xml.toString(), result, request, response);
    }

    private String createSearchResultXML(String orderNumber, StringBuilder xml) {

        String success = VALID;

        if (eOrderStatus == ExternalOrderStatus.Cancelled) {
            return CANCELED;
        } else if (eOrderStatus == ExternalOrderStatus.Realized) {
            return REALIZED;
        } else if (eOrders.isEmpty()) {
            return NOT_FOUND;
        }

        patientGuid = getPatientGuid(eOrder);
        for (Identifier identifier : patient.getIdentifier()) {
            if (identifier.hasSystem()) {
                // if
                // (identifier.getSystem().equalsIgnoreCase("https://isanteplusdemo.com/openmrs/ws/fhir2/"))
                // {
                if (identifier.getSystem().equalsIgnoreCase("iSantePlus ID")
                        || identifier.getSystem().equalsIgnoreCase("https://host.openelis.org/locator-form")) {
                    patientGuid = identifier.getId();
                } else if (identifier.getSystem().equalsIgnoreCase(fhirConfig.getOeFhirSystem() + "/pat_guid")) {
                    patientGuid = identifier.getValue();
                }
            }
        }
        LogEvent.logDebug(this.getClass().getSimpleName(), "createSearchResultXML",
                "using patient guid " + patientGuid);

        createOrderXML(eOrder.getData(), patientGuid, xml);

        return success;
    }

    private String getPatientGuid(ElectronicOrder eOrder) {
        PatientService patientPatientService = SpringContext.getBean(PatientService.class);
        PersonService personService = SpringContext.getBean(PersonService.class);
        personService.getData(eOrder.getPatient().getPerson());
        return patientPatientService.getGUID(eOrder.getPatient());
    }

    private void createOrderXML(String orderMessage, String patientGuid, StringBuilder xml) {
        List<Request> tests = new ArrayList<>();
        List<Request> panels = new ArrayList<>();
        getTestsAndPanels(tests, panels, orderMessage);
        createMaps(tests, panels);
        xml.append("<order>");
        addRequester(xml);
        addRequestingOrg(xml);
        addLocation(xml);
        addPatientGuid(xml, patientGuid);
        addSampleTypes(xml);
        addCrossPanels(xml);
        addCrosstests(xml);
        addAlerts(xml, patientGuid);
        xml.append("</order>");
    }

    private void addRequestingOrg(StringBuilder xml) {
        xml.append("<requestingOrg>");
        org.openelisglobal.organization.valueholder.Organization organization = null;
        if (referringOrganization != null) {
            organization = organizationService
                    .getOrganizationByFhirId(referringOrganization.getIdElement().getIdPart());
        }
        if (organization == null && task.getLocation() != null) {
            organization = organizationService
                    .getOrganizationByFhirId(task.getLocation().getReferenceElement().getIdPart());
        }

        if (organization != null) {
            XMLUtil.appendKeyValue("fhir-id", organization.getFhirUuidAsString(), xml);
            XMLUtil.appendKeyValue("id", organization.getId(), xml);
            XMLUtil.appendKeyValue("name", organization.getOrganizationName(), xml);
        }
        xml.append("</requestingOrg>");
    }

    private void addLocation(StringBuilder xml) {
        xml.append("<location>");
        if (location != null) {
            org.openelisglobal.organization.valueholder.Organization organization = organizationService
                    .getOrganizationByFhirId(location.getIdElement().getIdPart());
            XMLUtil.appendKeyValue("fhir-id", location.getIdElement().getIdPart(), xml);
            XMLUtil.appendKeyValue("id", organization.getId(), xml);
        }
        xml.append("</location>");
    }

    private void addRequester(StringBuilder xml) {

        Map<String, String> requesterValuesMap = new HashMap<>();

        if (requesterPerson != null) {
            for (ContactPoint contact : requesterPerson.getTelecom()) {
                if (ContactPointSystem.PHONE.equals(contact.getSystem())) {
                    requesterValuesMap.put(PROVIDER_PHONE, contact.getValue());
                } else if (ContactPointSystem.EMAIL.equals(contact.getSystem())) {
                    requesterValuesMap.put(PROVIDER_EMAIL, contact.getValue());
                } else if (ContactPointSystem.FAX.equals(contact.getSystem())) {
                    requesterValuesMap.put(PROVIDER_FAX, contact.getValue());
                }
            }
            Provider provider = providerService
                    .getProviderByFhirId(UUID.fromString(requesterPerson.getIdElement().getIdPart()));
            if (provider != null) {
                requesterValuesMap.put(PROVIDER_ID, provider.getId());
                requesterValuesMap.put(PROVIDER_PERSON_ID, provider.getPerson().getId());
            }
            requesterValuesMap.put(PROVIDER_LAST_NAME, requesterPerson.getNameFirstRep().getFamily());
            requesterValuesMap.put(PROVIDER_FIRST_NAME, requesterPerson.getNameFirstRep().getGivenAsSingleString());
        } else {
            Provider provider = providerService
                    .getProviderByFhirId(UUID.fromString(task.getOwner().getReferenceElement().getIdPart()));
            if (provider != null) {
                requesterValuesMap.put(PROVIDER_ID, provider.getId());
                requesterValuesMap.put(PROVIDER_PERSON_ID, provider.getPerson().getId());
            }

        }
        xml.append("<requester>");
        XMLUtil.appendKeyValue(PROVIDER_ID, requesterValuesMap.get(PROVIDER_ID), xml);
        XMLUtil.appendKeyValue(PROVIDER_PERSON_ID, requesterValuesMap.get(PROVIDER_PERSON_ID), xml);
        XMLUtil.appendKeyValue(PROVIDER_FIRST_NAME, requesterValuesMap.get(PROVIDER_FIRST_NAME), xml);
        XMLUtil.appendKeyValue(PROVIDER_LAST_NAME, requesterValuesMap.get(PROVIDER_LAST_NAME), xml);
        XMLUtil.appendKeyValue(PROVIDER_PHONE, requesterValuesMap.get(PROVIDER_PHONE), xml);
        XMLUtil.appendKeyValue(PROVIDER_EMAIL, requesterValuesMap.get(PROVIDER_EMAIL), xml);
        XMLUtil.appendKeyValue(PROVIDER_FAX, requesterValuesMap.get(PROVIDER_FAX), xml);
        xml.append("</requester>");
    }

    private void getTestsAndPanels(List<Request> tests, List<Request> panels, String orderMessage) {
        // OML_O21 hapiMsg = (OML_O21) p.parse(orderMessage);
        // ORC commonOrderSegment = hapiMsg.getORDER().getORC();
        // pass loinc
        String loinc = "";
        String sampleTypeAbbreviation = "";
        for (Coding code : serviceRequest.getCode().getCoding()) {
            if (code.hasSystem()) {
                if (code.getSystem().equalsIgnoreCase("http://loinc.org")) {
                    loinc = code.getCode();
                    break;
                }
            }
        }
        if (specimen != null) {
            for (Coding type : specimen.getType().getCoding()) {
                if (type.hasSystem()) {
                    if (type.getSystem().equals(fhirConfig.getOeFhirSystem() + "/sampleType")) {
                        sampleTypeAbbreviation = type.getCode();
                        break;
                    }
                }
            }
        }

        addToTestOrPanel(tests, panels, loinc, sampleTypeAbbreviation);
    }

    // private List<ServiceRequest> getBasedOnServiceRequestFromBundle(Bundle
    // bundle, Task task) {
    // List<ServiceRequest> basedOn = new ArrayList<>();
    // for (Reference reference : task.getBasedOn()) {
    // basedOn.add((ServiceRequest) findResourceInBundle(bundle,
    // reference.getReference()));
    // }
    // return basedOn;
    // }
    //
    // private Patient getForPatientFromBundle(Bundle bundle, Task task) {
    // return (Patient) findResourceInBundle(bundle, task.getFor().getReference());
    // }
    //
    // private IBaseResource findResourceInBundle(Bundle bundle, String reference) {
    // for (BundleEntryComponent bundleComponent : bundle.getEntry()) {
    // if (bundleComponent.hasResource() &&
    // bundleComponent.getFullUrl().endsWith(reference)) {
    // return bundleComponent.getResource();
    // }
    // }
    // return null;
    //
    // }

    private void addToTestOrPanel(List<Request> tests, List<Request> panels, String loinc,
            String sampleTypeAbbreviation) {
        Test test = null;
        Panel panel = null;
        TypeOfSample typeOfSample = null;
        if (!GenericValidator.isBlankOrNull(sampleTypeAbbreviation)) {
            String typeOfSampleId = typeOfSampleService.getTypeOfSampleIdForLocalAbbreviation(sampleTypeAbbreviation);
            if (!GenericValidator.isBlankOrNull(typeOfSampleId)) {
                typeOfSample = typeOfSampleService.get(typeOfSampleId);
                if (typeOfSample != null) {
                    test = testService.getActiveTestByLoincCodeAndSampleType(loinc, typeOfSample.getId()).orElse(null);
                }
            }
        }
        if (test == null) {
            List<Test> alltests = testService.getActiveTestsByLoinc(loinc);
            if (alltests != null && alltests.size() > 0) {
                test = alltests.get(0);
            }
        }
        if (test != null) {
            if (typeOfSample == null) {
                typeOfSample = typeOfSampleService.getTypeOfSampleForTest(test.getId()).get(0);
            }
            tests.add(new Request(test.getName(), loinc, typeOfSample.getLocalizedName()));
            return;
        }
        panel = panelService.getPanelByLoincCode(loinc);
        if (panel != null) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "addToTestOrPanel",
                    "panel matching loinc is: " + panel.getDescription());

            if (typeOfSample == null) {
                typeOfSample = typeOfSampleService.getTypeOfSampleForPanelId(panel.getId()).get(0);
                LogEvent.logDebug(this.getClass().getSimpleName(), "addToTestOrPanel",
                        "typeOfSample matching for panel is: " + typeOfSample.getDescription());

            }
            panels.add(new Request(panel.getPanelName(), loinc, typeOfSample.getLocalizedName()));
        }

    }

    private void createMaps(List<Request> testRequests, List<Request> panelNames) {
        typeOfSampleMap = new HashMap<>();
        panelSampleTypesMap = new HashMap<>();
        testNameTestSampleTypeMap = new HashMap<>();

        createMapsForTests(testRequests);

        createMapsForPanels(panelNames);
    }

    private void addPatientGuid(StringBuilder xml, String patientGuid) {
        xml.append("<patient>");
        XMLUtil.appendKeyValue("guid", patientGuid, xml);
        xml.append("</patient>");
    }

    private void createMapsForTests(List<Request> testRequests) {
        for (Request testRequest : testRequests) {
            List<Test> tests = testService.getActiveTestsByLoinc(testRequest.getLoinc());

            Test singleTest = tests.get(0);
            List<TypeOfSample> sampleTypes = typeOfSampleService.getTypeOfSampleForTest(singleTest.getId());
            TypeOfSample singleSampleType = sampleTypes.get(0);
            boolean hasSingleSampleType = sampleTypes.size() == 1 && tests.size() == 1;

            if (tests.size() > 1) {
                if (!GenericValidator.isBlankOrNull(testRequest.getSampleType())) {
                    for (Test test : tests) {
                        List<TypeOfSample> typeOfSamples = typeOfSampleService.getTypeOfSampleForTest(test.getId());
                        Optional<TypeOfSample> matchingSampleType = typeOfSamples.stream()
                                .filter(e -> e.getDescription().equals(testRequest.getSampleType())).findFirst();
                        if (matchingSampleType.isPresent()) {
                            hasSingleSampleType = true;
                            singleSampleType = matchingSampleType.get();
                            singleTest = test;
                            break;
                        }
                    }
                }

                if (!hasSingleSampleType) {
                    List<TestSampleType> testSampleTypeList = testNameTestSampleTypeMap.get(testRequest.getName());

                    if (testSampleTypeList == null) {
                        testSampleTypeList = new ArrayList<>();
                        testNameTestSampleTypeMap.put(testRequest.getName(), testSampleTypeList);
                    }

                    for (Test test : tests) {
                        sampleTypes = typeOfSampleService.getTypeOfSampleForTest(test.getId());
                        for (TypeOfSample sampleType : sampleTypes) {
                            testSampleTypeList.add(new TestSampleType(test, sampleType));
                        }
                    }
                }
            }

            if (hasSingleSampleType) {
                PanelTestLists panelTestLists = typeOfSampleMap.get(singleSampleType);
                if (panelTestLists == null) {
                    panelTestLists = new PanelTestLists();
                    typeOfSampleMap.put(singleSampleType, panelTestLists);
                }

                panelTestLists.addTest(singleTest);
            }
        }
    }

    private void createMapsForPanels(List<Request> panelRequests) {
        for (Request panelRequest : panelRequests) {
            Panel panel = null;
            if (!GenericValidator.isBlankOrNull(panelRequest.getLoinc())) {
                panel = panelService.getPanelByLoincCode(panelRequest.getLoinc());
            }
            if (panel == null && !GenericValidator.isBlankOrNull(panelRequest.getName())) {
                panel = panelService.getPanelByName(panelRequest.getName());
            }

            if (panel != null) {
                List<TypeOfSample> typeOfSamples = typeOfSampleService.getTypeOfSampleForPanelId(panel.getId());
                boolean hasSingleSampleType = typeOfSamples.size() == 1;
                TypeOfSample singleTypeOfSample = typeOfSamples.get(0);

                if (!GenericValidator.isBlankOrNull(panelRequest.getSampleType())) {
                    if (typeOfSamples.size() > 1) {
                        for (TypeOfSample typeOfSample : typeOfSamples) {
                            if (typeOfSample.getDescription().equals(panelRequest.getSampleType())) {
                                hasSingleSampleType = true;
                                singleTypeOfSample = typeOfSample;
                                break;
                            }
                        }
                    }
                }

                if (hasSingleSampleType) {
                    PanelTestLists panelTestLists = typeOfSampleMap.get(singleTypeOfSample);
                    if (panelTestLists == null) {
                        panelTestLists = new PanelTestLists();
                        typeOfSampleMap.put(singleTypeOfSample, panelTestLists);
                    }

                    panelTestLists.addPanel(panel);
                } else {
                    panelSampleTypesMap.put(panel, typeOfSamples);
                }
            }
        }
    }

    private void addSampleTypes(StringBuilder xml) {
        xml.append("<sampleTypes>");
        for (TypeOfSample typeOfSample : typeOfSampleMap.keySet()) {
            if (typeOfSample != null) {
                addSampleType(xml, typeOfSample, typeOfSampleMap.get(typeOfSample));
            }
        }
        xml.append("</sampleTypes>");
    }

    private void addSampleType(StringBuilder xml, TypeOfSample typeOfSample, PanelTestLists panelTestLists) {
        xml.append("<sampleType>");
        XMLUtil.appendKeyValue("id", typeOfSample.getId(), xml);
        XMLUtil.appendKeyValue("name", typeOfSample.getLocalizedName(), xml);
        addPanels(xml, panelTestLists.getPanels(), typeOfSample.getId());
        addTests(xml, "tests", panelTestLists.getTests());
        addCollection(xml, "collection", specimen, collector);
        xml.append("</sampleType>");
    }

    private void addCollection(StringBuilder xml, String parent, Specimen specimen, Practitioner collector) {
        xml.append(XMLUtil.makeStartTag(parent));
        if (specimen != null && !GenericValidator
                .isBlankOrNull(specimen.getCollection().getCollectedDateTimeType().getValueAsString())) {
            XMLUtil.appendKeyValue("date",
                    DateUtil.formatDateAsText(specimen.getCollection().getCollectedDateTimeType().getValue()), xml);
            XMLUtil.appendKeyValue("time",
                    DateUtil.formatTimeAsText(specimen.getCollection().getCollectedDateTimeType().getValue()), xml);
            XMLUtil.appendKeyValue("dateTime", specimen.getCollection().getCollectedDateTimeType().getValueAsString(),
                    xml);
        }
        if (collector != null) {
            XMLUtil.appendKeyValue("collector",
                    collector.getNameFirstRep().getGivenAsSingleString() + collector.getNameFirstRep().getFamily(),
                    xml);
        }
        xml.append(XMLUtil.makeEndTag(parent));
    }

    private void addPanels(StringBuilder xml, List<Panel> panels, String sampleTypeId) {
        xml.append("<panels>");
        for (Panel panel : panels) {
            xml.append("<panel>");
            XMLUtil.appendKeyValue("id", panel.getId(), xml);
            XMLUtil.appendKeyValue("name", panel.getLocalizedName(), xml);
            addPanelTests(xml, panel.getId(), sampleTypeId);
            xml.append("</panel>");
        }
        xml.append("</panels>");
    }

    private void addPanelTests(StringBuilder xml, String panelId, String sampleTypeId) {
        List<Test> panelTests = getTestsForPanelAndType(panelId, sampleTypeId);
        addTests(xml, "panelTests", panelTests);
    }

    private List<Test> getTestsForPanelAndType(String panelId, String sampleTypeId) {
        List<TypeOfSampleTest> sampleTestList = typeOfSampleTestService.getTypeOfSampleTestsForSampleType(sampleTypeId);
        List<Integer> testList = new ArrayList<>();
        for (TypeOfSampleTest typeTest : sampleTestList) {
            testList.add(Integer.parseInt(typeTest.getTestId()));
        }

        List<PanelItem> panelList = panelItemService.getPanelItemsForPanelAndItemList(panelId, testList);
        List<Test> tests = new ArrayList<>();
        for (PanelItem item : panelList) {
            tests.add(item.getTest());
        }

        return tests;
    }

    private void addTests(StringBuilder xml, String parent, List<Test> tests) {

        xml.append(XMLUtil.makeStartTag(parent));
        for (Test test : tests) {
            xml.append("<test>");
            XMLUtil.appendKeyValue("id", test.getId(), xml);
            XMLUtil.appendKeyValue("name", test.getLocalizedName(), xml);
            xml.append("</test>");
        }
        xml.append(XMLUtil.makeEndTag(parent));
    }

    private void addCrossPanels(StringBuilder xml) {
        xml.append("<crosspanels>");
        for (Panel panel : panelSampleTypesMap.keySet()) {
            addCrosspanel(xml, panel, panelSampleTypesMap.get(panel));
        }

        xml.append("</crosspanels>");
    }

    private void addCrosspanel(StringBuilder xml, Panel panel, List<TypeOfSample> typeOfSampleList) {
        xml.append("<crosspanel>");
        XMLUtil.appendKeyValue("name", panel.getLocalizedName(), xml);
        XMLUtil.appendKeyValue("id", panel.getId(), xml);
        addPanelCrosssampletypes(xml, typeOfSampleList);
        xml.append("</crosspanel>");
    }

    private void addPanelCrosssampletypes(StringBuilder xml, List<TypeOfSample> typeOfSampleList) {
        xml.append("<crosssampletypes>");
        for (TypeOfSample typeOfSample : typeOfSampleList) {
            addCrosspanelTypeOfSample(xml, typeOfSample);
        }
        xml.append("</crosssampletypes>");
    }

    private void addCrosspanelTypeOfSample(StringBuilder xml, TypeOfSample typeOfSample) {
        xml.append("<crosssampletype>");
        XMLUtil.appendKeyValue("id", typeOfSample.getId(), xml);
        XMLUtil.appendKeyValue("name", typeOfSample.getLocalizedName(), xml);
        xml.append("</crosssampletype>");
    }

    private void addCrosstests(StringBuilder xml) {
        xml.append("<crosstests>");
        for (String testName : testNameTestSampleTypeMap.keySet()) {
            addCrosstestForTestName(xml, testName, testNameTestSampleTypeMap.get(testName));
        }
        xml.append("</crosstests>");
    }

    private void addCrosstestForTestName(StringBuilder xml, String testName, List<TestSampleType> list) {
        xml.append("<crosstest>");
        XMLUtil.appendKeyValue("name", testName, xml);
        xml.append("<crosssampletypes>");
        for (TestSampleType testSampleType : list) {
            addTestCrosssampleType(xml, testSampleType);
        }
        xml.append("</crosssampletypes>");
        xml.append("</crosstest>");
    }

    private void addTestCrosssampleType(StringBuilder xml, TestSampleType testSampleType) {
        xml.append("<crosssampletype>");
        XMLUtil.appendKeyValue("id", testSampleType.getSampleType().getId(), xml);
        XMLUtil.appendKeyValue("name", testSampleType.getSampleType().getLocalizedName(), xml);
        XMLUtil.appendKeyValue("testid", testSampleType.getTest().getId(), xml);
        xml.append("</crosssampletype>");
    }

    private void addAlerts(StringBuilder xml, String patientGuid) {
        PatientService patientService = SpringContext.getBean(PatientService.class);
        org.openelisglobal.patient.valueholder.Patient patient = patientService.getPatientForGuid(patientGuid);
        if (patient == null) {
            XMLUtil.appendKeyValue("user_alert", MessageUtil.getMessage("electronic.order.warning.missingPatient"),
                    xml);
        } else if (GenericValidator.isBlankOrNull(patientService.getEnteredDOB(patient))
                || GenericValidator.isBlankOrNull(patientService.getGender(patient))) {
            XMLUtil.appendKeyValue("user_alert", MessageUtil.getMessage("electroinic.order.warning.missingPatientInfo"),
                    xml);
        }
    }

    public class PanelTestLists {
        private List<Test> tests = new ArrayList<>();
        private List<Panel> panels = new ArrayList<>();
        private Timestamp collectionTime;
        private String collector;

        public List<Test> getTests() {
            return tests;
        }

        public List<Panel> getPanels() {
            return panels;
        }

        public void addPanel(Panel panel) {
            if (panel != null) {
                panels.add(panel);
            }
        }

        public void addTest(Test test) {
            if (test != null) {
                tests.add(test);
            }
        }

        public Timestamp getCollectionTime() {
            return collectionTime;
        }

        public void setCollectionTime(Timestamp collectionTime) {
            this.collectionTime = collectionTime;
        }

        public String getCollector() {
            return collector;
        }

        public void setCollector(String collector) {
            this.collector = collector;
        }
    }

    public class TestSampleType {
        private Test test;
        private TypeOfSample sampleType;

        public TestSampleType(Test test, TypeOfSample sampleType) {
            this.test = test;
            this.sampleType = sampleType;
        }

        public Test getTest() {
            return test;
        }

        public TypeOfSample getSampleType() {
            return sampleType;
        }
    }

    private class Request {
        private String name;
        private String loinc;
        private String sampleType;

        public Request(String name, String loinc, String sampleType) {
            this.name = name;
            this.loinc = loinc;
            this.sampleType = sampleType;
        }

        public String getSampleType() {
            return sampleType;
        }

        public String getLoinc() {
            return loinc;
        }

        public String getName() {
            return name;
        }
    }
}
