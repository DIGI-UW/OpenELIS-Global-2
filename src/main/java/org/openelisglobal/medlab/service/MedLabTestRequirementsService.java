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
import org.openelisglobal.medlab.valueholder.MedLabTestRequirements;

/**
 * Service interface for MedLabTestRequirements operations.
 *
 * <p>
 * Provides configuration data for order-driven sample collection requirements
 * (FR-006, FR-007).
 */
public interface MedLabTestRequirementsService extends BaseObjectService<MedLabTestRequirements, Integer> {

    /**
     * Get requirements for a specific test.
     *
     * @param testId the test ID
     * @return list of requirements for the test
     */
    List<MedLabTestRequirements> getRequirementsByTestId(Integer testId);

    /**
     * Get requirements for a specific test and sample type combination.
     *
     * @param testId         the test ID
     * @param typeOfSampleId the sample type ID
     * @return the requirements, or null if not found
     */
    MedLabTestRequirements getRequirementsByTestAndSampleType(Integer testId, Integer typeOfSampleId);

    /**
     * Get all active requirements.
     *
     * @return list of active requirements
     */
    List<MedLabTestRequirements> getActiveRequirements();

    /**
     * Get requirements for multiple tests (for bulk order display).
     *
     * <p>
     * Used when displaying collection requirements for an order with multiple
     * tests.
     *
     * @param testIds list of test IDs
     * @return list of requirements for all specified tests
     */
    List<MedLabTestRequirements> getRequirementsByTestIds(List<Integer> testIds);

    /**
     * Get requirements by department for routing.
     *
     * @param departmentId the department ID
     * @return list of requirements for the department
     */
    List<MedLabTestRequirements> getRequirementsByDepartment(Integer departmentId);
}
