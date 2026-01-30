package org.openelisglobal.sample.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Pattern;
import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.StaleObjectStateException;
import org.hl7.fhir.r4.model.Enumerations.ResourceType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.formfields.FormFields;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.provider.validation.AlphanumAccessionValidator;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.services.SampleOrderService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.validator.BaseErrors;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.notifications.dao.NotificationDAO;
import org.openelisglobal.notifications.entity.Notification;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.action.IPatientUpdate;
import org.openelisglobal.patient.action.IPatientUpdate.PatientUpdateStatus;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.action.bean.PatientSearch;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.controller.BaseSampleEntryController;
import org.openelisglobal.sample.event.SamplePatientUpdateDataCreatedEvent;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.sample.service.PatientManagementUpdate;
import org.openelisglobal.sample.service.SamplePatientEntryService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.validator.SamplePatientEntryFormValidator;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.sample.valueholder.SampleAdditionalField;
import org.openelisglobal.sample.valueholder.SampleAdditionalField.AdditionalFieldName;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

@Controller
@RequestMapping(value = "/rest/")
public class SamplePatientEntryRestController extends BaseSampleEntryController {

    @Value("${org.openelisglobal.requester.identifier:}")
    private String requestFhirUuid;

    private static final String[] ALLOWED_FIELDS = new String[] { "rememberSiteAndRequester", "customNotificationLogic",
            "patientEmailNotificationTestIds", "patientSMSNotificationTestIds", "providerEmailNotificationTestIds",
            "providerSMSNotificationTestIds", "patientProperties.currentDate", "patientProperties.patientLastUpdated",
            "patientProperties.personLastUpdated", "patientProperties.patientUpdateStatus",
            "patientProperties.patientPK", "patientProperties.guid", "patientProperties.fhirUuid",
            "patientProperties.STnumber", "patientProperties.subjectNumber", "patientProperties.nationalId",
            "patientProperties.lastName", "patientProperties.firstName", "patientProperties.aka",
            "patientProperties.mothersName", "patientProperties.mothersInitial", "patientProperties.streetAddress",
            "patientProperties.commune", "patientProperties.city", "patientProperties.addressDepartment",
            "patientProperties.addressDepartment", "patientPhone", "patientProperties.primaryPhone",
            "patientProperties.email", "patientProperties.healthRegion", "patientProperties.healthDistrict",
            "patientProperties.birthDateForDisplay", "patientProperties.age", "patientProperties.gender",
            "patientProperties.patientType", "patientProperties.insuranceNumber", "patientProperties.occupation",
            "patientProperties.education", "patientProperties.maritialStatus", "patientProperties.nationality",
            "patientProperties.otherNationality", "patientClinicalProperties.stdOther",
            "patientClinicalProperties.tbDiarrhae", "patientClinicalProperties.stdZona",
            "patientClinicalProperties.tbPrurigol", "patientClinicalProperties.stdKaposi",
            "patientClinicalProperties.tbMenigitis", "patientClinicalProperties.stdCandidiasis",
            "patientClinicalProperties.tbCerebral", "patientClinicalProperties.stdColonCancer",
            "patientClinicalProperties.tbExtraPulmanary", "patientClinicalProperties.arvProphyaxixType",
            "patientClinicalProperties.arvTreatmentReceiving", "patientClinicalProperties.arvTreatmentRemembered",
            "patientClinicalProperties.arvTreatment1", "patientClinicalProperties.arvTreatment2",
            "patientClinicalProperties.arvTreatment3", "patientClinicalProperties.arvTreatment4",
            "patientClinicalProperties.cotrimoxazoleReceiving", "patientClinicalProperties.cotrimoxazoleType",
            "patientClinicalProperties.infectionExtraPulmanary", "patientClinicalProperties.stdInfectionColon",
            "patientClinicalProperties.infectionCerebral", "patientClinicalProperties.stdInfectionCandidiasis",
            "patientClinicalProperties.infectionMeningitis", "patientClinicalProperties.stdInfectionKaposi",
            "patientClinicalProperties.infectionPrurigol", "patientClinicalProperties.stdInfectionZona",
            "patientClinicalProperties.infectionOther", "patientClinicalProperties.infectionUnderTreatment",
            "patientClinicalProperties.weight", "patientClinicalProperties.karnofskyScore",
            //
            "initialSampleConditionList", "sampleXML",
            //
            "sampleOrderItems.newRequesterName", "sampleOrderItems.modified", "sampleOrderItems.sampleId",
            "sampleOrderItems.labNo", "sampleOrderItems.requestDate", "sampleOrderItems.receivedDateForDisplay",
            "sampleOrderItems.receivedTime", "sampleOrderItems.nextVisitDate", "sampleOrderItems.requesterSampleID",
            "sampleOrderItems.referringPatientNumber", "sampleOrderItems.referringSiteId",
            "referringSiteDepartmentName", "sampleOrderItems.referringSiteDepartmentId",
            "sampleOrderItems.referringSiteName", "sampleOrderItems.referringSiteCode", "sampleOrderItems.program",
            "sampleOrderItems.providerPersonId", "sampleOrderItems.providerLastName",
            "sampleOrderItems.providerFirstName", "sampleOrderItems.providerWorkPhone", "sampleOrderItems.providerFax",
            "sampleOrderItems.providerEmail", "sampleOrderItems.facilityAddressStreet",
            "sampleOrderItems.facilityAddressCommune", "sampleOrderItems.facilityPhone", "sampleOrderItems.facilityFax",
            "sampleOrderItems.paymentOptionSelection", "sampleOrderItems.billingReferenceNumber",
            "sampleOrderItems.testLocationCode", "sampleOrderItems.otherLocationCode",
            "sampleOrderItems.contactTracingIndexName", "sampleOrderItems.contactTracingIndexRecordNumber",
            "sampleOrderItems.priority",
            //
            "currentDate", "sampleOrderItems.newRequesterName", "sampleOrderItems.externalOrderNumber",
            // referral
            "referralItems*.additionalTestsXMLWad", "referralItems*.referralResultId", "referralItems*.referralId",
            "referralItems*.referredResultType", "referralItems*.modified", "referralItems*.inLabResultId",
            "referralItems*.referralReasonId", "referralItems*.referrer", "referralItems*.referredInstituteId",
            "referralItems*.referredSendDate", "referralItems*.referredTestId", "referralItems*.referredReportDate",
            "referralItems*.note", "useReferral", "sampleOrderItems.additionalQuestions",
            "sampleOrderItems.programId" };

