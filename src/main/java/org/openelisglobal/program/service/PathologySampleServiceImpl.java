package org.openelisglobal.program.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import org.openelisglobal.program.util.DesignationScheme;
import org.openelisglobal.program.util.PathologyDesignations;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample;
import org.openelisglobal.program.valueholder.pathology.CassetteState;
import org.openelisglobal.program.valueholder.pathology.PathologyBlock;
import org.openelisglobal.program.valueholder.pathology.PathologyConclusion;
import org.openelisglobal.program.valueholder.pathology.PathologyConclusion.ConclusionType;
import org.openelisglobal.program.valueholder.pathology.PathologyRequest;
import org.openelisglobal.program.valueholder.pathology.PathologyRequest.RequestStatus;
import org.openelisglobal.program.valueholder.pathology.PathologyRequest.RequestType;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.openelisglobal.program.valueholder.pathology.PathologySlide;
import org.openelisglobal.program.valueholder.pathology.PathologyTechnique;
import org.openelisglobal.program.valueholder.pathology.PathologyTechnique.TechniqueType;
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

    /** Dictionary category a laboratory fills with the tissue types it cuts. */
    private static final String TISSUE_TYPE_CATEGORY = "pathology_tissue_types";

    private static final ObjectMapper AUDIT_PAYLOAD_MAPPER = new ObjectMapper();

    @Autowired
    protected PathologySampleDAO baseObjectDAO;

    @Autowired
    private PathologyBlockDAO pathologyBlockDAO;

    @Autowired
    private PathologySlideDAO pathologySlideDAO;

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
        return baseObjectDAO.get(pathologySampleId).map(pathologySample -> new ArrayList<>(pathologySample.getSlides()))
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
        return storedState;
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
        slide.setLastupdated(stored.getLastupdated());
        return slide;
    }

    @Transactional
    @Override
    public void updateWithFormValues(Integer pathologySampleId, PathologySampleForm form) {
        if (form.getSlides() != null) {
            form.getSlides().forEach(ClinicalAttachmentService::validate);
        }
        if (form.getReports() != null) {
            form.getReports().forEach(ClinicalAttachmentService::validate);
        }
        // copying is so we get an object that is detached from hibernate
        PathologySample storedCase = get(pathologySampleId);
        PathologySample pathologySample = copyPathologySample(storedCase);
        PathologySample storedState = auditedStateOf(storedCase);
        pathologySample.setSysUserId(form.getSystemUserId());
        if (!GenericValidator.isBlankOrNull(form.getAssignedPathologistId())) {
            pathologySample.setPathologist(systemUserService.get(form.getAssignedPathologistId()));
        }
        if (!GenericValidator.isBlankOrNull(form.getAssignedTechnicianId())) {
            pathologySample.setTechnician(systemUserService.get(form.getAssignedTechnicianId()));
        }
        pathologySample.setStatus(form.getStatus());
        DesignationScheme scheme = DesignationScheme.fromConfiguration();
        String accessionNumber = pathologySample.getSample() == null ? null
                : pathologySample.getSample().getAccessionNumber();
        List<String> cassettesCut = reconcileBlocks(pathologySample, form.getBlocks(), scheme, accessionNumber);
        reconcileSlides(pathologySample, form.getSlides(), scheme, accessionNumber);
        pathologySample.setGrossExam(form.getGrossExam());
        // The flag records what grossing found. Past the stage an untick would make a
        // decalcified specimen read as one that never needed it, so it is no longer
        // taken.
        if (form.getDecalcificationRequired() != null && !pastDecalcification(storedCase)) {
            pathologySample.setDecalcificationRequired(form.getDecalcificationRequired());
        }
        if (movesInto(PathologyStatus.DECALCIFICATION, storedCase, form)
                || movesOnFrom(PathologyStatus.DECALCIFICATION, storedCase, form)) {
            // A case that went through the stage must never read as not needing it.
            pathologySample.setDecalcificationRequired(true);
        }
        pathologySample.setMicroscopyExam(form.getMicroscopyExam());
        pathologySample.getConclusions().removeAll(pathologySample.getConclusions());
        pathologySample.getConclusions().add(createConclusion(form.getConclusionText(), ConclusionType.TEXT));
        if (form.getConclusions() != null)
            pathologySample.getConclusions().addAll(form.getConclusions().stream()
                    .map(e -> createConclusion(e, ConclusionType.DICTIONARY)).collect(Collectors.toList()));
        pathologySample.getRequests().removeAll(pathologySample.getRequests());
        if (form.getRequests() != null) {
            pathologySample.getRequests()
                    .addAll(form.getRequests().stream()
                            .map(e -> createRequest(e.getValue(), RequestType.DICTIONARY, e.getStatus()))
                            .collect(Collectors.toList()));
        }
        pathologySample.getTechniques().removeAll(pathologySample.getTechniques());
        if (form.getTechniques() != null)
            pathologySample.getTechniques().addAll(form.getTechniques().stream()
                    .map(e -> createTechnique(e, TechniqueType.DICTIONARY)).collect(Collectors.toList()));
        pathologySample.getReports().removeAll(pathologySample.getReports());
        if (form.getReports() != null)
            form.getReports().stream().forEach(e -> e.setId(null));
        pathologySample.getReports().addAll(form.getReports());
        if (form.getRelease()) {
            validatePathologySample(pathologySample, form);
        }
        if (form.getReferToImmunoHistoChemistry()) {
            referToImmunoHistoChemistry(pathologySample, form);
        }
        try {
            baseObjectDAO.evict(storedCase);
            PathologySample savedCase = updateAgainst(pathologySample, storedState);
            recordCassettesCut(savedCase, cassettesCut, pathologySample.getSysUserId());
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
        if (found.isEmpty() || !found.get().isActive()) {
            return found;
        }

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
    public Optional<PathologySlide> deactivateSlide(Integer slideId, String reason, String sysUserId) {
        Optional<PathologySlide> found = pathologySlideDAO.get(slideId);
        if (found.isEmpty() || !found.get().isActive()) {
            return found;
        }

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
     */
    private void reconcileSlides(PathologySample pathologySample, List<PathologySlideForm> postedSlides,
            DesignationScheme scheme, String accessionNumber) {
        if (postedSlides == null) {
            return;
        }
        List<PathologySlide> slidesOnCase = pathologySample.getSlides();
        for (PathologySlideForm posted : postedSlides) {
            if (posted.getId() != null) {
                applyToSlide(slideOnCase(pathologySample, posted.getId()), posted);
            } else {
                slidesOnCase.add(cutSlide(posted, pathologySample, slidesOnCase, scheme, accessionNumber));
            }
        }
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

    /**
     * An unchanged value is never checked again, so an entry retired after a
     * cassette recorded it cannot block a save of the case.
     */
    private String tissueTypeAfterSave(String stored, String posted) {
        String requested = StringUtils.trimToNull(posted);
        if (requested == null || Objects.equals(requested, stored)) {
            return requested;
        }
        if (!isActiveTissueType(requested)) {
            throw new PathologyCaseRuleException(
                    "tissue type " + requested + " is not an active entry of " + TISSUE_TYPE_CATEGORY);
        }
        return requested;
    }

    /**
     * A tissue type a cassette may be given: an entry of the laboratory's own
     * tissue category that is still in use.
     */
    private boolean isActiveTissueType(String dictionaryId) {
        try {
            Integer.parseInt(dictionaryId);
        } catch (NumberFormatException e) {
            // The dictionary key binds as an integer, so anything else names no entry
            // and is the client's mistake, not a failure of the lookup.
            return false;
        }
        Dictionary entry = dictionaryService.getDataForId(dictionaryId);
        return entry != null && "Y".equals(entry.getIsActive()) && entry.getDictionaryCategory() != null
                && TISSUE_TYPE_CATEGORY.equals(entry.getDictionaryCategory().getCategoryName());
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
     * Whether this save moves the case onto the stage from another one; a save of a
     * case already standing there does not.
     */
    private static boolean movesInto(PathologyStatus stage, PathologySample storedCase, PathologySampleForm form) {
        return form.getStatus() == stage && storedCase.getStatus() != stage;
    }

    /**
     * Whether this save takes a case standing at the stage on to a later one, so
     * the case has been through it; a move back to an earlier stage has not.
     */
    private static boolean movesOnFrom(PathologyStatus stage, PathologySample storedCase, PathologySampleForm form) {
        return storedCase.getStatus() == stage && form.getStatus() != null
                && form.getStatus().ordinal() > stage.ordinal();
    }

    /**
     * A retired slide is left as it is. An image is only replaced when one was
     * posted: the case view sends the slide rows back without their stored images,
     * and treating that as an erasure would lose the image on the next save of any
     * other field.
     */
    private void applyToSlide(PathologySlide slide, PathologySlide posted) {
        if (!slide.isActive()) {
            return;
        }
        slide.setLocation(posted.getLocation());
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

    private PathologySlide cutSlide(PathologySlide posted, PathologySample pathologySample,
            List<PathologySlide> slidesOnCase, DesignationScheme scheme, String accessionNumber) {
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
        slide.setImage(posted.getImage());
        slide.setFileType(posted.getFileType());
        slide.setSysUserId(pathologySample.getSysUserId());
        return slide;
    }

    private PathologyBlock blockOnCase(PathologySample pathologySample, Integer blockId) {
        for (PathologyBlock block : pathologySample.getBlocks()) {
            if (blockId.equals(block.getId())) {
                return block;
            }
        }
        throw new PathologyCaseRuleException(
                "block " + blockId + " is not on pathology case " + pathologySample.getId());
    }

    private PathologySlide slideOnCase(PathologySample pathologySample, Integer slideId) {
        for (PathologySlide slide : pathologySample.getSlides()) {
            if (slideId.equals(slide.getId())) {
                return slide;
            }
        }
        throw new PathologyCaseRuleException(
                "slide " + slideId + " is not on pathology case " + pathologySample.getId());
    }

    /**
     * Only a block the case already holds, has persisted and still has in use can
     * be named: a cassette added in the same post has no id yet, so the slides cut
     * from it are added on the save that follows.
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

    private void validatePathologySample(PathologySample pathologySample, PathologySampleForm form) {
        pathologySample.setStatus(PathologyStatus.COMPLETED);
        Sample sample = pathologySample.getSample();
        Patient patient = sampleService.getPatient(sample);
        ResultsUpdateDataSet actionDataSet = new ResultsUpdateDataSet(form.getSystemUserId());

        ResultsLoadUtility resultsUtility = SpringContext.getBean(ResultsLoadUtility.class);
        List<TestResultItem> testResultItems = resultsUtility.getGroupedTestsForSample(sample);
        for (TestResultItem testResultItem : testResultItems) {
            if (!testResultItem.getIsGroupSeparator()) {
                if (ResultType.isTextOnlyVariant(testResultItem.getResultType())) {
                    testResultItem.setResultValue(MessageUtil.getMessage("result.pathology.seereport"));
                }
                Analysis analysis = analysisService.get(testResultItem.getAnalysisId());
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
        sample.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(OrderStatus.Finished));
    }

    private void referToImmunoHistoChemistry(PathologySample pathologySample, PathologySampleForm form) {
        List<Test> immunoHistologyTests = new ArrayList<>();
        if (!form.getImmunoHistoChemistryTestIds().isEmpty()) {
            form.getImmunoHistoChemistryTestIds().forEach(id -> {
                Test t = testService.get(id);
                if (t != null) {
                    immunoHistologyTests.add(t);
                }
            });
        }

        ImmunohistochemistrySample immunoHistoSample = immunohistochemistrySampleService
                .getByPathologySampleId(pathologySample.getId());
        if (immunoHistoSample == null) {
            immunoHistoSample = new ImmunohistochemistrySample();
        }
        immunoHistoSample.setProgram(pathologySample.getProgram());
        immunoHistoSample.setQuestionnaireResponseUuid(pathologySample.getQuestionnaireResponseUuid());
        immunoHistoSample.setSample(pathologySample.getSample());
        immunoHistoSample.setPathologySample(pathologySample);
        immunoHistoSample.setReffered(true);
        immunohistochemistrySampleService.save(immunoHistoSample);

        if (immunoHistologyTests.isEmpty()) {
            return;
        }
        List<Analysis> analyses = analysisService.getAnalysesBySampleId(pathologySample.getSample().getId());
        if (analyses == null || analyses.isEmpty()) {
            return;
        }
        Analysis currentAnalysis = analyses.get(0);
        immunoHistologyTests.forEach(test -> {
            CreateNewAnalysis(test, currentAnalysis, pathologySample.getProgram().getProgramName(),
                    form.getSystemUserId());
        });
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
        TestSection testSection = testSectionService.getTestSectionByName("Immunohistochemistry");
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

    private PathologyConclusion createConclusion(String text, ConclusionType type) {
        PathologyConclusion conclusion = new PathologyConclusion();
        conclusion.setValue(text);
        conclusion.setType(type);
        return conclusion;
    }

    /**
     * A request the case view raises carries no status of its own: the status is
     * chosen later, when the request is answered or withdrawn.
     * {@link PathologyRequest} declares {@link RequestStatus#OPENED} as its own
     * default for exactly that reason, but passing the form's null through would
     * overwrite it and leave a row that reads as neither open nor closed, which no
     * query can see and no screen can act on. A request that has just been raised
     * is open.
     */
    private PathologyRequest createRequest(String text, RequestType type, RequestStatus status) {
        PathologyRequest request = new PathologyRequest();
        request.setValue(text);
        request.setType(type);
        request.setStatus(status == null ? RequestStatus.OPENED : status);
        return request;
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
            Sample sample = sampleService.getSampleByAccessionNumber(searchTerm);
            if (sample != null) {
                pathologySamples = baseObjectDAO.searchWithStatusAndAccesionNumber(statuses, searchTerm);
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

    @Override
    public Long getCountWithStatusBetweenDates(List<PathologyStatus> statuses, Timestamp from, Timestamp to) {
        return baseObjectDAO.getCountWithStatusBetweenDates(statuses, from, to);
    }
}
