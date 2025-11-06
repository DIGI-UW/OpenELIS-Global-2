package org.openelisglobal.program.dao;

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
}
