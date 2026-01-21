package org.openelisglobal.biorepository.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.openelisglobal.biorepository.dao.BioSampleDAO;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BioSample.BiosafetyLevel;
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
}
