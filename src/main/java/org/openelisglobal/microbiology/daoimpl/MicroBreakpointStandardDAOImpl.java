package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroBreakpointStandardDAO;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroBreakpointStandardDAOImpl extends BaseDAOImpl<MicroBreakpointStandard, String>
        implements MicroBreakpointStandardDAO {

    public MicroBreakpointStandardDAOImpl() {
        super(MicroBreakpointStandard.class);
    }

    @Override
    @Transactional(readOnly = true)
    public MicroBreakpointStandard getActiveStandard(String authority, String version) {
        Query<MicroBreakpointStandard> query = entityManager.unwrap(Session.class)
                .createQuery("from MicroBreakpointStandard s where s.isActive = 'Y' and s.authority = :authority"
                        + " and s.version = :version", MicroBreakpointStandard.class);
        query.setParameter("authority", authority);
        query.setParameter("version", version);
        return query.uniqueResultOptional().orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroBreakpointStandard> getActiveStandards() {
        Query<MicroBreakpointStandard> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroBreakpointStandard s where s.isActive = 'Y' order by s.authority, s.version",
                MicroBreakpointStandard.class);
        return query.list();
    }
}
