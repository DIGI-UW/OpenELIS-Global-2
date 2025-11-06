package org.openelisglobal.program.dao.cytology;

import jakarta.transaction.Transactional;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.program.valueholder.cytology.CytologySample;
import org.openelisglobal.program.valueholder.cytology.CytologySample.CytologyStatus;
import org.springframework.stereotype.Component;

@Component
@Transactional
public class CytologySampleDAOImpl extends BaseDAOImpl<CytologySample, Integer> implements CytologySampleDAO {
    CytologySampleDAOImpl() {
        super(CytologySample.class);
    }

    @Override
    public List<CytologySample> getWithStatus(List<CytologyStatus> statuses) {
        String sql = "from CytologySample cs where status in (:statuses)";
        Query<CytologySample> query = entityManager.unwrap(Session.class).createQuery(sql, CytologySample.class);
        query.setParameterList("statuses", statuses.stream().map(e -> e.toString()).collect(Collectors.toList()));
        List<CytologySample> list = query.list();

        return list;
    }

    @Override
    public Long getCountWithStatus(List<CytologyStatus> statuses) {
        String sql = "select count(*) from CytologySample cs where status in (:statuses)";
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(sql, Long.class);
        query.setParameterList("statuses", statuses.stream().map(e -> e.toString()).collect(Collectors.toList()));
        Long count = query.uniqueResult();

        return count;
    }

    @Override
    public List<CytologySample> searchWithStatusAndAccesionNumber(List<CytologyStatus> statuses, String labNumber) {
        String sql = "from CytologySample cs where cs.status in (:statuses) and cs.sample.accessionNumber ="
                + " :labNumber";
        Query<CytologySample> query = entityManager.unwrap(Session.class).createQuery(sql, CytologySample.class);
        query.setParameterList("statuses", statuses.stream().map(e -> e.toString()).collect(Collectors.toList()));
        query.setParameter("labNumber", labNumber);
        List<CytologySample> list = query.list();

        return list;
    }

    @Override
    public Long getCountWithStatusBetweenDates(List<CytologyStatus> statuses, Timestamp from, Timestamp to) {
        String sql = "select count(*) from CytologySample cs where cs.status in (:statuses) and cs.lastupdated"
                + " between :datefrom and :dateto";
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(sql, Long.class);
        query.setParameterList("statuses", statuses.stream().map(e -> e.toString()).collect(Collectors.toList()));
        query.setParameter("datefrom", from);
        query.setParameter("dateto", to);
        Long count = query.uniqueResult();
        return count;
    }
}
