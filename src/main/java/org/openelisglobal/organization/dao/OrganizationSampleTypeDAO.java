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
package org.openelisglobal.organization.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.organization.valueholder.OrganizationSampleType;

/**
 * DAO interface for managing OrganizationSampleType associations. Provides
 * methods to query and manage the relationship between organizations and their
 * supported sample types.
 */
public interface OrganizationSampleTypeDAO extends BaseDAO<OrganizationSampleType, String> {

    /**
     * Get all sample type associations for an organization.
     *
     * @param organizationId the organization ID
     * @return list of OrganizationSampleType associations
     * @throws LIMSRuntimeException if database error occurs
     */
    List<OrganizationSampleType> getByOrganizationId(String organizationId) throws LIMSRuntimeException;

    /**
     * Get all organization associations for a sample type.
     *
     * @param sampleTypeId the sample type ID
     * @return list of OrganizationSampleType associations
     * @throws LIMSRuntimeException if database error occurs
     */
    List<OrganizationSampleType> getBySampleTypeId(String sampleTypeId) throws LIMSRuntimeException;

    /**
     * Get active sample type associations for an organization, ordered by
     * sortOrder.
     *
     * @param organizationId the organization ID
     * @return list of active OrganizationSampleType associations ordered by
     *         sortOrder
     * @throws LIMSRuntimeException if database error occurs
     */
    List<OrganizationSampleType> getActiveByOrganizationId(String organizationId) throws LIMSRuntimeException;

    /**
     * Check if a specific organization-sample type association exists.
     *
     * @param organizationId the organization ID
     * @param sampleTypeId   the sample type ID
     * @return the OrganizationSampleType if exists, null otherwise
     * @throws LIMSRuntimeException if database error occurs
     */
    OrganizationSampleType getByOrganizationAndSampleType(String organizationId, String sampleTypeId)
            throws LIMSRuntimeException;

    /**
     * Delete all sample type associations for an organization.
     *
     * @param organizationId the organization ID
     * @throws LIMSRuntimeException if database error occurs
     */
    void deleteAllForOrganization(String organizationId) throws LIMSRuntimeException;

    /**
     * Get sample type IDs for an organization.
     *
     * @param organizationId the organization ID
     * @return list of sample type IDs
     * @throws LIMSRuntimeException if database error occurs
     */
    List<String> getSampleTypeIdsForOrganization(String organizationId) throws LIMSRuntimeException;

    /**
     * Get organization IDs for a sample type.
     *
     * @param sampleTypeId the sample type ID
     * @return list of organization IDs
     * @throws LIMSRuntimeException if database error occurs
     */
    List<String> getOrganizationIdsForSampleType(String sampleTypeId) throws LIMSRuntimeException;
}
