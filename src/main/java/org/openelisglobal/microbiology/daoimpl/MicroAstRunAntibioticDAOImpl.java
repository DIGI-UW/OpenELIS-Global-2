package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroAstRunAntibioticDAO;
import org.openelisglobal.microbiology.valueholder.MicroAstRunAntibiotic;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroAstRunAntibioticDAOImpl extends BaseDAOImpl<MicroAstRunAntibiotic, String>
        implements MicroAstRunAntibioticDAO {

    public MicroAstRunAntibioticDAOImpl() {
        super(MicroAstRunAntibiotic.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroAstRunAntibiotic> getByRunId(String runId) {
        Query<MicroAstRunAntibiotic> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroAstRunAntibiotic r where r.astRunId = :runId order by r.displayOrder, r.id",
                MicroAstRunAntibiotic.class);
        query.setParameter("runId", runId);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MicroAstRunAntibiotic> getByRunIdAndAntibioticId(String runId, String antibioticId) {
        Query<MicroAstRunAntibiotic> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroAstRunAntibiotic r where r.astRunId = :runId and r.antibioticId = :antibioticId",
                MicroAstRunAntibiotic.class);
        query.setParameter("runId", runId);
        query.setParameter("antibioticId", antibioticId);
        return query.uniqueResultOptional();
    }
}
