package org.openelisglobal.program.dao;

import jakarta.persistence.LockModeType;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.program.valueholder.pathology.PathologyReport;
import org.openelisglobal.program.valueholder.pathology.PathologyReport.PathologyReportType;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class PathologyReportDAOImpl extends BaseDAOImpl<PathologyReport, Integer> implements PathologyReportDAO {

    PathologyReportDAOImpl() {
        super(PathologyReport.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PathologyReport> getForCase(Integer pathologySampleId) {
        Query<PathologyReport> query = entityManager.unwrap(Session.class)
                .createQuery("from PathologyReport r where r.pathologySampleId = :caseId"
                        + " order by r.versionNumber desc nulls last, r.id desc", PathologyReport.class);
        query.setParameter("caseId", pathologySampleId);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PathologyReport> getListedForCase(Integer pathologySampleId) {
        Query<Object[]> query = entityManager.unwrap(Session.class)
                .createQuery("select r.id, r.fileType, r.reportType, r.versionNumber, r.voided, r.generatedAt, g"
                        + " from PathologyReport r left join r.generatedBy g"
                        + " where r.pathologySampleId = :caseId and r.image is not null"
                        + " order by r.versionNumber desc nulls last, r.id desc", Object[].class);
        query.setParameter("caseId", pathologySampleId);
        return query.list().stream().map(columns -> listed(pathologySampleId, columns)).collect(Collectors.toList());
    }

    private static PathologyReport listed(Integer pathologySampleId, Object[] columns) {
        PathologyReport row = new PathologyReport();
        row.setId((Integer) columns[0]);
        row.setPathologySampleId(pathologySampleId);
        row.setFileType((String) columns[1]);
        row.setReportType((PathologyReportType) columns[2]);
        row.setVersionNumber((Integer) columns[3]);
        row.setVoided((Boolean) columns[4]);
        row.setGeneratedAt((Timestamp) columns[5]);
        row.setGeneratedBy((SystemUser) columns[6]);
        return row;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PathologyReport> getOnCase(Integer pathologySampleId, Integer reportId) {
        Query<PathologyReport> query = entityManager.unwrap(Session.class).createQuery(
                "from PathologyReport r where r.pathologySampleId = :caseId and r.id = :reportId",
                PathologyReport.class);
        query.setParameter("caseId", pathologySampleId);
        query.setParameter("reportId", reportId);
        return query.list().stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public int nextVersion(Integer pathologySampleId) {
        Query<Integer> query = entityManager.unwrap(Session.class).createQuery(
                "select max(r.versionNumber) from PathologyReport r where r.pathologySampleId = :caseId",
                Integer.class);
        query.setParameter("caseId", pathologySampleId);
        Integer highest = query.uniqueResult();
        return highest == null ? 1 : highest + 1;
    }

    @Override
    public PathologySample lockCase(Integer pathologySampleId) {
        return entityManager.find(PathologySample.class, pathologySampleId, LockModeType.PESSIMISTIC_WRITE);
    }
}
