package org.openelisglobal.dashboard.management.quality;

import java.math.BigDecimal;
import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QiWeightsServiceImpl extends AuditableBaseObjectServiceImpl<QiWeights, Long> implements QiWeightsService {

    static final WeightsView DEFAULTS = new WeightsView(new BigDecimal("0.30"), new BigDecimal("0.25"),
            new BigDecimal("0.25"), new BigDecimal("0.20"));

    @Autowired
    protected QiWeightsDAO baseObjectDAO;

    public QiWeightsServiceImpl() {
        super(QiWeights.class);
        this.auditTrailLog = true;
    }

    @Override
    protected QiWeightsDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public WeightsView getWeights() {
        QiWeights row = current();
        return row == null ? DEFAULTS
                : new WeightsView(row.getQcWeight(), row.getEqaWeight(), row.getNceInverseWeight(),
                        row.getAmendmentInverseWeight());
    }

    @Override
    @Transactional
    public void saveWeights(WeightsView weights, String sysUserId) {
        if (!weights.isValid()) {
            throw new IllegalArgumentException("Weights must each be 0 to 1 and sum to 1.00");
        }
        QiWeights existing = current();
        // A fresh instance, so the audit trail diffs against the stored row.
        QiWeights row = new QiWeights();
        if (existing != null) {
            row.setId(existing.getId());
            row.setLastupdated(existing.getLastupdated());
        }
        row.setQcWeight(weights.qc());
        row.setEqaWeight(weights.eqa());
        row.setNceInverseWeight(weights.nceInverse());
        row.setAmendmentInverseWeight(weights.amendmentInverse());
        row.setSysUserId(sysUserId);
        if (existing == null) {
            insert(row);
        } else {
            update(row);
        }
    }

    private QiWeights current() {
        List<QiWeights> rows = baseObjectDAO.getAll();
        return rows.isEmpty() ? null : rows.get(0);
    }
}
