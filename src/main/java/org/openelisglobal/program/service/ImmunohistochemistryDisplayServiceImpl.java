package org.openelisglobal.program.service;

import jakarta.transaction.Transactional;
import java.util.Optional;
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
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistryCaseViewDisplayItem;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistryDisplayItem;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample;
import org.openelisglobal.program.valueholder.pathology.PathologyConclusion;
import org.openelisglobal.program.valueholder.pathology.PathologyConclusion.ConclusionType;
import org.openelisglobal.program.valueholder.pathology.PathologyRequest.RequestType;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.program.valueholder.pathology.PathologyTechnique.TechniqueType;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.service.SampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ImmunohistochemistryDisplayServiceImpl implements ImmunohistochemistryDisplayService {

    @Autowired
    private SampleService sampleService;
    @Autowired
    private ImmunohistochemistrySampleService immunohistochemistrySampleService;
    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private FhirUtil fhirUtil;
    @Autowired
    private OrganizationService organizationService;

    @Override
    @Transactional
    public ImmunohistochemistryDisplayItem convertToDisplayItem(Integer immunohistochemistrySampleId) {
        ImmunohistochemistrySample immunohistochemistrySample = immunohistochemistrySampleService
                .get(immunohistochemistrySampleId);
        ImmunohistochemistryDisplayItem displayItem = new ImmunohistochemistryDisplayItem();
        displayItem.setStatus(immunohistochemistrySample.getStatus());
        displayItem.setRequestDate(immunohistochemistrySample.getSample().getEnteredDate());
        if (immunohistochemistrySample.getPathologist() != null) {
            displayItem.setAssignedPathologist(immunohistochemistrySample.getPathologist().getDisplayName());
        }
        if (immunohistochemistrySample.getTechnician() != null) {
            displayItem.setAssignedTechnician(immunohistochemistrySample.getTechnician().getDisplayName());
        }
        Patient patient = sampleService.getPatient(immunohistochemistrySample.getSample());
        displayItem.setFirstName(patient.getPerson().getFirstName());
        displayItem.setLastName(patient.getPerson().getLastName());
        displayItem.setLabNumber(immunohistochemistrySample.getSample().getAccessionNumber());
        displayItem.setImmunohistochemistrySampleId(immunohistochemistrySample.getId());
        return displayItem;
    }

    @Override
    @Transactional
    public ImmunohistochemistryCaseViewDisplayItem convertToCaseDisplayItem(Integer immunohistochemistrySampleId) {
        ImmunohistochemistrySample immunohistochemistrySample = immunohistochemistrySampleService
                .get(immunohistochemistrySampleId);
        ImmunohistochemistryCaseViewDisplayItem displayItem = new ImmunohistochemistryCaseViewDisplayItem();
        displayItem.setStatus(immunohistochemistrySample.getStatus());
        displayItem.setRequestDate(immunohistochemistrySample.getSample().getEnteredDate());
        if (immunohistochemistrySample.getPathologist() != null) {
            displayItem.setAssignedPathologist(immunohistochemistrySample.getPathologist().getDisplayName());
            displayItem.setAssignedPathologistId(immunohistochemistrySample.getPathologist().getId());
        }
        if (immunohistochemistrySample.getTechnician() != null) {
            displayItem.setAssignedTechnician(immunohistochemistrySample.getTechnician().getDisplayName());
            displayItem.setAssignedTechnicianId(immunohistochemistrySample.getTechnician().getId());
        }
        immunohistochemistrySample.getReports().size();
        if (immunohistochemistrySample.getReports() != null) {
            displayItem.setReports(immunohistochemistrySample.getReports());
        }
        Patient patient = sampleService.getPatient(immunohistochemistrySample.getSample());
        displayItem.setFirstName(patient.getPerson().getFirstName());
        displayItem.setLastName(patient.getPerson().getLastName());
        displayItem.setLabNumber(immunohistochemistrySample.getSample().getAccessionNumber());
        displayItem.setImmunohistochemistrySampleId(immunohistochemistrySample.getId());
        displayItem.setPatientPK(patient.getId());
        displayItem.setProgramQuestionnaire(fhirUtil.getLocalFhirClient().read().resource(Questionnaire.class)
                .withId(immunohistochemistrySample.getProgram().getQuestionnaireUUID().toString()).execute());
        displayItem.setProgramQuestionnaireResponse(
                fhirUtil.getLocalFhirClient().read().resource(QuestionnaireResponse.class)
                        .withId(immunohistochemistrySample.getQuestionnaireResponseUuid().toString()).execute());

        SampleOrderService sampleOrderService = new SampleOrderService(immunohistochemistrySample.getSample());
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
        displayItem.setReffered(immunohistochemistrySample.getReffered());
        // set items refered from Pathology .Read only
        PathologySample pathologySample = immunohistochemistrySample.getPathologySample();
        if (pathologySample != null && immunohistochemistrySample.getReffered()) {
            pathologySample.getBlocks().size();
            pathologySample.getSlides().size();
            displayItem.setBlocks(pathologySample.getBlocks());
            displayItem.setSlides(pathologySample.getSlides());
            displayItem.setGrossExam(pathologySample.getGrossExam());
            displayItem.setMicroscopyExam(pathologySample.getMicroscopyExam());

            displayItem.setConclusions(pathologySample.getConclusions().stream()
                    .filter(e -> e.getType() == ConclusionType.DICTIONARY)
                    .map(e -> new IdValuePair(e.getValue(), dictionaryService.get(e.getValue()).getLocalizedName()))
                    .collect(Collectors.toList()));
            Optional<PathologyConclusion> conclusion = pathologySample.getConclusions().stream()
                    .filter(e -> e.getType() == ConclusionType.TEXT).findFirst();
            if (conclusion.isPresent())
                displayItem.setConclusionText(conclusion.get().getValue());

            displayItem.setConclusions(pathologySample.getConclusions().stream()
                    .filter(e -> e.getType() == ConclusionType.DICTIONARY)
                    .map(e -> new IdValuePair(e.getValue(), dictionaryService.get(e.getValue()).getLocalizedName()))
                    .collect(Collectors.toList()));
            displayItem.setTechniques(pathologySample.getTechniques().stream()
                    .filter(e -> e.getType() == TechniqueType.DICTIONARY)
                    .map(e -> new IdValuePair(e.getValue(), dictionaryService.get(e.getValue()).getLocalizedName()))
                    .collect(Collectors.toList()));
            displayItem.setRequests(pathologySample.getRequests().stream()
                    .filter(e -> e.getType() == RequestType.DICTIONARY)
                    .map(e -> new IdValuePair(e.getValue(), dictionaryService.get(e.getValue()).getLocalizedName()))
                    .collect(Collectors.toList()));
            displayItem.setSex(patient.getGender());
        }
        return displayItem;
    }
}
