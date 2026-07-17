package org.openelisglobal.shipment.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.shipment.valueholder.BoxSampleItem;
import org.openelisglobal.shipment.valueholder.ReceptionStatus;

public interface BoxSampleItemDAO extends BaseDAO<BoxSampleItem, Integer> {

    /**
     * Find box sample items by shipping box ID
     *
     * @param shippingBoxId Shipping box ID
     * @return List of box sample items
     */
    List<BoxSampleItem> findByShippingBoxId(Integer shippingBoxId);

    /**
     * Find box sample item by sample item ID
     *
     * @param sampleItemId Sample item ID (PK of SampleItem)
     * @return BoxSampleItem or null if not found
     */
    BoxSampleItem findBySampleItemId(String sampleItemId);

    /**
     * Find box sample item by shipping box ID and sample item ID
     *
     * @param shippingBoxId Shipping box ID
     * @param sampleItemId  Sample item ID
     * @return BoxSampleItem or null if not found
     */
    BoxSampleItem findByShippingBoxIdAndSampleItemId(Integer shippingBoxId, String sampleItemId);

    /**
     * Find box sample items by reception status
     *
     * @param shippingBoxId   Shipping box ID
     * @param receptionStatus Reception status
     * @return List of box sample items
     */
    List<BoxSampleItem> findByShippingBoxIdAndReceptionStatus(Integer shippingBoxId, ReceptionStatus receptionStatus);

    /**
     * Count sample items in a box
     *
     * @param shippingBoxId Shipping box ID
     * @return Count of sample items
     */
    int countByShippingBoxId(Integer shippingBoxId);

    /**
     * Check if sample item exists in any box
     *
     * @param sampleItemId Sample item ID
     * @return true if sample item is assigned to a box
     */
    boolean existsBySampleItemId(String sampleItemId);

    /**
     * Get all sample item IDs that are already assigned to boxes. Used to exclude
     * them from unassigned sample searches.
     *
     * @return List of sample item IDs (PKs) that are in boxes
     */
    List<String> getAllAssignedSampleItemIds();
}
