package org.openelisglobal.vector.deconvolution.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.vector.deconvolution.dto.DeconvolutionDTOs.DeconvolutionInitiateRequest;
import org.openelisglobal.vector.deconvolution.dto.DeconvolutionDTOs.DeconvolutionPreview;
import org.openelisglobal.vector.deconvolution.dto.DeconvolutionDTOs.DeconvolutionResult;
import org.openelisglobal.vector.deconvolution.dto.DeconvolutionDTOs.DeconvolutionWorklistRowDTO;
import org.openelisglobal.vector.deconvolution.dto.DeconvolutionDTOs.PoolResultSummary;
import org.openelisglobal.vector.deconvolution.service.VectorDeconvolutionService;
import org.openelisglobal.vector.deconvolution.service.VectorDeconvolutionServiceImpl;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/vector/deconvolution")
@PreAuthorize("hasAnyRole('RESULTS', 'ADMIN')")
public class VectorDeconvolutionRestController extends BaseRestController {

    private static final String VECTOR_DOMAIN = "V";

    @Autowired
    private VectorDeconvolutionService deconvolutionService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private VectorPoolService vectorPoolService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ResultService resultService;

    @Autowired
    private ObservationHistoryService observationHistoryService;

    @Autowired
    private DictionaryService dictionaryService;

