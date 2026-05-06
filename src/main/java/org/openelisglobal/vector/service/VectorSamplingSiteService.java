package org.openelisglobal.vector.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.vector.valueholder.VectorSamplingSite;

public interface VectorSamplingSiteService extends BaseObjectService<VectorSamplingSite, Integer> {

    List<VectorSamplingSite> getByType(String type);

    List<VectorSamplingSite> getActive();

    VectorSamplingSite getByCode(String code);

    VectorSamplingSite patchUpdate(Integer id, VectorSamplingSite patch, String sysUserId);
}
