package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.openelisglobal.analyzer.valueholder.AnalyzerLabUnit;
import org.openelisglobal.analyzer.valueholder.AnalyzerLabUnitId;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AnalyzerLabUnitDAOImpl extends BaseDAOImpl<AnalyzerLabUnit, AnalyzerLabUnitId>
        implements AnalyzerLabUnitDAO {

    public AnalyzerLabUnitDAOImpl() {
        super(AnalyzerLabUnit.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerLabUnit> findByAnalyzerId(Integer analyzerId) {
        return getAllMatching("analyzerId", analyzerId);
    }

    @Override
    public void replaceForAnalyzer(Integer analyzerId, List<String> labUnitIds) {
        try {
            Session session = entityManager.unwrap(Session.class);
            // TECH-DEBT: Hibernate 5.6 createQuery() rejects HQL DELETE here
            // ("query must begin with SELECT or FROM"), so this uses native SQL.
            // Replace with createMutationQuery() after Hibernate 6 upgrade.
            String deleteSql = "DELETE FROM analyzer_lab_unit WHERE analyzer_id = :analyzerId";
            NativeQuery<?> deleteQuery = session.createNativeQuery(deleteSql);
            deleteQuery.setParameter("analyzerId", analyzerId);
            deleteQuery.executeUpdate();

            if (labUnitIds != null) {
                for (String labUnitId : labUnitIds) {
                    if (labUnitId != null && !labUnitId.trim().isEmpty()) {
                        AnalyzerLabUnit unit = new AnalyzerLabUnit();
                        unit.setAnalyzerId(analyzerId);
                        unit.setLabUnitId(labUnitId.trim());
                        unit.setLastupdatedFields();
                        entityManager.persist(unit);
                    }
                }
            }
            entityManager.flush();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error replacing lab units for analyzer", e);
        }
    }
}
