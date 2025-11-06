package org.openelisglobal.program.dao;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample.ImmunohistochemistryStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ImmunohistochemistrySampleDAOImpl extends BaseDAOImpl<ImmunohistochemistrySample, Integer>
        implements ImmunohistochemistrySampleDAO {
    ImmunohistochemistrySampleDAOImpl() {
        super(ImmunohistochemistrySample.class);
    }

    @Override
    public List<ImmunohistochemistrySample> getWithStatus(List<ImmunohistochemistryStatus> statuses) {
        String sql = "from ImmunohistochemistrySample is where is.status in (:statuses)";
        Query<ImmunohistochemistrySample> query = entityManager.unwrap(Session.class).createQuery(sql,
                ImmunohistochemistrySample.class);
        query.setParameterList("statuses", statuses.stream().map(e -> e.toString()).collect(Collectors.toList()));
        List<ImmunohistochemistrySample> list = query.list();

        return list;
    }

    @Override
    public Long getCountWithStatus(List<ImmunohistochemistryStatus> statuses) {
        String sql = "select count(*) from ImmunohistochemistrySample is where is.status in (:statuses)";
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(sql, Long.class);
        query.setParameterList("statuses", statuses.stream().map(e -> e.toString()).collect(Collectors.toList()));
        Long count = query.uniqueResult();

        return count;
    }

    @Override
    public Long getCountWithStatusBetweenDates(List<ImmunohistochemistryStatus> statuses, Timestamp from,
            Timestamp to) {
        String sql = "select count(*) from ImmunohistochemistrySample is where is.status in (:statuses) and"
                + " is.lastupdated between :datefrom and :dateto";
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(sql, Long.class);
        query.setParameterList("statuses", statuses.stream().map(e -> e.toString()).collect(Collectors.toList()));
        query.setParameter("datefrom", from);
        query.setParameter("dateto", to);
        Long count = query.uniqueResult();
        return count;
    }

    @Override
    public List<ImmunohistochemistrySample> searchWithStatusAndAccesionNumber(List<ImmunohistochemistryStatus> statuses,
            String labNumber) {
        String sql = "from ImmunohistochemistrySample is where is.status in (:statuses) and"
                + " is.sample.accessionNumber = :labNumber";
        Query<ImmunohistochemistrySample> query = entityManager.unwrap(Session.class).createQuery(sql,
                ImmunohistochemistrySample.class);
        query.setParameterList("statuses", statuses.stream().map(e -> e.toString()).collect(Collectors.toList()));
        query.setParameter("labNumber", labNumber);
        List<ImmunohistochemistrySample> list = query.list();

        return list;
    }

    @Override
    public ImmunohistochemistrySample getByPathologySampleId(Integer pathologySampleId) {
        String sql = "from ImmunohistochemistrySample is where is.pathologySample.id = :pathologySampleId";
        Query<ImmunohistochemistrySample> query = entityManager.unwrap(Session.class).createQuery(sql,
                ImmunohistochemistrySample.class);
        query.setParameter("pathologySampleId", pathologySampleId);
        query.setMaxResults(1);
        ImmunohistochemistrySample sample = query.uniqueResult();
        return sample;
    }
}
