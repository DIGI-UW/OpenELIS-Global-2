package org.openelisglobal.qaevent.daoimpl;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qaevent.dao.NcePromptDismissalDAO;
import org.openelisglobal.qaevent.valueholder.NcePromptDismissal;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class NcePromptDismissalDAOImpl extends BaseDAOImpl<NcePromptDismissal, Integer>
        implements NcePromptDismissalDAO {

    public NcePromptDismissalDAOImpl() {
        super(NcePromptDismissal.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NcePromptDismissal> getByResultId(String resultId) {
        if (resultId == null) {
            throw new LIMSRuntimeException("ResultId cannot be null");
        }

        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<NcePromptDismissal> cq = cb.createQuery(NcePromptDismissal.class);
            Root<NcePromptDismissal> root = cq.from(NcePromptDismissal.class);

            cq.select(root).where(cb.equal(root.get("resultId"), resultId)).orderBy(cb.desc(root.get("dismissedDate")));

            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving prompt dismissals for result: " + resultId, e);
        }
    }
}
