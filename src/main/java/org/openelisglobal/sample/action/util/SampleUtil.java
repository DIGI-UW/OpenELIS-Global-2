package org.openelisglobal.sample.action.util;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;

import org.openelisglobal.common.provider.validation.IAccessionNumberValidator.ValidationResults;
import org.openelisglobal.common.util.validator.GenericValidator;
import org.openelisglobal.common.validator.BaseErrors;
import org.openelisglobal.patient.action.IPatientUpdate;
import org.openelisglobal.patient.action.IPatientUpdate.PatientUpdateStatus;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.sample.form.SampleEditForm;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.util.AccessionNumberUtil;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
@Component
public class SampleUtil {
    @Autowired 
    private SampleService sampleService; 

    public static void testAndInitializePatientForSaving(HttpServletRequest request, PatientManagementInfo patientInfo,
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

        public  boolean accessionNumberChanged(SampleEditForm form) {
        String newAccessionNumber = form.getNewAccessionNumber();

        return !GenericValidator.isBlankOrNull(newAccessionNumber)
                && !newAccessionNumber.equals(form.getAccessionNumber());
    }

        public  Sample updateAccessionNumberInSample(SampleEditForm form, String sysUserId) {
        Sample sample = sampleService.getSampleByAccessionNumber(form.getAccessionNumber());

        if (sample != null) {
            sample.setAccessionNumber(form.getNewAccessionNumber());
            sample.setSysUserId(sysUserId);
        }

        return sample;
    }

        public Errors validateNewAccessionNumber(String accessionNumber, Errors errors) {
        ValidationResults results = AccessionNumberUtil.correctFormat(accessionNumber, false);

        if (results != ValidationResults.SUCCESS) {
            errors.reject("sample.entry.invalid.accession.number.format",
                    "sample.entry.invalid.accession.number.format");
        } else if (AccessionNumberUtil.isUsed(accessionNumber)) {
            errors.reject("sample.entry.invalid.accession.number.used", "sample.entry.invalid.accession.number.used");
        }

        return errors;
    }

}
