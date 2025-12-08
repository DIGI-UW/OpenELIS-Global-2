package org.openelisglobal.program.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.program.valueholder.ProgramSample;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ProgramSampleDAOImpl extends BaseDAOImpl<ProgramSample, Integer> implements ProgramSampleDAO {

    ProgramSampleDAOImpl() {
        super(ProgramSample.class);
    }

    @Override
    public ProgramSample getProgrammeSampleBySample(Integer sampleId, String programName) {

        String className = "ProgramSample";
        if (programName != null) {
            if (programName.toLowerCase().contains("pathology")) {
                className = "PathologySample";
            } else if (programName.toLowerCase().contains("immunohistochemistry")) {
                className = "ImmunohistochemistrySample";
            } else if (programName.toLowerCase().contains("cytology")) {
                className = "CytologySample";
            }
        }

        String sql = "from " + className + " ps where ps.sample.id = :sampleId";
        Query<ProgramSample> query = entityManager.unwrap(Session.class).createQuery(sql, ProgramSample.class);
        query.setParameter("sampleId", sampleId);
        query.setMaxResults(1);
        ProgramSample programme = (ProgramSample) query.uniqueResult();
        return programme;
    }

    public String getTableName() {
        return "program_sample";
    }

    @Override
    public List<ProgramSample> getPaginatedProgramSamples(Integer startIndex, Integer pageSize) {
        String sql = "select ps from ProgramSample ps " + "join ps.program p " + "join ps.sample s " + "order by ps.id";

        Query<ProgramSample> query = entityManager.unwrap(Session.class).createQuery(sql, ProgramSample.class);
        query.setFirstResult(startIndex);
        query.setMaxResults(pageSize);
        return query.list();
    }

    @Override
    public List<ProgramSample> searchProgramSamples(String filter, Integer startIndex, Integer pageSize) {
        String sql = "select ps from ProgramSample ps " + "join ps.program p " + "join ps.sample s "
                + "where lower(p.programName) like :filter " + "or lower(s.accessionNumber) like :filter "
                + "order by ps.id";

        Query<ProgramSample> query = entityManager.unwrap(Session.class).createQuery(sql, ProgramSample.class);
        query.setParameter("filter", "%" + filter.toLowerCase() + "%");
        query.setFirstResult(startIndex);
        query.setMaxResults(pageSize);
        return query.list();
    }

}
