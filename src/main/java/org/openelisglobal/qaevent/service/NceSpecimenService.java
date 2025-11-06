package org.openelisglobal.qaevent.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.qaevent.valueholder.NceSpecimen;

public interface NceSpecimenService extends BaseObjectService<NceSpecimen, String> {

    List<NceSpecimen> getSpecimenByNceId(String nceId);

    List<NceSpecimen> getSpecimenBySampleItemId(String sampleId);
}
