/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.medlab.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.medlab.valueholder.OrderSampleLink;

/**
 * DAO interface for OrderSampleLink entity operations.
 *
 * <p>
 * Enables order-driven validation: samples without orders are rejected at QC.
 */
public interface OrderSampleLinkDAO extends BaseDAO<OrderSampleLink, Integer> {

    /**
     * Check if a sample has a corresponding order link.
     *
     * <p>
     * This is the critical method for QC validation (FR-021, FR-025).
     *
     * @param sampleId the sample ID
     * @return true if sample has at least one order link, false otherwise
     */
    boolean hasOrderForSample(Integer sampleId);

    /**
     * Get all order links for a sample.
     *
     * @param sampleId the sample ID
     * @return list of order links
     */
    List<OrderSampleLink> getLinksBySampleId(Integer sampleId);

    /**
     * Get all sample links for an order.
     *
     * @param electronicOrderId the order ID
     * @return list of sample links
     */
    List<OrderSampleLink> getLinksByOrderId(Integer electronicOrderId);

    /**
     * Get link by sample item (for specific sample portion).
     *
     * @param sampleItemId the sample item ID
     * @return list of order links for the sample item
     */
    List<OrderSampleLink> getLinksBySampleItemId(Integer sampleItemId);

    /**
     * Get validated links for an order.
     *
     * @param electronicOrderId the order ID
     * @return list of validated links
     */
    List<OrderSampleLink> getValidatedLinksByOrderId(Integer electronicOrderId);

    /**
     * Get unvalidated links (pending QC).
     *
     * @return list of unvalidated links
     */
    List<OrderSampleLink> getUnvalidatedLinks();

    /**
     * Get link by order, sample, and test combination.
     *
     * @param electronicOrderId the order ID
     * @param sampleId          the sample ID
     * @param testId            the test ID (optional)
     * @return the link, or null if not found
     */
    OrderSampleLink getLinkByOrderSampleTest(Integer electronicOrderId, Integer sampleId, Integer testId);
}
