package org.openelisglobal.program.service.cytology;

import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.openelisglobal.common.services.SampleOrderService;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.program.valueholder.cytology.CytologyCaseViewDisplayItem;
import org.openelisglobal.program.valueholder.cytology.CytologyDiagnosis;
import org.openelisglobal.program.valueholder.cytology.CytologyDiagnosis.CytologyDiagnosisResultType;
import org.openelisglobal.program.valueholder.cytology.CytologyDisplayItem;
import org.openelisglobal.program.valueholder.cytology.CytologySample;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.service.SampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CytologyDisplayServiceImpl implements CytologyDisplayService {

    @Autowired
    private SampleService sampleService;

    @Autowired
    private CytologySampleService cytologySampleService;

    @Autowired
    private FhirUtil fhirUtil;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private DictionaryService dictionaryService;

    @Override
    @Transactional
    public CytologyCaseViewDisplayItem convertToCaseDisplayItem(Integer cytologySampleId) {
        CytologySample cytologySample = cytologySampleService.get(cytologySampleId);
        CytologyCaseViewDisplayItem displayItem = new CytologyCaseViewDisplayItem();
        displayItem.setStatus(cytologySample.getStatus());
        displayItem.setRequestDate(cytologySample.getSample().getEnteredDate());
        if (cytologySample.getCytoPathologist() != null) {
            displayItem.setAssignedCytoPathologist(cytologySample.getCytoPathologist().getDisplayName());
            displayItem.setAssignedPathologistId(cytologySample.getCytoPathologist().getId());
        }
        if (cytologySample.getTechnician() != null) {
            displayItem.setAssignedTechnician(cytologySample.getTechnician().getDisplayName());
            displayItem.setAssignedTechnicianId(cytologySample.getTechnician().getId());
        }
        Patient patient = sampleService.getPatient(cytologySample.getSample());
        displayItem.setFirstName(patient.getPerson().getFirstName());
        displayItem.setLastName(patient.getPerson().getLastName());
        displayItem.setLabNumber(cytologySample.getSample().getAccessionNumber());
        displayItem.setPathologySampleId(cytologySample.getId());
        displayItem.setPatientPK(patient.getId());
        displayItem.setProgramQuestionnaire(fhirUtil.getLocalFhirClient().read().resource(Questionnaire.class)
                .withId(cytologySample.getProgram().getQuestionnaireUUID().toString()).execute());
        displayItem.setProgramQuestionnaireResponse(
                fhirUtil.getLocalFhirClient().read().resource(QuestionnaireResponse.class)
                        .withId(cytologySample.getQuestionnaireResponseUuid().toString()).execute());

        cytologySample.getSlides().size();
        displayItem.setSlides(cytologySample.getSlides());
        cytologySample.getReports().size();
        if (cytologySample.getReports() != null) {
            displayItem.setReports(cytologySample.getReports());
        }
        if (cytologySample.getSpecimenAdequacy() != null) {
            CytologyCaseViewDisplayItem.SpecimenAdequacy adquecy = new CytologyCaseViewDisplayItem.SpecimenAdequacy();
            adquecy.setSatisfaction(cytologySample.getSpecimenAdequacy().getSatisfaction());
            adquecy.setValues(cytologySample.getSpecimenAdequacy().getValues().stream().filter(e -> e != null)
                    .map(e -> new IdValuePair(e, dictionaryService.get(e).getLocalizedName()))
                    .collect(Collectors.toList()));
            displayItem.setSpecimenAdequacy(adquecy);
        }
        CytologyDiagnosis cytoDiagnosis = cytologySample.getDiagnosis();
        if (cytoDiagnosis != null) {
            CytologyCaseViewDisplayItem.Diagnosis diagnosis = new CytologyCaseViewDisplayItem.Diagnosis();
            diagnosis.setNegativeDiagnosis(cytologySample.getDiagnosis().getNegativeDiagnosis());

            List<CytologyCaseViewDisplayItem.Diagnosis.DiagnosisResultsMap> resultsMaps = new ArrayList<>();
            if (cytoDiagnosis.getDiagnosisResultsMaps() != null) {
                cytoDiagnosis.getDiagnosisResultsMaps().forEach(diagnosisResult -> {
                    CytologyCaseViewDisplayItem.Diagnosis.DiagnosisResultsMap resultsMap = new CytologyCaseViewDisplayItem.Diagnosis.DiagnosisResultsMap();
                    resultsMap.setCategory(diagnosisResult.getCategory());
                    resultsMap.setResultType(diagnosisResult.getResultType());
                    if (diagnosisResult.getResultType().equals(CytologyDiagnosisResultType.DICTIONARY)) {
                        resultsMap
                                .setResults(diagnosisResult.getResults().stream().filter(e -> StringUtils.isNotBlank(e))
                                        .map(e -> new IdValuePair(e, dictionaryService.get(e).getLocalizedName()))
                                        .collect(Collectors.toList()));
                    } else {
                        List<IdValuePair> result = new ArrayList<>();
                        result.add(new IdValuePair(diagnosisResult.getResults().get(0),
                                diagnosisResult.getResults().get(0)));
                        resultsMap.setResults(result);
                    }

                    resultsMaps.add(resultsMap);
                });
            }
            diagnosis.setDiagnosisResultsMaps(resultsMaps);
            displayItem.setDiagnosis(diagnosis);
        }
        SampleOrderService sampleOrderService = new SampleOrderService(cytologySample.getSample());
        SampleOrderItem sampleItem = sampleOrderService.getSampleOrderItem();
        displayItem.setReferringFacility(sampleItem.getReferringSiteName());
        if (StringUtils.isNotBlank(sampleItem.getReferringSiteDepartmentId())) {
            Organization org = organizationService.get(sampleItem.getReferringSiteDepartmentId());
            if (org != null) {
                displayItem.setDepartment(org.getOrganizationName());
            }
        }
        displayItem.setRequester(sampleItem.getProviderLastName() + " " + sampleItem.getProviderFirstName());
        displayItem.setAge(DateUtil.getCurrentAgeForDate(patient.getBirthDate(), DateUtil.getNowAsTimestamp()));
        displayItem.setSex(patient.getGender());
        return displayItem;
    }

    @Override
    @Transactional
    public CytologyDisplayItem convertToDisplayItem(Integer cytologySampleId) {
        CytologySample cytologySample = cytologySampleService.get(cytologySampleId);
        CytologyDisplayItem displayItem = new CytologyDisplayItem();
        displayItem.setStatus(cytologySample.getStatus());
        displayItem.setRequestDate(cytologySample.getSample().getEnteredDate());
        if (cytologySample.getCytoPathologist() != null) {
            displayItem.setAssignedCytoPathologist(cytologySample.getCytoPathologist().getDisplayName());
        }
        if (cytologySample.getTechnician() != null) {
            displayItem.setAssignedTechnician(cytologySample.getTechnician().getDisplayName());
        }
        Patient patient = sampleService.getPatient(cytologySample.getSample());
        displayItem.setFirstName(patient.getPerson().getFirstName());
        displayItem.setLastName(patient.getPerson().getLastName());
        displayItem.setLabNumber(cytologySample.getSample().getAccessionNumber());
        displayItem.setPathologySampleId(cytologySample.getId());
        return displayItem;
    }

    @Override
    @Transactional
    public CytologySample getCytologySampleWithLoadedAttributes(Integer cytologySampleId) {
        CytologySample cytologySample = cytologySampleService.get(cytologySampleId);
        if (cytologySample.getDiagnosis() != null) {
            cytologySample.getDiagnosis().getDiagnosisResultsMaps().size();
        }
        return cytologySample;
    }
}
