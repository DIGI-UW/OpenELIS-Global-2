package org.openelisglobal.microbiology.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroCaseOrderDetailDAO;
import org.openelisglobal.microbiology.form.MicroCaseOrderDetailRequestForm;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivityType;
import org.openelisglobal.microbiology.valueholder.MicroCaseOrderDetail;
import org.openelisglobal.microbiology.valueholder.MicroCulturePurpose;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Captures the microbiology order-detail fields against a case. One record per
 * case. A repeat save updates the current order context in place and records an
 * audited timeline event when culture purpose changes.
 */
@Service
public class MicroCaseOrderDetailServiceImpl implements MicroCaseOrderDetailService {

    private final MicroCaseOrderDetailDAO orderDetailDAO;
    private final MicroCaseDAO caseDAO;
    private final MicroCaseActivityDAO activityDAO;
    private final MicrobiologyReferenceService referenceService;
    private final ObjectMapper objectMapper;

    @Autowired
    public MicroCaseOrderDetailServiceImpl(MicroCaseOrderDetailDAO orderDetailDAO, MicroCaseDAO caseDAO,
            MicroCaseActivityDAO activityDAO, MicrobiologyReferenceService referenceService) {
        this(orderDetailDAO, caseDAO, activityDAO, referenceService, new ObjectMapper());
    }

