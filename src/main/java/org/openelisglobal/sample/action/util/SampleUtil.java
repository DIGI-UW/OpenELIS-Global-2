package org.openelisglobal.sample.action.util;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.openelisglobal.common.provider.validation.IAccessionNumberValidator.ValidationResults;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.validator.GenericValidator;
import org.openelisglobal.common.validator.BaseErrors;
import org.openelisglobal.patient.action.IPatientUpdate;
import org.openelisglobal.patient.action.IPatientUpdate.PatientUpdateStatus;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.sample.form.SampleEditForm;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.util.AccessionNumberUtil;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.valueholder.Test;
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

    public boolean accessionNumberChanged(SampleEditForm form) {
        String newAccessionNumber = form.getNewAccessionNumber();

        return !GenericValidator.isBlankOrNull(newAccessionNumber)
                && !newAccessionNumber.equals(form.getAccessionNumber());
    }

    public Sample updateAccessionNumberInSample(SampleEditForm form, String sysUserId) {
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

    public static String buildSampleXml(List<Test> tests, SampleItem sampleItem, String sampleItemId) {

        String date = DateUtil.getCurrentDateAsText();

        StringBuilder testIds = new StringBuilder();
        StringBuilder sectionMap = new StringBuilder();
        StringBuilder sampleTypeMap = new StringBuilder();

        if (sampleItem.getTypeOfSample() == null) {
            throw new RuntimeException("SampleItem missing typeOfSample");
        }

        String sampleTypeId = sampleItem.getTypeOfSample().getId();

        for (int i = 0; i < tests.size(); i++) {
            Test test = tests.get(i);
            String testId = test.getId();

            if (test.getTestSection() == null) {
                throw new RuntimeException("Test " + testId + " has no section configured");
            }

            String sectionId = test.getTestSection().getId();

            // --- Build strings ---
            testIds.append(testId);
            sectionMap.append(testId).append(":").append(sectionId);
            sampleTypeMap.append(testId).append(":").append(sampleTypeId);

            if (i < tests.size() - 1) {
                testIds.append(",");
                sectionMap.append(",");
                sampleTypeMap.append(",");
            }
        }

        StringBuilder xml = new StringBuilder();

        xml.append("<samples>");
        xml.append("<sample ");

        xml.append("sampleID=\"").append(sampleTypeId).append("\" ");
        xml.append("tests=\"").append(testIds).append("\" ");
        xml.append("panels=\"\" ");
        xml.append("testSectionMap=\"").append(sectionMap).append("\" ");
        xml.append("testSampleTypeMap=\"").append(sampleTypeMap).append("\" ");
        xml.append("date=\"").append(date).append("\" ");
        xml.append("time=\"00:00\" ");
        xml.append("receivedDate=\"").append(date).append("\" ");
        xml.append("receivedTime=\"00:00\" ");

        if (!GenericValidator.isBlankOrNull(sampleItemId)) {
            xml.append("sampleItemId=\"").append(sampleItemId).append("\" ");
        }

        xml.append("/>");
        xml.append("</samples>");

        return xml.toString();
    }

}
