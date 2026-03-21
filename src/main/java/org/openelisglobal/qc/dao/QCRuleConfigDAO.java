package org.openelisglobal.qc.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.qc.valueholder.QCRuleConfig;

public interface QCRuleConfigDAO extends BaseDAO<QCRuleConfig, Long> {

    List<QCRuleConfig> findByTestTypeId(Long testTypeId);

    List<QCRuleConfig> findEnabledByTestTypeId(Long testTypeId);
}
