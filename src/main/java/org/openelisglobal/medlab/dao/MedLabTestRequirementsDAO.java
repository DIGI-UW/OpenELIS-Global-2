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
import org.openelisglobal.medlab.valueholder.MedLabTestRequirements;

/**
 * DAO interface for MedLabTestRequirements entity operations.
 *
 * <p>
 * Provides configuration data for order-driven sample collection requirements.
 */
public interface MedLabTestRequirementsDAO extends BaseDAO<MedLabTestRequirements, Integer> {

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
     * @param testIds list of test IDs
     * @return list of requirements for all specified tests
     */
    List<MedLabTestRequirements> getRequirementsByTestIds(List<Integer> testIds);

    /**
     * Get requirements by department.
     *
     * @param departmentId the department ID
     * @return list of requirements for the department
     */
    List<MedLabTestRequirements> getRequirementsByDepartment(Integer departmentId);
}
