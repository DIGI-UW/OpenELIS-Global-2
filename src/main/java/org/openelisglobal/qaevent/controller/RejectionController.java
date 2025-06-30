package org.openelisglobal.qaevent.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.ObjectUtils;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.exception.LIMSInvalidConfigurationException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.provider.validation.IAccessionNumberGenerator;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.project.service.ProjectService;
import org.openelisglobal.project.valueholder.Project;
import org.openelisglobal.qaevent.form.RejectionForm;
import org.openelisglobal.qaevent.service.NCEventService;
import org.openelisglobal.qaevent.service.NceSpecimenService;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.qaevent.valueholder.NceSpecimen;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.util.AccessionNumberUtil;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampleorganization.service.SampleOrganizationService;
import org.openelisglobal.sampleorganization.valueholder.SampleOrganization;
import org.openelisglobal.sampleproject.service.SampleProjectService;
import org.openelisglobal.sampleproject.valueholder.SampleProject;
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
public class RejectionController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "patientId", "receptionDateForDisplay",
            "collectionDateForDisplay", "labNo", "projectId", "subjectNo", "doctor", "typeOfSampleId", "sectionId",
            "comment", "biologist", "samplerName", "organizationId" };

    @Autowired
    private SampleService sampleService;
    @Autowired
    private SampleItemService sampleItemService;
    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private SampleOrganizationService sampleOrganizationService;
    @Autowired
    private TestSectionService testSectionService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private SampleProjectService sampleProjectService;
    @Autowired
    private NCEventService ncEventService;

    @Autowired
    private NceSpecimenService nceSpecimenService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = "/SampleRejection", method = RequestMethod.GET)
    public ModelAndView showRejectionForm(HttpServletRequest request, @Validated RejectionForm form,
            BindingResult result) throws LIMSInvalidConfigurationException, IllegalAccessException,
            InvocationTargetException, NoSuchMethodException {
        if (result.hasErrors()) {
            saveErrors(result);
            form = new RejectionForm();
            return findForward(FWD_FAIL, form);
        }
        form = new RejectionForm();
        setupForm(form);
        addFlashMsgsToRequest(request);
        request.getSession().setAttribute(SAVE_DISABLED, TRUE);

        return findForward(FWD_SUCCESS, form);
    }

    private void setupForm(RejectionForm form) throws LIMSInvalidConfigurationException, IllegalAccessException,
            InvocationTargetException, NoSuchMethodException {

        setProjectList(form);
        form.setSections(createSectionList());
        form.setQaEventTypes(DisplayListService.getInstance().getList(ListType.QA_EVENTS));
        form.setTypeOfSamples(DisplayListService.getInstance().getList(ListType.SAMPLE_TYPE_ACTIVE));
        form.setSiteList(DisplayListService.getInstance().getFreshList(ListType.SAMPLE_PATIENT_REFERRING_CLINIC));
    }

    private void setProjectList(RejectionForm form)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        List<Project> projects = projectService.getAll();
        form.setProjects(projects);
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

    @RequestMapping(value = "/SampleRejection", method = RequestMethod.POST)
    public ModelAndView showNonConformityUpdate(HttpServletRequest request,
            @ModelAttribute("form") @Validated RejectionForm form, BindingResult result,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            saveErrors(result);
            return findForward(FWD_FAIL_INSERT, form);
        }

        Sample rejectedSample = new Sample();
        Date today = Calendar.getInstance().getTime();
        try {
            IAccessionNumberGenerator accessionValidator = AccessionNumberUtil.getMainAccessionNumberGenerator();
            if (ObjectUtils.isEmpty(form.getLabNo())) {
                form.setLabNo(accessionValidator.getNextAvailableAccessionNumber("", true));
            }
            rejectedSample.setAccessionNumber(form.getLabNo());
            rejectedSample.setCollectionDate(
                    DateUtil.convertStringDateToTruncatedTimestamp(form.getCollectionDateForDisplay()));
            rejectedSample.setEnteredDate(DateUtil.getNowAsSqlDate());
            rejectedSample.setPriority(OrderPriority.ROUTINE);
            rejectedSample.setDomain(ConfigurationProperties.getInstance().getPropertyValue("domain.human"));
            rejectedSample.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(OrderStatus.Entered));
            rejectedSample.setReceivedTimestamp(
                    DateUtil.convertStringDateToTruncatedTimestamp(form.getReceptionDateForDisplay()));
            rejectedSample.setSysUserId(getSysUserId(request));
            rejectedSample = sampleService.save(rejectedSample);

            TypeOfSample sampleType = typeOfSampleService.get(form.getTypeOfSampleId());

            // sampleProject
            SampleProject sampProject = new SampleProject();
            sampProject.setProjectId(form.getProjectId());
            sampProject.setSample(rejectedSample);
            sampProject = sampleProjectService.save(sampProject);

            // sampleOrg
            Organization organization = ObjectUtils.isNotEmpty(form.getOrganizationId())
                    ? organizationService.get(form.getOrganizationId())
                    : new Organization();
            SampleOrganization sampOrg = new SampleOrganization();
            sampOrg.setOrganization(organization);
            sampOrg.setSample(rejectedSample);
            sampOrg.setSysUserId(getSysUserId(request));
            sampOrg = sampleOrganizationService.save(sampOrg);

            // SampleItem
            SampleItem sampItem = new SampleItem();
            sampItem.setSortOrder(Integer.toString(1));
            sampItem.setCollectionDate(rejectedSample.getCollectionDate());
            sampItem.setSample(rejectedSample);
            sampItem.setTypeOfSample(sampleType);
            sampItem.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(SampleStatus.SampleRejected));
            sampItem.setRejected(true);
            sampItem.setRejectReasonId(form.getQaEventId());
            sampItem = sampleItemService.save(sampItem);

            // NCEvent
            NcEvent event = new NcEvent();
            event.setLabOrderNumber(rejectedSample.getAccessionNumber());
            event.setComments(form.getComment());
            event.setNameOfReporter(form.getBiologist());
            event.setReportDate(DateUtil.convertDateTimeToSqlDate(today));
            event.setSite(organization.getName());
            event.setPrescriberName(form.getDoctor());
            event.setReportingUnitId(Integer.parseInt(form.getSectionId()));
            event.setNceTypeId(Integer.parseInt(form.getQaEventId()));
            event = ncEventService.save(event);

            // NCESpecimen
            NceSpecimen nceSpecimen = new NceSpecimen();
            nceSpecimen.setNceId(Integer.parseInt(event.getId()));
            nceSpecimen.setSampleItemId(Integer.parseInt(sampItem.getId()));
            nceSpecimen.setSysUserId(getSysUserId(request));
            nceSpecimenService.save(nceSpecimen);
            setupForm(form);
            redirectAttributes.addFlashAttribute(FWD_SUCCESS, true);

            return findForward(FWD_SUCCESS_INSERT, form);
        } catch (LIMSRuntimeException | NoSuchMethodException | InvocationTargetException
                | LIMSInvalidConfigurationException | IllegalAccessException e) {
            return findForward(FWD_FAIL_INSERT, form);
        }
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
            return "sampleRejectionDefiniton";
        } else if (FWD_FAIL.equals(forward)) {
            return "sampleRejectionDefiniton";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/SampleRejection";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "sampleRejectionDefiniton";
        } else {
            return "PageNotFound";
        }
    }
}
