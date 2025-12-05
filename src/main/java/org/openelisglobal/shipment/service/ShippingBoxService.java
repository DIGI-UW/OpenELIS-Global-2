package org.openelisglobal.shipment.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.openelisglobal.shipment.valueholder.BoxState;
import org.openelisglobal.shipment.valueholder.ShippingBox;

public interface ShippingBoxService {

    /**
     * Get all shipping boxes (non-archived)
     *
     * @return List of active shipping boxes
     */
    List<ShippingBox> getAllActiveBoxes();

    /**
     * Get shipping box by ID
     *
     * @param id Box ID
     * @return ShippingBox or null if not found
     */
    ShippingBox getBoxById(Integer id);

    /**
     * Get shipping box by box ID
     *
     * @param boxId Box identifier
     * @return ShippingBox or null if not found
     */
    ShippingBox getBoxByBoxId(Integer boxId);

    /**
     * Get shipping box by FHIR UUID
     *
     * @param fhirUuid FHIR resource UUID
     * @return ShippingBox or null if not found
     */
    ShippingBox getBoxByFhirUuid(UUID fhirUuid);

    /**
     * Get shipping boxes by state
     *
     * @param state Box state
     * @return List of shipping boxes
     */
    List<ShippingBox> getBoxesByState(BoxState state);

    /**
     * Get shipping boxes by destination facility
     *
     * @param facilityId Destination facility ID
     * @return List of shipping boxes
     */
    List<ShippingBox> getBoxesByDestinationFacility(Integer facilityId);

    /**
     * Get shipping boxes created within a date range
     *
     * @param startDate Start date
     * @param endDate   End date
     * @return List of shipping boxes
     */
    List<ShippingBox> getBoxesByCreatedDateRange(Timestamp startDate, Timestamp endDate);

    /**
     * Create a new shipping box
     *
     * @param box ShippingBox to create
     * @return Created ShippingBox with ID
     */
    ShippingBox createBox(ShippingBox box);

    /**
     * Update existing shipping box
     *
     * @param box ShippingBox to update
     * @return Updated ShippingBox
     */
    ShippingBox updateBox(ShippingBox box);

    /**
     * Delete shipping box (soft delete by archiving)
     *
     * @param id Box ID
     */
    void deleteBox(Integer id);

    /**
     * Change box state
     *
     * @param id       Box ID
     * @param newState New state
     * @return Updated ShippingBox
     */
    ShippingBox changeBoxState(Integer id, BoxState newState);

    /**
     * Mark box as ready to send (validates box has at least one sample)
     *
     * @param id Box ID
     * @return Updated ShippingBox
     * @throws IllegalStateException if box is empty
     */
    ShippingBox markReadyToSend(Integer id);

    /**
     * Get boxes for dashboard with sample counts and metadata Services MUST compile
     * all DTOs within transaction to prevent LazyInitializationException
     *
     * @return List of box data as Maps
     */
    List<Map<String, Object>> getBoxesForDashboard();

    /**
     * Count shipping boxes by state
     *
     * @param state Box state
     * @return Count of boxes
     */
    int countBoxesByState(BoxState state);
}
