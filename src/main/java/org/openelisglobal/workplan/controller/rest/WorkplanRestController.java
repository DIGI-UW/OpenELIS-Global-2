package org.openelisglobal.workplan.controller.rest;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.formfields.FormFields;
import org.openelisglobal.common.formfields.FormFields.Field;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class WorkplanRestController extends BaseRestController {

    @Autowired
    protected TestService testService;

    protected static List<String> statusList;
    protected static boolean useReceptionTime = FormFields.getInstance().useField(Field.SampleEntryUseReceptionHour);
    protected static List<String> nfsTestIdList;

    @PostConstruct
    private void initialize() {
        IStatusService statusService = SpringContext.getBean(IStatusService.class);
        List<String> statuses = new ArrayList<>();
        statuses.add(statusService.getStatusID(AnalysisStatus.NotStarted));
        statuses.add(statusService.getStatusID(AnalysisStatus.BiologistRejected));
        statuses.add(statusService.getStatusID(AnalysisStatus.TechnicalRejected));
        statuses.add(statusService.getStatusID(AnalysisStatus.NonConforming_depricated));
        statusList = Collections.unmodifiableList(statuses);

        List<String> nfsTests = new ArrayList<>();
        nfsTests.add(getTestId("GB"));
        nfsTests.add(getTestId("Neut %"));
        nfsTests.add(getTestId("Lymph %"));
        nfsTests.add(getTestId("Mono %"));
        nfsTests.add(getTestId("Eo %"));
        nfsTests.add(getTestId("Baso %"));
        nfsTests.add(getTestId("GR"));
        nfsTests.add(getTestId("Hb"));
        nfsTests.add(getTestId("HCT"));
        nfsTests.add(getTestId("VGM"));
        nfsTests.add(getTestId("TCMH"));
        nfsTests.add(getTestId("CCMH"));
        nfsTests.add(getTestId("PLQ"));
        nfsTestIdList = Collections.unmodifiableList(nfsTests);
    }

    protected List<IdValuePair> adjustNFSTests(List<IdValuePair> allTestsList) {
        List<IdValuePair> adjustedList = new ArrayList<>(allTestsList.size());
        for (IdValuePair idValuePair : allTestsList) {
            if (!nfsTestIdList.contains(idValuePair.getId())) {
                adjustedList.add(idValuePair);
            }
        }
        // add NFS to the list
        adjustedList.add(new IdValuePair("NFS", "NFS"));
        return adjustedList;
    }

    protected boolean allNFSTestsRequested(List<String> testIdList) {
        return (testIdList.containsAll(nfsTestIdList));
    }

    protected String getTestId(String testName) {
        Test test = testService.getTestByLocalizedName(testName);
        if (test == null) {
            test = new Test();
        }
        return test.getId();
    }

    protected String getSubjectNumber(Analysis analysis) {
        if (ConfigurationProperties.getInstance().isPropertyValueEqual(Property.SUBJECT_ON_WORKPLAN, "true")) {
            PatientService patientService = SpringContext.getBean(PatientService.class);
            SampleHumanService sampleHumanService = SpringContext.getBean(SampleHumanService.class);
            Patient patient = sampleHumanService.getPatientForSample(analysis.getSampleItem().getSample());
            return patientService.getSubjectNumber(patient);
        } else {
            return "";
        }
    }

    protected String getPatientName(Analysis analysis) {
        if (ConfigurationProperties.getInstance().isPropertyValueEqual(Property.configurationName, "Haiti LNSP")) {
            Sample sample = analysis.getSampleItem().getSample();
            PatientService patientService = SpringContext.getBean(PatientService.class);
            SampleHumanService sampleHumanService = SpringContext.getBean(SampleHumanService.class);
            Patient patient = sampleHumanService.getPatientForSample(sample);
            List<String> values = new ArrayList<>();
            values.add(patientService.getLastName(patient) == null ? ""
                    : patientService.getLastName(patient).toUpperCase());
            values.add(patientService.getNationalId(patient));

            String referringPatientId = SpringContext.getBean(ObservationHistoryService.class)
                    .getValueForSample(ObservationType.REFERRERS_PATIENT_ID, sample.getId());
            values.add(referringPatientId == null ? "" : referringPatientId);
            return StringUtil.buildDelimitedStringFromList(values, " / ", true);

        } else {
            return "";
        }
    }

    protected String getReceivedDateDisplay(Sample sample) {
        String receptionTime = useReceptionTime ? " " + sample.getReceivedTimeForDisplay() : "";
        return sample.getReceivedDateForDisplay() + receptionTime;
    }

    class ValueComparator implements Comparator<IdValuePair> {

        @Override
        public int compare(IdValuePair p1, IdValuePair p2) {
            return p1.getValue().toUpperCase().compareTo(p2.getValue().toUpperCase());
        }
    }
}
