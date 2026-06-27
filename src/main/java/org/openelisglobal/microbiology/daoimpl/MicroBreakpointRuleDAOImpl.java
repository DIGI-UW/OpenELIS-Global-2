package org.openelisglobal.microbiology.daoimpl;

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
}
