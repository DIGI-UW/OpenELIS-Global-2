package org.openelisglobal.vector.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.vector.valueholder.VectorSamplingSite;

public interface VectorSamplingSiteDAO extends BaseDAO<VectorSamplingSite, Integer> {

    List<VectorSamplingSite> getByType(String type) throws LIMSRuntimeException;

    List<VectorSamplingSite> getActive() throws LIMSRuntimeException;

    VectorSamplingSite getByCode(String code) throws LIMSRuntimeException;
}