    @GetMapping(value = "/worklist", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DeconvolutionWorklistRowDTO>> getWorklist() {
        try {
            List<Sample> samples = sampleService.getAllMatching("domain", VECTOR_DOMAIN);
            List<DeconvolutionWorklistRowDTO> rows = new ArrayList<>();
            for (Sample s : samples) {
                for (VectorPool pool : vectorPoolService.getBySampleId(s.getId())) {
                    if (pool.getParentPool() != null || !Boolean.TRUE.equals(pool.getActive())) {
                        continue;
                    }
                    String status = pool.getDeconvolutionStatus();
                    if (status == null || VectorDeconvolutionServiceImpl.STATUS_NOT_APPLICABLE.equals(status)) {
                        continue;
                    }
                    rows.add(buildRow(s, pool));
                }
            }
            return ResponseEntity.ok(rows);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/initiate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> initiate(@RequestBody DeconvolutionInitiateRequest request, HttpServletRequest http) {
        try {
            DeconvolutionResult result = deconvolutionService.initiate(request, getSysUserId(http));
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(errorBody(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorBody(e.getMessage()));
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody("An unexpected error occurred."));
        }
    }

    private static java.util.Map<String, String> errorBody(String message) {
        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("error", message == null ? "Unknown error" : message);
        body.put("message", message == null ? "Unknown error" : message);
        return body;
    }

    /**
     * Reflex preview — read-only. Shows which test orders will land on each
     * sub-pool before the user commits. Path uses the pool id (the same id passed
     * to {@code POST /initiate}).
     */
    @GetMapping(value = "/preview/{vectorPoolId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DeconvolutionPreview> previewReflexes(@PathVariable Long vectorPoolId) {
        try {
            return ResponseEntity.ok(deconvolutionService.previewReflexes(vectorPoolId));
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Returns all panels for the pool's sample type with tests not yet on the pool.
     * Business logic lives in
     * {@link VectorDeconvolutionService#getAvailablePanelTests}.
     */
    @GetMapping(value = "/pool/{poolId}/available-panel-tests", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<org.openelisglobal.vector.deconvolution.dto.DeconvolutionDTOs.PanelTestGroup>> getAvailablePanelTests(
            @PathVariable Long poolId) {
        try {
            return ResponseEntity.ok(deconvolutionService.getAvailablePanelTests(poolId));
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/pool/{poolId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DeconvolutionResult> getDeconvolution(@PathVariable Long poolId) {
        try {
            return ResponseEntity.ok(deconvolutionService.getDeconvolution(poolId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Supervisor override — manually mark a deconvolution COMPLETE. Used when a
     * positive pool was deconvoluted on paper / outside the system and the
     * supervisor needs to clear the "Decon Needed" tag from the worklist. Should be
     * called sparingly and audited at the route level.
     */
    @PostMapping(value = "/pool/{poolId}/confirm-all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> confirmResultForAllMembers(@PathVariable Long poolId, HttpServletRequest http) {
        try {
            deconvolutionService.confirmResultForAllMembers(poolId, getSysUserId(http));
            return ResponseEntity.ok(java.util.Map.of("status", VectorDeconvolutionServiceImpl.STATUS_COMPLETE));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(errorBody(e.getMessage()));
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody("An unexpected error occurred."));
        }
    }

    @PostMapping(value = "/pool/{poolId}/confirm-result/{analysisId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> confirmResultForAllMembersPerAnalysis(@PathVariable Long poolId,
            @PathVariable String analysisId, HttpServletRequest http) {
        try {
            deconvolutionService.confirmAnalysisForAllMembers(poolId, analysisId, getSysUserId(http));
            return ResponseEntity.ok(java.util.Map.of("status", "ok"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(errorBody(e.getMessage()));
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody("An unexpected error occurred."));
        }
    }

    @PutMapping(value = "/pool/{poolId}/complete", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> forceComplete(@PathVariable Long poolId, HttpServletRequest http) {
        try {
            deconvolutionService.forceComplete(poolId, getSysUserId(http));
            DeconvolutionResult result = new DeconvolutionResult(null, poolId, List.of(), List.of(), 0,
                    VectorDeconvolutionServiceImpl.STATUS_COMPLETE);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(errorBody(e.getMessage()));
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody("An unexpected error occurred."));
        }
    }

    private List<PoolResultSummary> getResultSummariesForPool(VectorPool pool) {
        List<PoolResultSummary> summaries = new ArrayList<>();
        try {
            List<Analysis> analyses = analysisService.getAnalysesByVectorPoolId(String.valueOf(pool.getId()));
            if (analyses == null) {
                return summaries;
            }
            List<SampleItem> members = vectorPoolService.getMembersByPoolId(pool.getId());
            for (Analysis a : analyses) {
                if (a == null || a.getTest() == null) {
                    continue;
                }
                List<Result> results = resultService.getResultsByAnalysis(a);
                if (results == null) {
                    continue;
                }
                for (Result r : results) {
                    String raw = r.getValue();
                    if (raw == null || raw.isBlank()) {
                        continue;
                    }
                    String display = raw;
                    if ("D".equals(r.getResultType())) {
                        try {
                            Dictionary dict = dictionaryService.getDataForId(raw);
                            if (dict != null && dict.getDictEntry() != null) {
                                display = dict.getDictEntry();
                            }
                        } catch (RuntimeException ignored) {
                        }
                    }
                    boolean confirmed = !members.isEmpty() && members.stream().allMatch(member -> {
                        try {
                            return analysisService.getAnalysisBySampleItemAndTest(member.getId(),
                                    a.getTest().getId()) != null;
                        } catch (RuntimeException ex) {
                            return false;
                        }
                    });
                    summaries.add(new PoolResultSummary(a.getTest().getName(), display, a.getId(), confirmed));
                }
            }
        } catch (RuntimeException e) {
            LogEvent.logError(e);
        }
        return summaries;
    }

    private String findPositiveTestNameForPool(VectorPool pool) {
        try {
            List<Analysis> analyses = analysisService.getAnalysesByVectorPoolId(String.valueOf(pool.getId()));
            if (analyses == null) {
                return null;
            }
            for (Analysis a : analyses) {
                if (a == null || a.getTest() == null) {
                    continue;
                }
                List<Result> results = resultService.getResultsByAnalysis(a);
                if (results == null) {
                    continue;
                }
                for (Result r : results) {
                    if (r.getValue() != null && !r.getValue().isBlank()) {
                        return a.getTest().getName();
                    }
                }
            }
        } catch (RuntimeException e) {
            LogEvent.logError(e);
        }
        return null;
    }

    private DeconvolutionWorklistRowDTO buildRow(Sample s, VectorPool intakePool) {
        DeconvolutionWorklistRowDTO row = new DeconvolutionWorklistRowDTO();
        row.setSampleId(parseLong(s.getId()));
        row.setAccessionNumber(s.getAccessionNumber());
        row.setVectorPoolId(intakePool.getId().longValue());
        row.setDeconvolutionStatus(intakePool.getDeconvolutionStatus());
        row.setDeconvolutionOutcomePct(intakePool.getDeconvolutionOutcomePct());
        row.setSamplingSiteName(
                observationHistoryService.getValueForSample(ObservationType.VS_COLLECTION_SITE_NAME, s.getId()));
        row.setPositiveTestName(findPositiveTestNameForPool(intakePool));

        List<VectorPool> allPools = vectorPoolService.getBySampleId(s.getId());
        List<VectorPool> subPools = new ArrayList<>();
        for (VectorPool p : allPools) {
            if (p.getParentPool() != null) {
                subPools.add(p);
            }
        }

        Set<Integer> nonLeafPoolIds = new HashSet<>();
        for (VectorPool sub : subPools) {
            if (sub.getParentPool() != null) {
                nonLeafPoolIds.add(sub.getParentPool().getId());
            }
        }
        List<VectorPool> leaves = new ArrayList<>();
        for (VectorPool sub : subPools) {
            if (!nonLeafPoolIds.contains(sub.getId())) {
                leaves.add(sub);
            }
        }
        row.setChildCount(leaves.size());

        int doneCount = 0;
        for (VectorPool leaf : leaves) {
            if (VectorDeconvolutionServiceImpl.STATUS_COMPLETE.equals(leaf.getDeconvolutionStatus())) {
                doneCount++;
            }
        }
        row.setDoneCount(doneCount);
        row.setPositiveCount(doneCount);
        return row;
    }

    /**
     * Display label for a pool — uses the first member's external_id since pool
     * members share the same suffix convention (".../{-s{N}-m}"). Falls back to
     * "Pool #id" for empty pools.
     */
    private String poolLabel(VectorPool pool) {
        if (pool.getExternalId() != null && !pool.getExternalId().isBlank()) {
            return pool.getExternalId();
        }
        return vectorPoolService.getFirstNonVoidedMemberByPoolId(pool.getId()).map(SampleItem::getExternalId)
                .map(VectorDeconvolutionRestController::stripLastSegment).orElse("Pool #" + pool.getId());
    }

    private static String stripLastSegment(String externalId) {
        if (externalId == null) {
            return null;
        }
        int idx = externalId.lastIndexOf('-');
        return idx > 0 ? externalId.substring(0, idx) : externalId;
    }

    private static long parseLong(String s) {
        try {
            return s == null ? 0L : Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

}
