package org.openelisglobal.microbiology.daoimpl;

import jakarta.persistence.LockModeType;
import java.sql.Timestamp;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroCaseDAOImpl extends BaseDAOImpl<MicroCase, String> implements MicroCaseDAO {

    static final String FINALIZED_EXPORT_BY_COLLECTION_DATE_HQL = "select c from MicroCase c"
            + " join Program p on p.id = c.programId" + " where c.finalReleaseState = :finalReleaseState"
            + " and exists (select track.id from MicroExportReportingTrack track"
            + " where track.exportKey = :exportKey and track.reportingTrackId = p.reportingTrackId)"
            + " and exists (select membership.id from MicroCaseSpecimen membership"
            + " join SampleItem specimen on specimen.id = membership.sampleItemId"
            + " where membership.caseId = c.id and specimen.collectionDate >= :fromInclusive"
            + " and specimen.collectionDate < :toExclusive) order by c.createdAt, c.id";

    public MicroCaseDAOImpl() {
        super(MicroCase.class);
    }

    @Override
    public void lockOrder(String sampleId) {
        entityManager.createQuery("from Sample s where s.id = :sampleId", Sample.class)
                .setParameter("sampleId", sampleId).setLockMode(LockModeType.PESSIMISTIC_WRITE).getSingleResult();
    }

    @Override
    public MicroCase getForUpdate(String caseId) {
        MicroCase microCase = entityManager.find(MicroCase.class, caseId);
        if (microCase == null) {
            throw new IllegalArgumentException("Case not found");
        }
        // Refresh a case already read in this transaction before waiting for
        // the order lock, so a preceding transfer cannot leave stale ownership.
        // Lock while refreshing, rather than version-checking a cached entity
        // in a locking query before its current version has been loaded.
        entityManager.refresh(microCase, LockModeType.PESSIMISTIC_WRITE);
        return microCase;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCase> getRoutingCandidates(String sampleId, String sampleTypeId, String testSectionId,
            String collectedInSetsTestId, String sampleItemId) {
        List<MicroCase> memberCases = entityManager
                .createQuery(
                        "select c from MicroCase c where c.sampleId = :sampleId and c.testSectionId = :unitId"
                                + " and exists (select m.id from MicroCaseSpecimen m"
                                + " where m.caseId = c.id and m.sampleItemId = :itemId) order by c.createdAt, c.id",
                        MicroCase.class)
                .setParameter("sampleId", sampleId).setParameter("unitId", testSectionId)
                .setParameter("itemId", sampleItemId).getResultList();
        if (!memberCases.isEmpty()) {
            return memberCases;
        }
        String match = collectedInSetsTestId == null ? "c.sampleTypeId = :sampleTypeId"
                : "exists (select l.id from MicroCaseAnalysis l join Analysis a on a.id = l.analysisId"
                        + " where l.caseId = c.id and l.collectedInSets = true and a.test.id = :setsTestId)";
        Query<MicroCase> query = entityManager.unwrap(Session.class)
                .createQuery("select c from MicroCase c where c.sampleId = :sampleId and c.testSectionId = :unitId"
                        + " and " + match + " order by c.createdAt, c.id", MicroCase.class);
        query.setParameter("sampleId", sampleId).setParameter("unitId", testSectionId);
        if (collectedInSetsTestId == null) {
            query.setParameter("sampleTypeId", sampleTypeId);
        } else {
            query.setParameter("setsTestId", collectedInSetsTestId);
        }
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCase> getByOrder(String sampleId) {
        return entityManager.createQuery("from MicroCase c where c.sampleId = :sampleId order by c.createdAt, c.id",
                MicroCase.class).setParameter("sampleId", sampleId).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCase> getBySampleItem(String sampleItemId) {
        Query<MicroCase> query = entityManager.unwrap(Session.class)
                .createQuery("select c from MicroCase c where exists (select m.id from MicroCaseSpecimen m"
                        + " where m.caseId = c.id and m.sampleItemId = :sampleItemId) order by c.createdAt, c.id",
                        MicroCase.class);
        query.setParameter("sampleItemId", sampleItemId);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCase> getBySampleItemIds(List<String> sampleItemIds) {
        if (sampleItemIds == null || sampleItemIds.isEmpty()) {
            return List.of();
        }
        Query<MicroCase> query = entityManager.unwrap(Session.class)
                .createQuery("select c from MicroCase c where exists (select m.id from MicroCaseSpecimen m"
                        + " where m.caseId = c.id and m.sampleItemId in (:sampleItemIds)) order by c.createdAt, c.id",
                        MicroCase.class);
        query.setParameterList("sampleItemIds", sampleItemIds);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCase> getOpenCases(boolean allUnits, java.util.Set<String> unitIds) {
        if (!allUnits && unitIds.isEmpty()) {
            return List.of();
        }
        Query<MicroCase> query = entityManager.unwrap(Session.class)
                .createQuery("from MicroCase c where c.closedAt is null"
                        + (allUnits ? "" : " and c.testSectionId in (:unitIds)") + " order by c.createdAt, c.id",
                        MicroCase.class);
        if (!allUnits) {
            query.setParameterList("unitIds", unitIds);
        }
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasExportReportingTracks(String exportKey) {
        return !entityManager
                .createQuery("select track.id from MicroExportReportingTrack track where track.exportKey = :exportKey",
                        String.class)
                .setParameter("exportKey", exportKey).setMaxResults(1).getResultList().isEmpty();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCase> getFinalizedForExportByCollectionDateRange(String exportKey, Timestamp fromInclusive,
            Timestamp toExclusive) {
        Query<MicroCase> query = entityManager.unwrap(Session.class)
                .createQuery(FINALIZED_EXPORT_BY_COLLECTION_DATE_HQL, MicroCase.class);
        query.setParameter("exportKey", exportKey);
        query.setParameter("finalReleaseState", "FINAL_RELEASED");
        query.setParameter("fromInclusive", fromInclusive);
        query.setParameter("toExclusive", toExclusive);
        return query.list();
    }

}