    @Autowired
    private SamplePatientEntryFormValidator formValidator;

    @Autowired
    private SamplePatientEntryService samplePatientService;

    @Autowired
    private FhirTransformService fhirTransformService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProviderService providerService;

    @Autowired
    private ElectronicOrderService electronicOrderService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private FhirUtil fhirUtil;
    @Autowired
    private NotificationDAO notificationDAO;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private SystemUserService systemUserService;
    @Autowired
    private SampleService sampleService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @GetMapping(value = "SamplePatientEntry", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public SamplePatientEntryForm showSamplePatientEntry(HttpServletRequest request,
            @RequestParam(value = ID, required = false) @Pattern(regexp = "[a-zA-Z0-9 -]*") String externalOrderNumber)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        SamplePatientEntryForm form = new SamplePatientEntryForm();

        request.getSession().setAttribute(SAVE_DISABLED, TRUE);
        setupForm(form, request, externalOrderNumber);
        Map<String, ?> inputFlashMap = RequestContextUtils.getInputFlashMap(request);
        if (inputFlashMap != null) {
            form.getSampleOrderItems().setProviderId((String) inputFlashMap.get("sampleOrderItems.providerId"));
            form.getSampleOrderItems()
                    .setProviderPersonId((String) inputFlashMap.get("sampleOrderItems.providerPersonId"));
            form.getSampleOrderItems().setProviderEmail((String) inputFlashMap.get("sampleOrderItems.providerEmail"));
            form.getSampleOrderItems().setProviderFax((String) inputFlashMap.get("sampleOrderItems.providerfax"));
            form.getSampleOrderItems()
                    .setProviderFirstName((String) inputFlashMap.get("sampleOrderItems.providerFirstName"));
            form.getSampleOrderItems()
                    .setProviderLastName((String) inputFlashMap.get("sampleOrderItems.providerLastName"));
            form.getSampleOrderItems()
                    .setProviderWorkPhone((String) inputFlashMap.get("sampleOrderItems.providerWorkPhone"));
            form.getSampleOrderItems()
                    .setReferringSiteId((String) inputFlashMap.get("sampleOrderItems.referringSiteId"));
            form.getSampleOrderItems()
                    .setReferringSiteCode((String) inputFlashMap.get("sampleOrderItems.referringSiteCode"));
            form.getSampleOrderItems()
                    .setReferringSiteName((String) inputFlashMap.get("sampleOrderItems.referringSiteName"));
            form.getSampleOrderItems().setReferringSiteDepartmentId(
                    (String) inputFlashMap.get("sampleOrderItems.referringSiteDepartmentId"));
            form.getSampleOrderItems().setReferringSiteDepartmentName(
                    (String) inputFlashMap.get("sampleOrderItems.referringSiteDepartmentName"));
        }
        addFlashMsgsToRequest(request);
        return form;
    }

