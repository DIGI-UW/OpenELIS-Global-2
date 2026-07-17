package org.openelisglobal.shipment.dao;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.shipment.valueholder.BoxState;
import org.openelisglobal.shipment.valueholder.ShippingBox;

public interface ShippingBoxDAO extends BaseDAO<ShippingBox, Integer> {

    /**
     * Find shipping box by box ID (the string identifier like "BOX-2025-001")
     *
     * @param boxId Box identifier string
     * @return ShippingBox or null if not found
     */
    ShippingBox findByBoxId(String boxId);

    /**
     * Find shipping box by FHIR UUID
     *
     * @param fhirUuid FHIR resource UUID
     * @return ShippingBox or null if not found
     */
    ShippingBox findByFhirUuid(UUID fhirUuid);

    /**
     * Find shipping boxes by state
     *
     * @param state Box state
     * @return List of shipping boxes
     */
    List<ShippingBox> findByState(BoxState state);

    /**
     * Find shipping boxes by destination facility
     *
     * @param facilityId Destination facility ID
     * @return List of shipping boxes
     */
    List<ShippingBox> findByDestinationFacilityId(Integer facilityId);

    /**
     * Find shipping boxes created within a date range
     *
     * @param startDate Start date
     * @param endDate   End date
     * @return List of shipping boxes
     */
    List<ShippingBox> findByCreatedDateRange(Timestamp startDate, Timestamp endDate);

    /**
     * Find non-archived shipping boxes for dashboard
     *
     * @return List of active shipping boxes
     */
    List<ShippingBox> findAllActive();

    /**
     * Count shipping boxes by state
     *
     * @param state Box state
     * @return Count of boxes in the state
     */
    int countByState(BoxState state);
}
