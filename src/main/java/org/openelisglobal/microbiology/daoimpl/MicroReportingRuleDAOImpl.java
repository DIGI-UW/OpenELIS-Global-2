package org.openelisglobal.microbiology.daoimpl;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroReportingRuleDAO;
import org.openelisglobal.microbiology.valueholder.MicroReportingRule;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroReportingRuleDAOImpl extends BaseDAOImpl<MicroReportingRule, String>
        implements MicroReportingRuleDAO {
    public MicroReportingRuleDAOImpl() {
        super(MicroReportingRule.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroReportingRule> getAll() {
        return entityManager
                .createQuery("from MicroReportingRule r order by r.organismGroup, r.organismId, r.antibioticId",
                        MicroReportingRule.class)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MicroReportingRule> findBest(String antibioticId, String organismId, String organismGroup,
            String sampleTypeId, String clinicalContext) {
        Query<MicroReportingRule> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroReportingRule r where r.active = true and r.antibioticId = :antibioticId"
                        + " and (r.organismId = :organismId or r.organismGroup = :organismGroup"
                        + " or (r.organismId is null and r.organismGroup is null))"
                        + " and (r.clinicalContext is null or r.clinicalContext = :clinicalContext)",
                MicroReportingRule.class);
        query.setParameter("antibioticId", antibioticId);
        query.setParameter("organismId", organismId);
        query.setParameter("organismGroup", organismGroup);
        query.setParameter("clinicalContext", clinicalContext);
        Comparator<MicroReportingRule> best = Comparator
                .comparingInt(
                        (MicroReportingRule r) -> r.getOrganismId() != null && r.getOrganismId().equals(organismId) ? 0
                                : r.getOrganismGroup() != null && r.getOrganismGroup().equals(organismGroup) ? 1 : 2)
                .thenComparingInt(r -> r.getSampleTypeIds().isEmpty() ? 1 : 0)
                .thenComparingInt(r -> r.getClinicalContext() == null ? 1 : 0);
        return query.list().stream()
                .filter(r -> r.getSampleTypeIds().isEmpty() || r.getSampleTypeIds().contains(sampleTypeId)).min(best);
    }
}
