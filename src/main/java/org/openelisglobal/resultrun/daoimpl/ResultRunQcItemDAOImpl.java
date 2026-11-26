package org.openelisglobal.resultrun.daoimpl;

import java.util.Collection;
import java.util.List;
import org.hibernate.Session;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.resultrun.dao.ResultRunQcItemDAO;
import org.openelisglobal.resultrun.valueholder.ResultRunQcItem;
import org.openelisglobal.resultrun.valueholder.RunQcItemKind;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ResultRunQcItemDAOImpl extends BaseDAOImpl<ResultRunQcItem, Long> implements ResultRunQcItemDAO {

    public ResultRunQcItemDAOImpl() {
        super(ResultRunQcItem.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResultRunQcItem> getItems(Collection<Long> runIds) {
        if (runIds == null || runIds.isEmpty()) {
            return List.of();
        }
        try {
            return entityManager.unwrap(Session.class)
                    .createQuery("FROM ResultRunQcItem i WHERE i.runId IN (:runIds) ORDER BY i.recordedAt, i.id",
                            ResultRunQcItem.class)
                    .setParameterList("runIds", runIds).list();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error getting the blanks and duplicates of runs", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResultRunQcItem> getDuplicatesOfAnalyses(Collection<String> parentAnalysisIds) {
        if (parentAnalysisIds == null || parentAnalysisIds.isEmpty()) {
            return List.of();
        }
        try {
            return entityManager.unwrap(Session.class).createQuery(
                    "FROM ResultRunQcItem i WHERE i.kind = :kind AND i.parentAnalysisId IN (:parents) ORDER BY i.id",
                    ResultRunQcItem.class).setParameter("kind", RunQcItemKind.DUPLICATE)
                    .setParameterList("parents", parentAnalysisIds).list();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error getting the duplicates of analyses", e);
        }
    }
}
