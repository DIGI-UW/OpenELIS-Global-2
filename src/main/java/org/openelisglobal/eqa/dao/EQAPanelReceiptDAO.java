package org.openelisglobal.eqa.dao;

import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.eqa.valueholder.EQAPanelReceipt;

public interface EQAPanelReceiptDAO extends BaseDAO<EQAPanelReceipt, Long> {

    Optional<EQAPanelReceipt> findByCycleAndEnrollment(Long cycleId, Long labEnrollmentId);
}
