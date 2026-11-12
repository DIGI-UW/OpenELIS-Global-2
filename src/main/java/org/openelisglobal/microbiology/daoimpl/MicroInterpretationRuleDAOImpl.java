package org.openelisglobal.microbiology.daoimpl;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroInterpretationRuleDAO;
import org.openelisglobal.microbiology.valueholder.MicroInterpretationRule;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroInterpretationRuleDAOImpl extends BaseDAOImpl<MicroInterpretationRule, String>
        implements MicroInterpretationRuleDAO {
    public MicroInterpretationRuleDAOImpl() {
        super(MicroInterpretationRule.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroInterpretationRule> getAll() {
        return entityManager
                .createQuery("from MicroInterpretationRule r order by r.organismGroup, r.organismId, r.antibioticId",
                        MicroInterpretationRule.class)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MicroInterpretationRule> findBest(String antibioticId, String organismId, String organismGroup) {
        Query<MicroInterpretationRule> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroInterpretationRule r where r.active = true and r.antibioticId = :antibioticId"
                        + " and (r.organismId = :organismId or r.organismGroup = :organismGroup)",
                MicroInterpretationRule.class);
        query.setParameter("antibioticId", antibioticId);
        query.setParameter("organismId", organismId);
        query.setParameter("organismGroup", organismGroup);
        Comparator<MicroInterpretationRule> best = Comparator.comparingInt(
                (MicroInterpretationRule r) -> r.getOrganismId() != null && r.getOrganismId().equals(organismId) ? 0
                        : 1);
        return query.list().stream().min(best);
    }
}
