package org.openelisglobal.program.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.barcode.form.LabelRowForm;
import org.openelisglobal.barcode.form.LabelsSectionForm;
import org.openelisglobal.barcode.form.PostSavePrintDialogForm;
import org.openelisglobal.barcode.service.BarcodeInfoService;
import org.openelisglobal.barcode.service.BarcodeWorkflowPrintService;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.exception.LocalizedValidationException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.ResultSaveService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.services.beanAdapters.ResultSaveBeanAdapter;
import org.openelisglobal.common.services.registration.ResultUpdateRegister;
import org.openelisglobal.common.services.serviceBeans.ResultSaveBean;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl.NoteType;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.controller.pathology.PathologySampleForm.PathologySlideForm;
import org.openelisglobal.program.dao.PathologyBlockDAO;
import org.openelisglobal.program.dao.PathologySampleDAO;
import org.openelisglobal.program.dao.PathologySlideDAO;
import org.openelisglobal.program.dao.PathologyStageEventDAO;
import org.openelisglobal.program.util.DesignationScheme;
import org.openelisglobal.program.util.DictionaryEntryNames;
import org.openelisglobal.program.util.EnumNames;
import org.openelisglobal.program.util.PathologyDesignations;
import org.openelisglobal.program.util.PathologyRowOrder;
import org.openelisglobal.program.util.PathologyStages;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample;
import org.openelisglobal.program.valueholder.pathology.CassetteState;
import org.openelisglobal.program.valueholder.pathology.CoverslipQc;
import org.openelisglobal.program.valueholder.pathology.PathologyBlock;
import org.openelisglobal.program.valueholder.pathology.PathologyConclusion;
import org.openelisglobal.program.valueholder.pathology.PathologyConclusion.ConclusionType;
import org.openelisglobal.program.valueholder.pathology.PathologyRequest;
import org.openelisglobal.program.valueholder.pathology.PathologyRequest.RequestStatus;
import org.openelisglobal.program.valueholder.pathology.PathologyRequest.RequestType;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.openelisglobal.program.valueholder.pathology.PathologySlide;
import org.openelisglobal.program.valueholder.pathology.PathologyStageEvent;
import org.openelisglobal.program.valueholder.pathology.PathologyTechnique;
import org.openelisglobal.program.valueholder.pathology.PathologyTechnique.TechniqueType;
import org.openelisglobal.program.valueholder.pathology.RequestKind;
import org.openelisglobal.program.valueholder.pathology.RequestPriority;
import org.openelisglobal.program.valueholder.pathology.StageDirection;
import org.openelisglobal.program.valueholder.pathology.StainStatus;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.referencetables.valueholder.ReferenceTables;
import org.openelisglobal.result.action.util.ResultSet;
import org.openelisglobal.result.action.util.ResultsLoadUtility;
import org.openelisglobal.result.action.util.ResultsUpdateDataSet;
import org.openelisglobal.result.service.LogbookResultsPersistService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl.ResultType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PathologySampleServiceImpl extends AuditableBaseObjectServiceImpl<PathologySample, Integer>
        implements PathologySampleService {

    /** reference_tables rows the block and slide audit entries point at. */
    private static final String BLOCK_REFERENCE_TABLE = "PATHOLOGY_BLOCK";

    private static final String SLIDE_REFERENCE_TABLE = "PATHOLOGY_SLIDE";

    /**
     * reference_tables row the case's own audit entries, stage moves among them,
     * point at.
     */
    private static final String CASE_REFERENCE_TABLE = "PATHOLOGY_SAMPLE";

    /** The narrative conclusion's column holds this many characters. */
    private static final int CONCLUSION_TEXT_MAX_LENGTH = 255;

    /** reference_tables row a bench request's own audit entries point at. */
    private static final String REQUEST_REFERENCE_TABLE = "PATHOLOGY_REQUEST";

    /** A slide cut in a save, named by its block and its designation under it. */
    private record SlideCut(Integer blockId, String designation) {
    }

    private static final ObjectMapper AUDIT_PAYLOAD_MAPPER = new ObjectMapper();

    @Autowired
    protected PathologySampleDAO baseObjectDAO;

    @Autowired
    private PathologyBlockDAO pathologyBlockDAO;

    @Autowired
    private PathologySlideDAO pathologySlideDAO;

    @Autowired
    private PathologyStageEventDAO pathologyStageEventDAO;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ReferenceTablesService referenceTablesService;

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    protected SystemUserService systemUserService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private LogbookResultsPersistService logbookResultsPersistService;

    @Autowired
    private TestService testService;

    @Autowired
    private NoteService noteService;

    @Autowired
    private ImmunohistochemistrySampleService immunohistochemistrySampleService;

    @Autowired
    private TestSectionService testSectionService;

    @Autowired
    private BarcodeInfoService barcodeInfoService;

    @Autowired
    private BarcodeWorkflowPrintService barcodeWorkflowPrintService;

    PathologySampleServiceImpl() {
        super(PathologySample.class);
        this.auditTrailLog = true;
    }

    @Override
    protected PathologySampleDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    public List<PathologySample> getWithStatus(List<PathologyStatus> statuses) {
        return baseObjectDAO.getWithStatus(statuses);
    }

    @Transactional
    @Override
    public void assignTechnician(Integer pathologySampleId, SystemUser systemUser, String curUserId) {
        PathologySample pathologySample = copyPathologySample(get(pathologySampleId));
        pathologySample.setTechnician(systemUser);
        pathologySample.setSysUserId(curUserId);
        update(pathologySample);
    }

    @Transactional
    @Override
    public void assignPathologist(Integer pathologySampleId, SystemUser systemUser, String curUserId) {
        PathologySample pathologySample = copyPathologySample(get(pathologySampleId));
        pathologySample.setPathologist(systemUser);
        pathologySample.setSysUserId(curUserId);
        update(pathologySample);
    }

    @Override
    public Long getCountWithStatus(List<PathologyStatus> statuses) {
        return baseObjectDAO.getCountWithStatus(statuses);
    }

    @Override
    public Long getCountWithOpenRequests() {
        return baseObjectDAO.getCountWithOpenRequests();
    }

    @Transactional
    @Override
    public List<PathologySlide> getSlides(Integer pathologySampleId) {
        return baseObjectDAO.get(pathologySampleId)
                .map(pathologySample -> new ArrayList<>(PathologyRowOrder.slides(pathologySample.getSlides())))
                .orElseGet(ArrayList::new);
    }

    private PathologySample copyPathologySample(PathologySample oldPathologySample) {
        PathologySample pathologySample = new PathologySample();
        pathologySample.setBlocks(new ArrayList<>(oldPathologySample.getBlocks()));
        pathologySample.setConclusions(new ArrayList<>(oldPathologySample.getConclusions()));
        pathologySample.setDecalcificationRequired(oldPathologySample.isDecalcificationRequired());
        pathologySample.setGrossExam(oldPathologySample.getGrossExam());
        pathologySample.setId(oldPathologySample.getId());
        pathologySample.setLastupdated(oldPathologySample.getLastupdated());
        pathologySample.setMicroscopyExam(oldPathologySample.getMicroscopyExam());
        pathologySample.setPathologist(oldPathologySample.getPathologist());
        pathologySample.setProgram(oldPathologySample.getProgram());
        pathologySample.setQuestionnaireResponseUuid(oldPathologySample.getQuestionnaireResponseUuid());
        pathologySample.setReports(new ArrayList<>(oldPathologySample.getReports()));
        pathologySample.setRequests(new ArrayList<>(oldPathologySample.getRequests()));
        pathologySample.setReviewNotes(oldPathologySample.getReviewNotes());
        pathologySample.setSample(oldPathologySample.getSample());
        pathologySample.setSlides(new ArrayList<>(oldPathologySample.getSlides()));
        pathologySample.setStatus(oldPathologySample.getStatus());
        pathologySample.setTechnician(oldPathologySample.getTechnician());
        pathologySample.setTechniques(new ArrayList<>(oldPathologySample.getTechniques()));
        return pathologySample;
    }

    /**
     * The case as stored, for the audit trail to compare a save against. A save
     * edits the stored cassettes and slides in place, so they are copied row by row
     * here; comparing against the rows themselves would find a moved cassette
     * unchanged.
     */
    private PathologySample auditedStateOf(PathologySample storedCase) {
        PathologySample storedState = copyPathologySample(storedCase);
        storedState.setBlocks(storedCase.getBlocks().stream().map(PathologySampleServiceImpl::copyOfBlock)
                .collect(Collectors.toList()));
        storedState.setSlides(storedCase.getSlides().stream().map(PathologySampleServiceImpl::copyOfSlide)
                .collect(Collectors.toList()));
        storedState.setRequests(storedCase.getRequests().stream().map(PathologySampleServiceImpl::copyOfRequest)
                .collect(Collectors.toList()));
        return storedState;
    }

    /** Every field the case's audit names a request by. */
    private static PathologyRequest copyOfRequest(PathologyRequest stored) {
        PathologyRequest request = new PathologyRequest();
        request.setId(stored.getId());
        request.setStatus(stored.getStatus());
        request.setType(stored.getType());
        request.setValue(stored.getValue());
        request.setKind(stored.getKind());
        request.setTargetBlockId(stored.getTargetBlockId());
        request.setTargetSlideId(stored.getTargetSlideId());
        request.setStainId(stored.getStainId());
        request.setPriority(stored.getPriority());
        request.setResultBlockId(stored.getResultBlockId());
        request.setResultSlideId(stored.getResultSlideId());
        request.setCancelReason(stored.getCancelReason());
        request.setRaisedAt(stored.getRaisedAt());
        request.setRaisedBy(stored.getRaisedBy());
        request.setLastupdated(stored.getLastupdated());
        return request;
    }

    private static PathologyBlock copyOfBlock(PathologyBlock stored) {
        PathologyBlock block = new PathologyBlock();
        block.setId(stored.getId());
        block.setBlockNumber(stored.getBlockNumber());
        block.setDesignation(stored.getDesignation());
        block.setBarcode(stored.getBarcode());
        block.setCassetteState(stored.getCassetteState());
        block.setTissueTypeId(stored.getTissueTypeId());
        block.setPartDesignation(stored.getPartDesignation());
        block.setActive(stored.isActive());
        block.setLocation(stored.getLocation());
        block.setLastupdated(stored.getLastupdated());
        return block;
    }

    private static PathologySlide copyOfSlide(PathologySlide stored) {
        PathologySlide slide = new PathologySlide();
        slide.setId(stored.getId());
        slide.setSlideNumber(stored.getSlideNumber());
        slide.setBlockId(stored.getBlockId());
        slide.setDesignation(stored.getDesignation());
        slide.setBarcode(stored.getBarcode());
        slide.setActive(stored.isActive());
        slide.setLocation(stored.getLocation());
        slide.setFileType(stored.getFileType());
        slide.setLevel(stored.getLevel());
        slide.setStainId(stored.getStainId());
        slide.setStainStatus(stored.getStainStatus());
        slide.setStainStatusAt(stored.getStainStatusAt());
        slide.setStainStatusBy(stored.getStainStatusBy());
        slide.setCoverslipQc(stored.getCoverslipQc());
        slide.setCoverslipQcAt(stored.getCoverslipQcAt());
        slide.setCoverslipQcBy(stored.getCoverslipQcBy());
        slide.setLabelVerifiedAt(stored.getLabelVerifiedAt());
        slide.setLabelVerifiedBy(stored.getLabelVerifiedBy());
        slide.setLastupdated(stored.getLastupdated());
        return slide;
    }

    @Transactional
    @Override
    public void updateWithFormValues(Integer pathologySampleId, PathologySampleForm form) {
        PathologySample storedCase = get(pathologySampleId);
        if (form.getSlides() != null) {
            form.getSlides().forEach(upload -> {
                if (!storedCase.getSlides().stream().anyMatch(stored -> ClinicalAttachmentService.isUnchanged(upload,
                        stored.getId(), stored.getImage(), stored.getFileType()))) {
                    ClinicalAttachmentService.validate(upload);
                }
            });
        }
        if (form.getReports() != null) {
            form.getReports().forEach(upload -> {
                if (!storedCase.getReports().stream().anyMatch(stored -> ClinicalAttachmentService.isUnchanged(upload,
                        stored.getId(), stored.getImage(), stored.getFileType()))) {
                    ClinicalAttachmentService.validate(upload);
                }
            });
        }
        if (form.getRelease()) {
            refuseReleaseOutsideReview(storedCase);
        }
        // Worked out against the stored case, before the copy.
        ReviewChanges changes = reviewChangesOf(storedCase, form);
        refuseReviewChanges(storedCase, changes, form);
        // Copy only after validating all new or changed attachments.
        PathologySample pathologySample = copyPathologySample(storedCase);
        PathologySample storedState = auditedStateOf(storedCase);
        pathologySample.setSysUserId(form.getSystemUserId());
        if (changes.pathologist()) {
            pathologySample.setPathologist(systemUserService.get(form.getAssignedPathologistId()));
        }
        if (!GenericValidator.isBlankOrNull(form.getAssignedTechnicianId())) {
            pathologySample.setTechnician(systemUserService.get(form.getAssignedTechnicianId()));
        }
        DesignationScheme scheme = DesignationScheme.fromConfiguration();
        String accessionNumber = pathologySample.getSample() == null ? null
                : pathologySample.getSample().getAccessionNumber();
        List<String> cassettesCut = reconcileBlocks(pathologySample, form.getBlocks(), scheme, accessionNumber);
        List<SlideCut> slidesCut = reconcileSlides(pathologySample, form.getSlides(), scheme, accessionNumber);
        if (changes.grossExam()) {
            pathologySample.setGrossExam(form.getGrossExam());
        }
        // The flag records what grossing found. Past the stage an untick would make a
        // decalcified specimen read as one that never needed it, so it is no longer
        // taken.
        if (form.getDecalcificationRequired() != null && !pastDecalcification(storedCase)) {
            pathologySample.setDecalcificationRequired(form.getDecalcificationRequired());
        }
        if (form.getRunReference() != null) {
            recordRunReference(storedCase, PathologyStatus.PROCESSING, form.getRunReference(),
                    pathologySample.getSysUserId());
        }
        if (form.getCoverslipRunReference() != null) {
            recordRunReference(storedCase, PathologyStatus.COVERSLIPPING, form.getCoverslipRunReference(),
                    pathologySample.getSysUserId());
        }
        if (changes.microscopyExam()) {
            pathologySample.setMicroscopyExam(form.getMicroscopyExam());
        }
        if (changes.reviewNotes()) {
            pathologySample.setReviewNotes(form.getReviewNotes());
        }
        // Conclusions and techniques are rewritten only when they changed, so
        // unchanged rows keep their ids.
        List<String> codesBefore = conclusionCodes(storedCase.getConclusions());
        List<String> codesAfter = changes.conclusions() == null ? codesBefore : changes.conclusions();
        if (changes.narrative() || changes.conclusions() != null) {
            String narrative = changes.narrative() ? form.getConclusionText() : storedNarrative(storedCase);
            pathologySample.getConclusions().clear();
            pathologySample.getConclusions()
                    .add(createConclusion(StringUtils.defaultString(narrative), ConclusionType.TEXT));
            codesAfter.forEach(
                    code -> pathologySample.getConclusions().add(createConclusion(code, ConclusionType.DICTIONARY)));
        }
        // Requests are raised and change status through their own calls, so a save
        // carries the case's requests as they stand and never writes them.
        if (changes.techniques() != null) {
            pathologySample.getTechniques().clear();
            changes.techniques().forEach(
                    code -> pathologySample.getTechniques().add(createTechnique(code, TechniqueType.DICTIONARY)));
        }
        pathologySample.getReports().removeAll(pathologySample.getReports());
        if (form.getReports() != null)
            form.getReports().stream().forEach(e -> e.setId(null));
        pathologySample.getReports().addAll(form.getReports());
        if (form.getRelease()) {
            refuseReleaseWithoutDescriptions(pathologySample);
        }
        Optional<Referral> referral = changes.referral() == null ? Optional.empty()
                : referToImmunoHistoChemistry(pathologySample, changes.referral(), form.getSystemUserId());
        if (form.getRelease()) {
            if (storedCase.getStatus() == PathologyStatus.COMPLETED) {
                // Already at Completed, so there is no stage to enter; the release is
                // still the sign-out.
                recordMove(storedCase, PathologyStatus.COMPLETED, StageDirection.FORWARD, null,
                        pathologySample.getSysUserId());
            } else {
                applyTransition(pathologySample, storedCase, PathologyStatus.COMPLETED, StageDirection.FORWARD, null,
                        pathologySample.getSysUserId());
            }
            validatePathologySample(pathologySample, form);
        }
        try {
            baseObjectDAO.evict(storedCase);
            PathologySample savedCase = updateAgainst(pathologySample, storedState);
            recordCassettesCut(savedCase, cassettesCut, pathologySample.getSysUserId());
            recordSlidesCut(savedCase, slidesCut, pathologySample.getSysUserId());
            if (changes.narrative() || changes.conclusions() != null) {
                recordConclusionSaved(savedCase, codesBefore, codesAfter, changes.narrative(),
                        pathologySample.getSysUserId());
            }
            referral.ifPresent(made -> recordReferral(savedCase, made, pathologySample.getSysUserId()));
            persistPathologyBarcodeCountsIfSupplied(pathologySample, form);
            populatePathologyWorkflowPrintModels(pathologySample, form);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw e;
        }
    }

    @Transactional
    @Override
    public Optional<PathologyBlock> deactivateBlock(Integer blockId, String reason, String sysUserId) {
        Optional<PathologyBlock> found = pathologyBlockDAO.get(blockId);
        if (found.isEmpty()) {
            return found;
        }
        if (!found.get().isActive()) {
            return found;
        }
        refuseOnACompletedCase(baseObjectDAO.getCaseOfBlock(blockId));

        PathologyBlock block = found.get();
        block.setActive(false);
        block.setSysUserId(sysUserId);
        pathologyBlockDAO.update(block);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("verb", PATHOLOGY_BLOCK_DEACTIVATED);
        putIfPresent(payload, "designation", block.displayIdentifier());
        putIfPresent(payload, "reason", reason);
        recordRowEvent(BLOCK_REFERENCE_TABLE, block.getStringId(), IActionConstants.AUDIT_TRAIL_UPDATE, payload,
                sysUserId);
        return Optional.of(block);
    }

    @Transactional
    @Override
    public Optional<PathologyBlock> embedBlock(Integer blockId, String sysUserId) {
        Optional<PathologyBlock> found = pathologyBlockDAO.get(blockId);
        if (found.isEmpty()) {
            return found;
        }
        PathologyBlock block = found.get();
        if (!block.isActive()) {
            throw new PathologyCaseRuleException("block " + blockId + " has been retired, so it cannot be embedded");
        }
        if (block.getCassetteState() == CassetteState.BLOCK) {
            return found;
        }
        refuseEmbeddingOutsideTheStage(baseObjectDAO.getCaseOfBlock(blockId));

        CassetteState from = block.getCassetteState() == null ? CassetteState.CASSETTE : block.getCassetteState();
        block.setCassetteState(CassetteState.BLOCK);
        block.setSysUserId(sysUserId);
        pathologyBlockDAO.update(block);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("verb", PATHOLOGY_BLOCK_EMBEDDED);
        putIfPresent(payload, "designation", block.displayIdentifier());
        payload.put("from", from.name());
        payload.put("to", CassetteState.BLOCK.name());
        recordRowEvent(BLOCK_REFERENCE_TABLE, block.getStringId(), IActionConstants.AUDIT_TRAIL_UPDATE, payload,
                sysUserId);
        return Optional.of(block);
    }

    @Transactional
    @Override
    public Optional<LabelVerification> verifySlideLabel(Integer slideId, String block, String slide, String sysUserId) {
        Optional<PathologySlide> found = pathologySlideDAO.get(slideId);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        String scannedBlock = StringUtils.trimToNull(block);
        String scannedSlide = StringUtils.trimToNull(slide);
        if (scannedBlock == null || scannedSlide == null) {
            throw new LocalizedValidationException("pathology.error.scanBothFields",
                    "a slide label is checked with both the block and the slide label scanned or typed");
        }
        PathologySlide stored = found.get();
        if (!stored.isActive()) {
            throw new LocalizedValidationException("pathology.error.slideRetired",
                    "slide " + slideId + " has been deactivated, so its label is not checked");
        }
        if (stored.getBlockId() == null) {
            throw new LocalizedValidationException("pathology.error.slideHasNoBlock",
                    "slide " + slideId + " has no block on record, so its label cannot be checked against one");
        }
        PathologySample owningCase = baseObjectDAO.getCaseOfSlide(slideId);
        refuseOnACompletedCase(owningCase);

        boolean scanRequired = scanVerificationRequired();
        String mode = scanRequired ? "scan" : "confirm";
        DesignationScheme scheme = DesignationScheme.fromConfiguration();
        PathologyBlock parentBlock = pathologyBlockDAO.get(stored.getBlockId()).orElse(null);
        boolean blockMatches = parentBlock != null && (sameIdentifier(scannedBlock, parentBlock.getBarcode())
                || (!scanRequired && sameIdentifier(scannedBlock, parentBlock.displayIdentifier())));
        boolean slideMatches = sameIdentifier(scannedSlide, stored.getBarcode()) || (!scanRequired
                && sameIdentifier(scannedSlide, PathologyDesignations.slideOnBlockName(parentBlock, stored, scheme)));

        if (!(blockMatches && slideMatches)) {
            Map<String, Object> payload = labelCheckPayload(PATHOLOGY_SLIDE_LABEL_MISMATCH, scannedBlock, scannedSlide);
            payload.put("mode", mode);
            recordRowEvent(SLIDE_REFERENCE_TABLE, stored.getStringId(), IActionConstants.AUDIT_TRAIL_UPDATE, payload,
                    sysUserId);
            return Optional.of(new LabelVerification(stored, false));
        }
        if (stored.getLabelVerifiedAt() != null) {
            return Optional.of(new LabelVerification(stored, true));
        }

        // Taken before the stamp, so the case's own audit trail records it as the
        // slide's change; the slide is saved on its own, so no save of the case
        // would.
        PathologySample storedState = owningCase == null ? null : auditedStateOf(owningCase);
        stored.setLabelVerifiedAt(new Timestamp(System.currentTimeMillis()));
        stored.setLabelVerifiedBy(sysUserId);
        stored.setSysUserId(sysUserId);
        pathologySlideDAO.update(stored);
        if (owningCase != null) {
            saveHistoryAgainst(owningCase, storedState, sysUserId);
        }

        Map<String, Object> payload = labelCheckPayload(PATHOLOGY_SLIDE_LABEL_VERIFIED, scannedBlock, scannedSlide);
        payload.put("match", true);
        payload.put("mode", mode);
        recordRowEvent(SLIDE_REFERENCE_TABLE, stored.getStringId(), IActionConstants.AUDIT_TRAIL_UPDATE, payload,
                sysUserId);
        return Optional.of(new LabelVerification(stored, true));
    }

    /** The part of a label check's audit entry both outcomes share. */
    private static Map<String, Object> labelCheckPayload(String verb, String scannedBlock, String scannedSlide) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("verb", verb);
        payload.put("blockBarcode", scannedBlock);
        payload.put("slideBarcode", scannedSlide);
        return payload;
    }

    /** Scanners and typists differ in case and stray spaces, never in content. */
    private static boolean sameIdentifier(String given, String expected) {
        return StringUtils.isNotBlank(expected)
                && StringUtils.equalsIgnoreCase(StringUtils.trim(given), StringUtils.trim(expected));
    }

    @Transactional
    @Override
    public List<PathologySlide> recordStainStatus(Integer pathologySampleId, List<Integer> slideIds, StainStatus status,
            String sysUserId) {
        refuseAnEmptyRequest(slideIds, status);
        PathologySample storedCase = get(pathologySampleId);
        List<PathologySlide> named = slidesToMark(storedCase, slideIds, PathologyStatus.STAINING,
                "pathology.error.stainBeforeStage");
        if (status == StainStatus.STAINED) {
            refuseSlides(
                    named.stream().filter(slide -> StringUtils.isBlank(slide.getStainId()))
                            .collect(Collectors.toList()),
                    storedCase, "pathology.error.stainMissing",
                    "a slide is marked stained only once the stain on it is recorded");
        }
        return markSlides(storedCase, named, slide -> slide.getStainStatus() == status, (slide, now) -> {
            slide.setStainStatus(status);
            slide.setStainStatusAt(now);
            slide.setStainStatusBy(sysUserId);
            // A slide restained, or found unsatisfactory, is coverslipped and checked
            // again, so an earlier check no longer speaks for it.
            slide.setCoverslipQc(null);
            slide.setCoverslipQcAt(null);
            slide.setCoverslipQcBy(null);
        }, slide -> {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("verb", PATHOLOGY_SLIDE_STAINED);
            putIfPresent(payload, "stainId", slide.getStainId());
            payload.put("status", status.name());
            return payload;
        }, sysUserId);
    }

    @Transactional
    @Override
    public List<PathologySlide> recordCoverslipQc(Integer pathologySampleId, List<Integer> slideIds, CoverslipQc result,
            String sysUserId) {
        refuseAnEmptyRequest(slideIds, result);
        PathologySample storedCase = get(pathologySampleId);
        List<PathologySlide> named = slidesToMark(storedCase, slideIds, PathologyStatus.COVERSLIPPING,
                "pathology.error.qcBeforeStage");
        if (deploymentEnabled().test(PathologyStatus.STAINING)) {
            refuseSlides(named.stream().filter(slide -> !isStained(slide)).collect(Collectors.toList()), storedCase,
                    "pathology.error.qcBeforeStain",
                    "a slide's coverslip is checked only once the slide is marked stained");
        }
        return markSlides(storedCase, named, slide -> slide.getCoverslipQc() == result, (slide, now) -> {
            slide.setCoverslipQc(result);
            slide.setCoverslipQcAt(now);
            slide.setCoverslipQcBy(sysUserId);
        }, slide -> {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("verb", PATHOLOGY_SLIDE_QC_RECORDED);
            payload.put("result", result.name());
            return payload;
        }, sysUserId);
    }

    /**
     * A request that names no slide, or does not say what was found, is refused.
     */
    private static void refuseAnEmptyRequest(List<Integer> slideIds, Object value) {
        if (value == null || slideIds == null || slideIds.isEmpty() || slideIds.stream().anyMatch(Objects::isNull)) {
            throw new LocalizedValidationException("pathology.error.slideSelectionRequired",
                    "name at least one slide and what was found for it");
        }
    }

    /**
     * Records a mark on each named slide whose stored value differs, with the
     * server clock and the session user, and audits it.
     *
     * <p>
     * The snapshot is taken before any write, so the case's own audit trail records
     * the batch as one change carrying the values it replaced. A slide already
     * holding the value is left alone.
     *
     * <p>
     * A mark and a stage move write different rows, a slide and the case, so on
     * their own both could commit: a slide found unsatisfactory as the case is sent
     * on would reach the pathologist with nothing holding it. The case's version is
     * therefore raised, and its row locked, before any slide is written, and of a
     * mark and a move or a save landing together one loses its version check.
     */
    private List<PathologySlide> markSlides(PathologySample storedCase, List<PathologySlide> named,
            Predicate<PathologySlide> alreadyHolds, BiConsumer<PathologySlide, Timestamp> mark,
            Function<PathologySlide, Map<String, Object>> payloadOf, String sysUserId) {
        if (named.stream().allMatch(alreadyHolds)) {
            // Nothing to record, so nothing is locked or written.
            return named;
        }
        // The case row is taken before any slide row, the order a save of the case
        // takes them in, so a mark and a save wait for each other rather than
        // deadlock; the version raised here makes whichever lands second lose.
        baseObjectDAO.forceVersionIncrement(storedCase);
        PathologySample storedState = auditedStateOf(storedCase);
        Timestamp now = new Timestamp(System.currentTimeMillis());
        List<PathologySlide> changed = new ArrayList<>();
        for (PathologySlide slide : named) {
            if (alreadyHolds.test(slide)) {
                continue;
            }
            mark.accept(slide, now);
            slide.setSysUserId(sysUserId);
            pathologySlideDAO.update(slide);
            changed.add(slide);
        }
        saveHistoryAgainst(storedCase, storedState, sysUserId);
        for (PathologySlide slide : changed) {
            recordRowEvent(SLIDE_REFERENCE_TABLE, slide.getStringId(), IActionConstants.AUDIT_TRAIL_UPDATE,
                    payloadOf.apply(slide), sysUserId);
        }
        return named;
    }

    /**
     * The slides a stain status or coverslip check is recorded on, after the
     * refusals every slide shares, in this order: a signed-out case, a case not yet
     * at the stage, a case already with the pathologist, a slide that is not on the
     * case, a retired slide. Nothing is written before all of them pass, so a batch
     * is recorded whole or not at all.
     *
     * <p>
     * Marks are taken only while the case is on the bench, from the stage up to
     * Coverslipping. With the pathologist, a slide found unreadable sends the case
     * back to the bench with the return's reason; a mark there would change a slide
     * under a case nothing holds any longer.
     *
     * @return the named slides in the order the case view lists them, each once
     */
    private List<PathologySlide> slidesToMark(PathologySample storedCase, List<Integer> slideIds,
            PathologyStatus firstStage, String beforeStageMessageKey) {
        refuseOutsideTheStage(storedCase, firstStage, beforeStageMessageKey);
        PathologyStatus stage = storedCase.getStatus();
        if (stage.ordinal() > PathologyStatus.COVERSLIPPING.ordinal()) {
            throw new LocalizedValidationException("pathology.error.slideWorkAfterBench", "pathology case "
                    + storedCase.getId() + " stands at " + stage + ", so it is returned to the bench to be changed");
        }
        List<PathologySlide> slidesOnCase = storedCase.getSlides() == null ? List.of() : storedCase.getSlides();
        LinkedHashSet<Integer> wanted = new LinkedHashSet<>(slideIds);
        for (Integer slideId : wanted) {
            if (slidesOnCase.stream().noneMatch(slide -> slideId.equals(slide.getId()))) {
                throw new LocalizedValidationException("pathology.error.slideNotOnCase",
                        "slide " + slideId + " is not on pathology case " + storedCase.getId());
            }
        }
        List<PathologySlide> named = PathologyRowOrder.slides(slidesOnCase).stream()
                .filter(slide -> wanted.contains(slide.getId())).collect(Collectors.toList());
        for (PathologySlide slide : named) {
            if (!slide.isActive()) {
                throw new LocalizedValidationException("pathology.error.slideRetired",
                        "slide " + slide.getId() + " has been deactivated, so nothing more is recorded on it");
            }
        }
        return named;
    }

    private static boolean isStained(PathologySlide slide) {
        return slide.getStainStatus() == StainStatus.STAINED;
    }

    /** Refuses the whole request, naming every slide that stands in its way. */
    private static void refuseSlides(List<PathologySlide> refused, PathologySample storedCase, String messageKey,
            String rule) {
        if (refused.isEmpty()) {
            return;
        }
        String designations = slideNames(refused, storedCase, DesignationScheme.fromConfiguration());
        throw new LocalizedValidationException(messageKey,
                "pathology case " + storedCase.getId() + ": " + rule + ": " + designations,
                Map.of("designations", designations));
    }

    @Transactional
    @Override
    public PathologySample getCaseOfBlock(Integer blockId) {
        return blockId == null ? null : baseObjectDAO.getCaseOfBlock(blockId);
    }

    @Transactional
    @Override
    public PathologySample getCaseOfSlide(Integer slideId) {
        return slideId == null ? null : baseObjectDAO.getCaseOfSlide(slideId);
    }

    @Transactional
    @Override
    public PathologyStageEvent advance(Integer pathologySampleId, PathologyStatus from, String sysUserId) {
        return advance(pathologySampleId, from, sysUserId, List.of());
    }

    @Transactional
    @Override
    public PathologyStageEvent advance(Integer pathologySampleId, PathologyStatus from, String sysUserId,
            List<Integer> acknowledgedRequestIds) {
        PathologySample storedCase = get(pathologySampleId);
        requireStoredStage(storedCase, from);
        refuseOnACompletedCase(storedCase);
        PathologyStatus next = nextStage(storedCase)
                .orElseThrow(() -> new LocalizedValidationException("pathology.error.advanceBeyondBench",
                        "the pathologist signs the case out from " + storedCase.getStatus()));
        refuseAnUnfinishedStage(storedCase);
        if (next == PathologyStatus.READY_PATHOLOGIST) {
            overrideOpenRequests(storedCase, acknowledgedRequestIds, sysUserId);
        }
        // Whoever begins the review is the one reading the case, unless it is
        // already assigned (FR-12.1).
        Consumer<PathologySample> takeTheCase = next == PathologyStatus.UNDER_REVIEW
                && storedCase.getPathologist() == null
                        ? movedCase -> movedCase.setPathologist(systemUserService.get(sysUserId))
                        : movedCase -> {
                        };
        return move(storedCase, next, StageDirection.FORWARD, null, sysUserId, takeTheCase);
    }

    /**
     * A case goes to the pathologist past open requests only once the sender has
     * seen each of them, by id, so a request raised behind an open page is shown
     * before it is passed over. An id that is not open is ignored. The requests
     * passed over are recorded on the case, ahead of the move's own entry.
     */
    private void overrideOpenRequests(PathologySample storedCase, List<Integer> acknowledgedRequestIds,
            String sysUserId) {
        List<PathologyRequest> open = openRequests(storedCase);
        if (open.isEmpty()) {
            return;
        }
        List<Integer> acknowledged = acknowledgedRequestIds == null ? List.of() : acknowledgedRequestIds;
        if (!open.stream().map(PathologyRequest::getId).allMatch(acknowledged::contains)) {
            String instructions = open.stream().map(request -> request.instruction(dictionaryService))
                    .collect(Collectors.joining(", "));
            throw new LocalizedValidationException("pathology.error.openRequestsUnacknowledged",
                    "pathology case " + storedCase.getId() + " has open requests: " + instructions,
                    Map.of("instructions", instructions));
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("verb", PATHOLOGY_REQUEST_OVERRIDDEN);
        payload.put("requestIds", open.stream().map(PathologyRequest::getId).collect(Collectors.toList()));
        recordRowEvent(CASE_REFERENCE_TABLE, storedCase.getStringId(), IActionConstants.AUDIT_TRAIL_UPDATE, payload,
                sysUserId);
    }

    /** The case's open requests, newest first. */
    private static List<PathologyRequest> openRequests(PathologySample storedCase) {
        return storedCase.getRequests() == null ? List.of()
                : PathologyRowOrder.requests(storedCase.getRequests()).stream()
                        .filter(request -> request.getStatus() == RequestStatus.OPENED).collect(Collectors.toList());
    }

    @Transactional
    @Override
    public PathologyStageEvent returnToPreviousStage(Integer pathologySampleId, PathologyStatus from, String reason,
            String sysUserId) {
        PathologySample storedCase = get(pathologySampleId);
        requireStoredStage(storedCase, from);
        refuseOnACompletedCase(storedCase);
        PathologyStatus previous = previousStage(storedCase)
                .orElseThrow(() -> new LocalizedValidationException("pathology.error.nothingToReturnTo",
                        "there is no earlier stage to return pathology case " + pathologySampleId + " to"));
        String trimmedReason = StringUtils.trimToNull(reason);
        if (trimmedReason == null) {
            throw new LocalizedValidationException("pathology.error.returnReasonRequired",
                    "a case is returned to an earlier stage only with a reason");
        }
        if (trimmedReason.length() > PathologyStageEvent.REASON_MAX_LENGTH) {
            throw new LocalizedValidationException("pathology.error.reasonTooLong",
                    "the reason can be at most " + PathologyStageEvent.REASON_MAX_LENGTH + " characters");
        }
        return move(storedCase, previous, StageDirection.RETURN, trimmedReason, sysUserId);
    }

    // ---- bench requests (FR-10) ----

    /**
     * The refusals a block or slide a request names answers with, by what it is
     * named as: the target a request asks about, or the row that fulfilled it.
     */
    private record RowRefusals(String notOnCase, String retired) {
    }

    private static final RowRefusals TARGET_REFUSALS = new RowRefusals("pathology.error.requestTargetNotOnCase",
            "pathology.error.requestTargetRetired");

    private static final RowRefusals RESULT_REFUSALS = new RowRefusals("pathology.error.requestResultNotOnCase",
            "pathology.error.requestResultRetired");

    @Transactional
    @Override
    public PathologyRequest raiseRequest(Integer pathologySampleId, RequestDraft draft, String sysUserId) {
        RequestKind kind = EnumNames.valueNamed(RequestKind.class, draft == null ? null : draft.kind());
        if (kind == null) {
            throw new LocalizedValidationException("pathology.error.requestKindRequired",
                    "a request asks for an additional block, slide or stain");
        }
        String instruction = StringUtils.trimToNull(draft.instruction());
        if (instruction == null) {
            throw new LocalizedValidationException("pathology.error.requestInstructionRequired",
                    "a request says what the bench is asked to do");
        }
        if (instruction.length() > PathologyRequest.INSTRUCTION_MAX_LENGTH) {
            throw new LocalizedValidationException("pathology.error.requestInstructionTooLong",
                    "the instruction can be at most " + PathologyRequest.INSTRUCTION_MAX_LENGTH + " characters");
        }
        RequestPriority priority = RequestPriority.NORMAL;
        if (StringUtils.isNotBlank(draft.priority())) {
            priority = EnumNames.valueNamed(RequestPriority.class, draft.priority());
            if (priority == null) {
                throw new LocalizedValidationException("pathology.error.requestPriorityUnknown",
                        "the priority " + draft.priority() + " is not one a request takes");
            }
        }
        PathologySample storedCase = get(pathologySampleId);
        refuseOnACompletedCase(storedCase);

        PathologyRequest request = new PathologyRequest();
        request.setKind(kind);
        request.setPriority(priority);
        // A target or stain the kind does not take is not what was asked for, so it
        // is dropped rather than stored beside the request.
        if (kind == RequestKind.SLIDE) {
            request.setTargetBlockId(blockOfCaseInUse(storedCase,
                    targetRequired(draft.targetBlockId(), "a slide request names its block"), TARGET_REFUSALS).getId());
        } else if (kind == RequestKind.STAIN) {
            request.setTargetSlideId(slideOfCaseInUse(storedCase,
                    targetRequired(draft.targetSlideId(), "a stain request names its slide"), TARGET_REFUSALS).getId());
            request.setStainId(stainInUse(draft.stainId()));
        }

        // The case row is taken first, as every other writer of the case takes it, so
        // a send or a save that read the case before this request existed loses its
        // version check rather than passing the request over or taking it off the
        // case.
        baseObjectDAO.forceVersionIncrement(storedCase);
        PathologySample storedState = auditedStateOf(storedCase);
        request.setStatus(RequestStatus.OPENED);
        request.setType(RequestType.TEXT);
        request.setValue(instruction);
        request.setRaisedAt(new Timestamp(System.currentTimeMillis()));
        request.setRaisedBy(sysUserId);
        request.setSysUserId(sysUserId);
        Set<Integer> requestsBefore = storedCase.getRequests().stream().map(PathologyRequest::getId)
                .collect(Collectors.toSet());
        storedCase.getRequests().add(request);
        storedCase.setSysUserId(sysUserId);
        // Flushed here so the new row has its id before anything is audited; the
        // merge may hold it as a copy of the instance added, so it is found by id.
        PathologySample savedCase = baseObjectDAO.update(storedCase);
        PathologyRequest raised = savedCase.getRequests().stream()
                .filter(saved -> !requestsBefore.contains(saved.getId())).findFirst()
                .orElseThrow(() -> new LIMSRuntimeException(
                        "the request raised on pathology case " + pathologySampleId + " was not saved with it"));
        saveHistoryAgainst(savedCase, storedState, sysUserId);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("verb", PATHOLOGY_REQUEST_RAISED);
        payload.put("kind", kind.name());
        payload.put("priority", priority.name());
        if (raised.getTargetBlockId() != null) {
            payload.put("targetBlockId", raised.getTargetBlockId());
            putIfPresent(payload, "target", savedCase.nameOfBlock(raised.getTargetBlockId()).orElse(null));
        }
        if (raised.getTargetSlideId() != null) {
            payload.put("targetSlideId", raised.getTargetSlideId());
            putIfPresent(payload, "target", savedCase.nameOfSlide(raised.getTargetSlideId()).orElse(null));
        }
        putIfPresent(payload, "stainId", raised.getStainId());
        recordRowEvent(REQUEST_REFERENCE_TABLE, raised.getStringId(), IActionConstants.AUDIT_TRAIL_INSERT, payload,
                sysUserId);
        return raised;
    }

    @Transactional
    @Override
    public PathologyRequest changeRequestStatus(Integer pathologySampleId, Integer requestId,
            RequestStatusChange change, String sysUserId) {
        RequestStatus from = EnumNames.valueNamed(RequestStatus.class, change == null ? null : change.from());
        RequestStatus to = EnumNames.valueNamed(RequestStatus.class, change == null ? null : change.status());
        if (from == null || to == null) {
            throw new LocalizedValidationException("pathology.error.requestStatusRequired",
                    "a status change names the status the page showed and the one asked for");
        }
        String reason = null;
        if (to == RequestStatus.CANCELLED) {
            reason = StringUtils.trimToNull(change.cancelReason());
            if (reason == null) {
                throw new LocalizedValidationException("pathology.error.requestCancelReasonRequired",
                        "a request is cancelled only with a reason");
            }
            if (reason.length() > PathologyRequest.CANCEL_REASON_MAX_LENGTH) {
                throw new LocalizedValidationException("pathology.error.reasonTooLong",
                        "the reason can be at most " + PathologyRequest.CANCEL_REASON_MAX_LENGTH + " characters");
            }
        }
        PathologySample storedCase = get(pathologySampleId);
        refuseOnACompletedCase(storedCase);
        PathologyRequest request = requestId == null ? null
                : storedCase.getRequests().stream().filter(stored -> requestId.equals(stored.getId())).findFirst()
                        .orElse(null);
        if (request == null) {
            throw new LocalizedValidationException("pathology.error.requestNotOnCase",
                    "request " + requestId + " is not on pathology case " + pathologySampleId);
        }
        RequestStatus stored = request.getStatus();
        if (stored != from) {
            throw new PathologyRequestConflictException(requestId, stored, from);
        }
        if (to == stored) {
            // Nothing to record, so nothing is locked or written.
            return request;
        }
        if (to != RequestStatus.OPENED && stored != RequestStatus.OPENED) {
            throw new LocalizedValidationException("pathology.error.requestTransitionNotAllowed",
                    "request " + requestId + " is " + stored + "; it is reopened before it can be " + to);
        }
        PathologyBlock resultBlock = null;
        PathologySlide resultSlide = null;
        // A request on file has no kind, so nothing can be said to have fulfilled it.
        if (to == RequestStatus.COMPLETED && request.getKind() != null) {
            boolean blockRequest = request.getKind() == RequestKind.BLOCK;
            if ((blockRequest && change.resultSlideId() != null) || (!blockRequest && change.resultBlockId() != null)) {
                throw new LocalizedValidationException("pathology.error.requestResultWrongKind",
                        "a block request is fulfilled by a block, a slide or stain request by a slide");
            }
            if (blockRequest && change.resultBlockId() != null) {
                resultBlock = blockOfCaseInUse(storedCase, change.resultBlockId(), RESULT_REFUSALS);
            }
            if (!blockRequest && change.resultSlideId() != null) {
                resultSlide = slideOfCaseInUse(storedCase, change.resultSlideId(), RESULT_REFUSALS);
            }
        }

        baseObjectDAO.forceVersionIncrement(storedCase);
        PathologySample storedState = auditedStateOf(storedCase);
        request.setStatus(to);
        if (to == RequestStatus.OPENED) {
            request.setCancelReason(null);
            request.setResultBlockId(null);
            request.setResultSlideId(null);
        } else if (to == RequestStatus.CANCELLED) {
            request.setCancelReason(reason);
        } else {
            request.setResultBlockId(resultBlock == null ? null : resultBlock.getId());
            request.setResultSlideId(resultSlide == null ? null : resultSlide.getId());
        }
        request.setSysUserId(sysUserId);
        storedCase.setSysUserId(sysUserId);
        baseObjectDAO.update(storedCase);
        saveHistoryAgainst(storedCase, storedState, sysUserId);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("verb", PATHOLOGY_REQUEST_STATUS_CHANGED);
        payload.put("statusFrom", stored.name());
        payload.put("statusTo", to.name());
        putIfPresent(payload, "reason", reason);
        if (resultBlock != null) {
            payload.put("resultBlockId", resultBlock.getId());
            putIfPresent(payload, "result", storedCase.nameOfBlock(resultBlock.getId()).orElse(null));
        }
        if (resultSlide != null) {
            payload.put("resultSlideId", resultSlide.getId());
            putIfPresent(payload, "result", storedCase.nameOfSlide(resultSlide.getId()).orElse(null));
        }
        recordRowEvent(REQUEST_REFERENCE_TABLE, request.getStringId(), IActionConstants.AUDIT_TRAIL_UPDATE, payload,
                sysUserId);
        return request;
    }

    /**
     * The block or slide a request is about; a slide or stain request names one.
     */
    private static Integer targetRequired(Integer rowId, String rule) {
        if (rowId == null) {
            throw new LocalizedValidationException("pathology.error.requestTargetRequired", rule);
        }
        return rowId;
    }

    /** A block of the case named by a request, refused when foreign or retired. */
    private static PathologyBlock blockOfCaseInUse(PathologySample storedCase, Integer blockId, RowRefusals refusals) {
        PathologyBlock block = storedCase.blockById(blockId)
                .orElseThrow(() -> new LocalizedValidationException(refusals.notOnCase(),
                        "block " + blockId + " is not on pathology case " + storedCase.getId()));
        if (!block.isActive()) {
            throw new LocalizedValidationException(refusals.retired(),
                    "block " + blockId + " has been retired, so no request names it");
        }
        return block;
    }

    /** A slide of the case named by a request, refused when foreign or retired. */
    private static PathologySlide slideOfCaseInUse(PathologySample storedCase, Integer slideId, RowRefusals refusals) {
        PathologySlide slide = storedCase.slideById(slideId)
                .orElseThrow(() -> new LocalizedValidationException(refusals.notOnCase(),
                        "slide " + slideId + " is not on pathology case " + storedCase.getId()));
        if (!slide.isActive()) {
            throw new LocalizedValidationException(refusals.retired(),
                    "slide " + slideId + " has been retired, so no request names it");
        }
        return slide;
    }

    /**
     * The stain a stain request asks for: an entry of the stain list still in use.
     * Unlike a slide's stain on a case save, an id off the list is answered with
     * its own message, since the request has no reload to fall back on.
     */
    private String stainInUse(String postedStainId) {
        String stainId = StringUtils.trimToNull(postedStainId);
        if (stainId == null) {
            throw new LocalizedValidationException("pathology.error.requestStainRequired",
                    "a stain request names the stain");
        }
        refuseUnlessInUse(stainId, STAIN_CATEGORY,
                rule -> new LocalizedValidationException("pathology.error.requestStainNotOnList", rule),
                "pathology.error.requestStainRetired");
        return stainId;
    }

    @Override
    public Optional<PathologyStatus> nextStage(PathologySample pathologySample) {
        return PathologyStages.next(pathologySample.getStatus(), deploymentEnabled(),
                pathologySample.isDecalcificationRequired());
    }

    @Override
    public Optional<PathologyStatus> previousStage(PathologySample pathologySample) {
        return PathologyStages.previous(pathologySample.getStatus(), deploymentEnabled(),
                pathologySample.isDecalcificationRequired());
    }

    @Transactional
    @Override
    public boolean isRunReferenceRecordable(PathologySample pathologySample, PathologyStatus stage) {
        return recordableRunEntry(pathologySample, stage).isPresent();
    }

    /**
     * Moves a copy of the stored case and saves it through the audited update, so
     * the case's own diff row, which the case history reads, records the stage
     * change as well.
     */
    private PathologyStageEvent move(PathologySample storedCase, PathologyStatus to, StageDirection direction,
            String reason, String sysUserId) {
        return move(storedCase, to, direction, reason, sysUserId, movedCase -> {
        });
    }

    /**
     * As {@link #move}, with a change made to the moved copy alongside the stage,
     * so the case's diff row records both. The stored case itself is never changed:
     * a change made there would not reach that row.
     */
    private PathologyStageEvent move(PathologySample storedCase, PathologyStatus to, StageDirection direction,
            String reason, String sysUserId, Consumer<PathologySample> alongside) {
        PathologySample movedCase = copyPathologySample(storedCase);
        alongside.accept(movedCase);
        PathologyStageEvent entered = applyTransition(movedCase, storedCase, to, direction, reason, sysUserId);
        update(movedCase);
        return entered;
    }

    @Transactional
    @Override
    public List<PathologyStageEvent> getStageEvents(Integer pathologySampleId) {
        return pathologyStageEventDAO.getForCase(pathologySampleId);
    }

    /**
     * Moves the case, already copied off the stored one, onto a stage: completes
     * the entry onto the stage it leaves, records the entry onto the new one, and
     * audits the move (a move onto Completed as the sign-out), all in the caller's
     * transaction. Operator and times are the session user and the server clock
     * (S-8.2). A case on file before stage events were recorded has no entry to
     * complete, and none is invented.
     *
     * @return the event recording the entry onto {@code to}
     */
    private PathologyStageEvent applyTransition(PathologySample movedCase, PathologySample storedCase,
            PathologyStatus to, StageDirection direction, String reason, String sysUserId) {
        PathologyStatus stageFrom = storedCase.getStatus();
        Timestamp now = new Timestamp(System.currentTimeMillis());
        SystemUser operator = systemUserService.get(sysUserId);

        pathologyStageEventDAO.getOpenEvent(storedCase.getId(), stageFrom).ifPresent(left -> {
            left.setCompletedAt(now);
            left.setCompletedBy(operator);
            left.setSysUserId(sysUserId);
            pathologyStageEventDAO.update(left);
        });

        PathologyStageEvent entered = new PathologyStageEvent();
        entered.setPathologySampleId(storedCase.getId());
        entered.setStage(to);
        entered.setEnteredAt(now);
        entered.setEnteredBy(operator);
        entered.setDirection(direction);
        entered.setReason(reason);
        entered.setSysUserId(sysUserId);
        pathologyStageEventDAO.insert(entered);

        // A case that goes into decalcification, or on past it, has been through it,
        // so it must never read as not needing it. A return keeps the bench's choice.
        if (direction == StageDirection.FORWARD
                && (to == PathologyStatus.DECALCIFICATION || stageFrom == PathologyStatus.DECALCIFICATION)) {
            movedCase.setDecalcificationRequired(true);
        }
        movedCase.setStatus(to);
        movedCase.setSysUserId(sysUserId);

        recordMove(storedCase, to, direction, reason, sysUserId);
        return entered;
    }

    /**
     * Audits a move on the case, a move onto Completed as the sign-out (FR-17),
     * naming the stage the case stood at.
     */
    private void recordMove(PathologySample storedCase, PathologyStatus to, StageDirection direction, String reason,
            String sysUserId) {
        PathologyStatus stageFrom = storedCase.getStatus();
        Map<String, Object> payload = new LinkedHashMap<>();
        if (to == PathologyStatus.COMPLETED) {
            payload.put("verb", PATHOLOGY_CASE_SIGNED_OUT);
            payload.put("stageFrom", stageFrom.name());
        } else {
            payload.put("verb",
                    direction == StageDirection.FORWARD ? PATHOLOGY_STAGE_ADVANCED : PATHOLOGY_STAGE_RETURNED);
            payload.put("stageFrom", stageFrom.name());
            payload.put("stageTo", to.name());
            if (direction == StageDirection.RETURN) {
                payload.put("reason", reason);
            }
        }
        recordRowEvent(CASE_REFERENCE_TABLE, storedCase.getStringId(), IActionConstants.AUDIT_TRAIL_UPDATE, payload,
                sysUserId);
    }

    /**
     * The expected stage is what makes a transition safe to repeat: a page that
     * still shows a stage the case has left cannot move it a second time.
     */
    private void requireStoredStage(PathologySample storedCase, PathologyStatus from) {
        if (from == null) {
            throw new LocalizedValidationException("pathology.error.fromRequired",
                    "the request did not say which stage pathology case " + storedCase.getId() + " is at");
        }
        if (storedCase.getStatus() != from) {
            throw new PathologyStageConflictException(storedCase.getId(), storedCase.getStatus(), from);
        }
    }

    /**
     * Grossing is finished once the specimen is described and at least one cassette
     * is cut (FR-3.4); embedding once there is a cassette in use and every one is a
     * block (FR-6.3); microtomy once at least one slide is cut and in use, since a
     * block may be held without slides, and once every slide in use that has a
     * block on record has had its label checked against it (FR-7.5), with or
     * without a scanner. A slide with no block on record has nothing to be checked
     * against, so it does not hold the case back. Staining is finished once every
     * slide in use is marked stained (FR-8.1); coverslipping once every slide in
     * use has passed its check (FR-8.4), and, where the laboratory tracks Staining,
     * is marked stained. A slide with no block on record counts at both, since
     * staining needs no parent; a retired slide counts at neither. None of the
     * three slide stages is left with no slide in use.
     */
    private void refuseAnUnfinishedStage(PathologySample storedCase) {
        List<PathologyBlock> inUse = storedCase.getBlocks() == null ? List.of()
                : PathologyRowOrder.blocks(storedCase.getBlocks()).stream().filter(PathologyBlock::isActive)
                        .collect(Collectors.toList());
        if (storedCase.getStatus() == PathologyStatus.GROSSING
                && (StringUtils.isBlank(storedCase.getGrossExam()) || inUse.isEmpty())) {
            throw new LocalizedValidationException("pathology.error.grossingIncomplete",
                    "pathology case " + storedCase.getId()
                            + " needs a macroscopic description and at least one cassette before it moves on");
        }
        if (storedCase.getStatus() == PathologyStatus.EMBEDDING) {
            if (inUse.isEmpty()) {
                throw new LocalizedValidationException("pathology.error.embeddingNoCassettes",
                        "pathology case " + storedCase.getId() + " has no cassette in use to embed");
            }
            List<String> outstanding = inUse.stream().filter(block -> block.getCassetteState() != CassetteState.BLOCK)
                    .map(PathologyBlock::displayIdentifier).collect(Collectors.toList());
            if (!outstanding.isEmpty()) {
                String designations = String.join(", ", outstanding);
                throw new LocalizedValidationException("pathology.error.embeddingIncomplete",
                        "pathology case " + storedCase.getId() + " has cassettes still to embed: " + designations,
                        Map.of("designations", designations));
            }
        }
        if (storedCase.getStatus() == PathologyStatus.MICROTOMY) {
            refuseWithNoSlideInUse(storedCase);
            refuseSlides(
                    slidesInUse(storedCase).stream()
                            .filter(slide -> slide.getBlockId() != null && slide.getLabelVerifiedAt() == null)
                            .collect(Collectors.toList()),
                    storedCase, "pathology.error.slidesUnverified", "slide labels still to verify");
        }
        if (storedCase.getStatus() == PathologyStatus.STAINING) {
            refuseWithNoSlideInUse(storedCase);
            refuseSlides(
                    slidesInUse(storedCase).stream().filter(slide -> !isStained(slide)).collect(Collectors.toList()),
                    storedCase, "pathology.error.stainingIncomplete", "slides still to stain");
        }
        if (storedCase.getStatus() == PathologyStatus.COVERSLIPPING) {
            refuseWithNoSlideInUse(storedCase);
            boolean stainingTracked = deploymentEnabled().test(PathologyStatus.STAINING);
            refuseSlides(
                    slidesInUse(storedCase).stream()
                            .filter(slide -> slide.getCoverslipQc() != CoverslipQc.PASS
                                    || (stainingTracked && !isStained(slide)))
                            .collect(Collectors.toList()),
                    storedCase, "pathology.error.coverslipQcIncomplete", "slides still to pass their coverslip check");
        }
    }

    /**
     * Microtomy, Staining and Coverslipping each work on slides, so a case with no
     * slide in use leaves none of them: retiring every slide must not open a gate.
     */
    private void refuseWithNoSlideInUse(PathologySample storedCase) {
        if (slidesInUse(storedCase).isEmpty()) {
            throw new LocalizedValidationException("pathology.error.microtomyIncomplete",
                    "pathology case " + storedCase.getId() + " has no slide in use, so it cannot move on");
        }
    }

    private static List<PathologySlide> slidesInUse(PathologySample pathologySample) {
        return pathologySample.getSlides() == null ? List.of()
                : pathologySample.getSlides().stream().filter(PathologySlide::isActive).collect(Collectors.toList());
    }

    /**
     * The slides as the bench names them, block first ("A1.1"), joined in the order
     * the case view lists them, for a sentence that names what holds the case.
     */
    private static String slideNames(List<PathologySlide> slides, PathologySample pathologySample,
            DesignationScheme scheme) {
        return PathologyRowOrder.slides(slides).stream()
                .map(slide -> PathologyDesignations
                        .slideOnBlockName(pathologySample.blockById(slide.getBlockId()).orElse(null), slide, scheme))
                .collect(Collectors.joining(", "));
    }

    /**
     * The pathologist signs a case out from the review queue (FR-2.4, S-8.3): from
     * any bench stage a release would skip the stages and their preconditions, and
     * a released case is not released again.
     */
    private void refuseReleaseOutsideReview(PathologySample storedCase) {
        if (isReleasable(storedCase)) {
            return;
        }
        refuseOnACompletedCase(storedCase);
        throw new LocalizedValidationException("pathology.error.releaseBeforeReview",
                "pathology case " + storedCase.getId() + " stands at " + storedCase.getStatus()
                        + ", so it is not ready to be signed out");
    }

    /**
     * A case in the pathologist's queue may be released. So may a case that stands
     * at Completed but was never released: the stage control on the case view once
     * set any stage, Completed included, without signing the case out, and such a
     * case would otherwise stay unreleased for good.
     *
     * <p>
     * A release leaves a sign-out entry in the case's audit trail, and a release
     * from the queue also an entry onto Completed. A case released before either
     * was written still has the released date the release stamps on its analyses.
     * The order's status is no record: a new result on the order, an
     * immunohistochemistry one for instance, sets it back to Started.
     */
    @Transactional
    @Override
    public boolean isReleasable(PathologySample pathologySample) {
        if (pathologySample.getStatus() == null) {
            return false;
        }
        switch (pathologySample.getStatus()) {
        case READY_PATHOLOGIST:
        case UNDER_REVIEW:
            return true;
        case COMPLETED:
            return !isSignedOut(pathologySample);
        default:
            return false;
        }
    }

    @Transactional
    @Override
    public PathologyStatus readingStage() {
        return PathologyStages.readingStage(deploymentEnabled());
    }

    @Transactional
    @Override
    public boolean findingsOpen(PathologySample pathologySample) {
        PathologyStatus status = pathologySample.getStatus();
        if (status == null) {
            return false;
        }
        return status == PathologyStatus.UNDER_REVIEW || status == readingStage()
                || (status == PathologyStatus.COMPLETED && !isSignedOut(pathologySample));
    }

    /** Signed out: on Completed and released, so nothing on the case changes. */
    private boolean isSignedOut(PathologySample pathologySample) {
        return pathologySample.getStatus() == PathologyStatus.COMPLETED && hasBeenReleased(pathologySample);
    }

    private boolean hasBeenReleased(PathologySample pathologySample) {
        return hasEnteredCompletedForward(pathologySample) || hasSignOutEntry(pathologySample)
                || hasReleasedAnalysis(pathologySample);
    }

    private boolean hasEnteredCompletedForward(PathologySample pathologySample) {
        return pathologyStageEventDAO.getForCase(pathologySample.getId()).stream()
                .anyMatch(event -> event.getStage() == PathologyStatus.COMPLETED
                        && event.getDirection() == StageDirection.FORWARD);
    }

    private boolean hasSignOutEntry(PathologySample pathologySample) {
        ReferenceTables referenceTable = referenceTablesService.getReferenceTableByName(CASE_REFERENCE_TABLE);
        if (referenceTable == null) {
            return false;
        }
        return historyService.getHistoryByRefIdAndRefTableId(pathologySample.getStringId(), referenceTable.getId())
                .stream().anyMatch(history -> PATHOLOGY_CASE_SIGNED_OUT.equals(verbOf(history)));
    }

    /**
     * The verb of a named event on the case. The case's own save diffs share its
     * reference row and are XML, so they have none.
     */
    private static String verbOf(History history) {
        if (history.getChanges() == null) {
            return null;
        }
        String changes = new String(history.getChanges(), StandardCharsets.UTF_8).trim();
        if (!changes.startsWith("{")) {
            return null;
        }
        try {
            return AUDIT_PAYLOAD_MAPPER.readTree(changes).path("verb").asText(null);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * The case's own analyses are those on its order that were not referred on to
     * immunohistochemistry; a release of the case stamps their released date.
     */
    private boolean hasReleasedAnalysis(PathologySample pathologySample) {
        Sample sample = pathologySample.getSample();
        if (sample == null || sample.getId() == null) {
            return false;
        }
        List<Analysis> analyses = analysisService.getAnalysesBySampleId(sample.getId());
        return analyses != null
                && analyses.stream().filter(analysis -> !ProgramCaseAnalyses.isImmunohistochemistryReferral(analysis))
                        .anyMatch(analysis -> analysis.getReleasedDate() != null);
    }

    /**
     * Cassettes are embedded once the case reaches Embedding, and the objects of a
     * signed-out case are what its report described.
     */
    private void refuseEmbeddingOutsideTheStage(PathologySample owningCase) {
        refuseOutsideTheStage(owningCase, PathologyStatus.EMBEDDING, "pathology.error.embedBeforeStage");
    }

    /**
     * Work on a case's blocks or slides is refused on a signed-out case first, then
     * on a case that has not reached the stage the work belongs to. A case with no
     * stage on record has reached none.
     */
    private void refuseOutsideTheStage(PathologySample owningCase, PathologyStatus firstStage,
            String beforeStageMessageKey) {
        if (owningCase == null) {
            return;
        }
        refuseOnACompletedCase(owningCase);
        PathologyStatus stage = owningCase.getStatus();
        if (stage == null || stage.ordinal() < firstStage.ordinal()) {
            throw new LocalizedValidationException(beforeStageMessageKey, "pathology case " + owningCase.getId()
                    + " stands at " + stage + ", so it has not reached " + firstStage + " yet");
        }
    }

    /** A signed-out case's blocks and slides change only after it is reopened. */
    private void refuseOnACompletedCase(PathologySample owningCase) {
        if (owningCase != null && owningCase.getStatus() == PathologyStatus.COMPLETED) {
            throw caseCompleted();
        }
    }

    private static LocalizedValidationException caseCompleted() {
        return new LocalizedValidationException("pathology.error.caseCompleted",
                "the case is signed out, so this can no longer be changed");
    }

    /**
     * Whether this deployment visits a stage: a switch set to false takes it out,
     * and a missing one leaves it in, as the case view reads them.
     */
    private static Predicate<PathologyStatus> deploymentEnabled() {
        return stage -> PathologyStages.enablementProperty(stage).map(PathologySampleServiceImpl::switchedOn)
                .orElse(true);
    }

    /**
     * Whether slide labels are scanned against their block; a setting of false is a
     * laboratory without a scanner, and a missing one asks for the scan.
     */
    private static boolean scanVerificationRequired() {
        return switchedOn(Property.PATHOLOGY_SCAN_VERIFICATION_REQUIRED);
    }

    /**
     * A pathology switch is on unless it says false, in any case and with stray
     * spaces, the way the case view reads it; a missing one is on.
     */
    private static boolean switchedOn(Property property) {
        return !"false"
                .equalsIgnoreCase(StringUtils.trim(ConfigurationProperties.getInstance().getPropertyValue(property)));
    }

    /**
     * Where the laboratory tracks Embedding, a slide is cut from a block; where it
     * does not, no row ever becomes one, so any cassette in use is cut from.
     */
    private static boolean slidesNeedAnEmbeddedBlock() {
        return deploymentEnabled().test(PathologyStatus.EMBEDDING);
    }

    /**
     * A run reference is free text on the case's entry onto a stage: the processor
     * run on Processing (FR-5.1), the coverslipping batch on Coverslipping
     * (FR-8.4). It is written once per entry and audited. A typo is put right by
     * returning the case to that stage, which opens a new entry. Anything posted
     * when no entry would take it is ignored, like the decalcification flag past
     * its stage.
     */
    private void recordRunReference(PathologySample storedCase, PathologyStatus stage, String postedRunReference,
            String sysUserId) {
        String runReference = StringUtils.trimToNull(postedRunReference);
        Optional<PathologyStageEvent> entry = recordableRunEntry(storedCase, stage);
        if (runReference == null || entry.isEmpty()) {
            return;
        }
        if (runReference.length() > PathologyStageEvent.RUN_REFERENCE_MAX_LENGTH) {
            throw new LocalizedValidationException("pathology.error.runReferenceTooLong",
                    "the processor run can be at most " + PathologyStageEvent.RUN_REFERENCE_MAX_LENGTH + " characters");
        }
        PathologyStageEvent stageEntry = entry.get();
        stageEntry.setRunReference(runReference);
        stageEntry.setSysUserId(sysUserId);
        pathologyStageEventDAO.update(stageEntry);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("verb", PATHOLOGY_RUN_REFERENCE_RECORDED);
        payload.put("stage", stage.name());
        payload.put("runReference", runReference);
        recordRowEvent(CASE_REFERENCE_TABLE, storedCase.getStringId(), IActionConstants.AUDIT_TRAIL_UPDATE, payload,
                sysUserId);
    }

    /**
     * The entry a run reference may still be written on: the open entry onto the
     * stage of a case standing there, while it holds none. A case that reached the
     * stage before entries were recorded has no entry, and none is invented.
     */
    private Optional<PathologyStageEvent> recordableRunEntry(PathologySample storedCase, PathologyStatus stage) {
        if (storedCase.getStatus() != stage) {
            return Optional.empty();
        }
        return pathologyStageEventDAO.getOpenEvent(storedCase.getId(), stage)
                .filter(entry -> entry.getRunReference() == null);
    }

    @Transactional
    @Override
    public Optional<PathologySlide> deactivateSlide(Integer slideId, String reason, String sysUserId) {
        Optional<PathologySlide> found = pathologySlideDAO.get(slideId);
        if (found.isEmpty()) {
            return found;
        }
        if (!found.get().isActive()) {
            return found;
        }
        refuseOnACompletedCase(baseObjectDAO.getCaseOfSlide(slideId));

        PathologySlide slide = found.get();
        slide.setActive(false);
        slide.setSysUserId(sysUserId);
        pathologySlideDAO.update(slide);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("verb", PATHOLOGY_SLIDE_DEACTIVATED);
        putIfPresent(payload, "designation", slide.displayIdentifier());
        if (slide.getBlockId() != null) {
            payload.put("blockId", slide.getBlockId());
        }
        putIfPresent(payload, "reason", reason);
        recordRowEvent(SLIDE_REFERENCE_TABLE, slide.getStringId(), IActionConstants.AUDIT_TRAIL_UPDATE, payload,
                sysUserId);
        return Optional.of(slide);
    }

    /**
     * Applies the posted cassettes to the case. A posted row is matched by its id
     * and keeps the identity the server gave it; a row with no id is a new cassette
     * and is named here; a row the case holds but the client did not post is left
     * exactly as it is.
     *
     * <p>
     * The identity the server assigns to a new row is readable only by fetching the
     * case again, because the save answers with the form that was posted.
     *
     * @return the designations of the cassettes cut in this save
     */
    private List<String> reconcileBlocks(PathologySample pathologySample, List<PathologyBlock> postedBlocks,
            DesignationScheme scheme, String accessionNumber) {
        List<String> cassettesCut = new ArrayList<>();
        if (postedBlocks == null) {
            return cassettesCut;
        }
        List<PathologyBlock> blocksOnCase = pathologySample.getBlocks();
        for (PathologyBlock posted : postedBlocks) {
            if (posted.getId() != null) {
                applyToBlock(blockOnCase(pathologySample, posted.getId()), posted);
            } else {
                PathologyBlock cassette = cutCassette(posted, blocksOnCase, scheme, accessionNumber,
                        pathologySample.getSysUserId());
                blocksOnCase.add(cassette);
                cassettesCut.add(cassette.getDesignation());
            }
        }
        return cassettesCut;
    }

    /**
     * Applies the posted slides to the case, on the same three rules as
     * {@link #reconcileBlocks}, with the addition that a new slide has to name the
     * block it was cut from.
     *
     * @return the slides cut in this save
     */
    private List<SlideCut> reconcileSlides(PathologySample pathologySample, List<PathologySlideForm> postedSlides,
            DesignationScheme scheme, String accessionNumber) {
        List<SlideCut> slidesCut = new ArrayList<>();
        if (postedSlides == null) {
            return slidesCut;
        }
        List<PathologySlide> slidesOnCase = pathologySample.getSlides();
        for (PathologySlideForm posted : postedSlides) {
            if (posted.getId() != null) {
                applyToSlide(slideOnCase(pathologySample, posted.getId()), posted);
            } else {
                PathologySlide slide = cutSlide(posted, pathologySample, slidesOnCase, scheme, accessionNumber);
                slidesOnCase.add(slide);
                slidesCut.add(new SlideCut(slide.getBlockId(), slide.getDesignation()));
            }
        }
        return slidesCut;
    }

    /**
     * Location and the tissue held are the fields a case save edits, and a retired
     * row is left as it is.
     */
    private void applyToBlock(PathologyBlock block, PathologyBlock posted) {
        if (!block.isActive()) {
            return;
        }
        String tissueTypeId = tissueTypeAfterSave(block.getTissueTypeId(), posted.getTissueTypeId());
        block.setLocation(posted.getLocation());
        block.setTissueTypeId(tissueTypeId);
    }

    private String tissueTypeAfterSave(String stored, String posted) {
        return dictionaryEntryAfterSave(stored, posted, TISSUE_TYPE_CATEGORY, "pathology.error.tissueTypeNotInUse");
    }

    private String stainAfterSave(String stored, String posted) {
        return dictionaryEntryAfterSave(stored, posted, STAIN_CATEGORY, "pathology.error.stainNotInUse");
    }

    /**
     * The dictionary entry a row holds after a save: blank clears it, and a changed
     * value must be an entry of the category that is still in use. An unchanged
     * value is never checked again, so an entry retired after a row recorded it
     * cannot block a save of the case.
     *
     * <p>
     * An entry of the category taken out of use is refused by name, so the bench
     * reads which choice went out of use. Anything else (another category's entry,
     * a value that names no entry) cannot come from the picker, which lists only
     * the category, so it is answered like the other refusals of a case save that
     * mean the page is out of date, with the reload message.
     */
    private String dictionaryEntryAfterSave(String stored, String posted, String categoryName,
            String retiredMessageKey) {
        String requested = StringUtils.trimToNull(posted);
        if (requested == null || Objects.equals(requested, stored)) {
            return requested;
        }
        refuseUnlessInUse(requested, categoryName, PathologyCaseRuleException::new, retiredMessageKey);
        return requested;
    }

    /**
     * Refuses an id that names no entry of the category with the caller's own
     * refusal, and an entry taken out of use by its name, so the reader learns
     * which choice went out of use.
     */
    private void refuseUnlessInUse(String dictionaryId, String categoryName,
            Function<String, RuntimeException> notAnEntry, String retiredMessageKey) {
        Dictionary entry = entryOf(dictionaryId, categoryName);
        if (entry == null) {
            throw notAnEntry.apply("dictionary entry " + dictionaryId + " is not an entry of " + categoryName);
        }
        if (!"Y".equals(entry.getIsActive())) {
            throw new LocalizedValidationException(retiredMessageKey,
                    "dictionary entry " + dictionaryId + " of " + categoryName + " is no longer in use",
                    Map.of("name", DictionaryEntryNames.localizedNameOf(dictionaryService, dictionaryId)
                            .orElse(dictionaryId)));
        }
    }

    /**
     * The entry of the named category the id names, in use or not; null if none.
     */
    private Dictionary entryOf(String dictionaryId, String categoryName) {
        try {
            Integer.parseInt(dictionaryId);
        } catch (NumberFormatException e) {
            // The dictionary key binds as an integer, so anything else names no entry
            // and is the client's mistake, not a failure of the lookup.
            return null;
        }
        Dictionary entry = dictionaryService.getDataForId(dictionaryId);
        return entry != null && entry.getDictionaryCategory() != null
                && categoryName.equals(entry.getDictionaryCategory().getCategoryName()) ? entry : null;
    }

    /**
     * PathologyStatus is declared in bench order (FR-2.6), so a later ordinal is a
     * stage the case reaches after decalcification.
     */
    private static boolean pastDecalcification(PathologySample storedCase) {
        return storedCase.getStatus() != null
                && storedCase.getStatus().ordinal() > PathologyStatus.DECALCIFICATION.ordinal();
    }

    /**
     * Location, level and stain are the fields a case save edits, and a retired
     * slide is left as it is. An image is only replaced when one was posted: the
     * case view sends the slide rows back without their stored images, and treating
     * that as an erasure would lose the image on the next save of any other field.
     * What staining and the coverslip check found is never taken from the save. A
     * slide marked stained keeps the stain it was marked with, since that is the
     * stain applied; marking it unsatisfactory lets the stain change again.
     */
    private void applyToSlide(PathologySlide slide, PathologySlide posted) {
        if (!slide.isActive()) {
            return;
        }
        String level = levelAfterSave(posted.getLevel());
        String stainId = slide.getStainStatus() == StainStatus.STAINED ? slide.getStainId()
                : stainAfterSave(slide.getStainId(), posted.getStainId());
        // The label check is stamped only by verifySlideLabel, so a posted stamp is
        // never copied.
        slide.setLocation(posted.getLocation());
        slide.setLevel(level);
        slide.setStainId(stainId);
        if (posted.getImage() != null) {
            slide.setImage(posted.getImage());
            slide.setFileType(posted.getFileType());
        }
    }

    /**
     * Cuts a cassette under the case's first part, holding the tissue type posted
     * with it; the part it came from is not captured on this screen.
     */
    private PathologyBlock cutCassette(PathologyBlock posted, List<PathologyBlock> blocksOnCase,
            DesignationScheme scheme, String accessionNumber, String sysUserId) {
        String part = PathologyDesignations.firstPart(scheme);
        String designation = PathologyDesignations.nextBlockDesignation(designationsOfBlocks(blocksOnCase), part,
                scheme);

        PathologyBlock block = new PathologyBlock();
        block.setPartDesignation(part);
        block.setDesignation(designation);
        block.setBarcode(PathologyDesignations.blockBarcode(accessionNumber, designation, scheme));
        block.setCassetteState(CassetteState.CASSETTE);
        block.setActive(true);
        block.setLocation(posted.getLocation());
        block.setTissueTypeId(tissueTypeAfterSave(null, posted.getTissueTypeId()));
        block.setSysUserId(sysUserId);
        return block;
    }

    /**
     * The level is free text that has to fit its column; it is not required.
     */
    private static String levelAfterSave(String posted) {
        String level = StringUtils.trimToNull(posted);
        if (level != null && level.length() > PathologySlide.LEVEL_MAX_LENGTH) {
            throw new LocalizedValidationException("pathology.error.levelTooLong",
                    "a slide's level can be at most " + PathologySlide.LEVEL_MAX_LENGTH + " characters");
        }
        return level;
    }

    /**
     * Cuts a slide under the block it names, holding the level and stain posted
     * with it. A signed-out case takes no new slide until it is reopened.
     */
    private PathologySlide cutSlide(PathologySlide posted, PathologySample pathologySample,
            List<PathologySlide> slidesOnCase, DesignationScheme scheme, String accessionNumber) {
        refuseOnACompletedCase(pathologySample);
        PathologyBlock block = blockCutFrom(posted.getBlockId(), pathologySample);
        String designation = PathologyDesignations
                .nextSlideDesignation(designationsOfSlidesCutFrom(slidesOnCase, block.getId()), scheme);

        PathologySlide slide = new PathologySlide();
        slide.setBlockId(block.getId());
        slide.setDesignation(designation);
        slide.setBarcode(
                PathologyDesignations.slideBarcode(accessionNumber, block.displayIdentifier(), designation, scheme));
        slide.setActive(true);
        slide.setLocation(posted.getLocation());
        slide.setLevel(levelAfterSave(posted.getLevel()));
        slide.setStainId(stainAfterSave(null, posted.getStainId()));
        // A new slide's label has not been printed yet, so a posted stamp is never
        // copied.
        slide.setImage(posted.getImage());
        slide.setFileType(posted.getFileType());
        slide.setSysUserId(pathologySample.getSysUserId());
        return slide;
    }

    private PathologyBlock blockOnCase(PathologySample pathologySample, Integer blockId) {
        return pathologySample.blockById(blockId).orElseThrow(() -> new PathologyCaseRuleException(
                "block " + blockId + " is not on pathology case " + pathologySample.getId()));
    }

    private PathologySlide slideOnCase(PathologySample pathologySample, Integer slideId) {
        return pathologySample.slideById(slideId).orElseThrow(() -> new PathologyCaseRuleException(
                "slide " + slideId + " is not on pathology case " + pathologySample.getId()));
    }

    /**
     * Only a block the case already holds, has persisted and still has in use can
     * be named: a cassette added in the same post has no id yet, so the slides cut
     * from it are added on the save that follows. Where the laboratory tracks
     * Embedding, the row must also have been embedded; a row with no recorded state
     * has not been.
     */
    private PathologyBlock blockCutFrom(Integer blockId, PathologySample pathologySample) {
        if (blockId == null) {
            throw new PathologyCaseRuleException(
                    "a slide must name the block it was cut from on pathology case " + pathologySample.getId());
        }
        PathologyBlock block = blockOnCase(pathologySample, blockId);
        if (!block.isActive()) {
            throw new PathologyCaseRuleException(
                    "block " + blockId + " has been retired, so no slide can be cut from it");
        }
        if (block.getCassetteState() != CassetteState.BLOCK && slidesNeedAnEmbeddedBlock()) {
            throw new LocalizedValidationException("pathology.error.slideFromCassette",
                    "cassette " + block.displayIdentifier() + " must be embedded before slides are cut from it",
                    Map.of("designation", block.displayIdentifier()));
        }
        return block;
    }

    private List<String> designationsOfBlocks(List<PathologyBlock> blocks) {
        return blocks.stream().map(PathologyBlock::getDesignation).collect(Collectors.toList());
    }

    private List<String> designationsOfSlidesCutFrom(List<PathologySlide> slides, Integer blockId) {
        return slides.stream().filter(slide -> blockId.equals(slide.getBlockId())).map(PathologySlide::getDesignation)
                .collect(Collectors.toList());
    }

    private void putIfPresent(Map<String, Object> payload, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            payload.put(key, value.trim());
        }
    }

    /**
     * One audit entry per cassette cut, written once the merge has given each row
     * its id; designations are unique within a case, so they find the rows.
     */
    private void recordCassettesCut(PathologySample savedCase, List<String> cassettesCut, String sysUserId) {
        for (PathologyBlock block : savedCase.getBlocks()) {
            if (cassettesCut.contains(block.getDesignation())) {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("verb", PATHOLOGY_BLOCK_CREATED);
                putIfPresent(payload, "designation", block.getDesignation());
                putIfPresent(payload, "barcode", block.getBarcode());
                recordRowEvent(BLOCK_REFERENCE_TABLE, block.getStringId(), IActionConstants.AUDIT_TRAIL_INSERT, payload,
                        sysUserId);
            }
        }
    }

    /**
     * One audit entry per slide cut, written once the merge has given each row its
     * id; a slide designation repeats under every block, so the block and the
     * designation together find the rows.
     */
    private void recordSlidesCut(PathologySample savedCase, List<SlideCut> slidesCut, String sysUserId) {
        for (PathologySlide slide : savedCase.getSlides()) {
            if (slidesCut.contains(new SlideCut(slide.getBlockId(), slide.getDesignation()))) {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("verb", PATHOLOGY_SLIDE_CREATED);
                putIfPresent(payload, "designation", slide.getDesignation());
                putIfPresent(payload, "barcode", slide.getBarcode());
                payload.put("blockId", slide.getBlockId());
                recordRowEvent(SLIDE_REFERENCE_TABLE, slide.getStringId(), IActionConstants.AUDIT_TRAIL_INSERT, payload,
                        sysUserId);
            }
        }
    }

    /**
     * Writes the audit entry an event on a block or slide leaves behind.
     * history.activity is a one-character code, so the verb travels in the changes
     * payload, which never carries anything about the patient.
     */
    private void recordRowEvent(String referenceTableName, String referenceId, String activity,
            Map<String, Object> payload, String sysUserId) {
        ReferenceTables referenceTable = referenceTablesService.getReferenceTableByName(referenceTableName);
        if (referenceTable == null) {
            throw new LIMSRuntimeException(
                    referenceTableName + " is missing from reference_tables, so the event cannot be audited");
        }

        History history = new History();
        history.setReferenceId(referenceId);
        history.setReferenceTable(referenceTable.getId());
        history.setActivity(activity);
        history.setTimestamp(new Timestamp(System.currentTimeMillis()));
        history.setSysUserId(sysUserId);
        history.setChanges(asJson(payload));
        historyService.insert(history);
    }

    private byte[] asJson(Map<String, Object> payload) {
        try {
            return AUDIT_PAYLOAD_MAPPER.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            throw new LIMSRuntimeException("the pathology audit payload could not be written", e);
        }
    }

    // OGC-285 flow migration — TODO (NEEDS-DESIGN-CALL, do NOT force):
    // The pathology / cytology / immunohistochemistry case-save flow is
    // intentionally NOT migrated to the OGC-285 preset/snapshot model and remains
    // on the legacy BarcodeWorkflowPrintService below. The gap is a product/design
    // decision, not missing wiring: the Block / Slide / Freezer system presets are
    // prints_per_sample and, like all per-sample presets, surface in the
    // aggregation (OrderEntryLabelRequestService) ONLY via test->preset links — but
    // this flow injects block/slide/freezer counts directly onto the order
    // (form.getNumBlockLabels()/Slide/Freezer), with no test linking to those
    // presets. The aggregation therefore cannot emit Block/Slide/Freezer columns
    // for a pathology order. Decision needed before migrating: extend the
    // aggregation/snapshot model to surface these per-sample presets without a
    // test link (e.g. a pathology-context preset set), or drive them from an
    // explicit non-test source. Until then, migrating here would silently drop the
    // Block/Slide/Freezer labels. See the OGC-285 flow-migration report.
    private void populatePathologyWorkflowPrintModels(PathologySample pathologySample, PathologySampleForm form) {
        int orderLabels = normalizePathologyLabelQuantity(form.getNumOrderLabels());
        int specimenLabels = normalizePathologyLabelQuantity(form.getNumSpecimenLabels());
        int blockLabels = normalizePathologyLabelQuantity(form.getNumBlockLabels());
        int slideLabels = normalizePathologyLabelQuantity(form.getNumSlideLabels());
        int freezerLabels = normalizePathologyLabelQuantity(form.getNumFreezerLabels());

        LabelsSectionForm labelsSection = barcodeWorkflowPrintService.buildLabelsSection(orderLabels,
                List.of(specimenLabels));
        java.util.Map<String, Integer> orderQuantities = new LinkedHashMap<>();
        if (labelsSection.getOrderRow() != null && labelsSection.getOrderRow().getQuantities() != null) {
            orderQuantities.putAll(labelsSection.getOrderRow().getQuantities());
        }
        orderQuantities.put("block", blockLabels);
        orderQuantities.put("slide", slideLabels);
        orderQuantities.put("freezer", freezerLabels);
        labelsSection.getOrderRow().setQuantities(orderQuantities);
        labelsSection.getOrderRow().setRowTotal(sumQuantities(orderQuantities));
        int sampleRowTotal = labelsSection.getSampleRows() == null ? 0
                : labelsSection.getSampleRows().stream().mapToInt(LabelRowForm::getRowTotal).sum();
        labelsSection.setRunningTotal(labelsSection.getOrderRow().getRowTotal() + sampleRowTotal);

        String accessionNumber = pathologySample.getSample() == null ? null
                : pathologySample.getSample().getAccessionNumber();
        PostSavePrintDialogForm postSavePrintDialog = barcodeWorkflowPrintService
                .buildPostSavePrintDialog(accessionNumber, labelsSection);

        form.setLabelsSection(labelsSection);
        form.setPostSavePrintDialog(postSavePrintDialog);
    }

    private int sumQuantities(java.util.Map<String, Integer> quantities) {
        return quantities.values().stream().mapToInt(quantity -> quantity == null ? 0 : quantity).sum();
    }

    private int normalizePathologyLabelQuantity(Integer quantity) {
        return quantity != null && quantity > 0 ? quantity : 1;
    }

    private void persistPathologyBarcodeCountsIfSupplied(PathologySample pathologySample, PathologySampleForm form) {
        if (form.getNumOrderLabels() == null || form.getNumSpecimenLabels() == null || form.getNumBlockLabels() == null
                || form.getNumSlideLabels() == null || form.getNumFreezerLabels() == null) {
            return;
        }
        if (form.getNumOrderLabels() < 1 || form.getNumSpecimenLabels() < 1 || form.getNumBlockLabels() < 1
                || form.getNumSlideLabels() < 1 || form.getNumFreezerLabels() < 1) {
            return;
        }

        barcodeInfoService.saveBarcodeInfoForSampleAndSampleItemsPathology(pathologySample.getSample(),
                form.getNumOrderLabels(), form.getNumSpecimenLabels(), form.getNumBlockLabels(),
                form.getNumSlideLabels(), form.getNumFreezerLabels());
    }

    /**
     * The sign-out reports the case's own tests. Tests an immunohistochemistry
     * referral added to the order wait for the referral's release, and the order is
     * finished only once every test on it is reported.
     */
    private void validatePathologySample(PathologySample pathologySample, PathologySampleForm form) {
        Sample sample = pathologySample.getSample();
        Patient patient = sampleService.getPatient(sample);
        ResultsUpdateDataSet actionDataSet = new ResultsUpdateDataSet(form.getSystemUserId());
        boolean referred = immunohistochemistrySampleService.getByPathologySampleId(pathologySample.getId()) != null;

        ResultsLoadUtility resultsUtility = SpringContext.getBean(ResultsLoadUtility.class);
        List<TestResultItem> testResultItems = resultsUtility.getGroupedTestsForSample(sample);
        for (TestResultItem testResultItem : testResultItems) {
            if (!testResultItem.getIsGroupSeparator()) {
                Analysis analysis = analysisService.get(testResultItem.getAnalysisId());
                if (referred && ProgramCaseAnalyses.isImmunohistochemistryReferral(analysis)) {
                    continue;
                }
                if (ResultType.isTextOnlyVariant(testResultItem.getResultType())) {
                    testResultItem.setResultValue(MessageUtil.getMessage("result.pathology.seereport"));
                }
                ResultSaveBean bean = ResultSaveBeanAdapter.fromTestResultItem(testResultItem);
                ResultSaveService resultSaveService = new ResultSaveService(analysis, form.getSystemUserId());
                List<Result> results = resultSaveService.createResultsFromTestResultItem(bean, new ArrayList<>());
                for (Result result : results) {
                    boolean newResult = result.getId() == null;
                    analysis.setEnteredDate(DateUtil.getNowAsTimestamp());

                    if (newResult) {
                        analysis.setRevision("1");
                        actionDataSet.getNewResults()
                                .add(new ResultSet(result, null, null, patient, sample, new HashMap<>(), false));
                    } else {
                        analysis.setRevision(String.valueOf(Integer.parseInt(analysis.getRevision()) + 1));
                        actionDataSet.getModifiedResults()
                                .add(new ResultSet(result, null, null, patient, sample, new HashMap<>(), false));
                    }

                    // analysis.setStartedDateForDisplay(testResultItem.getTestDate());

                    // This needs to be refactored -- part of the logic is in
                    // getStatusForTestResult. RetroCI over rides to whatever was set before
                    if (ConfigurationProperties.getInstance().getPropertyValueUpperCase(Property.StatusRules)
                            .equals(IActionConstants.STATUS_RULES_RETROCI)) {
                        if (!SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Canceled)
                                .equals(analysis.getStatusId())) {
                            analysis.setCompletedDate(
                                    DateUtil.convertStringDateToSqlDate(testResultItem.getTestDate()));
                            analysis.setStatusId(SpringContext.getBean(IStatusService.class)
                                    .getStatusID(AnalysisStatus.TechnicalAcceptance));
                        }
                    } else if (SpringContext.getBean(IStatusService.class).matches(analysis.getStatusId(),
                            AnalysisStatus.Finalized)
                            || SpringContext.getBean(IStatusService.class).matches(analysis.getStatusId(),
                                    AnalysisStatus.TechnicalAcceptance)
                            || (analysis.isReferredOut()
                                    && !GenericValidator.isBlankOrNull(testResultItem.getShadowResultValue()))) {
                        analysis.setCompletedDate(DateUtil.convertStringDateToSqlDate(testResultItem.getTestDate()));
                        analysis.setStatusId(
                                SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Finalized));
                    }

                    // this code is pulled from LogbookResultsRestController
                    // addResult(result, testResultItem, analysis, results.size() > 1,
                    // actionDataSet, useTechnicianName);
                    //
                    // if (analysisShouldBeUpdated(testResultItem, result, supportReferrals)) {
                    // updateAnalysis(testResultItem, testResultItem.getTestDate(),
                    // analysis, statusRuleSet);
                    // }
                }
                analysis.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Finalized));
                analysis.setReleasedDate(new java.sql.Timestamp(System.currentTimeMillis()));
            }
        }

        logbookResultsPersistService.persistDataSet(actionDataSet, ResultUpdateRegister.getRegisteredUpdaters(),
                form.getSystemUserId());
        IStatusService statusService = SpringContext.getBean(IStatusService.class);
        if (ProgramCaseAnalyses.allFinished(analysisService.getAnalysesBySampleId(sample.getId()), statusService)) {
            Sample storedOrder = sampleService.get(sample.getId());
            storedOrder.setStatusId(statusService.getStatusID(OrderStatus.Finished));
            storedOrder.setSysUserId(form.getSystemUserId());
            sampleService.update(storedOrder);
        }
    }

    /**
     * Refers the case to immunohistochemistry, creating its case there when there
     * is none, and orders the planned markers under the order's own analysis (the
     * first that is not itself a referral, so a second referral never hangs off an
     * earlier marker).
     *
     * @return what this save referred, when it created the case or ordered a marker
     */
    private Optional<Referral> referToImmunoHistoChemistry(PathologySample pathologySample, ReferralPlan plan,
            String sysUserId) {
        ImmunohistochemistrySample immunoHistoSample = plan.existing();
        boolean created = immunoHistoSample == null;
        if (created) {
            immunoHistoSample = new ImmunohistochemistrySample();
            immunoHistoSample.setProgram(pathologySample.getProgram());
            immunoHistoSample.setQuestionnaireResponseUuid(pathologySample.getQuestionnaireResponseUuid());
            immunoHistoSample.setSample(pathologySample.getSample());
            immunoHistoSample.setPathologySample(pathologySample);
            immunoHistoSample.setReffered(true);
            immunoHistoSample = immunohistochemistrySampleService.save(immunoHistoSample);
        }
        List<Test> immunoHistologyTests = plan.testIds().stream().map(testService::get).filter(Objects::nonNull)
                .collect(Collectors.toList());
        Optional<Analysis> parentAnalysis = plan.analyses().stream()
                .filter(analysis -> !ProgramCaseAnalyses.isImmunohistochemistryReferral(analysis)).findFirst();
        List<String> ordered = new ArrayList<>();
        if (parentAnalysis.isPresent()) {
            immunoHistologyTests.forEach(test -> {
                CreateNewAnalysis(test, parentAnalysis.get(), pathologySample.getProgram().getProgramName(), sysUserId);
                ordered.add(test.getId());
            });
        }
        if (!created && ordered.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Referral(immunoHistoSample.getId(), ordered, created));
    }

    private void CreateNewAnalysis(Test immunoHistologyTest, Analysis currentAnalysis, String programmeName,
            String systemUserId) {
        Analysis analysis = new Analysis();
        analysis.setTest(immunoHistologyTest);
        analysis.setIsReportable(currentAnalysis.getIsReportable());
        analysis.setAnalysisType(currentAnalysis.getAnalysisType());
        analysis.setRevision(currentAnalysis.getRevision());
        analysis.setStartedDate(DateUtil.getNowAsTimestamp());
        analysis.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NotStarted));
        analysis.setParentAnalysis(currentAnalysis);
        analysis.setSampleItem(currentAnalysis.getSampleItem());
        TestSection testSection = testSectionService
                .getTestSectionByName(ProgramCaseAnalyses.IMMUNOHISTOCHEMISTRY_SECTION);
        analysis.setTestSection(testSection);
        analysis.setSampleTypeName(currentAnalysis.getSampleTypeName());
        analysis.setSysUserId(systemUserId);
        analysisService.insert(analysis);

        List<Note> notes = new ArrayList<>();
        Note note = noteService.createSavableNote(analysis, NoteType.INTERNAL,
                "Refered From Pathology Programme : " + programmeName + "to Immunohistochemistry",
                "Refered to Immunohistochemistry", systemUserId);
        if (!noteService.duplicateNoteExists(note)) {
            notes.add(note);
        }
        noteService.saveAll(notes);
    }

    /**
     * What a case save changes of the review and the findings, against the stored
     * case (D5): a field not sent is no change, and a text posted as it is stored,
     * null and empty being the same, is none either. A list is the posted codes
     * with repeats dropped, or null when unchanged.
     */
    private record ReviewChanges(boolean grossExam, boolean microscopyExam, boolean reviewNotes, boolean narrative,
            List<String> conclusions, List<String> techniques, boolean pathologist, ReferralPlan referral) {

        boolean any() {
            return grossExam || reviewNotes || pathologist || findings();
        }

        /** The fields written only while the case is being read (D3). */
        boolean findings() {
            return microscopyExam || narrative || conclusions != null || techniques != null || referral != null;
        }
    }

    /**
     * A referral a save makes: the case's immunohistochemistry case, null when it
     * is to be created, the markers the order does not hold yet, and the order's
     * analyses.
     */
    private record ReferralPlan(ImmunohistochemistrySample existing, List<String> testIds, List<Analysis> analyses) {
    }

    /** What a save referred, for its audit row. */
    private record Referral(Integer ihcCaseId, List<String> testIds, boolean created) {
    }

    private ReviewChanges reviewChangesOf(PathologySample storedCase, PathologySampleForm form) {
        return new ReviewChanges(textChanged(form.getGrossExam(), storedCase.getGrossExam()),
                textChanged(form.getMicroscopyExam(), storedCase.getMicroscopyExam()),
                textChanged(form.getReviewNotes(), storedCase.getReviewNotes()),
                textChanged(form.getConclusionText(), storedNarrative(storedCase)),
                changedCodes(form.getConclusions(), conclusionCodes(storedCase.getConclusions())),
                changedCodes(form.getTechniques(), techniqueCodes(storedCase.getTechniques())),
                !GenericValidator.isBlankOrNull(form.getAssignedPathologistId()) && (storedCase.getPathologist() == null
                        || !form.getAssignedPathologistId().equals(storedCase.getPathologist().getId())),
                Boolean.TRUE.equals(form.getReferToImmunoHistoChemistry()) ? referralPlanOf(storedCase, form) : null);
    }

    private static boolean textChanged(String posted, String stored) {
        return posted != null && !posted.equals(StringUtils.defaultString(stored));
    }

    /**
     * The posted codes without repeats, or null when they are the stored ones. A
     * stored id that names no entry is not counted, so leaving it off is no change.
     */
    private List<String> changedCodes(List<String> posted, List<String> stored) {
        if (posted == null) {
            return null;
        }
        List<String> codes = new ArrayList<>(new LinkedHashSet<>(posted));
        if (codes.equals(stored)) {
            return null;
        }
        List<String> named = stored.stream()
                .filter(code -> DictionaryEntryNames.localizedNameOf(dictionaryService, code).isPresent())
                .collect(Collectors.toList());
        return codes.equals(named) ? null : codes;
    }

    /**
     * The referral a save asks for, or null when it asks for nothing new: the case
     * is referred already and every posted marker is on the order as a referral.
     */
    private ReferralPlan referralPlanOf(PathologySample storedCase, PathologySampleForm form) {
        ImmunohistochemistrySample existing = immunohistochemistrySampleService
                .getByPathologySampleId(storedCase.getId());
        List<Analysis> analyses = analysesOf(storedCase);
        Set<String> held = referredTestIds(analyses);
        List<String> posted = form.getImmunoHistoChemistryTestIds() == null ? List.of()
                : form.getImmunoHistoChemistryTestIds();
        List<String> toOrder = new LinkedHashSet<>(posted).stream().filter(testId -> !held.contains(testId))
                .collect(Collectors.toList());
        return existing != null && toOrder.isEmpty() ? null : new ReferralPlan(existing, toOrder, analyses);
    }

    @Transactional
    @Override
    public List<String> ihcReferredTestIds(PathologySample pathologySample) {
        return new ArrayList<>(referredTestIds(analysesOf(pathologySample)));
    }

    private List<Analysis> analysesOf(PathologySample pathologySample) {
        return pathologySample.getSample() == null ? List.of()
                : Optional.ofNullable(analysisService.getAnalysesBySampleId(pathologySample.getSample().getId()))
                        .orElse(List.of());
    }

    /** The tests of the analyses that are immunohistochemistry referrals. */
    private static Set<String> referredTestIds(List<Analysis> analyses) {
        return analyses.stream().filter(ProgramCaseAnalyses::isImmunohistochemistryReferral)
                .filter(analysis -> analysis.getTest() != null).map(analysis -> analysis.getTest().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * The review and findings rules of a case save, in the order the design lists
     * them; each refuses the whole save before anything is written.
     */
    private void refuseReviewChanges(PathologySample storedCase, ReviewChanges changes, PathologySampleForm form) {
        if (!changes.any()) {
            return;
        }
        if (isSignedOut(storedCase)) {
            throw caseCompleted();
        }
        if (changes.findings() && !findingsOpen(storedCase)) {
            throw new LocalizedValidationException("pathology.error.findingsLocked",
                    "pathology case " + storedCase.getId() + " stands at " + storedCase.getStatus()
                            + ", where its findings are not written");
        }
        if (changes.narrative() && form.getConclusionText().length() > CONCLUSION_TEXT_MAX_LENGTH) {
            throw new LocalizedValidationException("pathology.error.conclusionTooLong",
                    "the narrative conclusion is longer than " + CONCLUSION_TEXT_MAX_LENGTH + " characters");
        }
        refuseNewCodesUnlessInUse(changes.conclusions(), conclusionCodes(storedCase.getConclusions()),
                CONCLUSION_CATEGORY, "pathology.error.conclusionNotInUse");
        refuseNewCodesUnlessInUse(changes.techniques(), techniqueCodes(storedCase.getTechniques()), TECHNIQUE_CATEGORY,
                "pathology.error.techniqueNotInUse");
    }

    /**
     * A posted code the case does not hold yet must be an entry in use in its
     * category; a stored code stays even after its entry is retired. Null posted
     * codes are no change.
     */
    private void refuseNewCodesUnlessInUse(List<String> posted, List<String> stored, String category,
            String retiredKey) {
        if (posted == null) {
            return;
        }
        posted.stream().filter(code -> !stored.contains(code))
                .forEach(code -> refuseUnlessInUse(code, category, PathologyCaseRuleException::new, retiredKey));
    }

    /** FR-12.5: a case is signed out only with both descriptions written. */
    private static void refuseReleaseWithoutDescriptions(PathologySample caseToSave) {
        if (StringUtils.isBlank(caseToSave.getGrossExam()) || StringUtils.isBlank(caseToSave.getMicroscopyExam())) {
            throw new LocalizedValidationException("pathology.error.releaseWithoutDescriptions",
                    "pathology case " + caseToSave.getId() + " lacks a macroscopic or microscopic description");
        }
    }

    /** The narrative conclusion: the value of the case's first text row. */
    private static String storedNarrative(PathologySample storedCase) {
        return storedCase.getConclusions().stream().filter(row -> row.getType() == ConclusionType.TEXT).findFirst()
                .map(PathologyConclusion::getValue).orElse(null);
    }

    /** The coded conclusions' dictionary ids, in row order. */
    private static List<String> conclusionCodes(List<PathologyConclusion> conclusions) {
        return conclusions.stream().filter(row -> row.getType() == ConclusionType.DICTIONARY)
                .map(PathologyConclusion::getValue).collect(Collectors.toList());
    }

    private static List<String> techniqueCodes(List<PathologyTechnique> techniques) {
        return techniques.stream().filter(row -> row.getType() == TechniqueType.DICTIONARY)
                .map(PathologyTechnique::getValue).collect(Collectors.toList());
    }

    /**
     * Records a change of the coded or narrative conclusion: the codes after the
     * save, those added and removed, and whether the narrative changed, never its
     * text.
     */
    private void recordConclusionSaved(PathologySample savedCase, List<String> codesBefore, List<String> codesAfter,
            boolean narrativeChanged, String sysUserId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("verb", PATHOLOGY_CONCLUSION_SAVED);
        payload.put("coded", codesAfter);
        payload.put("added",
                codesAfter.stream().filter(code -> !codesBefore.contains(code)).collect(Collectors.toList()));
        payload.put("removed",
                codesBefore.stream().filter(code -> !codesAfter.contains(code)).collect(Collectors.toList()));
        payload.put("narrativeChanged", narrativeChanged);
        recordRowEvent(CASE_REFERENCE_TABLE, savedCase.getStringId(), IActionConstants.AUDIT_TRAIL_UPDATE, payload,
                sysUserId);
    }

    private void recordReferral(PathologySample savedCase, Referral referral, String sysUserId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("verb", PATHOLOGY_IHC_REFERRED);
        payload.put("ihcCaseId", referral.ihcCaseId());
        payload.put("testIds", referral.testIds());
        payload.put("created", referral.created());
        recordRowEvent(CASE_REFERENCE_TABLE, savedCase.getStringId(), IActionConstants.AUDIT_TRAIL_UPDATE, payload,
                sysUserId);
    }

    private PathologyConclusion createConclusion(String text, ConclusionType type) {
        PathologyConclusion conclusion = new PathologyConclusion();
        conclusion.setValue(text);
        conclusion.setType(type);
        return conclusion;
    }

    private PathologyTechnique createTechnique(String text, TechniqueType type) {
        PathologyTechnique request = new PathologyTechnique();
        request.setValue(text);
        request.setType(type);
        return request;
    }

    @Override
    public List<PathologySample> searchWithStatusAndTerm(List<PathologyStatus> statuses, String searchTerm) {
        List<PathologySample> pathologySamples = baseObjectDAO.getWithStatus(statuses);
        if (StringUtils.isNotBlank(searchTerm)) {
            // The search is by the order's own lab number, not the term: a scanned
            // label carries more than the lab number.
            Sample sample = sampleService.getSampleByAccessionNumber(searchTerm);
            if (sample == null) {
                sample = sampleOfScannedLabel(searchTerm);
            }
            if (sample != null) {
                pathologySamples = baseObjectDAO.searchWithStatusAndAccesionNumber(statuses,
                        sample.getAccessionNumber());
            } else {
                List<PathologySample> filteredpathologySamples = new ArrayList<>();
                pathologySamples.forEach(pathologySample -> {
                    Patient patient = sampleService.getPatient(pathologySample.getSample());
                    if (patient.getPerson().getFirstName().equals(searchTerm)
                            || patient.getPerson().getLastName().equals(searchTerm)) {
                        filteredpathologySamples.add(pathologySample);
                    }
                });
                pathologySamples = filteredpathologySamples;
            }
        }

        return pathologySamples;
    }

    /**
     * The order a scanned block or slide label belongs to. Those labels carry the
     * row's own barcode, which the lab number only opens. The accession lookup
     * reads the lab number off the front of a dotted term, but a scheme with
     * another separator leaves nothing it can read, so the term is looked up as a
     * block or slide barcode. Null when no block or slide carries it.
     */
    private Sample sampleOfScannedLabel(String searchTerm) {
        String barcode = StringUtils.trim(searchTerm);
        PathologySample owner = pathologyBlockDAO.getByBarcode(barcode)
                .map(block -> baseObjectDAO.getCaseOfBlock(block.getId())).or(() -> pathologySlideDAO
                        .getByBarcode(barcode).map(slide -> baseObjectDAO.getCaseOfSlide(slide.getId())))
                .orElse(null);
        return owner == null ? null : owner.getSample();
    }

    @Override
    public Long getCountWithStatusBetweenDates(List<PathologyStatus> statuses, Timestamp from, Timestamp to) {
        return baseObjectDAO.getCountWithStatusBetweenDates(statuses, from, to);
    }
}