    MicroCaseOrderDetailServiceImpl(MicroCaseOrderDetailDAO orderDetailDAO, MicroCaseDAO caseDAO,
            MicroCaseActivityDAO activityDAO, MicrobiologyReferenceService referenceService,
            ObjectMapper objectMapper) {
        this.orderDetailDAO = orderDetailDAO;
        this.caseDAO = caseDAO;
        this.activityDAO = activityDAO;
        this.referenceService = referenceService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public MicroCaseOrderDetail saveOrderDetail(String caseId, MicroCaseOrderDetailRequestForm request,
            String performedBy) {
        MicroCaseServiceImpl.requireText(caseId, "caseId");
        var microCase = caseDAO.get(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Microbiology case not found"));
        MicroCaseMutationGuard.requireMutable(microCase);

        MicroCaseOrderDetail detail = orderDetailDAO.getByCaseId(caseId);
        boolean isNew = detail == null;
        String previousPurpose = isNew ? null : detail.getCulturePurpose();
        if (isNew) {
            detail = new MicroCaseOrderDetail();
            detail.setCaseId(caseId);
            detail.setCreatedAt(MicroCaseServiceImpl.now());
            detail.setCreatedBy(performedBy);
        } else {
            detail.setUpdatedAt(MicroCaseServiceImpl.now());
            detail.setUpdatedBy(performedBy);
        }
        apply(detail, request, false);
        detail.setCultureMethodId(microCase.getCultureMethodId());

        if (isNew) {
            orderDetailDAO.insert(detail);
        } else {
            orderDetailDAO.update(detail);
        }
        recordActivity(caseId, performedBy, isNew, previousPurpose, detail.getCulturePurpose());
        return detail;
    }

    @Override
    @Transactional(readOnly = true)
    public MicroCaseOrderDetail getOrderDetail(String caseId) {
        return orderDetailDAO.getByCaseId(caseId);
    }

    @Override
    @Transactional
    public MicroCaseOrderDetail saveOrderDraft(Sample sample, MicroCaseOrderDetailRequestForm request,
            String performedBy) {
        if (sample == null || sample.getId() == null || sample.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("Saved sample is required for microbiology order detail");
        }
        if (request == null) {
            throw new IllegalArgumentException("Microbiology order detail is required");
        }
        MicroCaseOrderDetail detail = orderDetailDAO.getDraftBySampleId(sample.getId());
        boolean isNew = detail == null;
        if (isNew) {
            detail = new MicroCaseOrderDetail();
            detail.setSampleId(sample.getId());
            detail.setCreatedAt(MicroCaseServiceImpl.now());
            detail.setCreatedBy(performedBy);
        } else {
            detail.setUpdatedAt(MicroCaseServiceImpl.now());
            detail.setUpdatedBy(performedBy);
        }
        apply(detail, request, true);
        if (isNew) {
            orderDetailDAO.insert(detail);
        } else {
            orderDetailDAO.update(detail);
        }
        return detail;
    }

    @Override
    @Transactional(readOnly = true)
    public MicroCaseOrderDetailRequestForm getOrderDraft(String sampleId) {
        MicroCaseOrderDetail detail = orderDetailDAO.getDraftBySampleId(sampleId);
        if (detail == null) {
            return null;
        }
        MicroCaseOrderDetailRequestForm form = new MicroCaseOrderDetailRequestForm();
        form.cultureMethodId = detail.getCultureMethodId();
        form.patientOrigin = detail.getPatientOrigin();
        form.culturePurpose = detail.getCulturePurpose();
        form.admissionDate = detail.getAdmissionDate() == null ? null : detail.getAdmissionDate().toString();
        form.numberOfSets = detail.getNumberOfSets();
        form.clinicalHistory = detail.getClinicalHistory();
        form.antibioticExposure = detail.getAntibioticExposure();
        return form;
    }

    private void apply(MicroCaseOrderDetail detail, MicroCaseOrderDetailRequestForm request,
            boolean requireCulturePurpose) {
        String patientOrigin = request.patientOrigin == null ? null
                : request.patientOrigin.trim().toUpperCase(Locale.ROOT);
        if (patientOrigin != null && !patientOrigin.isEmpty()
                && !referenceService.isActivePatientOriginCode(patientOrigin)) {
            throw new IllegalArgumentException("Unknown or inactive patient origin code");
        }
        detail.setCultureMethodId(request.cultureMethodId);
        detail.setPatientOrigin(patientOrigin);
        String culturePurpose = normalizeCulturePurpose(request.culturePurpose, requireCulturePurpose);
        if (culturePurpose == null && detail.getCulturePurpose() != null) {
            throw new IllegalArgumentException("Culture purpose is required once classified");
        }
        detail.setCulturePurpose(culturePurpose);
        detail.setAdmissionDate(parseAdmissionDate(request.admissionDate, patientOrigin));
        detail.setNumberOfSets(request.numberOfSets);
        detail.setClinicalHistory(request.clinicalHistory);
        detail.setAntibioticExposure(request.antibioticExposure);
    }

    private String normalizeCulturePurpose(String value, boolean required) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            if (required) {
                throw new IllegalArgumentException("Culture purpose is required");
            }
            return null;
        }
        try {
            return MicroCulturePurpose.valueOf(normalized).name();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported culture purpose", exception);
        }
    }

    private LocalDate parseAdmissionDate(String value, String patientOrigin) {
        if ("OUTPATIENT".equals(patientOrigin) || value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Admission date must use yyyy-MM-dd", e);
        }
    }

    private void recordActivity(String caseId, String performedBy, boolean isNew, String previousPurpose,
            String currentPurpose) {
        MicroCaseActivity activity = new MicroCaseActivity();
        activity.setCaseId(caseId);
        boolean purposeChanged = !isNew && !Objects.equals(previousPurpose, currentPurpose);
        activity.setActivityType((purposeChanged ? MicroCaseActivityType.CULTURE_PURPOSE_CHANGED
                : MicroCaseActivityType.ORDER_DETAIL_CAPTURED).name());
        activity.setOccurredAt(MicroCaseServiceImpl.now());
        activity.setPerformedBy(performedBy);
        activity.setNote(purposeChanged ? "Culture purpose corrected" : "Order detail captured");
        if (purposeChanged) {
            try {
                activity.setStructuredData(objectMapper.writeValueAsString(Map.of("fromPurpose",
                        previousPurpose == null ? "UNSPECIFIED" : previousPurpose, "toPurpose", currentPurpose)));
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Unable to record culture purpose correction", exception);
            }
        }
        activityDAO.insert(activity);
    }
}
