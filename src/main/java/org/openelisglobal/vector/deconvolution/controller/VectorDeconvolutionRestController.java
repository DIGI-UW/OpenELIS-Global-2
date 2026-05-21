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
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.vector.common.VectorResultClassifier;
import org.openelisglobal.vector.deconvolution.dto.DeconvolutionDTOs.DeconvolutionInitiateRequest;
import org.openelisglobal.vector.deconvolution.dto.DeconvolutionDTOs.DeconvolutionNode;
import org.openelisglobal.vector.deconvolution.dto.DeconvolutionDTOs.DeconvolutionPreview;
import org.openelisglobal.vector.deconvolution.dto.DeconvolutionDTOs.DeconvolutionResult;
import org.openelisglobal.vector.deconvolution.dto.DeconvolutionDTOs.DeconvolutionWorklistRowDTO;
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
    private AnalysisService analysisService;

    @Autowired
    private ResultService resultService;

    @Autowired
    private VectorPoolService vectorPoolService;

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

    @GetMapping(value = "/pool/{poolId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DeconvolutionResult> getDeconvolution(@PathVariable Long poolId) {
        try {
            VectorPool intakePool = vectorPoolService.findById(poolId.intValue()).orElse(null);
            if (intakePool == null) {
                return ResponseEntity.notFound().build();
            }
            String status = intakePool.getDeconvolutionStatus();
            if (status == null || VectorDeconvolutionServiceImpl.STATUS_NOT_APPLICABLE.equals(status)) {
                return ResponseEntity.notFound().build();
            }
            Sample sample;
            try {
                sample = sampleService.get(intakePool.getSampleId());
            } catch (org.hibernate.ObjectNotFoundException e) {
                return ResponseEntity.notFound().build();
            }
            Long sampleId = Long.parseLong(sample.getId());

            List<VectorPool> allPools = vectorPoolService.getBySampleId(String.valueOf(sampleId));

            // Intake (top-level) pool isn't a deconvolution node.
            List<VectorPool> subPools = new ArrayList<>();
            for (VectorPool p : allPools) {
                if (p.getParentPool() != null) {
                    subPools.add(p);
                }
            }

            List<Long> childIds = new ArrayList<>();
            List<String> childExternalIds = new ArrayList<>();
            List<DeconvolutionNode> tree = new ArrayList<>();
            Set<Integer> nonLeafPoolIds = new HashSet<>();
            for (VectorPool sub : subPools) {
                if (sub.getParentPool() != null) {
                    nonLeafPoolIds.add(sub.getParentPool().getId());
                }
            }
            for (VectorPool sub : subPools) {
                int memberCount = vectorPoolService.countMembersByPoolId(sub.getId());
                String label = poolLabel(sub);
                Long parentPoolId = sub.getParentPool() == null ? null : sub.getParentPool().getId().longValue();
                tree.add(new DeconvolutionNode(sub.getId().longValue(), label, parentPoolId, memberCount));

                for (SampleItem member : vectorPoolService.getMembersByPoolId(sub.getId())) {
                    childIds.add(parseLong(member.getId()));
                    childExternalIds.add(member.getExternalId());
                }
            }

            DeconvolutionResult result = new DeconvolutionResult(sampleId, poolId, childIds, childExternalIds, 0,
                    status);
            result.setDeconvolutionOutcomePct(intakePool.getDeconvolutionOutcomePct());
            result.setTree(tree);

            // Leaf totals drive "N of M positive (X%)" once the lot is COMPLETE.
            String finalizedStatusId = SpringContext.getBean(IStatusService.class)
                    .getStatusID(AnalysisStatus.Finalized);
            int leafTotal = 0;
            int leafPositive = 0;
            for (VectorPool sub : subPools) {
                if (nonLeafPoolIds.contains(sub.getId())) {
                    continue;
                }
                leafTotal++;
                List<Analysis> analyses = analysisService.getAnalysesByVectorPoolId(String.valueOf(sub.getId()));
                if (analyses == null) {
                    continue;
                }
                boolean any = false;
                for (Analysis a : analyses) {
                    if (!finalizedStatusId.equals(a.getStatusId())) {
                        continue;
                    }
                    List<Result> results = resultService.getResultsByAnalysis(a);
                    if (results == null) {
                        continue;
                    }
                    for (Result r : results) {
                        if (VectorResultClassifier.isPositiveResult(r)) {
                            any = true;
                            break;
                        }
                    }
                    if (any) {
                        break;
                    }
                }
                if (any) {
                    leafPositive++;
                }
            }
            result.setLeafTotalCount(leafTotal);
            result.setLeafPositiveCount(leafPositive);
            return ResponseEntity.ok(result);
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

    private DeconvolutionWorklistRowDTO buildRow(Sample s, VectorPool intakePool) {
        DeconvolutionWorklistRowDTO row = new DeconvolutionWorklistRowDTO();
        row.setSampleId(parseLong(s.getId()));
        row.setAccessionNumber(s.getAccessionNumber());
        row.setVectorPoolId(intakePool.getId().longValue());
        row.setDeconvolutionStatus(intakePool.getDeconvolutionStatus());
        row.setDeconvolutionOutcomePct(intakePool.getDeconvolutionOutcomePct());

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

        String finalizedStatusId = SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Finalized);
        int doneCount = 0;
        int positiveCount = 0;
        for (VectorPool leaf : leaves) {
            List<Analysis> analyses = analysisService.getAnalysesByVectorPoolId(String.valueOf(leaf.getId()));
            if (analyses == null || analyses.isEmpty()) {
                continue;
            }
            boolean allFinalized = true;
            boolean anyPositive = false;
            for (Analysis a : analyses) {
                if (!finalizedStatusId.equals(a.getStatusId())) {
                    allFinalized = false;
                }
                List<Result> results = resultService.getResultsByAnalysis(a);
                if (results == null) {
                    continue;
                }
                for (Result r : results) {
                    if (VectorResultClassifier.isPositiveResult(r)) {
                        anyPositive = true;
                        break;
                    }
                }
            }
            if (allFinalized) {
                doneCount++;
            }
            if (anyPositive) {
                positiveCount++;
            }
        }
        row.setDoneCount(doneCount);
        row.setPositiveCount(positiveCount);
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
