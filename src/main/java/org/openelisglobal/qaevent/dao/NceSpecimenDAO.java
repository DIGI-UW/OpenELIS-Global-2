package org.openelisglobal.qaevent.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qaevent.valueholder.NceSpecimen;

public interface NceSpecimenDAO extends BaseDAO<NceSpecimen, Integer> {

    List<NceSpecimen> getSpecimenByNceId(Integer nceId) throws LIMSRuntimeException;

    List<NceSpecimen> getSpecimenBySampleId(Integer sampleId);

    boolean existsByNceIdAndSampleItemId(Integer nceId, Integer sampleItemId);
}