    private void setupReferralOption(SamplePatientEntryForm form) {
        form.setReferralOrganizations(DisplayListService.getInstance().getList(ListType.REFERRAL_ORGANIZATIONS));
        form.setReferralReasons(DisplayListService.getInstance().getList(ListType.REFERRAL_REASONS));
    }

    @PostMapping(value = "SamplePatientEntry", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public SamplePatientEntryForm samplePatientEntrySave(HttpServletRequest request,
            @Validated(SamplePatientEntryForm.SamplePatientEntry.class) @RequestBody SamplePatientEntryForm form,
            BindingResult result, RedirectAttributes redirectAttributes)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        formValidator.validate(form, result);
        if (result.hasErrors()) {
            saveErrors(result);
            setupForm(form, request, "");
        }
        SamplePatientUpdateData updateData = new SamplePatientUpdateData(getSysUserId(request));

        PatientManagementInfo patientInfo = form.getPatientProperties();
        SampleOrderItem sampleOrder = form.getSampleOrderItems();

        boolean trackPayments = ConfigurationProperties.getInstance()
                .isPropertyValueEqual(Property.TRACK_PATIENT_PAYMENT, "true");

        String receivedDateForDisplay = sampleOrder.getReceivedDateForDisplay();

        if (!GenericValidator.isBlankOrNull(sampleOrder.getReceivedTime())) {
            receivedDateForDisplay += " " + sampleOrder.getReceivedTime();
        } else {
            receivedDateForDisplay += " 00:00";
        }

        updateData.setCollectionDateFromRecieveDateIfNeeded(receivedDateForDisplay);
        updateData.initializeRequester(sampleOrder);

        PatientManagementUpdate patientUpdate = SpringContext.getBean(PatientManagementUpdate.class);
        patientUpdate.setSysUserIdFromRequest(request);
        testAndInitializePatientForSaving(request, patientInfo, patientUpdate, updateData);

        updateData.setAccessionNumber(sampleOrder.getLabNo());
        updateData.setReferringId(sampleOrder.getExternalOrderNumber());
        updateData.setPriority(sampleOrder.getPriority());
        updateData.initProvider(sampleOrder);
        if (!GenericValidator.isBlankOrNull(sampleOrder.getProgramId())) {
            updateData.initProgramQuestions(sampleOrder.getProgramId(), sampleOrder.getAdditionalQuestions());
        }
        updateData.initSampleData(form.getSampleXML(), receivedDateForDisplay, trackPayments, sampleOrder);
        updateData.setPatientEmailNotificationTestIds(form.getPatientEmailNotificationTestIds());
        updateData.setPatientSMSNotificationTestIds(form.getPatientSMSNotificationTestIds());
        updateData.setProviderEmailNotificationTestIds(form.getProviderEmailNotificationTestIds());
        updateData.setProviderSMSNotificationTestIds(form.getProviderSMSNotificationTestIds());
        updateData.setCustomNotificationLogic(form.getCustomNotificationLogic());
        if (Boolean.valueOf(ConfigurationProperties.getInstance().getPropertyValue(Property.CONTACT_TRACING))) {
            setContactTracingInfo(updateData, sampleOrder);
        }
        updateData.validateSample(result);
        if (result.hasErrors()) {
            saveErrors(result);
            setupForm(form, request, "");
        }

        try {
            samplePatientService.persistData(updateData, patientUpdate, patientInfo, form, request);
            try {
                SamplePatientUpdateDataCreatedEvent event = new SamplePatientUpdateDataCreatedEvent(this, updateData,
                        patientInfo, form);
                eventPublisher.publishEvent(event);
            } catch (Exception e) {
                LogEvent.logError(e);
            }

            if (sampleOrder.getPriority().equals(OrderPriority.STAT)) {
                List<String> systemUserIds = userRoleService.getUserIdsForRole(Constants.ROLE_RESULTS);
                List<Analysis> analyses = sampleService
                        .getAnalysis(sampleService.getSampleByAccessionNumber(sampleOrder.getLabNo()));
                String message = MessageUtil.getMessage("notification.order.stat",
                        AlphanumAccessionValidator.convertAlphaNumLabNumForDisplay(sampleOrder.getLabNo()));
                StringBuffer sb = new StringBuffer(message);
                for (String userId : systemUserIds) {
                    List<Analysis> userAnalyses = userService.filterAnalysesByLabUnitRoles(userId, analyses,
                            Constants.ROLE_RESULTS);
                    if (userAnalyses != null && !userAnalyses.isEmpty()) {
                        List<String> tests = userAnalyses.stream().map(a -> a.getTest().getLocalizedName())
                                .collect(Collectors.toList());
                        String testString = String.join(", ", tests);
                        sb.append(testString);
                        try {
                            Notification notification = new Notification();
                            notification.setMessage(sb.toString());
                            notification.setUser(systemUserService.getUserById(userId));
                            notification.setCreatedDate(OffsetDateTime.now());
                            notification.setReadAt(null);
                            notificationDAO.save(notification);
                        } catch (Exception e) {
                        }
                    }
                }
            }

            // String fhir_json = fhirTransformService.CreateFhirFromOESample(updateData,
            // patientUpdate, patientInfo, form, request);
        } catch (LIMSRuntimeException e) {
            // ActionError error;
            if (e.getCause() instanceof StaleObjectStateException) {
                // error = new ActionError("errors.OptimisticLockException", null, null);
                result.reject("errors.OptimisticLockException", "errors.OptimisticLockException");
            } else {
                LogEvent.logDebug(e);
                // error = new ActionError("errors.UpdateException", null, null);
                result.reject("errors.UpdateException", "errors.UpdateException");
            }
            LogEvent.logInfo(this.getClass().getSimpleName(), "samplePatientEntrySave", result.toString());

            // errors.add(ActionMessages.GLOBAL_MESSAGE, error);
            saveErrors(result);// TODO theses errors are not communicated to the frontend return an error code
                               // if svae is not successful

            setupForm(form, request, "");
            request.setAttribute(ALLOW_EDITS_KEY, "false");
        }
        redirectAttributes.addFlashAttribute(FWD_SUCCESS, true);
        if (form.getRememberSiteAndRequester()) {
            redirectAttributes.addFlashAttribute("sampleOrderItems.providerId",
                    form.getSampleOrderItems().getProviderId());
            redirectAttributes.addFlashAttribute("sampleOrderItems.providerPersonId",
                    form.getSampleOrderItems().getProviderPersonId());
            redirectAttributes.addFlashAttribute("sampleOrderItems.providerEmail",
                    form.getSampleOrderItems().getProviderEmail());
            redirectAttributes.addFlashAttribute("sampleOrderItems.providerfax",
                    form.getSampleOrderItems().getProviderFax());
            redirectAttributes.addFlashAttribute("sampleOrderItems.providerFirstName",
                    form.getSampleOrderItems().getProviderFirstName());
            redirectAttributes.addFlashAttribute("sampleOrderItems.providerLastName",
                    form.getSampleOrderItems().getProviderLastName());
            redirectAttributes.addFlashAttribute("sampleOrderItems.providerWorkPhone",
                    form.getSampleOrderItems().getProviderWorkPhone());

            redirectAttributes.addFlashAttribute("sampleOrderItems.referringSiteId",
                    form.getSampleOrderItems().getReferringSiteId());
            redirectAttributes.addFlashAttribute("sampleOrderItems.referringSiteCode",
                    form.getSampleOrderItems().getReferringSiteCode());
            redirectAttributes.addFlashAttribute("sampleOrderItems.referringSiteName",
                    form.getSampleOrderItems().getReferringSiteName());

            redirectAttributes.addFlashAttribute("sampleOrderItems.referringSiteDepartmentId",
                    form.getSampleOrderItems().getReferringSiteDepartmentId());
            redirectAttributes.addFlashAttribute("sampleOrderItems.referringSiteDepartmentName",
                    form.getSampleOrderItems().getReferringSiteDepartmentName());
        }

        return (form);
    }

