package org.openelisglobal.sample.override.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.sample.override.valueholder.OverrideType;
import org.openelisglobal.sample.override.valueholder.SampleOrderOverride;

public interface SampleOrderOverrideDAO extends BaseDAO<SampleOrderOverride, Long> {

    List<SampleOrderOverride> findBySampleId(Long sampleId);

    Optional<SampleOrderOverride> findBySampleIdAndType(Long sampleId, OverrideType overrideType);
}
