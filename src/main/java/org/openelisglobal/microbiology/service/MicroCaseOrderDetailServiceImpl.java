package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroCaseOrderDetailDAO;
import org.openelisglobal.microbiology.form.MicroCaseOrderDetailRequestForm;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivityType;
import org.openelisglobal.microbiology.valueholder.MicroCaseOrderDetail;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Captures the microbiology order-detail fields (FR-002: patient origin, number
 * of sets, clinical history, antibiotic exposure, critical notification
 * preference) against a case. One record per case; a repeat save updates the
 * existing record in place rather than creating history, since this data is
 * order context rather than a clinical timeline event.
 */
@Service
public class MicroCaseOrderDetailServiceImpl implements MicroCaseOrderDetailService {

    private final MicroCaseOrderDetailDAO orderDetailDAO;
    private final MicroCaseDAO caseDAO;
    private final MicroCaseActivityDAO activityDAO;

    public MicroCaseOrderDetailServiceImpl(MicroCaseOrderDetailDAO orderDetailDAO, MicroCaseDAO caseDAO,
            MicroCaseActivityDAO activityDAO) {
        this.orderDetailDAO = orderDetailDAO;
        this.caseDAO = caseDAO;
        this.activityDAO = activityDAO;
    }

    @Override
    @Transactional
    public MicroCaseOrderDetail saveOrderDetail(String caseId, MicroCaseOrderDetailRequestForm request,
            String performedBy) {
        MicroCaseServiceImpl.requireText(caseId, "caseId");
        caseDAO.get(caseId).orElseThrow(() -> new IllegalArgumentException("Microbiology case not found"));

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
        detail.setPatientOrigin(request.patientOrigin);
        detail.setNumberOfSets(request.numberOfSets);
        detail.setClinicalHistory(request.clinicalHistory);
        detail.setAntibioticExposure(request.antibioticExposure);
        detail.setCriticalNotificationPreference(request.criticalNotificationPreference);

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
