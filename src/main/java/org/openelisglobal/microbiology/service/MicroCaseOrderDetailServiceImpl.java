package org.openelisglobal.microbiology.service;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Locale;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroCaseOrderDetailDAO;
import org.openelisglobal.microbiology.form.MicroCaseOrderDetailRequestForm;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivityType;
import org.openelisglobal.microbiology.valueholder.MicroCaseOrderDetail;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Captures the microbiology order-detail fields against a case. One record per
 * case; a repeat save updates the existing record in place rather than creating
 * history, since this data is order context rather than a clinical timeline
 * event.
 */
@Service
public class MicroCaseOrderDetailServiceImpl implements MicroCaseOrderDetailService {

    private final MicroCaseOrderDetailDAO orderDetailDAO;
    private final MicroCaseDAO caseDAO;
    private final MicroCaseActivityDAO activityDAO;
    private final MicrobiologyReferenceService referenceService;

    public MicroCaseOrderDetailServiceImpl(MicroCaseOrderDetailDAO orderDetailDAO, MicroCaseDAO caseDAO,
            MicroCaseActivityDAO activityDAO, MicrobiologyReferenceService referenceService) {
        this.orderDetailDAO = orderDetailDAO;
        this.caseDAO = caseDAO;
        this.activityDAO = activityDAO;
        this.referenceService = referenceService;
    }

    @Override
    @Transactional
    public MicroCaseOrderDetail saveOrderDetail(String caseId, MicroCaseOrderDetailRequestForm request,
            String performedBy) {
        MicroCaseServiceImpl.requireText(caseId, "caseId");
        var microCase = caseDAO.get(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Microbiology case not found"));

        MicroCaseOrderDetail detail = orderDetailDAO.getByCaseId(caseId);
        boolean isNew = detail == null;
        if (isNew) {
            detail = new MicroCaseOrderDetail();
            detail.setCaseId(caseId);
            detail.setCreatedAt(MicroCaseServiceImpl.now());
            detail.setCreatedBy(performedBy);
        } else {
            detail.setUpdatedAt(MicroCaseServiceImpl.now());
            detail.setUpdatedBy(performedBy);
        }
        apply(detail, request);
        detail.setCultureMethodId(microCase.getCultureMethodId());

        if (isNew) {
            orderDetailDAO.insert(detail);
        } else {
            orderDetailDAO.update(detail);
        }
        recordActivity(caseId, performedBy);
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
        apply(detail, request);
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
        form.admissionDate = detail.getAdmissionDate() == null ? null : detail.getAdmissionDate().toString();
        form.numberOfSets = detail.getNumberOfSets();
        form.clinicalHistory = detail.getClinicalHistory();
        form.antibioticExposure = detail.getAntibioticExposure();
        return form;
    }

    private void apply(MicroCaseOrderDetail detail, MicroCaseOrderDetailRequestForm request) {
        String patientOrigin = request.patientOrigin == null ? null
                : request.patientOrigin.trim().toUpperCase(Locale.ROOT);
        if (patientOrigin != null && !patientOrigin.isEmpty()
                && !referenceService.isActivePatientOriginCode(patientOrigin)) {
            throw new IllegalArgumentException("Unknown or inactive patient origin code");
        }
        detail.setCultureMethodId(request.cultureMethodId);
        detail.setPatientOrigin(patientOrigin);
        detail.setAdmissionDate(parseAdmissionDate(request.admissionDate, patientOrigin));
        detail.setNumberOfSets(request.numberOfSets);
        detail.setClinicalHistory(request.clinicalHistory);
        detail.setAntibioticExposure(request.antibioticExposure);
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

    private void recordActivity(String caseId, String performedBy) {
        MicroCaseActivity activity = new MicroCaseActivity();
        activity.setCaseId(caseId);
        activity.setActivityType(MicroCaseActivityType.ORDER_DETAIL_CAPTURED.name());
        activity.setOccurredAt(MicroCaseServiceImpl.now());
        activity.setPerformedBy(performedBy);
        activity.setNote("Order detail captured");
        activityDAO.insert(activity);
    }
}
