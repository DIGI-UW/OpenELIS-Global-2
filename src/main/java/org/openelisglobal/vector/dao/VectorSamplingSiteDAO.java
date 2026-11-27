package org.openelisglobal.vector.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.vector.valueholder.VectorSamplingSite;

public interface VectorSamplingSiteDAO extends BaseDAO<VectorSamplingSite, Integer> {

    List<VectorSamplingSite> getByType(String type) throws LIMSRuntimeException;

    List<VectorSamplingSite> getActive() throws LIMSRuntimeException;

    VectorSamplingSite getByCode(String code) throws LIMSRuntimeException;

    List<VectorSamplingSite> search(String searchTerm) throws LIMSRuntimeException;

    /** The site linked to an organization (OGC-1363 FR-A3), or null. */
    VectorSamplingSite getByOrganizationId(Integer organizationId) throws LIMSRuntimeException;

    /** The sites linked to any of the organizations. */
    List<VectorSamplingSite> getByOrganizationIds(List<Integer> organizationIds) throws LIMSRuntimeException;
}
