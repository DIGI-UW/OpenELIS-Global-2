package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroCaseSpecimenDAO;
import org.openelisglobal.microbiology.valueholder.MicroCaseSpecimen;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroCaseSpecimenDAOImpl extends BaseDAOImpl<MicroCaseSpecimen, String> implements MicroCaseSpecimenDAO {
    public MicroCaseSpecimenDAOImpl() {
        super(MicroCaseSpecimen.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCaseSpecimen> getByCaseId(String caseId) {
        return entityManager
                .createQuery("from MicroCaseSpecimen m where m.caseId = :caseId" + " order by m.createdAt, m.id",
                        MicroCaseSpecimen.class)
                .setParameter("caseId", caseId).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCaseSpecimen> getByCaseIds(List<String> caseIds) {
        if (caseIds == null || caseIds.isEmpty()) {
            return List.of();
        }
        return entityManager
                .createQuery("from MicroCaseSpecimen m where m.caseId in (:caseIds)"
                        + " order by m.caseId, m.createdAt, m.id", MicroCaseSpecimen.class)
                .setParameter("caseIds", caseIds).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public MicroCaseSpecimen getByCaseAndSampleItem(String caseId, String sampleItemId) {
        return entityManager
                .createQuery(
                        "from MicroCaseSpecimen m where m.caseId = :caseId" + " and m.sampleItemId = :sampleItemId",
                        MicroCaseSpecimen.class)
                .setParameter("caseId", caseId).setParameter("sampleItemId", sampleItemId).getResultList().stream()
                .findFirst().orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasRecordedResults(String caseId, String sampleItemId) {
        boolean hasResult = !entityManager
                .createQuery("select r.id from Result r where r.analysis.sampleItem.id = :sampleItemId"
                        + " and r.analysis.id in (select l.analysisId from MicroCaseAnalysis l where l.caseId = :caseId)",
                        String.class)
                .setParameter("sampleItemId", sampleItemId).setParameter("caseId", caseId).setMaxResults(1)
                .getResultList().isEmpty();
        if (hasResult) {
            return true;
        }
        boolean hasIsolate = !entityManager
                .createQuery("select i.id from MicroIsolate i where i.sourceSampleItemId = :sampleItemId"
                        + " and i.caseId = :caseId", String.class)
                .setParameter("sampleItemId", sampleItemId).setParameter("caseId", caseId).setMaxResults(1)
                .getResultList().isEmpty();
        if (hasIsolate) {
            return true;
        }
        return !entityManager
                .createQuery("select a.id from MicroCaseActivity a where a.resultSourceSampleItemId = :sampleItemId"
                        + " and a.caseId = :caseId", String.class)
                .setParameter("sampleItemId", sampleItemId).setParameter("caseId", caseId).setMaxResults(1)
                .getResultList().isEmpty();
    }
}
