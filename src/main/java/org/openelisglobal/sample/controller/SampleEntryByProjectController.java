package org.openelisglobal.sample.controller;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Encounter.EncounterParticipantComponent;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Specimen;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.ParameterComponent;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.ExternalOrderStatus;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.dictionary.ObservationHistoryList;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.service.OrganizationTypeService;
import org.openelisglobal.organization.util.OrganizationTypeList;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.organization.valueholder.OrganizationType;
import org.openelisglobal.patient.saving.ISampleEntry;
import org.openelisglobal.patient.saving.ISampleEntryAfterPatientEntry;
import org.openelisglobal.patient.saving.ISampleSecondEntry;
import org.openelisglobal.patient.valueholder.ObservationData;
import org.openelisglobal.sample.form.ProjectData;
import org.openelisglobal.sample.form.SampleEntryByProjectForm;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.owasp.encoder.Encode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SampleEntryByProjectController extends BaseSampleEntryController {

    @Value("${org.openelisglobal.requester.identifier:}")
    private String requestFhirUuid;

    @Value("${org.openelisglobal.fhir.subscriber}")
    private String defaultRemoteServer;

    @Autowired
    private ElectronicOrderService electronicOrderService;
    @Autowired
    private FhirPersistanceService fhirPersistanceService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private OrganizationTypeService organizationTypeService;
    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private FhirUtil fhirUtil;

    private Task task = null;
    private Practitioner requesterPerson = null;
    private Practitioner collector = null;
    private org.hl7.fhir.r4.model.Organization referringOrganization = null;
    private Location location = null;
    private ServiceRequest serviceRequest = null;
    private Specimen specimen = null;
    private Patient fhirPatient = null;
    private Encounter encounter = null;
    private static String OPENMRS_SYSTEM_URL = "https://openmrs.org";
    public static final String REFERRING_ORG_TYPE = "referring clinic";
    public static final String ARV_ORG_TYPE = "ARV Service Loc";

    private static final String[] ALLOWED_FIELDS = new String[] { "currentDate", "domain", "project",
            "patientLastUpdated", "personLastUpdated", "patientUpdateStatus", "patientPK", "patientFhirUuid",
            "samplePK", "observations.projectFormName", "ProjectData.ARVcenterName", "ProjectData.ARVcenterCode",
            "observations.nameOfDoctor", "receivedDateForDisplay", "receivedTimeForDisplay", "interviewDate",
            "interviewTime", "subjectNumber", "siteSubjectNumber", "upidCode", "labNo", "gender", "birthDateForDisplay",
            "ProjectData.dryTubeTaken", "ProjectData.edtaTubeTaken", "ProjectData.serologyHIVTest",
            "ProjectData.glycemiaTest", "ProjectData.creatinineTest", "ProjectData.transaminaseTest",
            "ProjectData.nfsTest", "ProjectData.cd4cd8Test", "ProjectData.viralLoadTest", "ProjectData.genotypingTest",
            "observations.underInvestigation", "ProjectData.underInvestigationNote", "observations.hivStatus",
            "ProjectData.EIDSiteName", "projectData.EIDsiteCode", "observations.whichPCR",
            "observations.reasonForSecondPCRTest", "observations.nameOfRequestor", "observations.nameOfSampler",
            "observations.eidInfantPTME", "observations.eidTypeOfClinic", "observations.eidHowChildFed",
            "observations.eidStoppedBreastfeeding", "observations.eidInfantSymptomatic", "observations.eidInfantsARV",
            "observations.eidInfantCotrimoxazole", "observations.eidMothersHIVStatus", "observations.eidMothersARV",
            "ProjectData.dbsTaken", "ProjectData.dbsvlTaken", "ProjectData.pscvlTaken", "ProjectData.dnaPCR",
            "ProjectData.INDsiteName", "ProjectData.address", "ProjectData.phoneNumber", "ProjectData.faxNumber",
            "ProjectData.email", "observations.indFirstTestDate", "observations.indFirstTestName",
            "observations.indFirstTestResult", "observations.indSecondTestDate", "observations.indSecondTestName",
            "observations.indSecondTestResult", "observations.indSiteFinalResult", "observations.reasonForRequest",
            "ProjectData.murexTest", "ProjectData.integralTest", "ProjectData.GenscreenTest",
            "ProjectData.vironostikaTest", "ProjectData.innoliaTest", "ProjectData.transaminaseALTLTest",
            "ProjectData.transaminaseASTLTest", "ProjectData.gbTest", "ProjectData.lymphTest", "ProjectData.monoTest",
            "ProjectData.eoTest", "ProjectData.basoTest", "ProjectData.grTest", "ProjectData.hbTest",
            "ProjectData.hctTest", "ProjectData.vgmTest", "ProjectData.tcmhTest", "ProjectData.ccmhTest",
            "ProjectData.plqTest", "ProjectData.cd3CountTest", "ProjectData.cd4CountTest", "observations.vlPregnancy",
            "observations.vlSuckle", "observations.currentARVTreatment", "observations.arvTreatmentInitDate",
            "observations.arvTreatmentRegime", "observations.currentARVTreatmentINNsList*",
            "observations.vlReasonForRequest", "observations.vlOtherReasonForRequest", "observations.initcd4Count",
            "observations.initcd4Percent", "observations.initcd4Date", "observations.demandcd4Count",
            "observations.demandcd4Percent", "observations.demandcd4Date", "observations.vlBenefit",
            "observations.priorVLValue", "observations.priorVLDate", "electronicOrder.externalId",
            "ProjectData.asanteTest", "ProjectData.hpvTest", "ProjectData.plasmaTaken", "ProjectData.serumTaken",
            "ProjectData.preservCytTaken", "observations.hpvSamplingMethod", "ProjectData.abbottOrRocheAnalysis",
            "ProjectData.geneXpertAnalysis", "ProjectData.hpvTest" };

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = "/SampleEntryByProject", method = RequestMethod.GET)
    public ModelAndView showSampleEntryByProject(HttpServletRequest request) {
        SampleEntryByProjectForm form = new SampleEntryByProjectForm();

        Date today = Calendar.getInstance().getTime();
        String dateAsText = DateUtil.formatDateAsText(today);
        form.setReceivedDateForDisplay(dateAsText);
        form.setInterviewDate(dateAsText);

        setupFormData(request, form);

        setDisplayLists(form);
        addFlashMsgsToRequest(request);

        return findForward(FWD_SUCCESS, form);
    }

    private void setupFormData(HttpServletRequest request, SampleEntryByProjectForm form) {
        try {
            String externalOrderNumber = request.getParameter("ID");
            if (StringUtils.isNotBlank(externalOrderNumber)) {
                ElectronicOrder eOrder = null;
                List<ElectronicOrder> eOrders = electronicOrderService
                        .getElectronicOrdersByExternalId(externalOrderNumber);
                if (eOrders.size() > 0)
                    eOrder = eOrders.get(eOrders.size() - 1);
                if (eOrder != null) {
                    form.setElectronicOrder(eOrder);
                    IGenericClient localFhirClient = fhirUtil.getLocalFhirClient();
                    IGenericClient remoteFhirClient = fhirUtil.getFhirClient(defaultRemoteServer);
                    for (String remotePath : fhirConfig.getRemoteStorePaths()) {
                        Bundle srBundle = (Bundle) localFhirClient.search().forResource(ServiceRequest.class)
                                .where(ServiceRequest.RES_ID.exactly().code(externalOrderNumber))
                                .include(ServiceRequest.INCLUDE_SPECIMEN).execute();
                        for (BundleEntryComponent bundleComponent : srBundle.getEntry()) {
                            if (bundleComponent.hasResource() && ResourceType.ServiceRequest
                                    .equals(bundleComponent.getResource().getResourceType())) {
                                serviceRequest = (ServiceRequest) bundleComponent.getResource();
                            }
                            if (bundleComponent.hasResource()
                                    && ResourceType.Specimen.equals(bundleComponent.getResource().getResourceType())) {
                                specimen = (Specimen) bundleComponent.getResource();
                            }
                        }
                        srBundle = (Bundle) localFhirClient
                                .search().forResource(ServiceRequest.class).where(ServiceRequest.IDENTIFIER.exactly()
                                        .systemAndIdentifier(remotePath, externalOrderNumber))
                                .include(ServiceRequest.INCLUDE_SPECIMEN).execute();
                        for (BundleEntryComponent bundleComponent : srBundle.getEntry()) {
                            if (bundleComponent.hasResource() && ResourceType.ServiceRequest
                                    .equals(bundleComponent.getResource().getResourceType())) {
                                serviceRequest = (ServiceRequest) bundleComponent.getResource();
                            }
                            if (bundleComponent.hasResource()
                                    && ResourceType.Specimen.equals(bundleComponent.getResource().getResourceType())) {
                                specimen = (Specimen) bundleComponent.getResource();
                            }
                        }
                    }
                    if (serviceRequest != null) {
                        LogEvent.logDebug(this.getClass().getName(), "processRequest",
                                "found matching serviceRequest " + serviceRequest.getIdElement().getIdPart());
                    } else {
                        LogEvent.logDebug(this.getClass().getName(), "processRequest", "no matching serviceRequest");
                    }
                    fhirPatient = localFhirClient.read() //
                            .resource(Patient.class) //
                            .withId(serviceRequest.getSubject().getReferenceElement().getIdPart()) //
                            .execute();
                    encounter = remoteFhirClient.read().resource(Encounter.class)
                            .withId(serviceRequest.getEncounter().getReferenceElement().getIdPart()).execute();
                    if (ObjectUtils.isEmpty(encounter)) {
                        LogEvent.logDebug(this.getClass().getName(), "processRequest", "Not found matching Ecounter "
                                + serviceRequest.getEncounter().getReferenceElement().getIdPart());
                    }

                    if (fhirPatient != null) {
                        LogEvent.logDebug(this.getClass().getName(), "processRequest",
                                "found matching patient " + fhirPatient.getIdElement().getIdPart());
                    } else {
                        LogEvent.logDebug(this.getClass().getName(), "processRequest", "no matching patient");
                    }
                    task = fhirPersistanceService.getTaskBasedOnServiceRequest(externalOrderNumber).orElseThrow();
                    if (task != null) {
                        LogEvent.logDebug(this.getClass().getName(), "processRequest",
                                "found matching task " + task.getIdElement().getIdPart());
                    } else {
                        LogEvent.logDebug(this.getClass().getName(), "processRequest", "no matching task");
                    }
                    if (!GenericValidator.isBlankOrNull(
                            task.getRestriction().getRecipientFirstRep().getReferenceElement().getIdPart())) {
                        referringOrganization = localFhirClient.read() //
                                .resource(org.hl7.fhir.r4.model.Organization.class) //
                                .withId(task.getRestriction().getRecipientFirstRep().getReferenceElement().getIdPart()) //
                                .execute();
                        if (referringOrganization != null) {
                            LogEvent.logDebug(this.getClass().getName(), "processRequest",
                                    "found matching organization " + referringOrganization.getIdElement().getIdPart());
                        } else {
                            LogEvent.logDebug(this.getClass().getName(), "processRequest", "no matching organization");
                        }
                    }
                    if (!GenericValidator.isBlankOrNull(
                            serviceRequest.getLocationReferenceFirstRep().getReferenceElement().getIdPart())) {
                        location = localFhirClient.read() //
                                .resource(Location.class) //
                                .withId(serviceRequest.getLocationReferenceFirstRep().getReferenceElement().getIdPart()) //
                                .execute();
                        if (location != null) {
                            LogEvent.logDebug(this.getClass().getName(), "processRequest",
                                    "found matching location " + location.getIdElement().getIdPart());
                        } else {
                            LogEvent.logDebug(this.getClass().getName(), "processRequest", "no matching location");
                        }
                    }
                    if (!GenericValidator.isBlankOrNull(serviceRequest.getRequester().getReferenceElement().getIdPart())
                            && serviceRequest.getRequester().getReference()
                                    .contains(ResourceType.Practitioner.toString())) {
                        requesterPerson = localFhirClient.read() //
                                .resource(Practitioner.class) //
                                .withId(serviceRequest.getRequester().getReferenceElement().getIdPart()) //
                                .execute();

                        if (requesterPerson != null) {
                            LogEvent.logDebug(this.getClass().getName(), "processRequest",
                                    "found matching requester " + requesterPerson.getIdElement().getIdPart());
                        } else {
                            LogEvent.logDebug(this.getClass().getName(), "processRequest", "no matching requester");
                        }
                    }
                    if (specimen != null && !GenericValidator
                            .isBlankOrNull(specimen.getCollection().getCollector().getReferenceElement().getIdPart())) {
                        collector = localFhirClient.read() //
                                .resource(Practitioner.class) //
                                .withId(specimen.getCollection().getCollector().getReferenceElement().getIdPart()) //
                                .execute();

                        if (collector != null) {
                            LogEvent.logDebug(this.getClass().getName(), "processRequest",
                                    "found matching collector " + collector.getIdElement().getIdPart());
                        } else {
                            LogEvent.logDebug(this.getClass().getName(), "processRequest", "no matching collector");
                        }
                    }
                    loadDataInForm(form);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadDataInForm(SampleEntryByProjectForm form) {
        ProjectData projectData = new ProjectData();
        ObservationData observationData = new ObservationData();
        form.setBirthDateForDisplay(DateUtil.formatDateAsText(fhirPatient.getBirthDate()));
        form.setGender(fhirPatient.getGender().getDisplay().substring(0, 1).toUpperCase());
        if (ObjectUtils.isNotEmpty(fhirPatient.getIdElement())) {
            form.setPatientFhirUuid(fhirPatient.getIdElement().getIdPart());
        }
        for (Identifier identifier : fhirPatient.getIdentifier()) {
            if ((fhirConfig.getOeFhirSystem() + "/pat_subjectNumber").equals(identifier.getSystem())) {
                form.setSubjectNumber(identifier.getValue());
            }
            if ((fhirConfig.getOeFhirSystem() + "/pat_nationalId").equals(identifier.getSystem())) {
                form.setSiteSubjectNumber(identifier.getValue());
            }
            if ((OPENMRS_SYSTEM_URL + "/UPI").equals(identifier.getSystem())) {
                form.setUpidCode(identifier.getValue());
            }
            // get location
            if (("http://fhir.openmrs.org/ext/patient/identifier#location")
                    .equals(identifier.getExtensionFirstRep().getUrl())) {
                Extension extension = identifier.getExtensionFirstRep();
                Reference locationReference = (Reference) extension.getValue();
                String reference = locationReference.getReference();
                // the short code can be 5 ou 4 digits base code
                String centerCode = reference.substring(reference.length() - 5);
                try {
                    Integer.parseInt(centerCode);
                } catch (Exception e) {
                    centerCode = reference.substring(reference.length() - 4);
                }
                String display = locationReference.getDisplay();

                Organization org = organizationService.getOrganizationByShortName(centerCode, true);
                try {
                    if (ObjectUtils.isEmpty(org)) {
                        // create a new Organization
                        org = new Organization();
                        org.setOrganizationName(display);
                        org.setName(display);
                        org.setShortName(centerCode);
                        org.setIsActive("Y");
                        org.setMlsSentinelLabFlag("N");
                        org.setLastupdated(DateUtil.getNowAsTimestamp());
                        organizationService.insert(org);
                        OrganizationType referringClinicSiteType = organizationTypeService
                                .getOrganizationTypeByName(REFERRING_ORG_TYPE);
                        OrganizationType arvSiteType = organizationTypeService.getOrganizationTypeByName(ARV_ORG_TYPE);
                        organizationService.linkOrganizationAndType(org, referringClinicSiteType.getId());
                        organizationService.linkOrganizationAndType(org, arvSiteType.getId());
                    }

                    projectData.setARVcenterCode(org.getId());
                    projectData.setARVcenterName(org.getId());
                } catch (Exception e) {
                    LogEvent.logDebug(this.getClass().getName(), "setOrganizationFromFhirObject", e.getMessage());
                }
            }
        }
        for (ParameterComponent parameter : task.getInput()) {

            if (parameter.getType().getCodingFirstRep().getCode().equals("160533AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) { // currentARVTreatment
                if (ObjectUtils.isNotEmpty(parameter.getValue())) {
                    Dictionary dict = dictionaryService
                            .getDictionaryByDictEntry("Demographic Response Yes (in Yes or No)");
                    if (ObjectUtils.isNotEmpty(dict)) {
                        observationData.setCurrentARVTreatment(dict.getId());
                    }
                }
            }
            if (parameter.getType().getCodingFirstRep().getCode().equals("CI0050002AAAAAAAAAAAAAAAAAAAAAAAAAAA")) { // vlReasonForRequest
                if (ObjectUtils.isNotEmpty(parameter.getValue())) {
                    if (parameter.getValue() instanceof StringType) {
                        StringType vlReasonType = (StringType) parameter.getValue();
                        Dictionary dict = null;
                        switch (vlReasonType.getValue().trim()) {
                        case "Charge Virale de controle":
                            dict = dictionaryService.getDictionaryByDictEntry("VL under ARV control");
                            break;
                        case "Echec Virologique":
                            dict = dictionaryService.getDictionaryByDictEntry("Virological Failure");
                            break;
                        case "Echec immunologique":
                            dict = dictionaryService.getDictionaryByDictEntry("Immunological Failure");
                            break;
                        case "Echec clinique":
                        case "GB J0":
                            dict = dictionaryService.getDictionaryByDictEntry("Clinical Failure");
                            break;
                        default:
                            break;
                        }
                        if (ObjectUtils.isNotEmpty(dict)) {
                            observationData.setVlReasonForRequest(dict.getId());
                        }
                    }
                }
            }

            if (ObjectUtils.isNotEmpty(encounter)) { // nameofsampler
                List<EncounterParticipantComponent> participants = encounter.getParticipant();
                if (ObjectUtils.isNotEmpty(participants)) {
                    if (ObjectUtils.isNotEmpty(participants.get(1))) { // get the second one for Sampler
                        String samplerReference = participants.get(1).getIndividual().getReference();
                        Practitioner sampler = fhirUtil.getLocalFhirClient().read().resource(Practitioner.class)
                                .withId(samplerReference).execute();
                        sampler.getName().forEach(humanName -> {
                            String lastName = humanName.getFamily();
                            String firstName = String.join("", humanName.getGiven().stream().map(e -> e.asStringValue())
                                    .collect(Collectors.toList()));
                            observationData.setNameOfSampler(lastName + " " + firstName);
                        });
                    }
                }
            }
            if (ObjectUtils.isNotEmpty(encounter)) { // get Collection Date
                Period period = encounter.getPeriod();
                if (ObjectUtils.isNotEmpty(period)) {
                    DateTimeType collectionDateType = period.getStartElement();
                    if (ObjectUtils.isNotEmpty(collectionDateType))
                        form.setInterviewDate(DateUtil.formatDateAsText(collectionDateType.getValue()));
                }
            }
            if (parameter.getType().getCodingFirstRep().getCode().equals("CI0050001AAAAAAAAAAAAAAAAAAAAAAAAAAA")) { // vlOtherReasonForRequest
                if (ObjectUtils.isNotEmpty(parameter.getValue())) {
                    if (parameter.getValue() instanceof StringType) {
                        StringType vlOtherReasonForRequestType = (StringType) parameter.getValue();
                        observationData.setVlOtherReasonForRequest(vlOtherReasonForRequestType.getValue());
                    }
                }
            }
            if (parameter.getType().getCodingFirstRep().getCode().equals("CI0050007AAAAAAAAAAAAAAAAAAAAAAAAAAA")) { // sampleType
                if (ObjectUtils.isNotEmpty(parameter.getValue())) {
                    if (parameter.getValue() instanceof StringType) {
                        StringType sampleTypeType = (StringType) parameter.getValue();
                        if (sampleTypeType.getValue().equalsIgnoreCase("Plasma")) {
                            projectData.setEdtaTubeTaken(true);
                        } else if (sampleTypeType.getValue().equalsIgnoreCase("DBS")) {
                            projectData.setdbsvlTaken(true);
                        } else if (sampleTypeType.getValue().equalsIgnoreCase("PSC")) {
                            projectData.setPscvlTaken(true);
                        }
                    }
                }
            }
            if (parameter.getType().getCodingFirstRep().getCode().equals("166073AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) { // arvTreatmentRegime
                if (ObjectUtils.isNotEmpty(parameter.getValue())) {
                    if (parameter.getValue() instanceof StringType) {
                        StringType arvTreatmentRegimeType = (StringType) parameter.getValue();
                        Dictionary dict = null;
                        if (arvTreatmentRegimeType.getValue().equalsIgnoreCase("Première")) {
                            dict = dictionaryService.getDictionaryByDictEntry("1st Line");
                        } else if (arvTreatmentRegimeType.getValue().equalsIgnoreCase("Deuxième")) {
                            dict = dictionaryService.getDictionaryByDictEntry("2nd Line");
                        } else if (arvTreatmentRegimeType.getValue().equalsIgnoreCase("Troisième")) {
                            dict = dictionaryService.getDictionaryByDictEntry("3rd Line");
                        }
                        if (ObjectUtils.isNotEmpty(dict)) {
                            observationData.setArvTreatmentRegime(dict.getId());
                        }
                    }
                }
            }
            if (parameter.getType().getCodingFirstRep().getCode().equals("164792AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) { // vl.initcd4Percent
                if (ObjectUtils.isNotEmpty(parameter.getValue())) {
                    if (parameter.getValue() instanceof DecimalType) {
                        DecimalType initcd4PercentType = (DecimalType) parameter.getValue();
                        observationData.setInitcd4Percent(initcd4PercentType.getValue().toString());
                    }
                }
            }
            if (parameter.getType().getCodingFirstRep().getCode().equals("730AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) { // vl.demandcd4Percent
                if (ObjectUtils.isNotEmpty(parameter.getValue())) {
                    if (parameter.getValue() instanceof DecimalType) {
                        DecimalType demandcd4PercentType = (DecimalType) parameter.getValue();
                        observationData.setDemandcd4Percent(demandcd4PercentType.getValue().toString());
                    }
                }
            }
            if (parameter.getType().getCodingFirstRep().getCode().equals("CI0050030AAAAAAAAAAAAAAAAAAAAAAAAAAA")) { // vl.priorVLValue
                if (ObjectUtils.isNotEmpty(parameter.getValue())) {
                    if (parameter.getValue() instanceof StringType) {
                        StringType priorVLValueType = (StringType) parameter.getValue();
                        observationData.setPriorVLValue(priorVLValueType.getValue().toString());
                    }
                }
            }
            if (parameter.getType().getCodingFirstRep().getCode().equals("CI0030001AAAAAAAAAAAAAAAAAAAAAAAAAAA")) { // vl.hivStatus
                if (ObjectUtils.isNotEmpty(parameter.getValue())) {
                    if (parameter.getValue() instanceof StringType) {
                        StringType hivStatusType = (StringType) parameter.getValue();
                        Dictionary dict = null;
                        if (hivStatusType.getValue().equalsIgnoreCase("VIH-1")) {
                            dict = dictionaryService.getDictionaryByDictEntry("HIV Status HIV-1 infection");
                        } else if (hivStatusType.getValue().equalsIgnoreCase("VIH-1+2")) {
                            dict = dictionaryService.getDictionaryByDictEntry("HIV Status HIV-1 and HIV-2");
                        } else if (hivStatusType.getValue().equalsIgnoreCase("VIH-2")) {
                            dict = dictionaryService.getDictionaryByDictEntry("HIV Status HIV-2 infection");
                        }
                        if (ObjectUtils.isNotEmpty(dict)) {
                            observationData.setHivStatus(dict.getId());
                        }
                    }
                }
            }
            if (parameter.getType().getCodingFirstRep().getCode().equals("159599AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) { // vl.arvTreatmentInitDate
                if (ObjectUtils.isNotEmpty(parameter.getValue())) {
                    if (parameter.getValue() instanceof DateTimeType) {
                        DateTimeType dateValue = (DateTimeType) parameter.getValue();
                        observationData.setArvTreatmentInitDate(DateUtil.formatDateAsText(dateValue.getValue()));
                    }
                }
            }
            if (parameter.getType().getCodingFirstRep().getCode().equals("160103AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) { // vl.demandcd4Date
                if (ObjectUtils.isNotEmpty(parameter.getValue())) {
                    if (parameter.getValue() instanceof DateTimeType) {
                        DateTimeType dateValue = (DateTimeType) parameter.getValue();
                        if (ObjectUtils.isNotEmpty(dateValue))
                            observationData.setDemandcd4Date(DateUtil.formatDateAsText(dateValue.getValue()));
                    }
                }
            }
            if (parameter.getType().getCodingFirstRep().getCode().equals("163281AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) { // vl.priorVLDate
                if (ObjectUtils.isNotEmpty(parameter.getValue())) {
                    if (parameter.getValue() instanceof DateTimeType) {
                        DateTimeType dateValue = (DateTimeType) parameter.getValue();
                        if (ObjectUtils.isNotEmpty(dateValue))
                            observationData.setPriorVLDate(DateUtil.formatDateAsText(dateValue.getValue()));
                    }
                }
            }
            if (parameter.getType().getCodingFirstRep().getCode().equals("CI0050020AAAAAAAAAAAAAAAAAAAAAAAAAAA")) { // vl.priorVLLab
                if (ObjectUtils.isNotEmpty(parameter.getValue())) {
                    if (parameter.getValue() instanceof StringType) {
                        StringType priorVLLab = (StringType) parameter.getValue();
                        observationData.setPriorVLLab(priorVLLab.getValueAsString());
                    }
                }
            }
            if (parameter.getType().getCodingFirstRep().getCode().equals("164429AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) { // vl.initcd4Count
                if (ObjectUtils.isNotEmpty(parameter.getValue())) {
                    if (parameter.getValue() instanceof DecimalType) {
                        DecimalType initcd4CountType = (DecimalType) parameter.getValue();
                        observationData.setInitcd4Count(initcd4CountType.getValue().toString());
                    }
                }
            }
            if (parameter.getType().getCodingFirstRep().getCode().equals("5497AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) { // vl.demandcd4Count
                if (ObjectUtils.isNotEmpty(parameter.getValue())) {
                    if (parameter.getValue() instanceof DecimalType) {
                        DecimalType demandcd4CountType = (DecimalType) parameter.getValue();
                        observationData.setDemandcd4Count(demandcd4CountType.getValue().toString());
                    }
                }
            }
            if (parameter.getType().getCodingFirstRep().getCode().equals("159376AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) { // vl.initcd4Date
                if (ObjectUtils.isNotEmpty(parameter.getValue())) {
                    if (parameter.getValue() instanceof DateTimeType) {
                        DateTimeType dateValue = (DateTimeType) parameter.getValue();
                        if (ObjectUtils.isNotEmpty(dateValue))
                            observationData.setInitcd4Date(DateUtil.formatDateAsText(dateValue.getValue()));
                    }
                }
            }
            if (parameter.getType().getCodingFirstRep().getCode().equals("CI0050004AAAAAAAAAAAAAAAAAAAAAAAAAAA")) { // vl.vlBenefit
                if (ObjectUtils.isNotEmpty(parameter.getValue())) {
                    if (parameter.getValue() instanceof StringType) {
                        StringType vlBenefitType = (StringType) parameter.getValue();
                        Dictionary dict = null;
                        if (vlBenefitType.getValue().equalsIgnoreCase("Oui")
                                || vlBenefitType.getValue().equalsIgnoreCase("Yes")) {
                            dict = dictionaryService
                                    .getDictionaryByDictEntry("Demographic Response Yes (in Yes or No)");
                        } else if (vlBenefitType.getValue().equalsIgnoreCase("Non")
                                || vlBenefitType.getValue().equalsIgnoreCase("No")) {
                            dict = dictionaryService.getDictionaryByDictEntry("Demographic Response No (in Yes or No)");
                        }
                        if (ObjectUtils.isNotEmpty(dict)) {
                            observationData.setVlBenefit(dict.getId());
                        }
                    }
                }
            }
            if (parameter.getType().getCodingFirstRep().getCode().equals("162240AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) { // ARV
                // Treatment
                if (ObjectUtils.isNotEmpty(parameter.getValue())) {
                    if (parameter.getValue() instanceof StringType) {
                        StringType regimenType = (StringType) parameter.getValue();
                        String regimen = regimenType.getValue();
                        String[] regimenParts = regimen.split(" ");
                        for (int i = 0; i < regimenParts.length && i < 3; i++) {
                            observationData.setCurrentARVTreatmentINNs(i, regimenParts[i].trim());
                        }
                    }
                }
            }
            if (parameter.getType().getCodingFirstRep().getCode().equals("5272AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) { // Pregnancy
                if (ObjectUtils.isNotEmpty(parameter.getValue())) {
                    if (parameter.getValue() instanceof StringType) {
                        StringType vlPregnancyType = (StringType) parameter.getValue();
                        Dictionary dict = null;
                        if (vlPregnancyType.getValue().equalsIgnoreCase("Oui")
                                || vlPregnancyType.getValue().equalsIgnoreCase("Yes")) {
                            dict = dictionaryService
                                    .getDictionaryByDictEntry("Demographic Response Yes (in Yes or No)");
                        } else if (vlPregnancyType.getValue().equalsIgnoreCase("Non")
                                || vlPregnancyType.getValue().equalsIgnoreCase("No")) {
                            dict = dictionaryService.getDictionaryByDictEntry("Demographic Response No (in Yes or No)");
                        }
                        if (ObjectUtils.isNotEmpty(dict)) {
                            observationData.setVlPregnancy(dict.getId());
                        }
                    }
                }
            }

            if (parameter.getType().getCodingFirstRep().getCode().equals("5632AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) { // Breastfeeding
                if (ObjectUtils.isNotEmpty(parameter.getValue())) {
                    if (parameter.getValue() instanceof StringType) {
                        StringType vlBreastFeedingType = (StringType) parameter.getValue();
                        Dictionary dict = null;
                        if (vlBreastFeedingType.getValue().equalsIgnoreCase("Oui")
                                || vlBreastFeedingType.getValue().equalsIgnoreCase("Yes")) {
                            dict = dictionaryService
                                    .getDictionaryByDictEntry("Demographic Response Yes (in Yes or No)");
                        } else if (vlBreastFeedingType.getValue().equalsIgnoreCase("Non")
                                || vlBreastFeedingType.getValue().equalsIgnoreCase("No")) {
                            dict = dictionaryService.getDictionaryByDictEntry("Demographic Response No (in Yes or No)");
                        }
                        if (ObjectUtils.isNotEmpty(dict)) {
                            observationData.setVlSuckle(dict.getId());
                        }
                    }
                }
            }
            // CI0050006AAAAAAAAAAAAAAAAAAAAAAAAAAA Heure de prélèvement
            if (parameter.getType().getCodingFirstRep().getCode().equals("CI0050006AAAAAAAAAAAAAAAAAAAAAAAAAAA")) { // Heure
                // de
                // prélèvement
                if (ObjectUtils.isNotEmpty(parameter.getValue())) {
                    if (parameter.getValue() instanceof DateTimeType) {
                        DateTimeType dateValue = (DateTimeType) parameter.getValue();
                        if (ObjectUtils.isNotEmpty(dateValue))
                            form.setInterviewTime(DateUtil.formatTimeAsText(dateValue.getValue()));
                    }
                }
            }
        }

        requesterPerson.getName().forEach(humanName -> {
            String lastName = humanName.getFamily();
            String firstName = String.join("",
                    humanName.getGiven().stream().map(e -> e.asStringValue()).collect(Collectors.toList()));
            observationData.setNameOfDoctor(lastName + " " + firstName);
        });

        projectData.setViralLoadTest(true);
        form.setProjectData(projectData);
        form.setObservations(observationData);
    }

    @RequestMapping(value = "/SampleEntryByProject", method = RequestMethod.POST)
    public ModelAndView postSampleEntryByProject(HttpServletRequest request,
            @ModelAttribute("form") @Valid SampleEntryByProjectForm form, BindingResult result,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            saveErrors(result);
            setDisplayLists(form);
            return findForward(FWD_FAIL_INSERT, form);
        }
        String forward;

        ISampleSecondEntry sampleSecondEntry = SpringContext.getBean(ISampleSecondEntry.class);
        sampleSecondEntry.setFieldsFromForm(form);
        sampleSecondEntry.setSysUserId(getSysUserId(request));
        sampleSecondEntry.setRequest(request);
        if (sampleSecondEntry.canAccession()) {
            forward = handleSave(request, sampleSecondEntry, form);
            updateElectronicOrderStatus(form);
            if (forward != null) {
                if (FWD_SUCCESS_INSERT.equals(forward)) {
                    redirectAttributes.addFlashAttribute(FWD_SUCCESS, true);
                } else {
                    setDisplayLists(form);
                }
                return findForward(forward, form);
            }
        }
        ISampleEntry sampleEntry = SpringContext.getBean(ISampleEntry.class);
        sampleEntry.setFieldsFromForm(form);
        sampleEntry.setSysUserId(getSysUserId(request));
        sampleEntry.setRequest(request);
        if (sampleEntry.canAccession()) {
            forward = handleSave(request, sampleEntry, form);
            updateElectronicOrderStatus(form);
            if (forward != null) {
                if (FWD_SUCCESS_INSERT.equals(forward)) {
                    redirectAttributes.addFlashAttribute(FWD_SUCCESS, true);
                } else {
                    setDisplayLists(form);
                }
                return findForward(forward, form);
            }
        }
        ISampleEntryAfterPatientEntry sampleEntryAfterPatientEntry = SpringContext
                .getBean(ISampleEntryAfterPatientEntry.class);
        sampleEntryAfterPatientEntry.setFieldsFromForm(form);
        sampleEntryAfterPatientEntry.setSysUserId(getSysUserId(request));
        sampleEntryAfterPatientEntry.setRequest(request);
        if (sampleEntryAfterPatientEntry.canAccession()) {
            forward = handleSave(request, sampleEntryAfterPatientEntry, form);
            updateElectronicOrderStatus(form);
            if (forward != null) {
                if (FWD_SUCCESS_INSERT.equals(forward)) {
                    redirectAttributes.addFlashAttribute(FWD_SUCCESS, true);
                } else {
                    setDisplayLists(form);
                }
                return findForward(forward, form);
            }
        }
        logAndAddMessage(request, "postSampleEntryByProject", "errors.UpdateException");

        setDisplayLists(form);
        return findForward(FWD_FAIL_INSERT, form);
    }

    private void updateElectronicOrderStatus(SampleEntryByProjectForm form) {
        try {
            if (ObjectUtils.isNotEmpty(form.getElectronicOrder())) {
                String externalOrderId = form.getElectronicOrder().getExternalId();
                List<ElectronicOrder> eOrders = electronicOrderService.getElectronicOrdersByExternalId(externalOrderId);
                if (eOrders.size() > 0) {
                    ElectronicOrder eOrder = eOrders.get(eOrders.size() - 1);
                    eOrder.setStatusId(
                            SpringContext.getBean(IStatusService.class).getStatusID(ExternalOrderStatus.Realized));
                    electronicOrderService.update(eOrder);
                    form.setElectronicOrder(eOrder);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogEvent.logError(e);
        }
    }

    public SampleItem getSampleItem(Sample sample, TypeOfSample typeofsample) {
        SampleItem item = new SampleItem();
        item.setSample(sample);
        item.setTypeOfSample(typeofsample);
        item.setSortOrder(Integer.toString(1));
        item.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(SampleStatus.Entered));

        return item;
    }

    private void setDisplayLists(SampleEntryByProjectForm form) {
        Map<String, List<Dictionary>> formListsMapOfLists = new HashMap<>();
        List<Dictionary> listOfDictionary = new ArrayList<>();
        List<IdValuePair> genders = DisplayListService.getInstance().getList(ListType.GENDERS);

        for (IdValuePair i : genders) {
            Dictionary dictionary = new Dictionary();
            dictionary.setId(i.getId());
            dictionary.setDictEntry(i.getValue());
            listOfDictionary.add(dictionary);
        }

        formListsMapOfLists.put("GENDERS", listOfDictionary);
        form.setFormLists(formListsMapOfLists);

        // Get Lists
        Map<String, List<Dictionary>> observationHistoryMapOfLists = new HashMap<>();
        observationHistoryMapOfLists.put("EID_WHICH_PCR", ObservationHistoryList.EID_WHICH_PCR.getList());
        observationHistoryMapOfLists.put("EID_SECOND_PCR_REASON",
                ObservationHistoryList.EID_SECOND_PCR_REASON.getList());
        observationHistoryMapOfLists.put("EID_TYPE_OF_CLINIC", ObservationHistoryList.EID_TYPE_OF_CLINIC.getList());
        observationHistoryMapOfLists.put("EID_HOW_CHILD_FED", ObservationHistoryList.EID_HOW_CHILD_FED.getList());
        observationHistoryMapOfLists.put("EID_STOPPED_BREASTFEEDING",
                ObservationHistoryList.EID_STOPPED_BREASTFEEDING.getList());
        observationHistoryMapOfLists.put("YES_NO", ObservationHistoryList.YES_NO.getList());
        observationHistoryMapOfLists.put("EID_INFANT_PROPHYLAXIS_ARV",
                ObservationHistoryList.EID_INFANT_PROPHYLAXIS_ARV.getList());
        observationHistoryMapOfLists.put("YES_NO_UNKNOWN", ObservationHistoryList.YES_NO_UNKNOWN.getList());
        observationHistoryMapOfLists.put("EID_MOTHERS_HIV_STATUS",
                ObservationHistoryList.EID_MOTHERS_HIV_STATUS.getList());
        observationHistoryMapOfLists.put("EID_MOTHERS_ARV_TREATMENT",
                ObservationHistoryList.EID_MOTHERS_ARV_TREATMENT.getList());
        observationHistoryMapOfLists.put("HIV_STATUSES", ObservationHistoryList.HIV_STATUSES.getList());
        observationHistoryMapOfLists.put("HIV_TYPES", ObservationHistoryList.HIV_TYPES.getList());
        observationHistoryMapOfLists.put("SPECIAL_REQUEST_REASONS",
                ObservationHistoryList.SPECIAL_REQUEST_REASONS.getList());
        observationHistoryMapOfLists.put("ARV_REGIME", ObservationHistoryList.ARV_REGIME.getList());
        observationHistoryMapOfLists.put("ARV_REASON_FOR_VL_DEMAND",
                ObservationHistoryList.ARV_REASON_FOR_VL_DEMAND.getList());
        observationHistoryMapOfLists.put("HPV_SAMPLING_METHOD", ObservationHistoryList.HPV_SAMPLING_METHOD.getList());

        form.setDictionaryLists(observationHistoryMapOfLists);

        // Get EID Sites
        Map<String, List<Organization>> organizationTypeMapOfLists = new HashMap<>();
        organizationTypeMapOfLists.put("ARV_ORGS", OrganizationTypeList.ARV_ORGS.getList());
        organizationTypeMapOfLists.put("ARV_ORGS_BY_NAME", OrganizationTypeList.ARV_ORGS_BY_NAME.getList());
        organizationTypeMapOfLists.put("EID_ORGS_BY_NAME", OrganizationTypeList.EID_ORGS_BY_NAME.getList());
        organizationTypeMapOfLists.put("EID_ORGS", OrganizationTypeList.EID_ORGS.getList());
        form.setOrganizationTypeLists(organizationTypeMapOfLists);
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "sampleEntryByProjectDefinition";
        } else if (FWD_FAIL.equals(forward)) {
            return "homePageDefinition";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/SampleEntryByProject?type=" + Encode.forUriComponent(request.getParameter("type"));
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "sampleEntryByProjectDefinition";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        return null;
    }

    @Override
    protected String getPageSubtitleKey() {
        return null;
    }
}
