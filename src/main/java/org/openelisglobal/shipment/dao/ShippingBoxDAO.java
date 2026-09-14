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
     * Find non-archived shipping boxes filtered by direction (inbound/outbound)
     *
     * @param inbound true for incoming boxes, false for outbound
     * @return List of matching shipping boxes
     */
    List<ShippingBox> findActiveByInbound(boolean inbound);

    /**
     * Count shipping boxes by state
     *
     * @param state Box state
     * @return Count of boxes in the state
     */
    int countByState(BoxState state);

    /**
     * Move a box's stored sample count by {@code delta} in one statement.
     *
     * <p>
     * Create Box adds its samples in concurrent requests, and the count has to
     * survive that. Reading the box, setting a count and saving it back loses the
     * race twice over: the box row carries an optimistic-lock version, so the
     * second request to save matches no row and its sample is refused outright; and
     * recounting in a subquery instead is no better, because each transaction sees
     * only its own insert and both write 1. Adjusting relative to the stored value
     * is correct because the database serialises the two updates on the row, so the
     * second one adds to what the first committed.
     *
     * @param shippingBoxId the box whose count is moving
     * @param delta         1 when a sample is added, -1 when one is removed
     */
    void adjustSampleCount(Integer shippingBoxId, int delta);
}