    private void setupForm(SamplePatientEntryForm form, HttpServletRequest request, String externalOrderNumber)
            throws LIMSRuntimeException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        SampleOrderService sampleOrderService = new SampleOrderService();
        form.setSampleOrderItems(sampleOrderService.getSampleOrderItem());
        if (requestFhirUuid != null
                && requestFhirUuid.toUpperCase().startsWith(ResourceType.PRACTITIONER.toString().toUpperCase())) {
            Reference providerReference = new Reference(requestFhirUuid);
            Provider provider = providerService
                    .getProviderByFhirId(UUID.fromString(providerReference.getReferenceElement().getIdPart()));
            if (provider != null) {
                form.getSampleOrderItems().setProviderPersonId(provider.getPerson().getId());
            }
        }
        form.getSampleOrderItems().setExternalOrderNumber(externalOrderNumber);
        if (StringUtils.isNotBlank(externalOrderNumber)) {
            ElectronicOrder eOrder = electronicOrderService.getElectronicOrdersByExternalId(externalOrderNumber).get(0);
            if (eOrder != null) {
                form.getSampleOrderItems().setPriority(eOrder.getPriority());
                Task task = fhirUtil.getFhirParser().parseResource(Task.class, eOrder.getData());
                if (!task.getLocation().isEmpty()) {
                    Organization organization = organizationService
                            .getOrganizationByFhirId(task.getLocation().getReferenceElement().getIdPart());
                    if (organization != null) {
                        form.getSampleOrderItems().setReferringSiteName(organization.getOrganizationName());
                        form.getSampleOrderItems().setReferringSiteId(organization.getId());
                    }
                }
                if (!task.getOwner().isEmpty()) {
                    if (StringUtils.isBlank(form.getSampleOrderItems().getProviderPersonId())) {
                        Reference providerReference = task.getOwner();
                        Provider provider = providerService.getProviderByFhirId(
                                UUID.fromString(providerReference.getReferenceElement().getIdPart()));
                        if (provider != null) {
                            form.getSampleOrderItems().setProviderPersonId(provider.getPerson().getId());
                        }
                    }
                }
            }
        }
        form.setPatientProperties(new PatientManagementInfo());
        form.setPatientSearch(new PatientSearch());
        form.setSampleTypes(userService.getUserSampleTypes(getSysUserId(request), Constants.ROLE_RECEPTION));
        form.setTestSectionList(DisplayListService.getInstance().getList(ListType.TEST_SECTION_ACTIVE));
        form.setCurrentDate(DateUtil.getCurrentDateAsText());
        form.setRejectReasonList(DisplayListService.getInstance().getList(ListType.REJECTION_REASONS));

