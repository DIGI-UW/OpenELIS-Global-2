package org.openelisglobal.vector.identification.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl.NoteType;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.vector.common.VectorResultClassifier;
import org.openelisglobal.vector.deconvolution.service.VectorDeconvolutionServiceImpl;
import org.openelisglobal.vector.identification.dto.IdentificationDTOs.BulkIdentifyRequest;
import org.openelisglobal.vector.identification.dto.IdentificationDTOs.BulkIdentifyResult;
import org.openelisglobal.vector.identification.dto.IdentificationDTOs.IdentificationRequest;
import org.openelisglobal.vector.identification.dto.IdentificationDTOs.IdentificationResult;
import org.openelisglobal.vector.identification.dto.IdentificationDTOs.LinkedResultCandidateDTO;
import org.openelisglobal.vector.identification.dto.IdentificationDTOs.SpecimenDetailDTO;
import org.openelisglobal.vector.identification.dto.IdentificationDTOs.WorklistRowDTO;
import org.openelisglobal.vector.identification.service.VectorSpecimenIdentificationService;
import org.openelisglobal.vector.identification.service.VectorSpecimenIdentificationServiceImpl;
import org.openelisglobal.vector.identification.valueholder.VectorSpecimenIdentification;
import org.openelisglobal.vector.service.VectorPoolService;
import org.openelisglobal.vector.valueholder.VectorPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Species Identification Workbench. */
@RestController
@RequestMapping("/rest/vector/identification")
@PreAuthorize("hasAnyRole('RESULTS', 'ADMIN')")
public class VectorIdentificationRestController extends BaseRestController {

    private static final String VECTOR_DOMAIN = "V";

    @Autowired
    private VectorSpecimenIdentificationService identificationService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private ObservationHistoryService observationHistoryService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ResultService resultService;

    @Autowired
    private NoteService noteService;

    @Autowired
    private PanelService panelService;

    @Autowired
    private PanelItemService panelItemService;

    @Autowired
    private VectorPoolService vectorPoolService;

    private static final String BLOODMEAL_PANEL_NAME = "Mosquito Blood-Meal Identification Panel";
    private static final String MOSQUITO_SAMPLE_TYPE = "Mosquito";

