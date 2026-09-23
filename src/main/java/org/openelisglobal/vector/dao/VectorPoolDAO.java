package org.openelisglobal.vector.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.vector.valueholder.VectorPool;

public interface VectorPoolDAO extends BaseDAO<VectorPool, Integer> {

    List<VectorPool> getBySampleId(String sampleId);

    /** Direct sub-pools only (not transitive). */
    List<VectorPool> getByParentPoolId(Integer parentPoolId);
}
