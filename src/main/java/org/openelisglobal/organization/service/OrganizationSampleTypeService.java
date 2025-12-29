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
package org.openelisglobal.organization.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.organization.valueholder.OrganizationSampleType;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;

/**
 * Service interface for managing OrganizationSampleType associations. Provides
 * methods to query and manage the relationship between organizations and their
 * supported sample types.
 */
public interface OrganizationSampleTypeService extends BaseObjectService<OrganizationSampleType, String> {

    /**
     * Get all sample type associations for an organization.
     *
     * @param organizationId the organization ID
     * @return list of OrganizationSampleType associations
     */
    List<OrganizationSampleType> getByOrganizationId(String organizationId);

    /**
     * Get all TypeOfSample entities for an organization (eagerly loaded).
     *
     * @param organizationId the organization ID
     * @return list of TypeOfSample entities
     */
    List<TypeOfSample> getSampleTypesForOrganization(String organizationId);

    /**
     * Get active TypeOfSample entities for an organization, ordered by sortOrder.
     *
     * @param organizationId the organization ID
     * @return list of active TypeOfSample entities ordered by sortOrder
     */
    List<TypeOfSample> getActiveSampleTypesForOrganization(String organizationId);

    /**
     * Link an organization with a sample type.
     *
     * @param organizationId the organization ID
     * @param sampleTypeId   the sample type ID
     */
    void linkOrganizationAndSampleType(String organizationId, String sampleTypeId);

    /**
     * Link an organization with a sample type (with sort order).
     *
     * @param organizationId the organization ID
     * @param sampleTypeId   the sample type ID
     * @param sortOrder      the sort order
     */
    void linkOrganizationAndSampleType(String organizationId, String sampleTypeId, Integer sortOrder);

    /**
     * Unlink an organization from a sample type.
     *
     * @param organizationId the organization ID
     * @param sampleTypeId   the sample type ID
     */
    void unlinkOrganizationAndSampleType(String organizationId, String sampleTypeId);

    /**
     * Delete all sample type associations for an organization.
     *
     * @param organizationId the organization ID
     */
    void deleteAllLinksForOrganization(String organizationId);

    /**
     * Get sample type IDs for an organization.
     *
     * @param organizationId the organization ID
     * @return list of sample type IDs
     */
    List<String> getSampleTypeIdsForOrganization(String organizationId);

    /**
     * Update sample type associations for an organization (replaces existing).
     *
     * @param organizationId the organization ID
     * @param sampleTypeIds  list of sample type IDs to associate
     */
    void updateSampleTypesForOrganization(String organizationId, List<String> sampleTypeIds);

    /**
     * Get all organization associations for a sample type.
     *
     * @param sampleTypeId the sample type ID
     * @return list of OrganizationSampleType associations
     */
    List<OrganizationSampleType> getBySampleTypeId(String sampleTypeId);

    /**
     * Get organization IDs for a sample type.
     *
     * @param sampleTypeId the sample type ID
     * @return list of organization IDs
     */
    List<String> getOrganizationIdsForSampleType(String sampleTypeId);

    /**
     * Get all sample types for multiple organizations (union). Useful when an
     * entity is linked to multiple organizations.
     *
     * @param organizationIds list of organization IDs
     * @return merged list of unique TypeOfSample entities from all organizations
     */
    List<TypeOfSample> getSampleTypesForOrganizations(List<String> organizationIds);
}
