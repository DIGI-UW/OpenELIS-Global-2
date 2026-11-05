package org.openelisglobal.microbiology.daoimpl;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
    public Optional<MicroInterpretationRule> findBest(String antibioticId, String organismId,
            Set<String> organismGroups) {
        Query<MicroInterpretationRule> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroInterpretationRule r where r.active = true and r.antibioticId = :antibioticId",
                MicroInterpretationRule.class);
        query.setParameter("antibioticId", antibioticId);
        Comparator<MicroInterpretationRule> best = Comparator.comparingInt(
                (MicroInterpretationRule r) -> r.getOrganismId() != null && r.getOrganismId().equals(organismId) ? 0
                        : r.getOrganismGroup() != null && organismGroups.contains(r.getOrganismGroup()) ? 1 : 2)
                .thenComparing(MicroInterpretationRule::getId);
        return query.list().stream().filter(r -> r.getOrganismId() != null && r.getOrganismId().equals(organismId)
                || r.getOrganismGroup() != null && organismGroups.contains(r.getOrganismGroup())).min(best);
    }
}
