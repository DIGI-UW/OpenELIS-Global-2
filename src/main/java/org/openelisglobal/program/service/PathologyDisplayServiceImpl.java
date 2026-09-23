package org.openelisglobal.program.service;

import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.common.services.SampleOrderService;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.program.valueholder.pathology.PathologyCaseViewDisplayItem;
import org.openelisglobal.program.valueholder.pathology.PathologyCaseViewDisplayItem.RequestDisplayBean;
import org.openelisglobal.program.valueholder.pathology.PathologyConclusion;
import org.openelisglobal.program.valueholder.pathology.PathologyConclusion.ConclusionType;
import org.openelisglobal.program.valueholder.pathology.PathologyDisplayItem;
import org.openelisglobal.program.valueholder.pathology.PathologyRequest.RequestType;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.program.valueholder.pathology.PathologyTechnique.TechniqueType;
import org.openelisglobal.questionnaire.service.QuestionnaireStorageService;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PathologyDisplayServiceImpl implements PathologyDisplayService {

    @Autowired
    private SampleService sampleService;
    @Autowired
    private PathologySampleService pathologySampleService;
    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private QuestionnaireStorageService questionnaireStorageService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private SampleItemService sampleItemService;

    @Override
    @Transactional
    public PathologyDisplayItem convertToDisplayItem(Integer pathologySampleId) {
        PathologySample pathologySample = pathologySampleService.get(pathologySampleId);
        PathologyDisplayItem displayItem = new PathologyDisplayItem();
        displayItem.setStatus(pathologySample.getStatus());
        displayItem.setRequestDate(localDate(pathologySample.getSample().getEnteredDate()));
        if (pathologySample.getPathologist() != null) {
            displayItem.setAssignedPathologist(pathologySample.getPathologist().getDisplayName());
        }
        if (pathologySample.getTechnician() != null) {
            displayItem.setAssignedTechnician(pathologySample.getTechnician().getDisplayName());
        }
        Patient patient = sampleService.getPatient(pathologySample.getSample());
        displayItem.setFirstName(patient.getPerson().getFirstName());
        displayItem.setLastName(patient.getPerson().getLastName());
        displayItem.setLabNumber(pathologySample.getSample().getAccessionNumber());
        displayItem.setPathologySampleId(pathologySample.getId());
        return displayItem;
    }

    @Override
    @Transactional
    public PathologyCaseViewDisplayItem convertToCaseDisplayItem(Integer pathologySampleId) {
        PathologySample pathologySample = pathologySampleService.get(pathologySampleId);
        PathologyCaseViewDisplayItem displayItem = new PathologyCaseViewDisplayItem();
        displayItem.setStatus(pathologySample.getStatus());
        displayItem.setRequestDate(localDate(pathologySample.getSample().getEnteredDate()));
        displayItem.setReceivedDate(localDate(pathologySample.getSample().getReceivedDate()));
        displayItem.setSpecimenTypes(specimenTypes(pathologySample.getSample().getId()));
        if (pathologySample.getPathologist() != null) {
            displayItem.setAssignedPathologist(pathologySample.getPathologist().getDisplayName());
            displayItem.setAssignedPathologistId(pathologySample.getPathologist().getId());
        }
        if (pathologySample.getTechnician() != null) {
            displayItem.setAssignedTechnician(pathologySample.getTechnician().getDisplayName());
            displayItem.setAssignedTechnicianId(pathologySample.getTechnician().getId());
        }
        pathologySample.getBlocks().size();
        pathologySample.getSlides().size();
        displayItem.setBlocks(pathologySample.getBlocks());
        displayItem.setSlides(pathologySample.getSlides());
        pathologySample.getReports().size();
        if (pathologySample.getReports() != null) {
            displayItem.setReports(pathologySample.getReports());
        }
        Patient patient = sampleService.getPatient(pathologySample.getSample());
        displayItem.setFirstName(patient.getPerson().getFirstName());
        displayItem.setLastName(patient.getPerson().getLastName());
        displayItem.setLabNumber(pathologySample.getSample().getAccessionNumber());
        displayItem.setPathologySampleId(pathologySample.getId());
        displayItem.setPatientPK(patient.getId());
        displayItem.setProgramQuestionnaire(questionnaireStorageService
                .getQuestionnaire(pathologySample.getProgram().getQuestionnaireUUID()).orElse(null));
        displayItem.setProgramQuestionnaireResponse(questionnaireStorageService
                .getQuestionnaireResponse(pathologySample.getQuestionnaireResponseUuid()).orElse(null));

        displayItem.setGrossExam(pathologySample.getGrossExam());
        displayItem.setMicroscopyExam(pathologySample.getMicroscopyExam());

        displayItem.setConclusions(
                pathologySample.getConclusions().stream().filter(e -> e.getType() == ConclusionType.DICTIONARY)
                        .map(e -> new IdValuePair(e.getValue(), dictionaryService.get(e.getValue()).getLocalizedName()))
                        .collect(Collectors.toList()));
        Optional<PathologyConclusion> conclusion = pathologySample.getConclusions().stream()
                .filter(e -> e.getType() == ConclusionType.TEXT).findFirst();
        if (conclusion.isPresent())
            displayItem.setConclusionText(conclusion.get().getValue());

        displayItem.setTechniques(
                pathologySample.getTechniques().stream().filter(e -> e.getType() == TechniqueType.DICTIONARY)
                        .map(e -> new IdValuePair(e.getValue(), dictionaryService.get(e.getValue()).getLocalizedName()))
                        .collect(Collectors.toList()));
        displayItem.setRequests(pathologySample.getRequests().stream()
                .filter(e -> e.getType() == RequestType.DICTIONARY).map(e -> new RequestDisplayBean(e.getValue(),
                        dictionaryService.get(e.getValue()).getLocalizedName(), e.getStatus()))
                .collect(Collectors.toList()));

        SampleOrderService sampleOrderService = new SampleOrderService(pathologySample.getSample());
        SampleOrderItem sampleItem = sampleOrderService.getSampleOrderItem();
        displayItem.setReferringFacility(sampleItem.getReferringSiteName());
        if (StringUtils.isNotBlank(sampleItem.getReferringSiteDepartmentId())) {
            Organization org = organizationService.get(sampleItem.getReferringSiteDepartmentId());
            if (org != null) {
                displayItem.setDepartment(org.getOrganizationName());
            }
        }
        displayItem.setRequester(requesterName(sampleItem.getProviderLastName(), sampleItem.getProviderFirstName()));
        displayItem.setAge(DateUtil.getCurrentAgeForDate(patient.getBirthDate(), DateUtil.getNowAsTimestamp()));
        displayItem.setSex(patient.getGender());
        return displayItem;
    }

    /**
     * The calendar day a stored instant falls on, resolved once here in the
     * server's own zone because the JSON layer formats dates in UTC.
     */
    private static LocalDate localDate(java.util.Date date) {
        // Not date.toInstant(): java.sql.Date, which the driver may hand back for a
        // date column, throws from it.
        return date == null ? null : Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * The case's specimen types, one entry per sample item in the order the items
     * were taken, so repeated types are repeated entries (FR-1).
     */
    private List<String> specimenTypes(String sampleId) {
        return sampleItemService.getSampleItemsBySampleId(sampleId).stream().map(SampleItem::getTypeOfSample)
                .filter(Objects::nonNull).map(TypeOfSample::getLocalizedName).collect(Collectors.toList());
    }

    /**
     * A case ordered without a requester has no name to show, rather than the two
     * words "null null" that joining the parts unconditionally produced.
     */
    private static String requesterName(String lastName, String firstName) {
        return StringUtils.trimToNull(StringUtils.trimToEmpty(lastName) + " " + StringUtils.trimToEmpty(firstName));
    }

    @Override
    @Transactional
    public PathologySample getPathologySampleWithLoadedAtttributes(Integer pathologySampleId) {
        PathologySample pathologySample = pathologySampleService.get(pathologySampleId);
        pathologySample.getBlocks().size();
        pathologySample.getSlides().size();
        pathologySample.getConclusions().size();
        return pathologySample;
    }
}
