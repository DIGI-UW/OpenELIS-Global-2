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
package org.openelisglobal.medlab.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.medlab.valueholder.OrderSampleLink;

/**
 * Service interface for OrderSampleLink operations.
 *
 * <p>
 * Enables order-driven validation (FR-021, FR-025):
 * <ul>
 * <li>Samples without corresponding orders are rejected at QC
 * <li>Links samples to their fulfilling orders with requirements snapshot
 * </ul>
 */
public interface OrderSampleLinkService extends BaseObjectService<OrderSampleLink, Integer> {

    /**
     * Check if a sample has a corresponding order link.
     *
     * <p>
     * This is the CRITICAL method for QC validation (FR-021, FR-025). Samples
     * without orders MUST be rejected.
     *
     * @param sampleId the sample ID
     * @return true if sample has at least one order link, false otherwise
     */
    boolean hasOrderForSample(Integer sampleId);

    /**
     * Link a sample to an order.
     *
     * <p>
     * Creates an OrderSampleLink with optional requirements snapshot.
     *
     * @param electronicOrderId the order ID
     * @param sampleId          the sample ID
     * @param sampleItemId      the sample item ID (optional)
     * @param testId            the test ID (optional)
     * @param createdBy         the user ID who created the link
     * @return the created OrderSampleLink
     */
    OrderSampleLink linkSampleToOrder(Integer electronicOrderId, Integer sampleId, Integer sampleItemId, Integer testId,
            Integer createdBy);

    /**
     * Link a sample to an order with requirements snapshot.
     *
     * @param electronicOrderId     the order ID
     * @param sampleId              the sample ID
     * @param sampleItemId          the sample item ID (optional)
     * @param testId                the test ID (optional)
     * @param containerTypeRequired the required container type
     * @param volumeRequiredMl      the required volume in ml
     * @param handlingRequirements  the handling requirements
     * @param createdBy             the user ID who created the link
     * @return the created OrderSampleLink
     */
    OrderSampleLink linkSampleToOrderWithRequirements(Integer electronicOrderId, Integer sampleId, Integer sampleItemId,
            Integer testId, String containerTypeRequired, java.math.BigDecimal volumeRequiredMl,
            String handlingRequirements, Integer createdBy);

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
     * Get link by sample item.
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
     * Mark a link as validated.
     *
     * @param linkId      the link ID
     * @param validatorId the user ID who validated
     * @return the updated OrderSampleLink
     */
    OrderSampleLink markAsValidated(Integer linkId, Integer validatorId);

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
