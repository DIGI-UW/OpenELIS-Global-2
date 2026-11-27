package org.openelisglobal.program.service;

import jakarta.transaction.Transactional;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.common.services.SampleOrderService;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.program.util.DictionaryEntryNames;
import org.openelisglobal.program.util.PathologyRowOrder;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample;
import org.openelisglobal.program.valueholder.pathology.PathologyBlock;
import org.openelisglobal.program.valueholder.pathology.PathologyCaseViewDisplayItem;
import org.openelisglobal.program.valueholder.pathology.PathologyCaseViewDisplayItem.ConsultationView;
import org.openelisglobal.program.valueholder.pathology.PathologyCaseViewDisplayItem.LabelVerificationView;
import org.openelisglobal.program.valueholder.pathology.PathologyCaseViewDisplayItem.RequestView;
import org.openelisglobal.program.valueholder.pathology.PathologyCaseViewDisplayItem.SlideStampView;
import org.openelisglobal.program.valueholder.pathology.PathologyCaseViewDisplayItem.StageEventView;
import org.openelisglobal.program.valueholder.pathology.PathologyConclusion;
import org.openelisglobal.program.valueholder.pathology.PathologyConclusion.ConclusionType;
import org.openelisglobal.program.valueholder.pathology.PathologyConsultation;
import org.openelisglobal.program.valueholder.pathology.PathologyDisplayItem;
import org.openelisglobal.program.valueholder.pathology.PathologyRequest;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.openelisglobal.program.valueholder.pathology.PathologySlide;
import org.openelisglobal.program.valueholder.pathology.PathologyStageEvent;
import org.openelisglobal.program.valueholder.pathology.PathologyTechnique;
import org.openelisglobal.program.valueholder.pathology.PathologyTechnique.TechniqueType;
import org.openelisglobal.questionnaire.service.QuestionnaireStorageService;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
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
    @Autowired
    private SystemUserService systemUserService;
    @Autowired
    private ImmunohistochemistrySampleService immunohistochemistrySampleService;
    @Autowired
    private PathologyConsultationService pathologyConsultationService;

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
        if (patient != null && patient.getPerson() != null) {
            displayItem.setFirstName(patient.getPerson().getFirstName());
            displayItem.setLastName(patient.getPerson().getLastName());
        }
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
        displayItem.setBlocks(PathologyRowOrder.blocks(pathologySample.getBlocks()));
        displayItem.setSlides(PathologyRowOrder.slides(pathologySample.getSlides()));
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
        displayItem.setReviewNotes(pathologySample.getReviewNotes());
        displayItem.setDecalcificationRequired(pathologySample.isDecalcificationRequired());
        displayItem.setTissueTypeNames(dictionaryNames(
                pathologySample.getBlocks().stream().map(PathologyBlock::getTissueTypeId), dictionaryService));
        displayItem.setStainNames(dictionaryNames(pathologySample.getSlides().stream().map(PathologySlide::getStainId),
                dictionaryService));
        displayItem.setLabelVerifications(labelVerifications(pathologySample.getSlides(), systemUserService));
        Map<String, SystemUser> users = new HashMap<>();
        displayItem.setStainStamps(slideStamps(pathologySample.getSlides(), PathologySlide::getStainStatusAt,
                PathologySlide::getStainStatusBy, users));
        displayItem.setCoverslipQcStamps(slideStamps(pathologySample.getSlides(), PathologySlide::getCoverslipQcAt,
                PathologySlide::getCoverslipQcBy, users));
        List<PathologyStageEvent> stageEvents = pathologySampleService.getStageEvents(pathologySampleId);
        displayItem.setStageEvents(stageEvents.stream().map(StageEventView::of).collect(Collectors.toList()));
        displayItem.setRunReference(runReference(stageEvents, PathologyStatus.PROCESSING));
        displayItem.setRunReferenceEditable(
                pathologySampleService.isRunReferenceRecordable(pathologySample, PathologyStatus.PROCESSING));
        displayItem.setCoverslipRunReference(runReference(stageEvents, PathologyStatus.COVERSLIPPING));
        displayItem.setCoverslipRunReferenceEditable(
                pathologySampleService.isRunReferenceRecordable(pathologySample, PathologyStatus.COVERSLIPPING));
        displayItem.setNextStage(pathologySampleService.nextStage(pathologySample).map(Enum::name).orElse(null));
        displayItem
                .setPreviousStage(pathologySampleService.previousStage(pathologySample).map(Enum::name).orElse(null));
        displayItem.setReleasable(pathologySampleService.isReleasable(pathologySample));
        displayItem.setReadingStage(pathologySampleService.readingStage().name());
        displayItem.setFindingsOpen(pathologySampleService.findingsOpen(pathologySample));
        ImmunohistochemistrySample referral = immunohistochemistrySampleService
                .getByPathologySampleId(pathologySample.getId());
        displayItem.setReferredToIhc(referral != null);
        displayItem.setIhcCaseId(referral == null ? null : referral.getId());
        displayItem.setIhcReferredTestIds(pathologySampleService.ihcReferredTestIds(pathologySample));

        // An id that names no entry (written before codes were checked) is left out.
        displayItem.setConclusions(
                pathologySample.getConclusions().stream().filter(e -> e.getType() == ConclusionType.DICTIONARY)
                        .flatMap(e -> DictionaryEntryNames.conclusionEntryOf(dictionaryService, e.getValue()).stream())
                        .collect(Collectors.toList()));
        Optional<PathologyConclusion> conclusion = pathologySample.getConclusions().stream()
                .filter(e -> e.getType() == ConclusionType.TEXT).findFirst();
        if (conclusion.isPresent())
            displayItem.setConclusionText(conclusion.get().getValue());

        displayItem.setTechniques(DictionaryEntryNames.namedPairs(dictionaryService, pathologySample.getTechniques()
                .stream().filter(e -> e.getType() == TechniqueType.DICTIONARY).map(PathologyTechnique::getValue)));
        displayItem.setRequests(requestViews(pathologySample, users));
        List<PathologyConsultation> consultations = pathologyConsultationService.getForCase(pathologySample.getId());
        displayItem.setConsultations(consultationViews(consultations, users));
        displayItem.setTakesConsultations(pathologyConsultationService.takesConsultations(pathologySample));
        displayItem.setSecondOpinionSuggestedFor(
                pathologyConsultationService.suggestedFor(pathologySample, consultations));

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
     * The run reference written on the case's latest entry onto the stage; a case
     * that never entered the stage with an entry recorded has none.
     */
    private static String runReference(List<PathologyStageEvent> stageEvents, PathologyStatus stage) {
        return stageEvents.stream().filter(event -> event.getStage() == stage).reduce((earlier, later) -> later)
                .map(PathologyStageEvent::getRunReference).orElse(null);
    }

    /**
     * Who recorded a slide's stain status, or its coverslip check, and when, by
     * slide id, for the slides where it was recorded. Each user is looked up once
     * across both maps.
     */
    private Map<Integer, SlideStampView> slideStamps(List<PathologySlide> slides,
            Function<PathologySlide, Timestamp> at, Function<PathologySlide, String> by,
            Map<String, SystemUser> users) {
        Map<Integer, SlideStampView> stamps = new LinkedHashMap<>();
        for (PathologySlide slide : slides) {
            if (slide.getId() == null || at.apply(slide) == null) {
                continue;
            }
            String userId = by.apply(slide);
            SystemUser user = userId == null ? null : users.computeIfAbsent(userId, systemUserService::get);
            stamps.put(slide.getId(), SlideStampView.of(at.apply(slide), user));
        }
        return stamps;
    }

    /**
     * Every request on the case, newest first, as the requests table shows it,
     * whatever its status or kind, a request on file included. Each user is looked
     * up once, across these and the slide stamps.
     */
    private List<RequestView> requestViews(PathologySample pathologySample, Map<String, SystemUser> users) {
        if (pathologySample.getRequests() == null) {
            return List.of();
        }
        Map<String, String> stainNames = dictionaryNames(
                pathologySample.getRequests().stream().map(PathologyRequest::getStainId), dictionaryService);
        return PathologyRowOrder.requests(pathologySample.getRequests()).stream().map(request -> {
            SystemUser raisedBy = request.getRaisedBy() == null ? null
                    : users.computeIfAbsent(request.getRaisedBy(), systemUserService::get);
            SlideStampView raised = SlideStampView.of(request.getRaisedAt(), raisedBy);
            return new RequestView(request.getId(), nameOf(request.getKind()), request.getTargetBlockId(),
                    request.getTargetSlideId(),
                    pathologySample.nameOfBlock(request.getTargetBlockId())
                            .or(() -> pathologySample.nameOfSlide(request.getTargetSlideId())).orElse(null),
                    request.getStainId(), stainNames.get(request.getStainId()), nameOf(request.getPriority()),
                    request.instruction(dictionaryService), nameOf(request.getStatus()), request.getCancelReason(),
                    request.getResultBlockId(), request.getResultSlideId(),
                    pathologySample.nameOfBlock(request.getResultBlockId())
                            .or(() -> pathologySample.nameOfSlide(request.getResultSlideId())).orElse(null),
                    raised == null ? null : raised.at(), raised == null ? null : raised.by());
        }).collect(Collectors.toList());
    }

    /**
     * Every second opinion on the case, newest first as stored, with each user
     * looked up once across these, the requests and the slide stamps. A reason id
     * that names no entry is left empty.
     */
    private List<ConsultationView> consultationViews(List<PathologyConsultation> consultations,
            Map<String, SystemUser> users) {
        Function<SystemUser, SystemUser> lookedUp = user -> user == null ? null
                : users.computeIfAbsent(user.getId(), systemUserService::get);
        return consultations.stream().map(consultation -> {
            SystemUser requestedBy = lookedUp.apply(consultation.getRequestedBy());
            SystemUser consultedUser = lookedUp.apply(consultation.getConsultedUser());
            SlideStampView answered = SlideStampView.of(consultation.getAnsweredAt(), consultedUser);
            return new ConsultationView(consultation.getId(), nameOf(consultation.getStatus()),
                    consultedUser == null ? null : consultedUser.getId(),
                    consultedUser == null ? null : consultedUser.getDisplayName(),
                    DictionaryEntryNames.localizedNameOf(dictionaryService, consultation.getReasonDictionaryId())
                            .orElse(null),
                    consultation.isPreTreatment(), SlideStampView.of(consultation.getRequestedAt(), requestedBy),
                    nameOf(consultation.getOutcome()), consultation.getOutcomeNote(), answered,
                    consultation.getAnsweredAt() == null ? null : consultation.getAnsweredAt().getTime());
        }).collect(Collectors.toList());
    }

    private static String nameOf(Enum<?> value) {
        return value == null ? null : value.name();
    }

    /**
     * Names every dictionary entry the rows hold, the tissue a cassette holds or
     * the stain on a slide, retired entries included, which the category list a
     * picker is filled from leaves out. An id that names no entry stands for
     * itself.
     */
    static Map<String, String> dictionaryNames(Stream<String> dictionaryIds, DictionaryService dictionaryService) {
        Map<String, String> names = new LinkedHashMap<>();
        dictionaryIds.filter(StringUtils::isNotBlank).distinct()
                .forEach(id -> names.put(id, DictionaryEntryNames.localizedNameOf(dictionaryService, id).orElse(id)));
        return names;
    }

    @Override
    @Transactional
    public LabelVerificationView labelVerificationOf(PathologySlide slide) {
        if (slide == null) {
            return null;
        }
        return LabelVerificationView.of(slide,
                slide.getLabelVerifiedBy() == null ? null : systemUserService.get(slide.getLabelVerifiedBy()));
    }

    /**
     * Who checked each slide's label and when, by slide id, for the slides whose
     * label was checked. Each verifier is looked up once, however many slides they
     * checked.
     */
    static Map<Integer, LabelVerificationView> labelVerifications(List<PathologySlide> slides,
            SystemUserService systemUserService) {
        Map<Integer, LabelVerificationView> verifications = new LinkedHashMap<>();
        Map<String, SystemUser> verifiers = new HashMap<>();
        for (PathologySlide slide : slides) {
            if (slide.getId() == null || slide.getLabelVerifiedAt() == null) {
                continue;
            }
            SystemUser verifier = slide.getLabelVerifiedBy() == null ? null
                    : verifiers.computeIfAbsent(slide.getLabelVerifiedBy(), systemUserService::get);
            verifications.put(slide.getId(), LabelVerificationView.of(slide, verifier));
        }
        return verifications;
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
