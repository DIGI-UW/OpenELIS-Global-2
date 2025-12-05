package org.openelisglobal.shipment.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.shipment.valueholder.BoxSample;
import org.openelisglobal.shipment.valueholder.ReceptionStatus;

public interface BoxSampleDAO extends BaseDAO<BoxSample, Integer> {

    /**
     * Find box samples by shipping box ID
     *
     * @param shippingBoxId Shipping box ID
     * @return List of box samples
     */
    List<BoxSample> findByShippingBoxId(Integer shippingBoxId);

    /**
     * Find box sample by sample ID
     *
     * @param sampleId Sample ID
     * @return BoxSample or null if not found
     */
    BoxSample findBySampleId(Integer sampleId);

    /**
     * Find box sample by shipping box ID and sample ID
     *
     * @param shippingBoxId Shipping box ID
     * @param sampleId      Sample ID
     * @return BoxSample or null if not found
     */
    BoxSample findByShippingBoxIdAndSampleId(Integer shippingBoxId, Integer sampleId);

    /**
     * Find box samples by reception status
     *
     * @param shippingBoxId   Shipping box ID
     * @param receptionStatus Reception status
     * @return List of box samples
     */
    List<BoxSample> findByShippingBoxIdAndReceptionStatus(Integer shippingBoxId, ReceptionStatus receptionStatus);

    /**
     * Count samples in a box
     *
     * @param shippingBoxId Shipping box ID
     * @return Count of samples
     */
    int countByShippingBoxId(Integer shippingBoxId);

    /**
     * Check if sample exists in any box
     *
     * @param sampleId Sample ID
     * @return true if sample is assigned to a box
     */
    boolean existsBySampleId(Integer sampleId);
}
