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
package org.openelisglobal.test.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.test.valueholder.DepartmentSampleType;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;

/**
 * Service interface for managing DepartmentSampleType associations. Provides
 * methods to query and manage the relationship between departments (test
 * sections) and their supported sample types.
 */
public interface DepartmentSampleTypeService extends BaseObjectService<DepartmentSampleType, String> {

    /**
     * Get all sample type associations for a department.
     *
     * @param testSectionId the test section (department) ID
     * @return list of DepartmentSampleType associations
     */
    List<DepartmentSampleType> getByTestSectionId(String testSectionId);

    /**
     * Get all TypeOfSample entities for a department (eagerly loaded).
     *
     * @param testSectionId the test section (department) ID
     * @return list of TypeOfSample entities
     */
    List<TypeOfSample> getSampleTypesForDepartment(String testSectionId);

    /**
     * Get active TypeOfSample entities for a department, ordered by sortOrder.
     *
     * @param testSectionId the test section (department) ID
     * @return list of active TypeOfSample entities ordered by sortOrder
     */
    List<TypeOfSample> getActiveSampleTypesForDepartment(String testSectionId);

    /**
     * Link a department with a sample type.
     *
     * @param testSectionId the test section (department) ID
     * @param sampleTypeId  the sample type ID
     */
    void linkDepartmentAndSampleType(String testSectionId, String sampleTypeId);

    /**
     * Link a department with a sample type (with sort order).
     *
     * @param testSectionId the test section (department) ID
     * @param sampleTypeId  the sample type ID
     * @param sortOrder     the sort order
     */
    void linkDepartmentAndSampleType(String testSectionId, String sampleTypeId, Integer sortOrder);

    /**
     * Unlink a department from a sample type.
     *
     * @param testSectionId the test section (department) ID
     * @param sampleTypeId  the sample type ID
     */
    void unlinkDepartmentAndSampleType(String testSectionId, String sampleTypeId);

    /**
     * Delete all sample type associations for a department.
     *
     * @param testSectionId the test section (department) ID
     */
    void deleteAllLinksForDepartment(String testSectionId);

    /**
     * Get sample type IDs for a department.
     *
     * @param testSectionId the test section (department) ID
     * @return list of sample type IDs
     */
    List<String> getSampleTypeIdsForDepartment(String testSectionId);

    /**
     * Update sample type associations for a department (replaces existing).
     *
     * @param testSectionId the test section (department) ID
     * @param sampleTypeIds list of sample type IDs to associate
     */
    void updateSampleTypesForDepartment(String testSectionId, List<String> sampleTypeIds);

    /**
     * Get all department associations for a sample type.
     *
     * @param sampleTypeId the sample type ID
     * @return list of DepartmentSampleType associations
     */
    List<DepartmentSampleType> getBySampleTypeId(String sampleTypeId);

    /**
     * Get department IDs for a sample type.
     *
     * @param sampleTypeId the sample type ID
     * @return list of test section (department) IDs
     */
    List<String> getTestSectionIdsForSampleType(String sampleTypeId);

    /**
     * Get all sample types for multiple departments (union). Useful when an entity
     * is linked to multiple departments.
     *
     * @param testSectionIds list of test section (department) IDs
     * @return merged list of unique TypeOfSample entities from all departments
     */
    List<TypeOfSample> getSampleTypesForDepartments(List<String> testSectionIds);
}
