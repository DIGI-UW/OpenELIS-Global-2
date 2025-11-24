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

import org.openelisglobal.sampleitem.dto.SearchSamplesResponse;

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
}
