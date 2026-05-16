package org.openelisglobal.vector.identification.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.dictionarycategory.service.DictionaryCategoryService;
import org.openelisglobal.dictionarycategory.valueholder.DictionaryCategory;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.vector.identification.dao.VectorSpecimenIdentificationDAO;
import org.openelisglobal.vector.identification.dto.IdentificationDTOs.BulkIdentifyRequest;
import org.openelisglobal.vector.identification.dto.IdentificationDTOs.BulkIdentifyResult;
import org.openelisglobal.vector.identification.dto.IdentificationDTOs.IdentificationRequest;
import org.openelisglobal.vector.identification.dto.IdentificationDTOs.IdentificationResult;
import org.openelisglobal.vector.identification.valueholder.VectorMolecularRecord;
import org.openelisglobal.vector.identification.valueholder.VectorSpecimenIdentification;
import org.openelisglobal.vector.service.VectorPoolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VectorSpecimenIdentificationServiceImpl
        extends AuditableBaseObjectServiceImpl<VectorSpecimenIdentification, Long>
        implements VectorSpecimenIdentificationService {

    public static final String STATUS_RECEIVED = "RECEIVED";
    public static final String STATUS_IDENTIFICATION_IN_PROGRESS = "IDENTIFICATION_IN_PROGRESS";
    public static final String STATUS_COMPLETE = "COMPLETE";

    // INSDC accession pattern.
    private static final Pattern GENBANK_ACCESSION_PATTERN = Pattern.compile("^[A-Z]{1,2}[0-9]{5,8}$");

    @Autowired
    protected VectorSpecimenIdentificationDAO baseObjectDAO;

    @Autowired
    private VectorMolecularRecordService molecularRecordService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private VectorPoolService vectorPoolService;

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private DictionaryCategoryService dictionaryCategoryService;

    // Dictionary category names the FRS pegs to validator gates. Driving
    // validation from these is the single-source-of-truth fix for "lab admin
    // added NYMPH via dictionary admin but backend rejects it" drift.
    static final String CATEGORY_PHYSIOLOGICAL_STATE = "vecPhysiologicalState";
    static final String CATEGORY_LIFECYCLE_STAGES = "vecLifecycleStages";

    public VectorSpecimenIdentificationServiceImpl() {
        super(VectorSpecimenIdentification.class);
    }

    @Override
    protected VectorSpecimenIdentificationDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VectorSpecimenIdentification> getBySampleItemId(Long sampleItemId) {
        return baseObjectDAO.getBySampleItemId(sampleItemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VectorSpecimenIdentification> getBySampleId(Long sampleId) {
        return baseObjectDAO.getBySampleId(sampleId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countBySampleId(Long sampleId) {
        return baseObjectDAO.countBySampleId(sampleId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countBySampleItemIds(List<Long> sampleItemIds) {
        return baseObjectDAO.countBySampleItemIds(sampleItemIds);
    }

    @Override
    @Transactional
    public IdentificationResult identify(IdentificationRequest request, String sysUserId) {
        validateCommonFields(request.getSampleItemId(), request.getVectorSpeciesId(), request.getIdentificationMethod(),
                request.getConfidence(), request.getPhysiologicalState(), request.getLifecycleStage(),
                request.getNotes());
        validateGenbankAccession(request.getGenbankAccession());

        VectorSpecimenIdentification saved = applyToOne(request.getSampleItemId(), request.getVectorSpeciesId(),
                request.getIdentificationMethod(), request.getConfidence(), request.getNotes(),
                request.getPhysiologicalState(), request.getLifecycleStage(), sysUserId);

        if (involvesMolecular(saved.getIdentificationMethod()) && hasAnyMolecularField(request)) {
            persistMolecularRecord(saved.getId(), request, sysUserId);
        }

        Long sampleId = resolveSampleIdFor(request.getSampleItemId());
        String newStatus = recomputeSampleIdentificationStatus(sampleId);

        boolean bloodMealSuggested = VectorSpecimenIdentification.PHYS_BLOOD_FED.equals(saved.getPhysiologicalState());

        return new IdentificationResult(saved, bloodMealSuggested, newStatus);
    }

    @Override
    @Transactional
    public BulkIdentifyResult bulkIdentify(BulkIdentifyRequest request, String sysUserId) {
        if (request == null) {
            throw new IllegalArgumentException("bulk identify request must not be null");
        }
        if (request.getSampleItemIds() == null || request.getSampleItemIds().isEmpty()) {
            throw new IllegalArgumentException("sampleItemIds must not be empty");
        }
        validateCommonFields(request.getSampleItemIds().get(0), request.getVectorSpeciesId(),
                request.getIdentificationMethod(), request.getConfidence(), request.getPhysiologicalState(),
                request.getLifecycleStage(), request.getNotes());

        List<VectorSpecimenIdentification> identifications = new ArrayList<>(request.getSampleItemIds().size());
        Map<Long, String> sampleStatuses = new LinkedHashMap<>();

        for (Long sampleItemId : request.getSampleItemIds()) {
            if (sampleItemId == null) {
                throw new IllegalArgumentException("sampleItemIds must not contain nulls");
            }
            VectorSpecimenIdentification saved = applyToOne(sampleItemId, request.getVectorSpeciesId(),
                    request.getIdentificationMethod(), request.getConfidence(), request.getNotes(),
                    request.getPhysiologicalState(), request.getLifecycleStage(), sysUserId);
            identifications.add(saved);

            // Defer Sample status recompute to single-pass below: 25 specimens
            // in one lot → 1 status update, not 25.
            Long sampleId = resolveSampleIdFor(sampleItemId);
            if (sampleId != null) {
                sampleStatuses.putIfAbsent(sampleId, null);
            }
        }

        for (Map.Entry<Long, String> entry : sampleStatuses.entrySet()) {
            entry.setValue(recomputeSampleIdentificationStatus(entry.getKey()));
        }

        boolean bloodMealSuggested = VectorSpecimenIdentification.PHYS_BLOOD_FED
                .equals(request.getPhysiologicalState());

        return new BulkIdentifyResult(identifications, sampleStatuses, bloodMealSuggested);
    }

    private VectorSpecimenIdentification applyToOne(Long sampleItemId, Long vectorSpeciesId,
            String identificationMethod, String confidence, String notes, String physiologicalState,
            String lifecycleStage, String sysUserId) {
        VectorSpecimenIdentification entity = baseObjectDAO.getBySampleItemId(sampleItemId).orElseGet(() -> {
            VectorSpecimenIdentification fresh = new VectorSpecimenIdentification();
            fresh.setSampleItemId(sampleItemId);
            return fresh;
        });

        entity.setVectorSpeciesId(vectorSpeciesId);
        entity.setIdentificationMethod(identificationMethod);
        entity.setConfidence(confidence);
        entity.setNotes(notes);
        entity.setPhysiologicalState(physiologicalState);
        entity.setLifecycleStage(lifecycleStage);
        entity.setIdentifiedByUserId(parseUserId(sysUserId));
        entity.setSysUserId(sysUserId);

        if (entity.getId() == null) {
            Long newId = insert(entity);
            VectorSpecimenIdentification refetched = get(newId);
            return refetched != null ? refetched : entity;
        }
        update(entity);
        return entity;
    }

    @Override
    @Transactional
    public String recomputeSampleIdentificationStatus(Long sampleId) {
        if (sampleId == null) {
            return null;
        }
        String lastStatus = STATUS_RECEIVED;
        for (org.openelisglobal.vector.valueholder.VectorPool pool : vectorPoolService
                .getBySampleId(String.valueOf(sampleId))) {
            if (pool.getParentPool() != null) {
                continue;
            }
            List<org.openelisglobal.sampleitem.valueholder.SampleItem> members = vectorPoolService
                    .getMembersByPoolId(pool.getId());
            List<Long> memberIds = new java.util.ArrayList<>(members.size());
            for (org.openelisglobal.sampleitem.valueholder.SampleItem m : members) {
                try {
                    memberIds.add(Long.valueOf(m.getId()));
                } catch (NumberFormatException ignored) {
                }
            }
            long identifiedCount = memberIds.isEmpty() ? 0 : baseObjectDAO.countBySampleItemIds(memberIds);
            long totalCount = members.size();
            String poolStatus;
            if (identifiedCount == 0) {
                poolStatus = STATUS_RECEIVED;
            } else if (identifiedCount < totalCount) {
                poolStatus = STATUS_IDENTIFICATION_IN_PROGRESS;
            } else {
                poolStatus = STATUS_COMPLETE;
            }
            if (!poolStatus.equals(pool.getIdentificationStatus())) {
                pool.setIdentificationStatus(poolStatus);
                pool.setSysUserId("system");
                vectorPoolService.update(pool);
            }
            lastStatus = poolStatus;
        }
        return lastStatus;
    }

    private void validateCommonFields(Long sampleItemIdForErrorMsg, Long vectorSpeciesId, String identificationMethod,
            String confidence, String physiologicalState, String lifecycleStage, String notes) {
        if (sampleItemIdForErrorMsg == null) {
            throw new IllegalArgumentException("sampleItemId is required");
        }
        if (vectorSpeciesId == null) {
            throw new IllegalArgumentException("vectorSpeciesId is required");
        }
        if (!isAllowedMethod(identificationMethod)) {
            throw new IllegalArgumentException(
                    "identificationMethod must be MORPHOLOGICAL, MOLECULAR, or BOTH; got: " + identificationMethod);
        }
        if (!isAllowedConfidence(confidence)) {
            throw new IllegalArgumentException("confidence must be CONFIRMED or PRESUMPTIVE; got: " + confidence);
        }
        if (physiologicalState != null) {
            Set<String> allowed = allowedCodesFor(CATEGORY_PHYSIOLOGICAL_STATE);
            if (!allowed.contains(physiologicalState)) {
                throw new IllegalArgumentException("physiologicalState must be one of [" + String.join(", ", allowed)
                        + "]; got: " + physiologicalState);
            }
        }
        if (lifecycleStage != null) {
            Set<String> allowed = allowedCodesFor(CATEGORY_LIFECYCLE_STAGES);
            if (!allowed.contains(lifecycleStage)) {
                throw new IllegalArgumentException(
                        "lifecycleStage must be one of [" + String.join(", ", allowed) + "]; got: " + lifecycleStage);
            }
        }
        if (notes != null && notes.length() > 500) {
            throw new IllegalArgumentException("notes must be 500 characters or less");
        }
    }

    private void validateGenbankAccession(String genbankAccession) {
        if (genbankAccession == null || genbankAccession.isBlank()) {
            return;
        }
        if (!GENBANK_ACCESSION_PATTERN.matcher(genbankAccession.trim()).matches()) {
            throw new IllegalArgumentException(
                    "genbankAccession must match INSDC pattern [A-Z]{1,2}[0-9]{5,8}; got: " + genbankAccession);
        }
    }

    private void persistMolecularRecord(Long identificationId, IdentificationRequest request, String sysUserId) {
        VectorMolecularRecord record = molecularRecordService.getByIdentificationId(identificationId).orElseGet(() -> {
            VectorMolecularRecord fresh = new VectorMolecularRecord();
            fresh.setIdentificationId(identificationId);
            return fresh;
        });
        record.setTargetGene(emptyToNull(request.getTargetGene()));
        record.setAssayName(emptyToNull(request.getAssayName()));
        record.setGenbankAccession(emptyToNull(request.getGenbankAccession()));
        record.setLinkedResultId(request.getLinkedResultId());
        record.setSysUserId(sysUserId);
        if (record.getId() == null) {
            molecularRecordService.insert(record);
        } else {
            molecularRecordService.update(record);
        }
    }

    private Long resolveSampleIdFor(Long sampleItemId) {
        var sampleItem = sampleItemService.get(String.valueOf(sampleItemId));
        if (sampleItem == null || sampleItem.getSample() == null) {
            return null;
        }
        try {
            return Long.valueOf(sampleItem.getSample().getId());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean involvesMolecular(String method) {
        return VectorSpecimenIdentification.METHOD_MOLECULAR.equals(method)
                || VectorSpecimenIdentification.METHOD_BOTH.equals(method);
    }

    private boolean hasAnyMolecularField(IdentificationRequest request) {
        return notBlank(request.getTargetGene()) || notBlank(request.getAssayName())
                || notBlank(request.getGenbankAccession()) || request.getLinkedResultId() != null;
    }

    private boolean isAllowedMethod(String m) {
        return VectorSpecimenIdentification.METHOD_MORPHOLOGICAL.equals(m)
                || VectorSpecimenIdentification.METHOD_MOLECULAR.equals(m)
                || VectorSpecimenIdentification.METHOD_BOTH.equals(m);
    }

    private boolean isAllowedConfidence(String c) {
        return VectorSpecimenIdentification.CONFIDENCE_CONFIRMED.equals(c)
                || VectorSpecimenIdentification.CONFIDENCE_PRESUMPTIVE.equals(c);
    }

    /**
     * Read the allowed codes for a dictionary category (active rows only). Codes
     * are taken from {@code Dictionary.localAbbreviation}, matching the existing
     * {@link org.openelisglobal.dictionary.controller.rest.DictionaryRestController}
     * contract — that way what the frontend Select hands us is exactly the shape
     * this validator accepts. A missing category or empty entry set yields an empty
     * allowed set, so any submitted value gets rejected — failing fast is
     * preferable to silently accepting a hardcoded enum in a system where the
     * dictionary is meant to be the source of truth.
     */
    private Set<String> allowedCodesFor(String categoryName) {
        DictionaryCategory category = dictionaryCategoryService.getDictionaryCategoryByName(categoryName);
        if (category == null || category.getId() == null) {
            return Set.of();
        }
        List<Dictionary> entries = dictionaryService.getDictionaryEntriesByCategoryId(category.getId());
        if (entries == null || entries.isEmpty()) {
            return Set.of();
        }
        Set<String> codes = new LinkedHashSet<>();
        for (Dictionary d : entries) {
            if (!"Y".equals(d.getIsActive())) {
                continue;
            }
            String code = d.getLocalAbbreviation();
            if (code == null || code.isBlank()) {
                code = d.getId();
            }
            if (code != null) {
                codes.add(code);
            }
        }
        return codes;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String emptyToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static long parseUserId(String sysUserId) {
        try {
            return Long.parseLong(sysUserId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("sysUserId must be numeric; got: " + sysUserId);
        }
    }
}
