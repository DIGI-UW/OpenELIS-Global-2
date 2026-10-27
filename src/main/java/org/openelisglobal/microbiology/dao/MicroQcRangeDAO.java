package org.openelisglobal.microbiology.dao;

import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroQcRange;

public interface MicroQcRangeDAO extends BaseDAO<MicroQcRange, String> {
    Optional<MicroQcRange> find(String qcStrain, String antibioticId, String method, String diskContent,
            String standardId);
}
