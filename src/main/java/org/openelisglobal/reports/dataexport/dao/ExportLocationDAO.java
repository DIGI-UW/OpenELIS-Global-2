package org.openelisglobal.reports.dataexport.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.organization.valueholder.OrganizationType;

public interface ExportLocationDAO extends BaseDAO<OrganizationType, String> {

    /**
     * The organization types found above facilities of the given type in
     * Organization Management, the level nearest the facilities first. Types named
     * in {@code excluded} are left out.
     */
    List<String> areaTypeIdsNearestFirst(String facilityTypeName, List<String> excluded);
}
