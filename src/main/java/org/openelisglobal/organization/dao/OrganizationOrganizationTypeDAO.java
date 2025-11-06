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
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.organization.dao;

import java.util.List;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.organization.valueholder.Organization;

// should be service layer
public interface OrganizationOrganizationTypeDAO {

    public void deleteAllLinksForOrganization(String id) throws LIMSRuntimeException;

    public void linkOrganizationAndType(Organization org, String typeId) throws LIMSRuntimeException;

    public List<String> getOrganizationIdsForType(String typeId) throws LIMSRuntimeException;

    public List<String> getTypeIdsForOrganizationId(String organizationId) throws LIMSRuntimeException;
}
