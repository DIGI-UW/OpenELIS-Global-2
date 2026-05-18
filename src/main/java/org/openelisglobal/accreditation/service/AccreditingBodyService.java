package org.openelisglobal.accreditation.service;

import java.util.List;
import org.openelisglobal.accreditation.valueholder.AccreditingBody;
import org.openelisglobal.common.service.BaseObjectService;

public interface AccreditingBodyService extends BaseObjectService<AccreditingBody, Long> {

    AccreditingBody getByCode(String code);

    List<AccreditingBody> getAllActive();

    List<AccreditingBody> getAllOrderedByDisplayOrder();

    long countTestAccreditations(Long id);

    AccreditingBody updateLogoPath(Long id, String logoPath, String sysUserId);
}
