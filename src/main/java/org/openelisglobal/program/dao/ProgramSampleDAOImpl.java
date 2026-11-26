package org.openelisglobal.program.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.program.valueholder.AnatomicPathologyBench;
import org.openelisglobal.program.valueholder.PriorCase;
import org.openelisglobal.program.valueholder.ProgramSample;
import org.openelisglobal.sample.valueholder.Sample;
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
        query.setParameter("sampleId", String.valueOf(sampleId));
        query.setMaxResults(1);
        ProgramSample programme = (ProgramSample) query.uniqueResult();
        return programme;
    }

    public String getTableName() {
        return "program_sample";
    }

    @Override
    public java.util.List<ProgramSample> getProgramSamplesByAccessionNumberOrProgramName(String filter) {
        String sql = "select ps from ProgramSample ps " + "join ps.program p " + "join ps.sample s "
                + "where lower(p.programName) like :filter " + "or lower(s.accessionNumber) like :filter "
                + "order by ps.id";

        Query<ProgramSample> query = entityManager.unwrap(Session.class).createQuery(sql, ProgramSample.class);
        query.setParameter("filter", "%" + filter.toLowerCase() + "%");
        return query.list();
    }

    @Override
    public long countByProgramId(String programId) {
        if (programId == null || programId.isBlank()) {
            return 0L;
        }
        String sql = "select count(ps) from ProgramSample ps where ps.program.id = :programId";
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(sql, Long.class);
        query.setParameter("programId", programId);
        Long result = query.uniqueResult();
        return result == null ? 0L : result;
    }

    @Override
    public Optional<ProgramSample> getCase(AnatomicPathologyBench bench, Integer id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(entityManager.find(bench.entityClass(), id));
    }

    @Override
    public List<PriorCase> getCasesSharingPatientWith(String sampleId) {
        List<PriorCase> cases = new ArrayList<>();
        for (AnatomicPathologyBench bench : AnatomicPathologyBench.values()) {
            // One query per bench: each case table holds its own status column, and a
            // query on ProgramSample would also read the rows of other programs.
            String sql = "select p.id, p.status, s from " + bench.entityClass().getSimpleName()
                    + " p join p.sample s, SampleHuman sh where sh.sampleId = s.id and sh.patientId in"
                    + " (select own.patientId from SampleHuman own where own.sampleId = :sampleId)";
            Query<Object[]> query = entityManager.unwrap(Session.class).createQuery(sql, Object[].class);
            query.setParameter("sampleId", sampleId);
            for (Object[] row : query.list()) {
                String status = row[1] == null ? null : ((Enum<?>) row[1]).name();
                cases.add(new PriorCase(bench, (Integer) row[0], status, (Sample) row[2]));
            }
        }
        return cases;
    }

    @Override
    public List<Object[]> getPathologyConclusions(Collection<Integer> caseIds) {
        if (caseIds == null || caseIds.isEmpty()) {
            return new ArrayList<>();
        }
        String sql = "select p.id, c.type, c.value from PathologySample p join p.conclusions c where p.id in (:ids)"
                + " order by p.id, c.id";
        Query<Object[]> query = entityManager.unwrap(Session.class).createQuery(sql, Object[].class);
        query.setParameterList("ids", caseIds);
        return query.list();
    }

    @Override
    public List<Object[]> getCytologyDiagnoses(Collection<Integer> caseIds) {
        if (caseIds == null || caseIds.isEmpty()) {
            return new ArrayList<>();
        }
        String sql = "select c.id, d, a.satisfaction from CytologySample c left join c.diagnosis d"
                + " left join c.specimenAdequacy a where c.id in (:ids)";
        Query<Object[]> query = entityManager.unwrap(Session.class).createQuery(sql, Object[].class);
        query.setParameterList("ids", caseIds);
        return query.list();
    }
}
