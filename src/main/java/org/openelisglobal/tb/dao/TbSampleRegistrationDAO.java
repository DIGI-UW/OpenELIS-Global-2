package org.openelisglobal.tb.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.tb.valueholder.TbSampleRegistration;

/**
 * Data access interface for TB sample registrations.
 */
public interface TbSampleRegistrationDAO extends BaseDAO<TbSampleRegistration, Integer> {

    /**
     * Find TB registration by sample item ID.
     */
    Optional<TbSampleRegistration> findBySampleItemId(String sampleItemId);

    /**
     * Find all registrations for a patient by document number.
     */
    List<TbSampleRegistration> findByDocumentNumber(String documentNumber);

    /**
     * Find all registrations from a referring facility.
     */
    List<TbSampleRegistration> findByReferringFacility(String facility);

    /**
     * Find registrations by received site, ordered by received datetime.
     */
    List<TbSampleRegistration> findByReceivedSite(String site);
}
