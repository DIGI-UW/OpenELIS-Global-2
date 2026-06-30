package org.openelisglobal.accreditation.service;

import java.util.List;
import org.openelisglobal.accreditation.valueholder.AccreditingBody;
import org.openelisglobal.common.service.BaseObjectService;
import org.springframework.web.multipart.MultipartFile;

public interface AccreditingBodyService extends BaseObjectService<AccreditingBody, Long> {

    AccreditingBody getByCode(String code);

    List<AccreditingBody> getAllActive();

    List<AccreditingBody> getAllOrderedByDisplayOrder();

    long countTestAccreditations(Long id);

    AccreditingBody updateLogoPath(Long id, String logoPath, String sysUserId);

    AccreditingBody uploadLogo(Long id, MultipartFile file, String sysUserId);

    AccreditingBody removeLogo(Long id, String sysUserId);
}
