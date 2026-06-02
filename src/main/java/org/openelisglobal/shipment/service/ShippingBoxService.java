package org.openelisglobal.shipment.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.openelisglobal.shipment.valueholder.BoxState;
import org.openelisglobal.shipment.valueholder.ShippingBox;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ShippingBoxService {

    /**
     * Get all shipping boxes (non-archived)
     *
     * @return List of active shipping boxes
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_VIEW')")
    List<ShippingBox> getAllActiveBoxes();

    /**
     * Get shipping box by ID
     *
     * @param id Box ID
     * @return ShippingBox or null if not found
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_VIEW')")
    ShippingBox getBoxById(Integer id);

    /**
     * Get shipping box by box ID (the string identifier like "BOX-2025-001")
     *
     * @param boxId Box identifier string
     * @return ShippingBox or null if not found
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_VIEW')")
    ShippingBox getBoxByBoxId(String boxId);

    /**
     * Get shipping box by FHIR UUID
     *
     * @param fhirUuid FHIR resource UUID
     * @return ShippingBox or null if not found
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_VIEW')")
    ShippingBox getBoxByFhirUuid(UUID fhirUuid);

    /**
     * Get shipping boxes by state
     *
     * @param state Box state
     * @return List of shipping boxes
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_VIEW')")
    List<ShippingBox> getBoxesByState(BoxState state);

    /**
     * Get shipping boxes by destination facility
     *
     * @param facilityId Destination facility ID
     * @return List of shipping boxes
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_VIEW')")
    List<ShippingBox> getBoxesByDestinationFacility(Integer facilityId);

    /**
     * Get shipping boxes created within a date range
     *
     * @param startDate Start date
     * @param endDate   End date
     * @return List of shipping boxes
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_VIEW')")
    List<ShippingBox> getBoxesByCreatedDateRange(Timestamp startDate, Timestamp endDate);

    /**
     * Create a new shipping box
     *
     * @param box ShippingBox to create
     * @return Created ShippingBox with ID
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_MANAGE')")
    ShippingBox createBox(ShippingBox box);

    /**
     * Update existing shipping box
     *
     * @param box ShippingBox to update
     * @return Updated ShippingBox
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_MANAGE')")
    ShippingBox updateBox(ShippingBox box);

    /**
     * Delete shipping box (soft delete by archiving)
     *
     * @param id           Box ID
     * @param systemUserId System user ID for audit trail
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_MANAGE')")
    void deleteBox(Integer id, Integer systemUserId);

    /**
     * Change box state
     *
     * @param id           Box ID
     * @param newState     New state
     * @param systemUserId System user ID for audit trail
     * @return Updated ShippingBox
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_MANAGE')")
    ShippingBox changeBoxState(Integer id, BoxState newState, Integer systemUserId);

    /**
     * Mark box as ready to send (validates box has at least one sample)
     *
     * @param id Box ID
     * @return Updated ShippingBox
     * @throws IllegalStateException if box is empty
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_MANAGE')")
    ShippingBox markReadyToSend(Integer id);

    /**
     * Get boxes for dashboard with sample counts and metadata Services MUST compile
     * all DTOs within transaction to prevent LazyInitializationException
     *
     * @return List of box data as Maps
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_VIEW')")
    List<Map<String, Object>> getBoxesForDashboard();

    /**
     * Count shipping boxes by state
     *
     * @param state Box state
     * @return Count of boxes
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_VIEW')")
    int countBoxesByState(BoxState state);
}
