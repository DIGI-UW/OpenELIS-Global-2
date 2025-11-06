package org.openelisglobal.program.dao;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class PathologySampleDAOImpl extends BaseDAOImpl<PathologySample, Integer> implements PathologySampleDAO {
    PathologySampleDAOImpl() {
        super(PathologySample.class);
    }

    @Override
    public List<PathologySample> getWithStatus(List<PathologyStatus> statuses) {
        String sql = "from PathologySample ps where status in (:statuses)";
        Query<PathologySample> query = entityManager.unwrap(Session.class).createQuery(sql, PathologySample.class);
        query.setParameterList("statuses", statuses.stream().map(e -> e.toString()).collect(Collectors.toList()));
        List<PathologySample> list = query.list();

        return list;
    }

    @Override
    public Long getCountWithStatus(List<PathologyStatus> statuses) {
        String sql = "select count(*) from PathologySample ps where status in (:statuses)";
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(sql, Long.class);
        query.setParameterList("statuses", statuses.stream().map(e -> e.toString()).collect(Collectors.toList()));
        Long count = query.uniqueResult();

        return count;
    }

    @Override
    public List<PathologySample> searchWithStatusAndAccesionNumber(List<PathologyStatus> statuses, String labNumber) {
        String sql = "from PathologySample ps where ps.status in (:statuses) and ps.sample.accessionNumber ="
                + " :labNumber";
        Query<PathologySample> query = entityManager.unwrap(Session.class).createQuery(sql, PathologySample.class);
        query.setParameterList("statuses", statuses.stream().map(e -> e.toString()).collect(Collectors.toList()));
        query.setParameter("labNumber", labNumber);
        List<PathologySample> list = query.list();

        return list;
    }

    @Override
    public Long getCountWithStatusBetweenDates(List<PathologyStatus> statuses, Timestamp from, Timestamp to) {
        String sql = "select count(*) from PathologySample ps where ps.status in (:statuses) and ps.lastupdated"
                + " between :datefrom and :dateto";
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(sql, Long.class);
        query.setParameterList("statuses", statuses.stream().map(e -> e.toString()).collect(Collectors.toList()));
        query.setParameter("datefrom", from);
        query.setParameter("dateto", to);
        Long count = query.uniqueResult();
        return count;
    }
}
