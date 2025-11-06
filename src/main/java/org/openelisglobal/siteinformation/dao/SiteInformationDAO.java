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
package org.openelisglobal.siteinformation.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;

public interface SiteInformationDAO extends BaseDAO<SiteInformation, String> {

    // public boolean insertData(SiteInformation siteInformation) throws
    // LIMSRuntimeException;

    // public void deleteData(String siteInformationId, String currentUserId) throws
    // LIMSRuntimeException;

    public List<SiteInformation> getAllSiteInformation() throws LIMSRuntimeException;

    public void getData(SiteInformation siteInformation) throws LIMSRuntimeException;

    // public void updateData(SiteInformation siteInformation) throws
    // LIMSRuntimeException;

    public SiteInformation getSiteInformationByName(String siteName) throws LIMSRuntimeException;

    public int getCountForDomainName(String domainName) throws LIMSRuntimeException;

    public List<SiteInformation> getPageOfSiteInformationByDomainName(int startingRecNo, String domainName)
            throws LIMSRuntimeException;

    public SiteInformation getSiteInformationById(String urlId) throws LIMSRuntimeException;

    public List<SiteInformation> getSiteInformationByDomainName(String domainName) throws LIMSRuntimeException;
}
