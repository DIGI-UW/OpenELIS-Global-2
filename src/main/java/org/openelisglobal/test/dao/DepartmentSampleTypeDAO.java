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
package org.openelisglobal.test.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.test.valueholder.DepartmentSampleType;

/**
 * DAO interface for managing DepartmentSampleType associations. Provides
 * methods to query and manage the relationship between departments (test
 * sections) and their supported sample types.
 */
public interface DepartmentSampleTypeDAO extends BaseDAO<DepartmentSampleType, String> {

    /**
     * Get all sample type associations for a department.
     *
     * @param testSectionId the test section (department) ID
     * @return list of DepartmentSampleType associations
     * @throws LIMSRuntimeException if database error occurs
     */
    List<DepartmentSampleType> getByTestSectionId(String testSectionId) throws LIMSRuntimeException;

    /**
     * Get all department associations for a sample type.
     *
     * @param sampleTypeId the sample type ID
     * @return list of DepartmentSampleType associations
     * @throws LIMSRuntimeException if database error occurs
     */
    List<DepartmentSampleType> getBySampleTypeId(String sampleTypeId) throws LIMSRuntimeException;

    /**
     * Get active sample type associations for a department, ordered by sortOrder.
     *
     * @param testSectionId the test section (department) ID
     * @return list of active DepartmentSampleType associations ordered by sortOrder
     * @throws LIMSRuntimeException if database error occurs
     */
    List<DepartmentSampleType> getActiveByTestSectionId(String testSectionId) throws LIMSRuntimeException;

    /**
     * Check if a specific department-sample type association exists.
     *
     * @param testSectionId the test section (department) ID
     * @param sampleTypeId  the sample type ID
     * @return the DepartmentSampleType if exists, null otherwise
     * @throws LIMSRuntimeException if database error occurs
     */
    DepartmentSampleType getByTestSectionAndSampleType(String testSectionId, String sampleTypeId)
            throws LIMSRuntimeException;

    /**
     * Delete all sample type associations for a department.
     *
     * @param testSectionId the test section (department) ID
     * @throws LIMSRuntimeException if database error occurs
     */
    void deleteAllForTestSection(String testSectionId) throws LIMSRuntimeException;

    /**
     * Get sample type IDs for a department.
     *
     * @param testSectionId the test section (department) ID
     * @return list of sample type IDs
     * @throws LIMSRuntimeException if database error occurs
     */
    List<String> getSampleTypeIdsForTestSection(String testSectionId) throws LIMSRuntimeException;

    /**
     * Get department IDs for a sample type.
     *
     * @param sampleTypeId the sample type ID
     * @return list of test section (department) IDs
     * @throws LIMSRuntimeException if database error occurs
     */
    List<String> getTestSectionIdsForSampleType(String sampleTypeId) throws LIMSRuntimeException;
}
