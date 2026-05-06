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
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.hibernate.StaleObjectStateException;
import org.hl7.fhir.r4.model.Enumerations.ResourceType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.barcode.form.LabelsSectionForm;
import org.openelisglobal.barcode.form.PostSavePrintDialogForm;
import org.openelisglobal.barcode.service.BarcodeWorkflowPrintService;
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
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.controller.BaseSampleEntryController;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.sample.service.PatientManagementUpdate;
import org.openelisglobal.sample.service.SamplePatientEntryService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.validator.SamplePatientEntryFormValidator;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sample.valueholder.SampleAdditionalField;
import org.openelisglobal.sample.valueholder.SampleAdditionalField.AdditionalFieldName;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
            .getLogger(SamplePatientEntryRestController.class);

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
            "sampleOrderItems.consentGiven", "sampleOrderItems.consentFormReference",
            "sampleOrderItems.consentRecordedAt", "sampleOrderItems.consentRecordedBy", "sampleOrderItems.priority",
            //
            "currentDate", "sampleOrderItems.newRequesterName", "sampleOrderItems.externalOrderNumber",
            // referral
            "referralItems*.additionalTestsXMLWad", "referralItems*.referralResultId", "referralItems*.referralId",
            "referralItems*.referredResultType", "referralItems*.modified", "referralItems*.inLabResultId",
            "referralItems*.referralReasonId", "referralItems*.referrer", "referralItems*.referredInstituteId",
            "referralItems*.referredSendDate", "referralItems*.referredTestId", "referralItems*.referredReportDate",
            "referralItems*.note", "useReferral", "sampleOrderItems.additionalQuestions", "sampleOrderItems.programId",
            "orderEntryOnly" };

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
    private PatientService patientService;

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
    private BarcodeWorkflowPrintService barcodeWorkflowPrintService;

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

    /**
     * Save a sample + patient order.
     *
     * <p>
     * OGC-584: This method historically returned HTTP 200 with the form body
     * regardless of success/failure (Struts 1 form-post pattern — validation errors
     * were stashed in a {@code BindingResult} and rendered inline by the
     * server-side page). In a JSON/AJAX context that's silent-failure: callers
     * can't distinguish "saved" from "dropped on the floor with errors in flash
     * scope." Converted to {@link ResponseEntity} so status codes are meaningful:
     * <ul>
     * <li>{@code 400 Bad Request} — validation failed (formValidator or
     * {@code updateData.validateSample})</li>
     * <li>{@code 500 Internal Server Error} — persistence exception caught from
     * {@code samplePatientService.persistData()}, or (belt-and- suspenders) the
     * response claims success but no row is found in {@code clinlims.sample}</li>
     * <li>{@code 200 OK} — verified success, row confirmed in DB</li>
     * </ul>
     * The response body is unchanged in every case — still the full form echo back
     * — so existing consumers (OrderContext.js, any integration that reads form
     * fields) keep working unchanged. Only the status code is new.
     */
    @PostMapping(value = "SamplePatientEntry", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> samplePatientEntrySave(HttpServletRequest request,
            @Validated(SamplePatientEntryForm.SamplePatientEntry.class) @RequestBody SamplePatientEntryForm form,
            BindingResult result, RedirectAttributes redirectAttributes)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        // Extract sampleOrder and workflowType early so we can check for environmental
        // workflow
        SampleOrderItem sampleOrder = form.getSampleOrderItems();
        String workflowType = sampleOrder != null ? sampleOrder.getEnvironmentalFieldAsString("workflowType") : null;

        formValidator.validate(form, result);

        // OGC-356: For environmental workflow, only check for non-patient validation
        // errors
        // Environmental samples don't require patient data (gender, nationalId, etc.)
        if (result.hasErrors()) {
            boolean hasNonPatientErrors = true;
            if ("environmental".equals(workflowType)) {
                List<org.springframework.validation.FieldError> nonPatientErrors = result.getFieldErrors().stream()
                        .filter(error -> !error.getField().startsWith("patientProperties."))
                        .collect(Collectors.toList());
                hasNonPatientErrors = !nonPatientErrors.isEmpty();
            }

            if (hasNonPatientErrors) {
                saveErrors(result);
                logger.warn("SamplePatientEntry 400 (formValidator): {}", result.getAllErrors());
                return ResponseEntity.badRequest().body(buildErrorBody(result, "Validation failed"));
            }
        }
        SamplePatientUpdateData updateData = new SamplePatientUpdateData(getSysUserId(request));

        PatientManagementInfo patientInfo = form.getPatientProperties();

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

        if (sampleOrder.getIsEQASample()) {
            Patient existingEqaPatient = patientService.getPatientByNationalId("NULL");
            if (existingEqaPatient != null) {
                patientInfo.setPatientPK(existingEqaPatient.getId());
                patientInfo.setPatientUpdateStatus(PatientUpdateStatus.NO_ACTION);
            }
        }

        testAndInitializePatientForSaving(request, patientInfo, patientUpdate, updateData);

        // OGC-356: For environmental workflow, don't save patient data
        if ("environmental".equals(workflowType)) {
            updateData.setSavePatient(false);
            updateData.setPatientErrors(new BaseErrors());
        }

        updateData.setAccessionNumber(sampleOrder.getLabNo());
        updateData.setReferringId(sampleOrder.getExternalOrderNumber());
        updateData.setPriority(sampleOrder.getPriority());
        updateData.initProvider(sampleOrder);

        // initSampleData MUST be called before initProgramQuestions so that the sample
        // object is loaded (for updates) before we try to load the existing
        // ProgramSample
        updateData.initSampleData(form.getSampleXML(), receivedDateForDisplay, trackPayments, sampleOrder);

        // Now that sample is loaded, we can initialize program questions (which needs
        // sample.id for updates)
        if (!GenericValidator.isBlankOrNull(sampleOrder.getProgramId())) {
            updateData.initProgramQuestions(sampleOrder.getProgramId(), sampleOrder.getAdditionalQuestions());
        }

        updateData.setPatientEmailNotificationTestIds(form.getPatientEmailNotificationTestIds());
        updateData.setPatientSMSNotificationTestIds(form.getPatientSMSNotificationTestIds());
        updateData.setProviderEmailNotificationTestIds(form.getProviderEmailNotificationTestIds());
        updateData.setProviderSMSNotificationTestIds(form.getProviderSMSNotificationTestIds());
        updateData.setCustomNotificationLogic(form.getCustomNotificationLogic());
        if (sampleOrder.getIsEQASample()) {
            updateData.setEqaSample(true);
            updateData.setEqaProgramId(sampleOrder.getEqaProgramId());
            updateData.setEqaProviderSampleId(sampleOrder.getEqaProviderSampleId());
            updateData.setEqaDeadline(sampleOrder.getEqaDeadline());
            updateData.setEqaPriority(sampleOrder.getEqaPriority());
        }
        if (Boolean.valueOf(ConfigurationProperties.getInstance().getPropertyValue(Property.CONTACT_TRACING))) {
            setContactTracingInfo(updateData, sampleOrder);
        }

        // For decoupled workflow (orderEntryOnly=true), samples are not required
        // They will be added in a later step (Collect Sample)
        boolean requireSampleItems = !form.isOrderEntryOnly();

        updateData.validateSample(result, requireSampleItems);

        // OGC-356: For environmental workflow, ignore patient-related validation errors
        // Environmental samples don't require patient data (gender, nationalId, etc.)
        boolean hasNonPatientErrors = result.hasErrors();
        if (hasNonPatientErrors && "environmental".equals(workflowType)) {
            // Check if all errors are patient-related
            List<org.springframework.validation.FieldError> nonPatientErrors = result.getFieldErrors().stream()
                    .filter(error -> !error.getField().startsWith("patientProperties.")).collect(Collectors.toList());
            hasNonPatientErrors = !nonPatientErrors.isEmpty();
        }

        if (hasNonPatientErrors) {
            saveErrors(result);
            logger.warn("SamplePatientEntry 400 (validateSample): {}", result.getAllErrors());
            return ResponseEntity.badRequest().body(buildErrorBody(result, "Validation failed"));
        }

        // OGC-584: track persistence failure so we can return a proper HTTP
        // status after the catch blocks. `result.hasErrors()` alone isn't
        // reliable because the environmental-workflow path above intentionally
        // skips patient-field errors while leaving them in the BindingResult.
        boolean persistFailed = false;
        // Captures the actual failure message (e.g. storage-position-occupied)
        // when persistData rolls back, so we can return it instead of a
        // generic "Failed to save order" / "Transaction silently rolled
        // back...". Spring may wrap the original exception, so we walk the
        // cause chain.
        String persistErrorMessage = null;

        try {
            // Note: persistData now publishes SamplePatientUpdateDataCreatedEvent
            // internally (inside its @Transactional boundary) so listener
            // failures roll back the whole save. Don't republish here.
            samplePatientService.persistData(updateData, patientUpdate, patientInfo, form, request);
            populateWorkflowPrintModels(form, sampleOrder.getLabNo());

            if (sampleOrder.getPriority() != null && sampleOrder.getPriority().equals(OrderPriority.STAT)) {
                List<String> systemUserIds = userRoleService.getUserIdsForRole(Constants.ROLE_RESULTS);
                Sample statSample = sampleService.getSampleByAccessionNumber(sampleOrder.getLabNo());
                List<Analysis> analyses = statSample != null ? sampleService.getAnalysis(statSample) : null;
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
            LogEvent.logError("persistData failed with LIMSRuntimeException", e);
            if (e.getCause() instanceof StaleObjectStateException) {
                result.reject("errors.OptimisticLockException", "errors.OptimisticLockException");
            } else {
                logger.error("Order save failed for labNo={}", sampleOrder.getLabNo(), e);
                result.reject("errors.UpdateException", "errors.UpdateException");
            }
            logger.error("SamplePatientEntry errors: {}", result.toString());
            persistErrorMessage = rootCauseMessage(e);
            persistFailed = true;
        } catch (Exception e) {
            logger.error("Unexpected error saving order for labNo={}", sampleOrder.getLabNo(), e);
            persistErrorMessage = rootCauseMessage(e);
            result.reject("errors.UpdateException", "errors.UpdateException");

            saveErrors(result);

            setupForm(form, request, "");
            request.setAttribute(ALLOW_EDITS_KEY, "false");
            persistFailed = true;
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

        // OGC-584: return a non-2xx status if persistence threw an exception that
        // the catch blocks above swallowed (previously this method still returned
        // HTTP 200 in that case — the silent-200-no-persist bug).
        // Prefer the captured root-cause message (e.g. "Position Box A is
        // already occupied...") over the generic BindingResult fallback.
        if (persistFailed) {
            if (StringUtils.isNotBlank(persistErrorMessage)
                    && !persistErrorMessage.startsWith("Transaction silently rolled back")) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", persistErrorMessage));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorBody(result, "Failed to save order"));
        }

        // Belt-and-suspenders: verify the row actually made it to the DB. Guards
        // against any future silent-failure path that forgets to set
        // persistFailed. @Transactional on persistData guarantees all-or-nothing,
        // so if the accession isn't found we know the write rolled back.
        String labNoForVerify = sampleOrder != null ? sampleOrder.getLabNo() : null;
        Sample persistedSample = !GenericValidator.isBlankOrNull(labNoForVerify)
                ? sampleService.getSampleByAccessionNumber(labNoForVerify)
                : null;
        if (persistedSample == null || persistedSample.getId() == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Order save did not persist (verification check failed). See server logs."));
        }

        return ResponseEntity.ok(form);
    }

    void populateWorkflowPrintModels(SamplePatientEntryForm form, String accessionNumber) {
        ParsedLabelQuantities labelQuantities = extractLabelQuantities(form.getSampleXML());
        LabelsSectionForm labelsSection = barcodeWorkflowPrintService.buildLabelsSection(labelQuantities.orderQuantity,
                labelQuantities.specimenQuantities);
        PostSavePrintDialogForm postSavePrintDialog = barcodeWorkflowPrintService
                .buildPostSavePrintDialog(accessionNumber, labelsSection);
        form.setLabelsSection(labelsSection);
        form.setPostSavePrintDialog(postSavePrintDialog);
    }

    ParsedLabelQuantities extractLabelQuantities(String sampleXml) {
        ParsedLabelQuantities quantities = new ParsedLabelQuantities();
        if (GenericValidator.isBlankOrNull(sampleXml)) {
            return quantities;
        }
        try {
            Document sampleDocument = DocumentHelper.parseText(sampleXml);
            List<org.dom4j.Element> sampleElements = sampleDocument.getRootElement().elements("sample");
            if (sampleElements != null && !sampleElements.isEmpty()) {
                org.dom4j.Element firstSample = sampleElements.get(0);
                quantities.orderQuantity = parseLabelQuantity(firstSample.attributeValue("numOrderLabels"));
                quantities.specimenQuantities.clear();
                for (org.dom4j.Element sampleElement : sampleElements) {
                    quantities.specimenQuantities
                            .add(parseLabelQuantity(sampleElement.attributeValue("numSpecimenLabels")));
                }
            }
        } catch (DocumentException e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "extractLabelQuantities",
                    "Unable to parse sample XML for label quantities");
        }
        return quantities;
    }

    static class ParsedLabelQuantities {
        int orderQuantity = 1;
        List<Integer> specimenQuantities = new java.util.ArrayList<>(List.of(1));
    }

    private int parseLabelQuantity(String value) {
        if (GenericValidator.isBlankOrNull(value)) {
            return 1;
        }
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : 1;
        } catch (NumberFormatException e) {
            return 1;
        }
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

    /**
     * Walk the exception cause chain to find the deepest non-blank message. When a
     * service throws inside a transactional event listener, Spring may wrap the
     * original LIMSRuntimeException in an UnexpectedRollbackException whose message
     * is the unhelpful "Transaction silently rolled back...". The actual message
     * ("Position Box A is already occupied...") sits on the root cause.
     */
    private static String rootCauseMessage(Throwable t) {
        Throwable cur = t;
        Throwable best = t;
        while (cur != null) {
            if (StringUtils.isNotBlank(cur.getMessage())) {
                best = cur;
            }
            if (cur.getCause() == null || cur.getCause() == cur) {
                break;
            }
            cur = cur.getCause();
        }
        return best != null ? best.getMessage() : null;
    }

    /**
     * Build a structured error response body from a failed BindingResult so the
     * frontend can surface a meaningful message. Returns a Map with a top-level
     * human-readable `error` plus the per-field list — kept separate from the form
     * (success path) to avoid mixing concerns.
     */
    private static Map<String, Object> buildErrorBody(BindingResult result, String fallbackMessage) {
        org.springframework.validation.FieldError firstFe = result.getFieldError();
        String message;
        if (firstFe != null) {
            message = firstFe.getField() + ": "
                    + (firstFe.getDefaultMessage() != null ? firstFe.getDefaultMessage() : "invalid value");
        } else if (!result.getAllErrors().isEmpty() && result.getAllErrors().get(0).getDefaultMessage() != null) {
            message = result.getAllErrors().get(0).getDefaultMessage();
        } else {
            message = fallbackMessage;
        }
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("error", message);
        body.put("fieldErrors", result.getFieldErrors().stream().map(fe -> {
            Map<String, String> entry = new java.util.HashMap<>();
            entry.put("field", fe.getField());
            entry.put("defaultMessage", fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "");
            return entry;
        }).collect(Collectors.toList()));
        return body;
    }
}