package org.openelisglobal.sample.service;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.StaleObjectStateException;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.provider.validation.AlphanumAccessionValidator;
import org.openelisglobal.common.validator.BaseErrors;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.notifications.dao.NotificationDAO;
import org.openelisglobal.notifications.entity.Notification;
import org.openelisglobal.patient.action.IPatientUpdate.PatientUpdateStatus;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.event.SamplePatientUpdateDataCreatedEvent;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

@Service
public class SamplePatientEntryOrderPlacementService {

    @Autowired
    private SamplePatientEntryService samplePatientService;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private UserService userService;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private NotificationDAO notificationDAO;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Transactional
    public void placeOrder(HttpServletRequest request, SamplePatientEntryForm form, BindingResult result)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        if (result.hasErrors()) {
            return;
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

        patientUpdate.setPatientUpdateStatus(patientInfo);
        updateData.setSavePatient(patientUpdate.getPatientUpdateStatus() != PatientUpdateStatus.NO_ACTION);

        if (updateData.isSavePatient()) {
            updateData.setPatientErrors(patientUpdate.preparePatientData(request, patientInfo));
        } else {
            updateData.setPatientErrors(new BaseErrors());
        }

        updateData.setAccessionNumber(sampleOrder.getLabNo());
        LogEvent.logInfo(this.getClass().getSimpleName(), "placeOrder",
                "labNo=" + sampleOrder.getLabNo() + ", externalOrderNumber=" + sampleOrder.getExternalOrderNumber()
                        + ", requesterSampleID=" + sampleOrder.getRequesterSampleID());
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
            if (!GenericValidator.isBlankOrNull(sampleOrder.getContactTracingIndexName())) {
                org.openelisglobal.sample.valueholder.SampleAdditionalField field = new org.openelisglobal.sample.valueholder.SampleAdditionalField();
                field.setFieldName(
                        org.openelisglobal.sample.valueholder.SampleAdditionalField.AdditionalFieldName.CONTACT_TRACING_INDEX_NAME);
                field.setFieldValue(sampleOrder.getContactTracingIndexName());
                updateData.addSampleField(field);
            }
            if (!GenericValidator.isBlankOrNull(sampleOrder.getContactTracingIndexRecordNumber())) {
                org.openelisglobal.sample.valueholder.SampleAdditionalField field = new org.openelisglobal.sample.valueholder.SampleAdditionalField();
                field.setFieldName(
                        org.openelisglobal.sample.valueholder.SampleAdditionalField.AdditionalFieldName.CONTACT_TRACING_INDEX_RECORD_NUMBER);
                field.setFieldValue(sampleOrder.getContactTracingIndexRecordNumber());
                updateData.addSampleField(field);
            }
        }

        updateData.validateSample(result);

        if (result.hasErrors()) {
            return;
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
        } catch (LIMSRuntimeException e) {
            if (e.getCause() instanceof StaleObjectStateException) {
                result.reject("errors.OptimisticLockException", "errors.OptimisticLockException");
            } else {
                LogEvent.logDebug(e);
                result.reject("errors.UpdateException", "errors.UpdateException");
            }
            LogEvent.logInfo(this.getClass().getSimpleName(), "placeOrder", result.toString());
        }
    }

    private String getSysUserId(HttpServletRequest request) {
        Object usd = request.getSession().getAttribute(org.openelisglobal.common.action.IActionConstants.USER_SESSION_DATA);
        if (usd == null) {
            usd = request.getAttribute(org.openelisglobal.common.action.IActionConstants.USER_SESSION_DATA);
            if (usd == null) {
                return null;
            }
        }
        return String.valueOf(((org.openelisglobal.login.valueholder.UserSessionData) usd).getSystemUserId());
    }
}
