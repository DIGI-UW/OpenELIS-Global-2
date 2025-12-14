package org.openelisglobal.shipment.service;

import java.util.List;
import org.openelisglobal.shipment.valueholder.BoxSample;
import org.openelisglobal.shipment.valueholder.ReceptionStatus;

public interface BoxSampleService {

    /**
     * Get box sample by ID
     *
     * @param id Box sample ID
     * @return BoxSample or null if not found
     */
    BoxSample getBoxSampleById(Integer id);

    /**
     * Get box samples by shipping box ID
     *
     * @param shippingBoxId Shipping box ID
     * @return List of box samples
     */
    List<BoxSample> getBoxSamplesByShippingBoxId(Integer shippingBoxId);

    /**
     * Get box sample by sample ID
     *
     * @param sampleId Sample ID
     * @return BoxSample or null if not found
     */
    BoxSample getBoxSampleBySampleId(Integer sampleId);

    /**
     * Get box samples by reception status
     *
     * @param shippingBoxId   Shipping box ID
     * @param receptionStatus Reception status
     * @return List of box samples
     */
    List<BoxSample> getBoxSamplesByReceptionStatus(Integer shippingBoxId, ReceptionStatus receptionStatus);

    /**
     * Add sample to box
     *
     * @param shippingBoxId Shipping box ID
     * @param sampleId      Sample ID
     * @param systemUserId  System user ID for audit trail
     * @return Created BoxSample
     * @throws IllegalStateException if sample already in a box
     */
    BoxSample addSampleToBox(Integer shippingBoxId, Integer sampleId, Integer systemUserId);

    /**
     * Remove sample from box
     *
     * @param boxSampleId Box sample ID
     */
    void removeSampleFromBox(Integer boxSampleId);

    /**
     * Update reception status for a sample
     *
     * @param boxSampleId     Box sample ID
     * @param receptionStatus New reception status
     * @param notes           Reception notes
     * @return Updated BoxSample
     */
    BoxSample updateReceptionStatus(Integer boxSampleId, ReceptionStatus receptionStatus, String notes);

    /**
     * Check if sample is already in a box
     *
     * @param sampleId Sample ID
     * @return true if sample is assigned to a box
     */
    boolean isSampleInBox(Integer sampleId);

    /**
     * Count samples in a box
     *
     * @param shippingBoxId Shipping box ID
     * @return Count of samples
     */
    int countSamplesInBox(Integer shippingBoxId);
}
