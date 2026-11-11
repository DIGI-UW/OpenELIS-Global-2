package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroSurveillanceDAO;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class MicroSurveillanceDAOImpl extends BaseDAOImpl<MicroCase, String> implements MicroSurveillanceDAO {
    public MicroSurveillanceDAOImpl() {
        super(MicroCase.class);
    }

    public List<Object[]> contexts(List<String> ids) {
        if (ids.isEmpty())
            return List.of();
        return entityManager.createQuery(
                "select c.id, s.domain, track.dictEntry, site.code, site.name, site.gpsLatitude, site.gpsLongitude"
                        + " from MicroCase c join Sample s on s.id = c.sampleId"
                        + " left join Program p on p.id = c.programId left join Dictionary track on track.id = p.reportingTrackId"
                        + " left join VectorSamplingSite site on site.id = c.siteId where c.id in (:ids)",
                Object[].class).setParameter("ids", ids).getResultList();
    }

    public List<Object[]> originalSpecimens(List<String> ids) {
        if (ids.isEmpty())
            return List.of();
        return entityManager.createQuery(
                "select i.id, t.id, t.whonetCode, t.description from MicroIsolate i"
                        + " join TypeOfSample t on t.id = i.originalSpecimenTypeId where i.caseId in (:ids)",
                Object[].class).setParameter("ids", ids).getResultList();
    }

    public List<String> externalSampleIds(List<String> ids) {
        if (ids.isEmpty())
            return List.of();
        var samples = new java.util.LinkedHashSet<>(entityManager.createQuery(
                "select distinct a.sampleItem.id from MicroCaseAnalysis link join Analysis a on a.id = link.analysisId"
                        + " where link.caseId in (:ids) and link.testedElsewhere = true and link.cancelledAt is null",
                String.class).setParameter("ids", ids).getResultList());
        samples.addAll(entityManager.createQuery("select distinct i.sampleItemId from MicroIncomingItem i"
                + " where i.microCase.id in (:ids) and i.source = 'REFERRAL' and i.state = 'PLACED' and i.sampleItemId is not null",
                String.class).setParameter("ids", ids).getResultList());
        return java.util.List.copyOf(samples);
    }

    public List<Object[]> outcomes(List<String> ids) {
        if (ids.isEmpty())
            return List.of();
        return entityManager
                .createQuery("select r.caseId, r.outcome from MicroCaseInoculation r"
                        + " where r.caseId in (:ids) and r.cancelledAt is null", Object[].class)
                .setParameter("ids", ids).getResultList();
    }
}
