package org.openelisglobal.shipment.service;

import java.util.List;
import org.openelisglobal.shipment.dto.SampleItemDTO;
import org.openelisglobal.shipment.valueholder.BoxSampleItem;
import org.openelisglobal.shipment.valueholder.ReceptionStatus;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Service for managing box sample items (sample items assigned to shipping
 * boxes).
 *
 * This service uses SampleItem (not Sample) as the correct granularity for
 * shipment operations.
 */
public interface BoxSampleItemService {

    /**
     * Get box sample item by ID
     *
     * @param id Box sample item ID
     * @return BoxSampleItem or null if not found
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_VIEW')")
    BoxSampleItem getBoxSampleItemById(Integer id);

    /**
     * Get box sample items by shipping box ID
     *
     * @param shippingBoxId Shipping box ID
     * @return List of box sample items
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_VIEW')")
    List<BoxSampleItem> getBoxSampleItemsByShippingBoxId(Integer shippingBoxId);

    /**
     * Get box sample items by shipping box ID as DTOs with full details. Includes
     * sample information, type of sample, and associated referral tests.
     *
     * @param shippingBoxId Shipping box ID
     * @return List of SampleItemDTO with full details
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_VIEW')")
    List<SampleItemDTO> getBoxSampleItemDTOsByShippingBoxId(Integer shippingBoxId);

    /**
     * Get box sample item by sample item ID
     *
     * @param sampleItemId Sample item ID (PK of SampleItem)
     * @return BoxSampleItem or null if not found
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_VIEW')")
    BoxSampleItem getBoxSampleItemBySampleItemId(String sampleItemId);

    /**
     * Get box sample items by reception status
     *
     * @param shippingBoxId   Shipping box ID
     * @param receptionStatus Reception status
     * @return List of box sample items
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_VIEW')")
    List<BoxSampleItem> getBoxSampleItemsByReceptionStatus(Integer shippingBoxId, ReceptionStatus receptionStatus);

    /**
     * Add sample item to box. Also updates all associated referrals to mark them as
     * assigned to this box.
     *
     * @param shippingBoxId Shipping box ID
     * @param sampleItemId  Sample item ID (PK of SampleItem)
     * @param systemUserId  System user ID for audit trail
     * @return Created BoxSampleItem
     * @throws IllegalStateException    if sample item already in a box
     * @throws IllegalArgumentException if sample item or box not found
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_MANAGE')")
    BoxSampleItem addSampleItemToBox(Integer shippingBoxId, String sampleItemId, Integer systemUserId);

    /**
     * Remove sample item from box. Also unassigns all associated referrals from
     * this box.
     *
     * @param boxSampleItemId Box sample item ID
     * @param systemUserId    System user ID for audit trail
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_MANAGE')")
    void removeSampleItemFromBox(Integer boxSampleItemId, Integer systemUserId);

    /**
     * Update reception status for a sample item
     *
     * @param boxSampleItemId Box sample item ID
     * @param receptionStatus New reception status
     * @param notes           Reception notes
     * @param systemUserId    System user ID for audit trail
     * @return Updated BoxSampleItem
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_MANAGE')")
    BoxSampleItem updateReceptionStatus(Integer boxSampleItemId, ReceptionStatus receptionStatus, String notes,
            Integer systemUserId);

    /**
     * Check if sample item is already in a box
     *
     * @param sampleItemId Sample item ID
     * @return true if sample item is assigned to a box
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_VIEW')")
    boolean isSampleItemInBox(String sampleItemId);

    /**
     * Count sample items in a box
     *
     * @param shippingBoxId Shipping box ID
     * @return Count of sample items
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_VIEW')")
    int countSampleItemsInBox(Integer shippingBoxId);
}