    /**
     * status: pending | notstarted | partialid | decon | complete (default
     * pending).
     *
     * <p>
     * One row per root VectorPool (lot == pool per FRS). A sample with N intake
     * pools produces N worklist rows. Samples that have no pool yet are excluded.
     */
    @GetMapping(value = "/worklist", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<WorklistRowDTO>> getWorklist(@RequestParam(required = false) String status) {
        try {
            String filter = status == null || status.isBlank() ? "pending" : status.toLowerCase();
            List<Sample> samples = sampleService.getAllMatching("domain", VECTOR_DOMAIN);

            List<WorklistRowDTO> rows = new ArrayList<>();
            for (Sample s : samples) {
                for (VectorPool rootPool : vectorPoolService.getBySampleId(s.getId())) {
                    if (rootPool.getParentPool() != null || !Boolean.TRUE.equals(rootPool.getActive())) {
                        continue;
                    }
                    if (!includeInWorklist(s, rootPool, filter)) {
                        continue;
                    }
                    rows.add(buildWorklistRow(s, rootPool));
                }
            }
            return ResponseEntity.ok(rows);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/lots/{lotId}/specimens", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<SpecimenDetailDTO>> getSpecimensForLot(@PathVariable Long lotId) {
        try {
            // lotId is a VectorPool id (lot == pool per FRS). Derive sampleId from pool.
            // Unknown pool → empty list (callers treat "no specimens" the same as "lot not
            // found").
            VectorPool pool = vectorPoolService.findById(lotId.intValue()).orElse(null);
            if (pool == null) {
                return ResponseEntity.ok(java.util.Collections.emptyList());
            }
            String sampleId = pool.getSampleId();

            // Scope to THIS pool's members only (lot == pool: expanding pool A
            // must not bleed pool B's specimens into the result). M:N membership
            // means a SampleItem still belongs to the intake pool after decon.
            List<SampleItem> items = vectorPoolService.getMembersByPoolId(pool.getId());
            List<VectorSpecimenIdentification> ids = identificationService.getBySampleId(Long.parseLong(sampleId));

            // Build the full pool tree for the sample (depth-sorted, shallow first)
            // so each SampleItem is assigned to its DEEPEST pool (sub-pool wins).
            List<VectorPool> pools = new ArrayList<>(vectorPoolService.getBySampleId(sampleId));
            java.util.Map<Integer, Integer> depthCache = new java.util.HashMap<>();
            pools.sort((a, b) -> Integer.compare(poolDepth(a, depthCache), poolDepth(b, depthCache)));

            // Pool external ID map — authoritative lot label from vector_pool.external_id.
            java.util.Map<Long, String> poolExternalIdById = new java.util.HashMap<>();
            java.util.Map<Long, String> poolDeconStatusById = new java.util.HashMap<>();
            java.util.Map<String, Long> poolIdByMember = new java.util.HashMap<>();
            java.util.Map<String, Long> parentPoolIdByMember = new java.util.HashMap<>();
            for (VectorPool p : pools) {
                poolExternalIdById.put(p.getId().longValue(), p.getExternalId());
                poolDeconStatusById.put(p.getId().longValue(), p.getDeconvolutionStatus());
                Long parentPoolId = p.getParentPool() == null ? null : p.getParentPool().getId().longValue();
                for (SampleItem member : vectorPoolService.getMembersByPoolId(p.getId())) {
                    poolIdByMember.put(member.getId(), p.getId().longValue());
                    parentPoolIdByMember.put(member.getId(), parentPoolId);
                }
            }

            List<SpecimenDetailDTO> dtos = new ArrayList<>(items.size());
            for (SampleItem item : items) {
                VectorSpecimenIdentification idForItem = findIdForItem(ids, parseLong(item.getId()));
                SpecimenDetailDTO dto = SpecimenDetailDTO.fromIdentification(idForItem);
                dto.setSampleItemId(parseLong(item.getId()));
                dto.setExternalId(item.getExternalId());
                dto.setSortOrder(item.getSortOrder());
                dto.setQuantity(item.getQuantity());
                Long itemPoolId = poolIdByMember.get(item.getId());
                dto.setVectorPoolId(itemPoolId);
                dto.setParentPoolId(parentPoolIdByMember.get(item.getId()));
                dto.setPoolExternalId(itemPoolId != null ? poolExternalIdById.get(itemPoolId) : null);
                Long parentId = parentPoolIdByMember.get(item.getId());
                dto.setParentPoolExternalId(parentId != null ? poolExternalIdById.get(parentId) : null);
                dto.setPoolDeconvolutionStatus(itemPoolId != null ? poolDeconStatusById.get(itemPoolId) : null);
                if (item.getTypeOfSample() != null) {
                    dto.setTypeOfSampleId(parseLong(item.getTypeOfSample().getId()));
                    dto.setTypeOfSampleName(item.getTypeOfSample().getDescription());
                }
                dtos.add(dto);
            }
            return ResponseEntity.ok(dtos);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /** specimenId path param must match request.sampleItemId. */
    @PostMapping(value = "/specimens/{specimenId}/identify", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> identify(@PathVariable Long specimenId, @RequestBody IdentificationRequest request,
            HttpServletRequest http) {
        try {
            if (request.getSampleItemId() == null) {
                request.setSampleItemId(specimenId);
            } else if (!specimenId.equals(request.getSampleItemId())) {
                return ResponseEntity.badRequest()
                        .body(errorBody(400, "Path specimenId does not match request.sampleItemId"));
            }
            IdentificationResult result = identificationService.identify(request, getSysUserId(http));
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            // Validation rejection — surface the actual message so the UI can
            // show "physiologicalState must be ..." instead of a generic error.
            return ResponseEntity.badRequest().body(errorBody(400, e.getMessage()));
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody(500, "An unexpected error occurred."));
        }
    }

    private static java.util.Map<String, Object> errorBody(int statusCode, String message) {
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("statusCode", statusCode);
        body.put("status", statusCode);
        body.put("error", message);
        body.put("message", message);
        return body;
    }

    @PostMapping(value = "/specimens/bulk-identify", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> bulkIdentify(@RequestBody BulkIdentifyRequest request, HttpServletRequest http) {
        try {
            BulkIdentifyResult result = identificationService.bulkIdentify(request, getSysUserId(http));
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(errorBody(400, e.getMessage()));
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody(500, "An unexpected error occurred."));
        }
    }

    /**
     * Candidates for the molecular "Link to Pathogen Result" picker — all Results
     * on this lot's SampleItems with a non-null value.
     */
    @GetMapping(value = "/lots/{lotId}/result-candidates", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<LinkedResultCandidateDTO>> getResultCandidates(@PathVariable Long lotId) {
        try {
            // lotId is a VectorPool id (lot == pool per FRS). Derive sampleId from pool.
            VectorPool lotPool = vectorPoolService.findById(lotId.intValue()).orElse(null);
            if (lotPool == null) {
                return ResponseEntity.notFound().build();
            }
            Sample sample = sampleService.get(lotPool.getSampleId());
            if (sample == null) {
                return ResponseEntity.notFound().build();
            }
            List<Result> results = resultService.getResultsForSample(sample);
            List<LinkedResultCandidateDTO> dtos = new ArrayList<>();
            if (results != null) {
                for (Result r : results) {
                    if (r.getValue() == null || r.getValue().isBlank()) {
                        continue;
                    }
                    String testName = r.getAnalysis() != null && r.getAnalysis().getTest() != null
                            ? r.getAnalysis().getTest().getName()
                            : null;
                    String sampleItemExternalId = r.getAnalysis() != null && r.getAnalysis().getSampleItem() != null
                            ? r.getAnalysis().getSampleItem().getExternalId()
                            : null;
                    dtos.add(new LinkedResultCandidateDTO(parseLong(r.getId()), testName, r.getValue(),
                            sampleItemExternalId));
                }
            }
            return ResponseEntity.ok(dtos);
        } catch (org.hibernate.ObjectNotFoundException notFound) {
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Idempotent and mosquito-only. Returns {@code created} / {@code skipped}
     * counts so the UI can distinguish a fresh insert from a re-click.
     */
    @PostMapping(value = "/specimens/{specimenId}/bloodmeal-panel", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addBloodmealPanel(@PathVariable Long specimenId, HttpServletRequest http) {
        try {
            SampleItem item;
            try {
                item = sampleItemService.get(String.valueOf(specimenId));
            } catch (org.hibernate.ObjectNotFoundException notFound) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(errorBody(404, "sample_item not found: " + specimenId));
            }
            if (item == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(errorBody(404, "sample_item not found: " + specimenId));
            }
            // Frontend already gates this — backend reject is the safety net.
            String sampleTypeName = item.getTypeOfSample() == null ? null : item.getTypeOfSample().getDescription();
            if (!MOSQUITO_SAMPLE_TYPE.equalsIgnoreCase(sampleTypeName)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(errorBody(409, "Blood-meal panel is mosquito-specific; sample_item " + specimenId
                                + " is sample type '" + (sampleTypeName == null ? "<unset>" : sampleTypeName) + "'"));
            }
            Panel panel = panelService.getPanelByName(BLOODMEAL_PANEL_NAME);
            if (panel == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(404,
                        "panel '" + BLOODMEAL_PANEL_NAME + "' is not configured — check example-panels.csv"));
            }
            String sysUserId = getSysUserId(http);
            String notStartedStatusId = SpringContext.getBean(IStatusService.class)
                    .getStatusID(AnalysisStatus.NotStarted);

            // Compare by test_id regardless of analysis status — the tech meant
            // "make sure these tests are present", not "give me a fresh set".
            Set<String> existingTestIds = new HashSet<>();
            List<Analysis> existing = analysisService.getAnalysesBySampleItem(item);
            if (existing != null) {
                for (Analysis a : existing) {
                    if (a.getTest() != null && a.getTest().getId() != null) {
                        existingTestIds.add(a.getTest().getId());
                    }
                }
            }

            List<PanelItem> panelItems = panelItemService.getPanelItemsForPanel(panel.getId());
            int created = 0;
            int skipped = 0;
            for (PanelItem pi : panelItems) {
                if (pi.getTest() == null) {
                    continue;
                }
                if (existingTestIds.contains(pi.getTest().getId())) {
                    skipped++;
                    continue;
                }
                Analysis analysis = new Analysis();
                analysis.setSampleItem(item);
                analysis.setTest(pi.getTest());
                analysis.setTestSection(pi.getTest().getTestSection());
                analysis.setStatusId(notStartedStatusId);
                analysis.setRevision("0");
                analysis.setSysUserId(sysUserId);
                analysis.setAnalysisType("MANUAL");
                if (item.getTypeOfSample() != null) {
                    analysis.setSampleTypeName(item.getTypeOfSample().getDescription());
                }
                analysisService.insert(analysis);
                created++;
            }
            LogEvent.logInfo(this.getClass().getName(), "addBloodmealPanel",
                    "sample_item " + specimenId + " created=" + created + " skipped=" + skipped);
            java.util.Map<String, Integer> body = new java.util.HashMap<>();
            body.put("created", created);
            body.put("skipped", skipped);
            return ResponseEntity.ok(body);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody(500, "An unexpected error occurred."));
        }
    }

    /** Records an audit Note so reviewers can see the dismissal was deliberate. */
    @PostMapping(value = "/specimens/{specimenId}/bloodmeal-dismiss", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> dismissBloodmealSuggestion(@PathVariable Long specimenId, HttpServletRequest http) {
        try {
            SampleItem item = sampleItemService.get(String.valueOf(specimenId));
            if (item == null) {
                return ResponseEntity.notFound().build();
            }
            Note note = new Note();
            note.setReferenceId(String.valueOf(specimenId));
            note.setReferenceTableId(
                    org.openelisglobal.sampleitem.service.SampleItemServiceImpl.getSampleItemTableReferenceId());
            note.setNoteType(NoteType.INTERNAL.getDBCode());
            note.setSubject("V-03 blood-meal");
            note.setText("Blood-meal panel suggested but not ordered");
            note.setSysUserId(getSysUserId(http));
            noteService.insert(note);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/specimens/{specimenId}/identification", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VectorSpecimenIdentification> getIdentification(@PathVariable Long specimenId) {
        try {
            return identificationService.getBySampleItemId(specimenId).map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private boolean includeInWorklist(Sample s, VectorPool pool, String filter) {
        String idStatus = pool.getIdentificationStatus();
        String decStatus = pool.getDeconvolutionStatus();
        switch (filter) {
        case "notstarted":
            return idStatus == null || VectorSpecimenIdentificationServiceImpl.STATUS_RECEIVED.equals(idStatus);
        case "partialid":
            return VectorSpecimenIdentificationServiceImpl.STATUS_IDENTIFICATION_IN_PROGRESS.equals(idStatus);
        case "decon":
            return "PENDING".equals(decStatus) || "IN_PROGRESS".equals(decStatus);
        case "complete":
            return VectorSpecimenIdentificationServiceImpl.STATUS_COMPLETE.equals(idStatus);
        case "pending":
        default:
            // Mirrors the mock's pending queue: identification not finished OR any
            // decon work in flight (PENDING / IN_PROGRESS). Once both id and decon
            // are COMPLETE, the lot drops out of "pending" and into "complete".
            return !VectorSpecimenIdentificationServiceImpl.STATUS_COMPLETE.equals(idStatus)
                    || "PENDING".equals(decStatus) || "IN_PROGRESS".equals(decStatus);
        }
    }

    private WorklistRowDTO buildWorklistRow(Sample s, VectorPool rootPool) {
        WorklistRowDTO row = new WorklistRowDTO();
        row.setSampleId(parseLong(s.getId()));
        row.setVectorPoolId(rootPool.getId().longValue());
        // Lot identifier lives on the pool (lot == pool per FRS). Intake pools
        // mirror accession_number; sub-pools carry a derived "<acc>-<n>" label.
        // Fall back to accession_number for legacy pools without external_id.
        String lotExternalId = rootPool.getExternalId() != null ? rootPool.getExternalId() : s.getAccessionNumber();
        row.setLotExternalId(lotExternalId);
        row.setAccessionNumber(s.getAccessionNumber());
        row.setIdentificationStatus(rootPool.getIdentificationStatus());
        row.setDeconvolutionStatus(rootPool.getDeconvolutionStatus());

        row.setSamplingSiteName(
                observationHistoryService.getValueForSample(ObservationType.VS_COLLECTION_SITE_NAME, s.getId()));

        // Scope specimens to this pool's members (lot == pool per FRS).
        List<SampleItem> items = vectorPoolService.getMembersByPoolId(rootPool.getId());

        // Collection date: the order-entry XML pipeline writes `date='YYYY-MM-DD'`
        // to each sample_item.collection_date — NOT to sample.collection_date.
        // Fall back to Sample.collection_date for legacy rows.
        java.sql.Timestamp collectionDate = items.stream().map(SampleItem::getCollectionDate)
                .filter(java.util.Objects::nonNull).findFirst().orElse(s.getCollectionDate());
        row.setCollectionDate(collectionDate);

        // Organism groups: DISTINCT typeOfSample descriptions across this pool's
        // members, preserving first-seen order.
        java.util.LinkedHashSet<String> distinctGroups = new java.util.LinkedHashSet<>();
        for (SampleItem item : items) {
            if (item.getTypeOfSample() != null && item.getTypeOfSample().getDescription() != null) {
                distinctGroups.add(item.getTypeOfSample().getDescription());
            }
        }
        if (distinctGroups.isEmpty()) {
            String observed = observationHistoryService.getValueForSample(ObservationType.VS_SAMPLE_TYPE_ID, s.getId());
            if (observed != null && !observed.isBlank()) {
                distinctGroups.add(observed);
            }
        }
        row.setOrganismGroups(new ArrayList<>(distinctGroups));
        row.setOrganismGroup(distinctGroups.isEmpty() ? null : distinctGroups.iterator().next());

        // Specimen count: members of this pool only; exclude the pool SampleItem
        // itself (qty > 1) so the count reflects individual vectors.
        long total = items.stream().filter(i -> i.getQuantity() == null || i.getQuantity() <= 1.0).count();
        if (total == 0) {
            total = items.size();
        }
        row.setTotalSpecimens(total);
        List<Long> memberIds = items.stream().map(i -> parseLong(i.getId())).filter(id -> id > 0)
                .collect(java.util.stream.Collectors.toList());
        row.setIdentifiedSpecimens(identificationService.countBySampleItemIds(memberIds));

        row.setPositiveTestName(findPositiveTestName(s));

        long pendingSubPools = vectorPoolService.getBySampleId(s.getId()).stream().filter(p -> p.getParentPool() != null
                && VectorDeconvolutionServiceImpl.STATUS_PENDING.equals(p.getDeconvolutionStatus())).count();
        row.setPendingSubPoolCount((int) pendingSubPools);

        return row;
    }

    private String findPositiveTestName(Sample s) {
        // Defensive: nice-to-have field, MUST NOT break the worklist if any
        // service is unavailable (covers test contexts that don't wire every
        // dependency).
        try {
            if (analysisService == null) {
                return null;
            }
            List<Analysis> analyses = analysisService.getAnalysesBySampleId(s.getId());
            if (analyses == null) {
                return null;
            }
            String finalizedStatusId = SpringContext.getBean(IStatusService.class)
                    .getStatusID(AnalysisStatus.Finalized);
            for (Analysis a : analyses) {
                if (a == null || a.getTest() == null) {
                    continue;
                }
                if (!finalizedStatusId.equals(a.getStatusId())) {
                    continue;
                }
                if (resultService == null) {
                    continue;
                }
                List<Result> results = resultService.getResultsByAnalysis(a);
                if (results == null) {
                    continue;
                }
                for (Result r : results) {
                    if (r.getValue() != null && VectorResultClassifier.isPositiveValue(r.getValue())) {
                        return a.getTest().getName();
                    }
                }
            }
        } catch (RuntimeException e) {
            // Status service / Spring context unavailable in some test paths;
            // silent fallback rather than 500ing the whole worklist.
            return null;
        }
        return null;
    }

    private VectorSpecimenIdentification findIdForItem(List<VectorSpecimenIdentification> ids, long sampleItemId) {
        for (VectorSpecimenIdentification id : ids) {
            if (id.getSampleItemId() != null && id.getSampleItemId() == sampleItemId) {
                return id;
            }
        }
        return null;
    }

    private static long parseLong(String s) {
        try {
            return s == null ? 0L : Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * Pool depth in the decon hierarchy: 0 for the intake pool (parentPool ==
     * null), {@code 1 + depth(parent)} otherwise. Memoised because the sort
     * comparator hits the same pools repeatedly.
     */
    private static int poolDepth(VectorPool pool, java.util.Map<Integer, Integer> cache) {
        if (pool == null) {
            return -1;
        }
        Integer cached = cache.get(pool.getId());
        if (cached != null) {
            return cached;
        }
        int d = pool.getParentPool() == null ? 0 : poolDepth(pool.getParentPool(), cache) + 1;
        cache.put(pool.getId(), d);
        return d;
    }

}
