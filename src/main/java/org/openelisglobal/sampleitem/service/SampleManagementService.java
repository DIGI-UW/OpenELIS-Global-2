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
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 */
package org.openelisglobal.sampleitem.service;

import org.openelisglobal.sampleitem.dto.AddTestsResponse;
import org.openelisglobal.sampleitem.dto.CancelTestResponse;
import org.openelisglobal.sampleitem.dto.CreateAliquotResponse;
import org.openelisglobal.sampleitem.dto.SearchSamplesResponse;
import org.openelisglobal.sampleitem.form.AddTestsForm;
import org.openelisglobal.sampleitem.form.CancelTestForm;
import org.openelisglobal.sampleitem.form.CreateAliquotForm;

/**
 * Service interface for Sample Management operations.
 *
 * <p>
 * Provides business logic for sample item search, aliquoting, and test
 * management functionality. All methods in this service execute within
 * transactional boundaries.
 *
 * <p>
 * Related: Feature 001-sample-management
 *
 * @see org.openelisglobal.sampleitem.controller.SampleManagementRestController
 */
public interface SampleManagementService {

    /**
     * Search for sample items by accession number.
     *
     * <p>
     * Returns all sample items associated with the given accession number,
     * including aliquot hierarchy information. Optionally loads ordered tests for
     * each sample item.
     *
     * @param accessionNumber the sample accession number to search for
     * @param includeTests    if true, loads ordered tests for each sample item
     * @return SearchSamplesResponse containing matching sample items and metadata
     */
    SearchSamplesResponse searchByAccessionNumber(String accessionNumber, boolean includeTests);

    /**
     * Create an aliquot from a parent sample item.
     *
     * <p>
     * Creates a new child sample item by transferring a specified quantity from the
     * parent. The parent's remaining quantity is decreased by the transferred
     * amount. The new aliquot receives an external ID with .{n} suffix where n is
     * the next sequence number.
     *
     * <p>
     * Validation rules:
     * <ul>
     * <li>Parent must exist and have remaining quantity > 0</li>
     * <li>Quantity to transfer must not exceed parent's remaining quantity</li>
     * <li>Quantity to transfer must be > 0</li>
     * </ul>
     *
     * @param form      the aliquot creation request
     * @param sysUserId the system user ID for audit trail
     * @return CreateAliquotResponse containing the created aliquot and updated
     *         parent quantity
     * @throws IllegalArgumentException if validation fails
     * @throws IllegalStateException    if parent has no remaining quantity
     */
    CreateAliquotResponse createAliquot(CreateAliquotForm form, String sysUserId);

    /**
     * Add tests to one or more sample items.
     *
     * <p>
     * Adds the specified tests to the specified sample items. Automatically detects
     * and skips duplicates (tests already ordered for a sample item). The operation
     * is atomic: all valid test additions succeed even if some duplicates are
     * skipped.
     *
     * <p>
     * Creates an Analysis entity for each sample item / test combination with
     * status "NOT_STARTED".
     *
     * <p>
     * Related: Feature 001-sample-management, User Story 2
     *
     * @param form      the add tests request containing sample item IDs and test
     *                  IDs
     * @param sysUserId the system user ID for audit trail
     * @return AddTestsResponse with success count and detailed results per sample
     *         item
     * @throws IllegalArgumentException if sample item or test not found
     */
    AddTestsResponse addTestsToSamples(AddTestsForm form, String sysUserId);

    /**
     * Cancel/remove a test from a sample item.
     *
     * <p>
     * Sets the analysis status to "Canceled" for the specified analysis. Only tests
     * that have not been started or completed can be cancelled.
     *
     * <p>
     * Related: Feature 001-sample-management
     *
     * @param form      the cancel test request containing analysis ID and sample
     *                  item ID
     * @param sysUserId the system user ID for audit trail
     * @return CancelTestResponse with cancellation result
     * @throws IllegalArgumentException if analysis not found or doesn't belong to
     *                                  sample item
     * @throws IllegalStateException    if analysis cannot be cancelled (already
     *                                  completed)
     */
    CancelTestResponse cancelTest(CancelTestForm form, String sysUserId);
}
