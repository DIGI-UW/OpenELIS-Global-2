package org.openelisglobal.sitebranding.dao;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.sitebranding.valueholder.SiteBranding;

/**
 * DAO interface for SiteBranding entity
 * 
 * Task Reference: T012
 */
public interface SiteBrandingDAO extends BaseDAO<SiteBranding, Integer> {

    /**
     * Get the single branding configuration record Since only one SiteBranding
     * record should exist, this returns the first record found or null if none
     * exists
     * 
     * @return SiteBranding entity or null if none exists
     */
    SiteBranding getBranding();
}
