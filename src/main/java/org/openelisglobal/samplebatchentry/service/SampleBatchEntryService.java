package org.openelisglobal.samplebatchentry.service;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.common.validator.BaseErrors;
import org.openelisglobal.dataexchange.fhir.exception.FhirPersistanceException;
import org.openelisglobal.dataexchange.fhir.exception.FhirTransformationException;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.patient.action.IPatientUpdate;
import org.openelisglobal.patient.action.IPatientUpdate.PatientUpdateStatus;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.service.PatientManagementUpdate;
import org.openelisglobal.sample.service.SamplePatientEntryService;
import org.openelisglobal.sample.validator.SamplePatientEntryFormValidator;
import org.openelisglobal.samplebatchentry.form.SampleBatchEntrySaveForm;
import org.openelisglobal.samplebatchentry.util.EidBatchSampleXml;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.validation.BindingResult;

/**
 * Batch sample entry: the same order-creation workflow as Sample Entry, over a
 * batch of samples. {@code save} validates the form and persists through
 * {@link SamplePatientEntryService#persistData}, which requires
 * PRIV_ORDER_CREATE, so the entry point takes the same privilege rather than
 * leaving a controller-reachable @Service class ungated.
 */
@Service
public class SampleBatchEntryService {
    @Autowired
    private TestService testService;
    @Autowired
    private TypeOfSampleTestService typeOfSampleTestService;
    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private SamplePatientEntryFormValidator entryFormValidator;
    @Autowired
    private SamplePatientEntryService samplePatientEntryService;
    @Autowired
    private FhirTransformService fhirTransformService;

    @PreAuthorize("hasAuthority('PRIV_ORDER_CREATE')")
    @Transactional(rollbackFor = Exception.class)
    public boolean save(SampleBatchEntrySaveForm form, BindingResult result, HttpServletRequest request,
            String sysUserId) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        if (StringUtil.isNullorNill(form.getSampleXML()) && form.getEidSelection() != null) {
            form.setSampleXML(new EidBatchSampleXml(testService, typeOfSampleService, typeOfSampleTestService)
                    .build(form.getEidSelection(), form.getCurrentDate()));
        }

        entryFormValidator.validate(form, result);
        if (result.hasErrors()) {
            return false;
        }
        SamplePatientUpdateData updateData = new SamplePatientUpdateData(sysUserId);

        PatientManagementInfo patientInfo = form.getPatientProperties();
        SampleOrderItem sampleOrder = form.getSampleOrderItems();

        boolean trackPayments = ConfigurationProperties.getInstance()
                .isPropertyValueEqual(Property.TRACK_PATIENT_PAYMENT, "true");

        String receivedDateForDisplay = sampleOrder.getReceivedDateForDisplay();
        if (org.apache.commons.validator.GenericValidator.isBlankOrNull(receivedDateForDisplay)) {
            receivedDateForDisplay = DateUtil.getCurrentDateAsText();
        }

        if (!org.apache.commons.validator.GenericValidator.isBlankOrNull(sampleOrder.getReceivedTime())) {
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
        updateData.initProvider(sampleOrder);
        updateData.initSampleData(form.getSampleXML(), receivedDateForDisplay, trackPayments, sampleOrder);
        updateData.validateSample(result);

        if (result.hasErrors()) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        samplePatientEntryService.persistData(updateData, patientUpdate, patientInfo, form, request);
        try {
            fhirTransformService.transformPersistOrderEntryFhirObjects(updateData, patientInfo, false, null);
        } catch (FhirTransformationException | FhirPersistanceException e) {
            LogEvent.logError(e);
        }
        return true;
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

}
