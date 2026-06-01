package org.openelisglobal.biorepository.daoimpl;

import java.util.List;
import java.util.Set;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.biorepository.dao.BioSampleDAO;
import org.openelisglobal.biorepository.dao.BioSampleRetrievalSearchCriteria;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BioSample.BiosafetyLevel;
import org.openelisglobal.biorepository.valueholder.BioSample.WorkflowStatus;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;

/**
 * DAO implementation for BioSample entity operations.
 *
 * BioSample is now an extension record for SampleItem, containing only
 * biorepository-specific metadata. Core sample data (barcode, type, quantity,
 * dates) is stored in Sample and SampleItem entities.
 */
@Component
public class BioSampleDAOImpl extends BaseDAOImpl<BioSample, Integer> implements BioSampleDAO {

    public BioSampleDAOImpl() {
        super(BioSample.class);
    }

    @Override
    public BioSample getBySampleItemId(Integer sampleItemId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM BioSample bs WHERE bs.sampleItem.id = :sampleItemId";
        List<BioSample> results = session.createQuery(hql, BioSample.class).setParameter("sampleItemId", sampleItemId)
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public List<BioSample> getByShipmentId(Integer shipmentId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM BioSample bs WHERE bs.shipment.id = :shipmentId";
        return session.createQuery(hql, BioSample.class).setParameter("shipmentId", shipmentId).getResultList();
    }

    @Override
    public List<BioSample> getByBiosafetyLevel(BiosafetyLevel biosafetyLevel) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM BioSample bs WHERE bs.biosafetyLevel = :level";
        return session.createQuery(hql, BioSample.class).setParameter("level", biosafetyLevel.getDisplayValue())
                .getResultList();
    }