        setupReferralOption(form);
        // for (Object program : form.getSampleOrderItems().getProgramList()) {
        // LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown",
        // ((IdValuePair)
        // program).getValue());
        // }

        addProjectList(form);
        addBillingLabel();

        if (FormFields.getInstance().useField(FormFields.Field.InitialSampleCondition)) {
            form.setInitialSampleConditionList(
                    DisplayListService.getInstance().getList(ListType.INITIAL_SAMPLE_CONDITION));
        }
        if (FormFields.getInstance().useField(FormFields.Field.SampleNature)) {
            form.setSampleNatureList(DisplayListService.getInstance().getList(ListType.SAMPLE_NATURE));
        }
    }

    private void setContactTracingInfo(SamplePatientUpdateData updateData, SampleOrderItem sampleOrder) {
        SampleAdditionalField field;
        if (!GenericValidator.isBlankOrNull(sampleOrder.getContactTracingIndexName())) {
            field = new SampleAdditionalField();
            field.setFieldName(AdditionalFieldName.CONTACT_TRACING_INDEX_NAME);
            field.setFieldValue(sampleOrder.getContactTracingIndexName());
            updateData.addSampleField(field);
        }
        if (!GenericValidator.isBlankOrNull(sampleOrder.getContactTracingIndexRecordNumber())) {
            field = new SampleAdditionalField();
            field.setFieldName(AdditionalFieldName.CONTACT_TRACING_INDEX_RECORD_NUMBER);
            field.setFieldValue(sampleOrder.getContactTracingIndexRecordNumber());
            updateData.addSampleField(field);
        }
    }

    private void testAndInitializePatientForSaving(HttpServletRequest request, PatientManagementInfo patientInfo,
            IPatientUpdate patientUpdate, SamplePatientUpdateData updateData)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        patientUpdate.setPatientUpdateStatus(patientInfo);
        updateData.setSavePatient(patientUpdate.getPatientUpdateStatus() != PatientUpdateStatus.NO_ACTION);

        if (updateData.isSavePatient()) {
            updateData.setPatientErrors(patientUpdate.preparePatientData(request, patientInfo));
        } else {
            updateData.setPatientErrors(new BaseErrors());
        }
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "samplePatientEntryDefinition";
        } else if (FWD_FAIL.equals(forward)) {
            return "homePageDefinition";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/SamplePatientEntry";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "samplePatientEntryDefinition";
        } else {
            return "PageNotFound";
        }
    }
}
