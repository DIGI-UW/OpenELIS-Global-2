package org.openelisglobal.reports.action.implementation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.program.service.cytology.CytologyDisplayService;
import org.openelisglobal.program.valueholder.cytology.CytologyDiagnosis;
import org.openelisglobal.program.valueholder.cytology.CytologyDiagnosis.CytologyDiagnosisResultType;
import org.openelisglobal.program.valueholder.cytology.CytologySample;
import org.openelisglobal.program.valueholder.cytology.CytologySpecimenAdequacy.SpecimenAdequancySatisfaction;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.spring.util.SpringContext;

public class PatientCytologyReport extends PatientProgramReport {

    protected CytologyDisplayService cytologySampleService = SpringContext.getBean(CytologyDisplayService.class);

    private CytologySample cytologySample;

    @Override
    protected String getReportName() {
        return "PatientCytologyReport";
    }

    @Override
    protected void setAdditionalReportItems() {
        if (cytologySample.getSpecimenAdequacy() != null) {
            data.setSpecimenAdequacy(cytologySample.getSpecimenAdequacy().getSatisfaction()
                    .equals(SpecimenAdequancySatisfaction.SATISFACTORY_FOR_EVALUATION)
                            ? MessageUtil.getMessage("cytology.label.satisfactory")
                            : MessageUtil.getMessage("cytology.label.unsatisfactory"));
        }
        if (cytologySample.getDiagnosis() != null) {
            if (cytologySample.getDiagnosis().getNegativeDiagnosis()) {
                data.setDiagnosis(MessageUtil.getMessage("cytology.label.negative"));

            } else {
                CytologyDiagnosis cytoDiagnosis = cytologySample.getDiagnosis();
                if (cytoDiagnosis.getDiagnosisResultsMaps() != null) {
                    cytoDiagnosis.getDiagnosisResultsMaps().forEach(diagnosisResult -> {
                        List<String> diagnoses = new ArrayList<>();

                        if (diagnosisResult.getResultType().equals(CytologyDiagnosisResultType.DICTIONARY)) {
                            diagnoses
                                    .addAll(diagnosisResult.getResults().stream().filter(e -> StringUtils.isNotBlank(e))
                                            .map(e -> dictionaryService.get(e).getLocalizedName())
                                            .collect(Collectors.toList()));
                        } else {
                            diagnoses.add(diagnosisResult.getResults().get(0));
                        }
                        switch (diagnosisResult.getCategory()) {
                        case EPITHELIAL_CELL_ABNORMALITY: {
                            List<String> existingDiagnoses = new ArrayList<>();
                            if (data.getEpithelialCellAbnomalities() != null) {
                                if (!data.getEpithelialCellAbnomalities().isEmpty()) {
                                    existingDiagnoses = data.getEpithelialCellAbnomalities();
                                }
                            }
                            existingDiagnoses.addAll(diagnoses);
                            data.setEpithelialCellAbnomalities(existingDiagnoses);
                            break;
                        }
                        case NON_NEOPLASTIC_CELLULAR_VARIATIONS: {
                            data.setNonNeoplasticCellularVariations(diagnoses);
                            break;
                        }
                        case ORGANISMS: {
                            data.setOrganisms(diagnoses);
                            break;
                        }
                        case REACTIVE_CELLULAR_CHANGES: {
                            data.setReactiveCellularChanges(diagnoses);
                            break;
                        }
                        case OTHER: {
                            data.setOtherDiagnoses(diagnoses);
                            break;
                        }
                        }
                    });
                }
            }
        }
    }

    @Override
    protected void innitializeSample(ReportForm form) {
        cytologySample = cytologySampleService
                .getCytologySampleWithLoadedAttributes(Integer.valueOf(form.getProgramSampleId()));
        sample = cytologySample.getSample();
    }
}
