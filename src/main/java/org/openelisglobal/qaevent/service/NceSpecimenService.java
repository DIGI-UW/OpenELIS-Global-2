package org.openelisglobal.qaevent.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.qaevent.valueholder.NceSpecimen;

public interface NceSpecimenService extends BaseObjectService<NceSpecimen, Integer> {

    List<NceSpecimen> getSpecimenByNceId(Integer nceId);

    List<NceSpecimen> getSpecimenBySampleItemId(Integer sampleId);

    boolean existsByNceIdAndSampleItemId(Integer nceId, Integer sampleItemId);
}
