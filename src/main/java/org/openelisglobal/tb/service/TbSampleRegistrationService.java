package org.openelisglobal.tb.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.tb.valueholder.TbSampleRegistration;

/**
 * Service interface for TB sample registration operations.
 */
public interface TbSampleRegistrationService extends BaseObjectService<TbSampleRegistration, Integer> {

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
     * Find registrations by received site.
     */
    List<TbSampleRegistration> findByReceivedSite(String site);
}
