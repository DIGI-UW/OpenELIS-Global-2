package org.openelisglobal.program.dao;

import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.program.valueholder.Program;
import org.springframework.stereotype.Repository;

@Repository
public class ProgramDAOImpl extends BaseDAOImpl<Program, String> implements ProgramDAO {
    ProgramDAOImpl() {
        super(Program.class);
    }

    @Override
    public List<Program> getGeneralPrograms(List<String> excludedNames) {
        if (excludedNames == null || excludedNames.isEmpty()) {
            return getAll();
        }
        // Convert excluded names to lower case for case-insensitive comparison
        List<String> lowerExcluded = excludedNames.stream().map(String::toLowerCase).collect(Collectors.toList());
        String hql = "FROM Program p WHERE LOWER(p.programName) NOT IN (:excluded)";
        TypedQuery<Program> query = entityManager.createQuery(hql, Program.class);
        query.setParameter("excluded", lowerExcluded);
        return query.getResultList();
    }
// ...existing code...
}