    @Override
    public List<BioSample> getByEthicsApprovalRef(String ethicsApprovalRef) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM BioSample bs WHERE bs.ethicsApprovalRef = :ethicsRef";
        return session.createQuery(hql, BioSample.class).setParameter("ethicsRef", ethicsApprovalRef).getResultList();
    }

    @Override
    public List<BioSample> getByMtaReference(String mtaReference) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM BioSample bs WHERE bs.mtaReference = :mtaRef";
        return session.createQuery(hql, BioSample.class).setParameter("mtaRef", mtaReference).getResultList();
    }

    @Override
    public List<BioSample> getByPrincipalInvestigator(String principalInvestigator) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM BioSample bs WHERE bs.principalInvestigator = :pi";
        return session.createQuery(hql, BioSample.class).setParameter("pi", principalInvestigator).getResultList();
    }

    @Override
    public boolean existsBySampleItemId(Integer sampleItemId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT COUNT(bs) FROM BioSample bs WHERE bs.sampleItem.id = :sampleItemId";
        Long count = session.createQuery(hql, Long.class).setParameter("sampleItemId", sampleItemId).getSingleResult();
        return count > 0;
    }

    @Override
    public long countByShipmentId(Integer shipmentId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT COUNT(bs) FROM BioSample bs WHERE bs.shipment.id = :shipmentId";
        return session.createQuery(hql, Long.class).setParameter("shipmentId", shipmentId).getSingleResult();
    }

    @Override
    public long countByBiosafetyLevel(BiosafetyLevel biosafetyLevel) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT COUNT(bs) FROM BioSample bs WHERE bs.biosafetyLevel = :level";
        return session.createQuery(hql, Long.class).setParameter("level", biosafetyLevel.getDisplayValue())
                .getSingleResult();
    }

    @Override
    public List<BioSample> getAllWithRelationships(int limit) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT DISTINCT bs FROM BioSample bs " + "LEFT JOIN FETCH bs.shipment "
                + "LEFT JOIN FETCH bs.sampleItem si " + "LEFT JOIN FETCH si.typeOfSample "
                + "LEFT JOIN FETCH si.sample " + "ORDER BY bs.id DESC";
        return session.createQuery(hql, BioSample.class).setMaxResults(limit).getResultList();
    }

    @Override
    public List<BioSample> getByShipmentIdWithRelationships(Integer shipmentId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT DISTINCT bs FROM BioSample bs " + "LEFT JOIN FETCH bs.shipment s "
                + "LEFT JOIN FETCH bs.sampleItem si " + "LEFT JOIN FETCH si.typeOfSample "
                + "LEFT JOIN FETCH si.sample " + "WHERE s.id = :shipmentId";
        return session.createQuery(hql, BioSample.class).setParameter("shipmentId", shipmentId).getResultList();
    }

    @Override
    public List<BioSample> getBySampleItemIds(List<Integer> sampleItemIds) {
        if (sampleItemIds == null || sampleItemIds.isEmpty()) {
            return List.of();
        }
        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT DISTINCT bs FROM BioSample bs " + "LEFT JOIN FETCH bs.shipment "
                + "LEFT JOIN FETCH bs.sampleItem si " + "LEFT JOIN FETCH si.typeOfSample "
                + "LEFT JOIN FETCH si.sample " + "WHERE si.id IN :sampleItemIds";
        return session.createQuery(hql, BioSample.class).setParameter("sampleItemIds", sampleItemIds).getResultList();
    }

    @Override
    public List<BioSample> getByWorkflowStatusWithRelationships(WorkflowStatus workflowStatus) {
        if (workflowStatus == null) {
            return List.of();
        }

        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT DISTINCT bs FROM BioSample bs "
                + "LEFT JOIN FETCH bs.shipment "
                + "LEFT JOIN FETCH bs.sampleItem si "
                + "LEFT JOIN FETCH si.typeOfSample "
                + "LEFT JOIN FETCH si.sample "
                + "WHERE bs.workflowStatus = :workflowStatus "
                + "ORDER BY bs.id DESC";
        return session.createQuery(hql, BioSample.class)
            .setParameter("workflowStatus", workflowStatus)
                .getResultList();
    }

    @Override
    public List<BioSample> getExpiringSamplesBefore(java.sql.Date expiryDate) {
        Session session = entityManager.unwrap(Session.class);
        // Get samples expiring between today and the cutoff date, not already disposed
        String hql = "SELECT DISTINCT bs FROM BioSample bs " + "LEFT JOIN FETCH bs.shipment "
                + "LEFT JOIN FETCH bs.sampleItem si " + "LEFT JOIN FETCH si.typeOfSample "
                + "LEFT JOIN FETCH si.sample " + "WHERE bs.retentionExpiryDate IS NOT NULL "
                + "AND bs.retentionExpiryDate > CURRENT_DATE " + "AND bs.retentionExpiryDate <= :expiryDate "
                + "AND (bs.workflowStatus IS NULL OR bs.workflowStatus != 'DISPOSED') "
                + "ORDER BY bs.retentionExpiryDate ASC";
        return session.createQuery(hql, BioSample.class).setParameter("expiryDate", expiryDate).getResultList();
    }

    @Override
    public List<BioSample> getExpiredSamples(java.sql.Date today) {
        Session session = entityManager.unwrap(Session.class);
        // Get samples already past their expiry date, not already disposed
        String hql = "SELECT DISTINCT bs FROM BioSample bs " + "LEFT JOIN FETCH bs.shipment "
                + "LEFT JOIN FETCH bs.sampleItem si " + "LEFT JOIN FETCH si.typeOfSample "
                + "LEFT JOIN FETCH si.sample " + "WHERE bs.retentionExpiryDate IS NOT NULL "
                + "AND bs.retentionExpiryDate < :today "
                + "AND (bs.workflowStatus IS NULL OR bs.workflowStatus != 'DISPOSED') "
                + "ORDER BY bs.retentionExpiryDate ASC";
        return session.createQuery(hql, BioSample.class).setParameter("today", today).getResultList();
    }

    @Override
    public List<BioSample> getSamplesWithRetentionData() {
        Session session = entityManager.unwrap(Session.class);
        // Get all samples with retention expiry date, not disposed
        String hql = "SELECT DISTINCT bs FROM BioSample bs " + "LEFT JOIN FETCH bs.shipment "
                + "LEFT JOIN FETCH bs.sampleItem si " + "LEFT JOIN FETCH si.typeOfSample "
                + "LEFT JOIN FETCH si.sample " + "WHERE bs.retentionExpiryDate IS NOT NULL "
                + "AND (bs.workflowStatus IS NULL OR bs.workflowStatus != 'DISPOSED') "
                + "ORDER BY bs.retentionExpiryDate ASC";
        return session.createQuery(hql, BioSample.class).getResultList();
    }

    @Override
    public List<BioSample> getByOriginLab(String originLab) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT DISTINCT bs FROM BioSample bs " + "LEFT JOIN FETCH bs.shipment "
                + "LEFT JOIN FETCH bs.sampleItem si " + "LEFT JOIN FETCH si.typeOfSample "
                + "LEFT JOIN FETCH si.sample " + "WHERE bs.originLab = :originLab";
        return session.createQuery(hql, BioSample.class).setParameter("originLab", originLab).getResultList();
    }

    @Override
    public List<BioSample> getByProjectId(String projectId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT DISTINCT bs FROM BioSample bs " + "LEFT JOIN FETCH bs.shipment "
                + "LEFT JOIN FETCH bs.sampleItem si " + "LEFT JOIN FETCH si.typeOfSample "
                + "LEFT JOIN FETCH si.sample " + "WHERE bs.projectId = :projectId";
        return session.createQuery(hql, BioSample.class).setParameter("projectId", projectId).getResultList();
    }

    @Override
    public List<BioSample> searchForRetrieval(BioSampleRetrievalSearchCriteria criteria) {
        if (criteria == null) {
            return List.of();
        }

        Session session = entityManager.unwrap(Session.class);
        StringBuilder hql = new StringBuilder();
        hql.append("SELECT DISTINCT bs FROM BioSample bs ");
        hql.append("LEFT JOIN FETCH bs.shipment ");
        hql.append("LEFT JOIN FETCH bs.sampleItem si ");
        hql.append("LEFT JOIN FETCH si.typeOfSample ");
        hql.append("LEFT JOIN FETCH si.sample s ");
        hql.append("WHERE bs.sampleItem IS NOT NULL ");

        if (criteria.getWorkflowStatus() != null) {
            hql.append("AND bs.workflowStatus = :workflowStatus ");
        }
        if (criteria.getIdentityPattern() != null) {
            hql.append("AND (LOWER(si.externalId) LIKE LOWER(:identityPattern) ");
            hql.append("OR LOWER(s.accessionNumber) LIKE LOWER(:identityPattern)) ");
        }
        if (criteria.getBarcodePattern() != null) {
            hql.append("AND LOWER(si.externalId) LIKE LOWER(:barcodePattern) ");
        }
        if (criteria.getAccessionPattern() != null) {
            hql.append("AND LOWER(s.accessionNumber) LIKE LOWER(:accessionPattern) ");
        }
        Set<String> sampleTypeIds = criteria.getSampleTypeIds();
        if (sampleTypeIds != null && !sampleTypeIds.isEmpty()) {
            hql.append("AND si.typeOfSample.id IN :sampleTypeIds ");
        }
        if (criteria.getSampleTypeDescriptionPattern() != null) {
            hql.append("AND (LOWER(si.typeOfSample.description) LIKE LOWER(:sampleTypeDescriptionPattern) ");
            hql.append("OR LOWER(si.typeOfSample.localAbbreviation) LIKE LOWER(:sampleTypeDescriptionPattern)) ");
        }
        if (criteria.getOriginLabPattern() != null) {
            hql.append("AND LOWER(bs.originLab) LIKE LOWER(:originLabPattern) ");
        }
        if (criteria.getProjectIdPattern() != null) {
            hql.append("AND LOWER(bs.projectId) LIKE LOWER(:projectIdPattern) ");
        }
        if (criteria.getCollectionDateFrom() != null) {
            hql.append("AND si.collectionDate >= :collectionDateFrom ");
        }
        if (criteria.getCollectionDateTo() != null) {
            hql.append("AND si.collectionDate <= :collectionDateTo ");
        }
        hql.append("AND ((si.remainingQuantity IS NOT NULL AND si.remainingQuantity > 0) ");
        hql.append("OR (si.remainingQuantity IS NULL AND si.quantity IS NOT NULL AND si.quantity > 0)) ");
        hql.append("ORDER BY si.collectionDate DESC, bs.id DESC");

        Query<BioSample> query = session.createQuery(hql.toString(), BioSample.class);
        if (criteria.getWorkflowStatus() != null) {
            query.setParameter("workflowStatus", criteria.getWorkflowStatus());
        }
        if (criteria.getIdentityPattern() != null) {
            query.setParameter("identityPattern", criteria.getIdentityPattern());
        }
        if (criteria.getBarcodePattern() != null) {
            query.setParameter("barcodePattern", criteria.getBarcodePattern());
        }
        if (criteria.getAccessionPattern() != null) {
            query.setParameter("accessionPattern", criteria.getAccessionPattern());
        }
        if (sampleTypeIds != null && !sampleTypeIds.isEmpty()) {
            query.setParameter("sampleTypeIds", sampleTypeIds);
        }
        if (criteria.getSampleTypeDescriptionPattern() != null) {
            query.setParameter("sampleTypeDescriptionPattern", criteria.getSampleTypeDescriptionPattern());
        }
        if (criteria.getOriginLabPattern() != null) {
            query.setParameter("originLabPattern", criteria.getOriginLabPattern());
        }
        if (criteria.getProjectIdPattern() != null) {
            query.setParameter("projectIdPattern", criteria.getProjectIdPattern());
        }
        if (criteria.getCollectionDateFrom() != null) {
            query.setParameter("collectionDateFrom", criteria.getCollectionDateFrom());
        }
        if (criteria.getCollectionDateTo() != null) {
            query.setParameter("collectionDateTo", criteria.getCollectionDateTo());
        }

        int limit = criteria.getLimit() > 0 ? criteria.getLimit() : 50;
        return query.setMaxResults(limit).getResultList();
    }
}
