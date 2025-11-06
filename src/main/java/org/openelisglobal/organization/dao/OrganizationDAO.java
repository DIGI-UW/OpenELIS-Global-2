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
import org.openelisglobal.organization.valueholder.Organization;

/**
 * @author diane benz
 *         <p>
 *         To change this generated comment edit the template variable
 *         "typecomment": Window>Preferences>Java>Templates. To enable and
 *         disable the creation of type comments go to
 *         Window>Preferences>Java>Code Generation.
 */
public interface OrganizationDAO extends BaseDAO<Organization, String> {

    // public boolean insertData(Organization organization) throws
    // LIMSRuntimeException;

    // public void deleteData(List organizations) throws LIMSRuntimeException;

    List<Organization> getAllOrganizations() throws LIMSRuntimeException;

    List<Organization> getPageOfOrganizations(int startingRecNo) throws LIMSRuntimeException;

    void getData(Organization organization) throws LIMSRuntimeException;

    // public void updateData(Organization organization) throws
    // LIMSRuntimeException;

    List<Organization> getOrganizations(String filter) throws LIMSRuntimeException;

    Organization getActiveOrganizationByName(Organization organization, boolean ignoreCase) throws LIMSRuntimeException;

    Integer getTotalOrganizationCount() throws LIMSRuntimeException;

    Organization getOrganizationByLocalAbbreviation(Organization organization, boolean ignoreCase)
            throws LIMSRuntimeException;

    List<Organization> getPagesOfSearchedOrganizations(int startRecNo, String searchString) throws LIMSRuntimeException;

    Integer getTotalSearchedOrganizationCount(String searchString) throws LIMSRuntimeException;

    /**
     * Find all organizations which are associated with a particular project.
     *
     * @param projectName project.projectName to match
     * @return a Set of organizations.
     */
    // public Set<Organization> getOrganizationsByProjectName(String projectName);

    /**
     * Find all organzations which are of the given organization type (matching the
     * types name).
     */
    List<Organization> getOrganizationsByTypeName(String orderByProperty, String... typeName);

    List<Organization> getOrganizationsByTypeNameAndLeadingChars(String partialName, String typeName)
            throws LIMSRuntimeException;

    Organization getOrganizationById(String organizationId) throws LIMSRuntimeException;

    // public void insertOrUpdateData(Organization organization) throws
    // LIMSRuntimeException;

    List<Organization> getOrganizationsByParentId(String parentId) throws LIMSRuntimeException;

    boolean duplicateOrganizationExists(Organization organization) throws LIMSRuntimeException;

    Organization getOrganizationByName(Organization organization, boolean ignoreCase);

    Organization getOrganizationByShortName(String shortName, boolean ignoreCase);

    List<Organization> getActiveOrganizations() throws LIMSRuntimeException;

    Organization getOrganizationByFhirId(String uuid);
}
