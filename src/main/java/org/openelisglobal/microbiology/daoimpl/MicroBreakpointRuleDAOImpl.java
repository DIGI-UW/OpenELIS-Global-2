package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroBreakpointRuleDAO;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointRule;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroBreakpointRuleDAOImpl extends BaseDAOImpl<MicroBreakpointRule, String>
        implements MicroBreakpointRuleDAO {

    public MicroBreakpointRuleDAOImpl() {
        super(MicroBreakpointRule.class);
    }

    @Override
    @Transactional(readOnly = true)
    public MicroBreakpointRule findBestRule(String standardId, String organismId, String organismGroup,
            String antibioticId, String method, String specimenTypeId, String breakpointType) {
        Query<MicroBreakpointRule> query = entityManager.unwrap(Session.class)
                .createQuery("from MicroBreakpointRule r where r.isActive = 'Y' and r.standardId = :standardId"
                        + " and r.antibioticId = :antibioticId and r.breakpointType = :breakpointType"
                        + " and (r.method is null or r.method = :method)"
                        + " and (r.specimenTypeId is null or r.specimenTypeId = :specimenTypeId)"
                        + " and (r.organismId = :organismId or r.organismGroup = :organismGroup"
                        + " or (r.organismId is null and r.organismGroup is null))"
                        + " order by case when r.organismId = :organismId then 0"
                        + " when r.organismGroup = :organismGroup then 1 else 2 end,"
                        + " case when r.specimenTypeId = :specimenTypeId then 0 else 1 end,"
                        + " case when r.method = :method then 0 else 1 end", MicroBreakpointRule.class);
        query.setParameter("standardId", standardId);
        query.setParameter("organismId", organismId);
        query.setParameter("organismGroup", organismGroup);
        query.setParameter("antibioticId", antibioticId);
        query.setParameter("method", method);
        query.setParameter("specimenTypeId", specimenTypeId);
        query.setParameter("breakpointType", breakpointType);
        query.setMaxResults(1);
        return query.uniqueResultOptional().orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MicroBreakpointRule> findBySourceRowHash(String sourceRowHash) {
        Query<MicroBreakpointRule> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroBreakpointRule r where r.sourceRowHash = :sourceRowHash", MicroBreakpointRule.class);
        query.setParameter("sourceRowHash", sourceRowHash);
        query.setMaxResults(1);
        return query.uniqueResultOptional();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MicroBreakpointRule> findByNaturalKey(String standardId, String organismId, String organismGroup,
            String antibioticId, String method, String specimenTypeId, String breakpointType) {
        Query<MicroBreakpointRule> query = entityManager.unwrap(Session.class)
                .createQuery("from MicroBreakpointRule r where r.standardId = :standardId"
                        + " and ((r.organismId = :organismId) or (r.organismId is null and :organismId is null))"
                        + " and ((r.organismGroup = :organismGroup) or (r.organismGroup is null and :organismGroup is null))"
                        + " and r.antibioticId = :antibioticId"
                        + " and ((r.method = :method) or (r.method is null and :method is null))"
                        + " and ((r.specimenTypeId = :specimenTypeId) or (r.specimenTypeId is null and :specimenTypeId is null))"
                        + " and r.breakpointType = :breakpointType", MicroBreakpointRule.class);
        query.setParameter("standardId", standardId);
        query.setParameter("organismId", organismId);
        query.setParameter("organismGroup", organismGroup);
        query.setParameter("antibioticId", antibioticId);
        query.setParameter("method", method);
        query.setParameter("specimenTypeId", specimenTypeId);
        query.setParameter("breakpointType", breakpointType);
        return query.uniqueResultOptional();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroBreakpointRule> getByStandardId(String standardId) {
        Query<MicroBreakpointRule> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroBreakpointRule r where r.standardId = :standardId order by r.organismGroup, r.organismId, r.antibioticId, r.method",
                MicroBreakpointRule.class);
        query.setParameter("standardId", standardId);
        return query.list();
    }
}
