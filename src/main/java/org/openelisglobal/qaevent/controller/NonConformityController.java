package org.openelisglobal.qaevent.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.exception.LIMSInvalidConfigurationException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.formfields.FormFields;
import org.openelisglobal.common.formfields.FormFields.Field;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.services.QAService;
import org.openelisglobal.common.services.QAService.QAObservationType;
import org.openelisglobal.common.services.TableIdService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.util.PatientUtil;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.project.service.ProjectService;
import org.openelisglobal.project.valueholder.Project;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.qaevent.form.NonConformityForm;
import org.openelisglobal.qaevent.form.NonConformityForm.NonConformitySearch;
import org.openelisglobal.qaevent.service.NonConformityHelper;
import org.openelisglobal.qaevent.validator.NonConformityFormValidator;
import org.openelisglobal.qaevent.valueholder.retroCI.QaEventItem;
import org.openelisglobal.qaevent.worker.INonConformityUpdateWorker;
import org.openelisglobal.qaevent.worker.NonConformityUpdateData;
import org.openelisglobal.requester.service.SampleRequesterService;
import org.openelisglobal.requester.valueholder.SampleRequester;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampleproject.service.SampleProjectService;
import org.openelisglobal.sampleproject.valueholder.SampleProject;
import org.openelisglobal.sampleqaevent.service.SampleQaEventService;
import org.openelisglobal.sampleqaevent.valueholder.SampleQaEvent;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class NonConformityController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "sampleId", "patientId", "sampleItemsTypeOfSampleIds",
            "date", "time", "project", "projectId", "subjectNew", "subjectNo", "newSTNumber", "STNumber",
            "nationalIdNew", "nationalId", "serviceNew", "newServiceName", "service", "doctorNew", "requesterSampleID",
            "providerLastName", "providerFirstName", "providerStreetAddress", "providerCity", "providerCommune",
            "providerDepartment", "providerWorkPhone", "doctor", "qaEvents*.recordNumber", "qaEvents*.id",
            "qaEvents*.qaEvent", "qaEvents*.sampleType", "qaEvents*.section", "qaEvents*.authorizer", "qaEvents*.note",
            "qaEvents*.remove", "commentNew", "comment" };

    @Autowired
    private NonConformityFormValidator formValidator;

    @Autowired
    private SampleService sampleService;
    @Autowired
    private SampleItemService sampleItemService;
    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private PersonService personService;
    @Autowired
    private ProviderService providerService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private ObservationHistoryService observationHistoryService;
    @Autowired
    private SampleProjectService sampleProjectService;
    @Autowired
    private TestSectionService testSectionService;
    @Autowired
    private SampleRequesterService sampleRequesterService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private SampleQaEventService sampleQaEventService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = "/NonConformity", method = RequestMethod.GET)
    public ModelAndView showNonConformity(HttpServletRequest request,
            @Validated(NonConformitySearch.class) NonConformityForm form, BindingResult result)
            throws LIMSInvalidConfigurationException, IllegalAccessException, InvocationTargetException,
            NoSuchMethodException {
        if (result.hasErrors()) {
            saveErrors(result);
            form = new NonConformityForm();
            return findForward(FWD_FAIL, form);
        }
        String labNumber = form.getLabNo();
        // reset form. We only need labNo at this stage
        form = new NonConformityForm();
        if (!GenericValidator.isBlankOrNull(labNumber)) {
            setupFormForLabNumber(labNumber, form);
        }
        addFlashMsgsToRequest(request);
        request.getSession().setAttribute(SAVE_DISABLED, TRUE);

        return findForward(FWD_SUCCESS, form);
    }

    private void setupFormForLabNumber(String labNumber, NonConformityForm form)
            throws LIMSInvalidConfigurationException, IllegalAccessException, InvocationTargetException,
            NoSuchMethodException {
        Sample sample = getSampleForLabNumber(labNumber);

        boolean sampleFound = !(sample == null || GenericValidator.isBlankOrNull(sample.getId()));

        form.setLabNo(labNumber);
        Date today = Calendar.getInstance().getTime();
        form.setDate(DateUtil.formatDateAsText(today));
        if (FormFields.getInstance().useField(Field.QATimeWithDate)) {
            form.setTime(DateUtil.nowTimeAsText());
        }

        if (sampleFound) {
            createForExistingSample(form, sample);
        }

        setProjectList(form);

        form.setSampleItemsTypeOfSampleIds(getSampleTypeOfSamplesString(sample));
        form.setSections(createSectionList());
        form.setQaEventTypes(DisplayListService.getInstance().getList(ListType.QA_EVENTS));
        form.setQaEvents(getSampleQaEventItems(sample));

        form.setTypeOfSamples(DisplayListService.getInstance().getList(ListType.SAMPLE_TYPE_ACTIVE));

        form.setReadOnly(false);
        form.setSiteList(DisplayListService.getInstance().getFreshList(ListType.SAMPLE_PATIENT_REFERRING_CLINIC));
        Provider provider = getProvider(sample);
        if (provider != null) {
            form.setProviderNew(Boolean.FALSE.toString());
            Person providerPerson = getProviderPerson(provider);
            if (providerPerson != null && !providerPerson.getId().equals(PatientUtil.getUnknownPerson().getId())) {
                form.setProviderFirstName(personService.getFirstName(providerPerson));
                form.setProviderLastName(personService.getLastName(providerPerson));
                form.setProviderWorkPhone(personService.getPhone(providerPerson));
                Map<String, String> addressComponents = personService.getAddressComponents(providerPerson);

                form.setProviderStreetAddress(addressComponents.get("Street"));
                form.setProviderCity(addressComponents.get("village"));
                form.setProviderCommune(addressComponents.get("commune"));
                form.setProviderDepartment(addressComponents.get("department"));
            }
        } else {
            form.setProviderNew(Boolean.TRUE.toString());
            form.setRequesterSampleID("");
            form.setProviderFirstName("");
            form.setProviderLastName("");
            form.setProviderWorkPhone("");
        }

        form.setDepartments(DisplayListService.getInstance().getList(ListType.HAITI_DEPARTMENTS));
    }

    private void createForExistingSample(NonConformityForm form, Sample sample)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Patient patient = sampleHumanService.getPatientForSample(sample);
        PatientService patientService = SpringContext.getBean(PatientService.class);
        PersonService personService = SpringContext.getBean(PersonService.class);
        personService.getData(patient.getPerson());
        List<ObservationHistory> observationHistoryList = getObservationHistory(sample, patient);
        form.setSampleId(sample.getId());
        form.setPatientId(patientService.getPatientId(patient));

        Project project = getProjectForSample(sample);
        if (project != null) {
            form.setProjectId(project.getId());
            form.setProject(project.getLocalizedName());
        }

        String subjectNo = patientService.getSubjectNumber(patient);
        if (!GenericValidator.isBlankOrNull(subjectNo)) {
            form.setSubjectNew(Boolean.FALSE);
            form.setSubjectNo(subjectNo);
        }

        String STNo = patientService.getSTNumber(patient);
        if (!GenericValidator.isBlankOrNull(STNo)) {
            form.setNewSTNumber(Boolean.FALSE);
            form.setSTNumber(STNo);
        }

        String nationalId = patientService.getNationalId(patient);
        if (!GenericValidator.isBlankOrNull(nationalId)) {
            form.setNationalIdNew(Boolean.FALSE);
            form.setNationalId(nationalId);
        }

        ObservationHistory doctorObservation = getRefererObservation(observationHistoryList);
        if (doctorObservation != null) {
            form.setDoctorNew(Boolean.FALSE);
            form.setDoctor(doctorObservation.getValue());
        }

        if (FormFields.getInstance().useField(Field.NON_CONFORMITY_SITE_LIST)) {
            form.setServiceNew(Boolean.FALSE);
            form.setService(getSampleRequesterOrganizationName(sample));
        } else {
            ObservationHistory serviceObservation = getServiceObservation(observationHistoryList);
            if (serviceObservation != null) {
                form.setServiceNew(Boolean.FALSE);
                form.setService(serviceObservation.getValue());
            }
        }

        form.setComment(NonConformityHelper.getNoteForSample(sample));

        form.setRequesterSampleID(sample.getReferringId());
    }

    /**
     * @return
     */
    private String getSampleRequesterOrganizationName(Sample sample) {
        List<SampleRequester> sampleRequestors = sampleRequesterService.getRequestersForSampleId(sample.getId());
        if (sampleRequestors.isEmpty()) {
            return null;
        }
        long typeID = TableIdService.getInstance().ORGANIZATION_REQUESTER_TYPE_ID;
        for (SampleRequester sampleRequester : sampleRequestors) {
            if (sampleRequester.getRequesterTypeId() == typeID) {
                String orgId = String.valueOf(sampleRequester.getRequesterId());
                Organization org = organizationService.get(orgId);
                if (org != null && org.getOrganizationTypes().stream()
                        .anyMatch(e -> e.getId().equals(TableIdService.getInstance().REFERRING_ORG_TYPE_ID))) {
                    String orgName = org.getOrganizationName();
                    orgName += GenericValidator.isBlankOrNull(org.getShortName()) ? "" : ("-" + org.getShortName());
                    return orgName;
                }
            }
        }
        return null;
    }

    /**
     * @param provider
     * @return
     */
    private Person getProviderPerson(Provider provider) {
        if (provider == null) {
            return null;
        }
        return personService.get(provider.getPerson().getId());
    }

    private Provider getProvider(Sample sample) {
        if (sample == null) {
            return null;
        }
        SampleHuman sampleHuman = getSampleHuman(sample);
        String id = sampleHuman.getProviderId();
        if (id == null) {
            return null;
        }
        return providerService.get(id);
    }

    /**
     * @return
     */
    private SampleHuman getSampleHuman(Sample sample) {
        SampleHuman sampleHuman = new SampleHuman();
        sampleHuman.setSampleId(sample.getId());
        return sampleHumanService.getDataBySample(sampleHuman);
    }

    /**
     * @param sample
     * @return
     */
    private List<QaEventItem> getSampleQaEventItems(Sample sample) {
        List<QaEventItem> qaEventItems = new ArrayList<>();
        if (sample != null) {
            List<SampleQaEvent> sampleQAEventList = getSampleQaEvents(sample);
            for (SampleQaEvent event : sampleQAEventList) {
                QAService qa = new QAService(event);
                QaEventItem item = new QaEventItem();
                item.setId(qa.getEventId());
                item.setQaEvent(qa.getQAEvent().getId());
                SampleItem sampleItem = qa.getSampleItem();
                // -1 is the index for "all samples"
                item.setSampleType((sampleItem == null) ? "-1" : sampleItemService.getTypeOfSampleId(sampleItem));
                item.setSection(qa.getObservationValue(QAObservationType.SECTION));
                item.setAuthorizer(qa.getObservationValue(QAObservationType.AUTHORIZER));
                item.setRecordNumber(qa.getObservationValue(QAObservationType.DOC_NUMBER));
                item.setRemove(false);
                item.setNote(NonConformityHelper.getNoteForSampleQaEvent(event));

                qaEventItems.add(item);
            }
        }

        int oldQaEvents = qaEventItems.size();
        for (int i = oldQaEvents; i < 10; i++) {
            qaEventItems.add(new QaEventItem());
        }
        return qaEventItems;
    }

    private Set<TypeOfSample> getSampleTypeOfSamples(Sample sample) {
        Set<TypeOfSample> typeOfSamples = new HashSet<>();
        List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sample.getId());
        for (SampleItem sampleItem : sampleItems) {
            TypeOfSample typeOfSample = typeOfSampleService.get(sampleItem.getTypeOfSampleId());
            if (!typeOfSamples.contains(typeOfSample)) {
                typeOfSamples.add(typeOfSample);
            }
        }
        return typeOfSamples;
    }

    private String getSampleTypeOfSamplesString(Sample sample) {
        if (sample == null) {
            return "";
        }
        Set<TypeOfSample> sampleTypeOfSamples = getSampleTypeOfSamples(sample);
        StringBuilder str = new StringBuilder(",");
        for (TypeOfSample typeOfSample : sampleTypeOfSamples) {
            str.append(typeOfSample.getId()).append(",");
        }
        return str.toString();
    }

    private List<SampleQaEvent> getSampleQaEvents(Sample sample) {
        return sampleQaEventService.getSampleQaEventsBySample(sample);
    }

    private void setProjectList(NonConformityForm form)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        List<Project> projects = projectService.getAll();
        form.setProjects(projects);
    }

    private Sample getSampleForLabNumber(String labNumber) throws LIMSInvalidConfigurationException {
        return sampleService.getSampleByAccessionNumber(labNumber);
    }

    private List<ObservationHistory> getObservationHistory(Sample sample, Patient patient) {
        return observationHistoryService.getAll(patient, sample);
    }

    private Project getProjectForSample(Sample sample) {
        SampleProject sampleProject = sampleProjectService.getSampleProjectBySampleId(sample.getId());

        return sampleProject == null ? null : sampleProject.getProject();
    }

    private ObservationHistory getRefererObservation(List<ObservationHistory> observationHistoryList) {
        for (ObservationHistory observation : observationHistoryList) {
            if (observation.getObservationHistoryTypeId()
                    .equals(TableIdService.getInstance().DOCTOR_OBSERVATION_TYPE_ID)) {
                return observation;
            }
        }

        return null;
    }

    private ObservationHistory getServiceObservation(List<ObservationHistory> observationHistoryList) {
        for (ObservationHistory observation : observationHistoryList) {
            if (observation.getObservationHistoryTypeId()
                    .equals(TableIdService.getInstance().SERVICE_OBSERVATION_TYPE_ID)) {
                return observation;
            }
        }

        return null;
    }

    private void sortSections(List<TestSection> list) {
        Collections.sort(list, new Comparator<TestSection>() {
            @Override
            public int compare(TestSection o1, TestSection o2) {
                return o1.getSortOrderInt() - o2.getSortOrderInt();
            }
        });
    }

    private List<TestSection> createSectionList() {

        List<TestSection> sections = testSectionService.getAllActiveTestSections();
        if (ConfigurationProperties.getInstance().isPropertyValueEqual(Property.NONCONFORMITY_RECEPTION_AS_UNIT,
                "true")) {
            TestSection extra = new TestSection();
            extra.setTestSectionName("Reception");
            extra.setSortOrder("0");
            extra.setNameKey("testSection.Reception");
            sections.add(extra);
        }

        if (ConfigurationProperties.getInstance().isPropertyValueEqual(Property.NONCONFORMITY_SAMPLE_COLLECTION_AS_UNIT,
                "true")) {
            TestSection extra = new TestSection();
            extra.setTestSectionName("Sample Collection");
            extra.setSortOrder("1");
            extra.setNameKey("testSection.SampleCollection");
            sections.add(extra);
        }

        sortSections(sections);
        return sections;
    }

    @RequestMapping(value = "/NonConformity", method = RequestMethod.POST)
    public ModelAndView showNonConformityUpdate(HttpServletRequest request,
            @ModelAttribute("form") @Validated(NonConformityForm.NonConformity.class) NonConformityForm form,
            BindingResult result, RedirectAttributes redirectAttributes) {
        formValidator.validate(form, result);
        if (result.hasErrors()) {
            saveErrors(result);
            return findForward(FWD_FAIL_INSERT, form);
        }

        NonConformityUpdateData data = new NonConformityUpdateData(form, getSysUserId(request));
        // NonConformityUpdateWorker worker = new NonConformityUpdateWorker(data);
        INonConformityUpdateWorker worker = SpringContext.getBean(INonConformityUpdateWorker.class);
        worker.setWebData(data);
        try {
            worker.update();
        } catch (LIMSRuntimeException e) {
            saveErrors(worker.getErrors());
            return findForward(FWD_FAIL_INSERT, form);
        }
        redirectAttributes.addFlashAttribute(FWD_SUCCESS, true);

        return findForward(FWD_SUCCESS_INSERT, form);
    }

    @Override
    protected String getPageSubtitleKey() {
        return "qaevent.add.title";
    }

    @Override
    protected String getPageTitleKey() {
        return "qaevent.add.title";
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "nonConformityDefiniton";
        } else if (FWD_FAIL.equals(forward)) {
            return "nonConformityDefiniton";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/NonConformity";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "nonConformityDefiniton";
        } else {
            return "PageNotFound";
        }
    }
}
