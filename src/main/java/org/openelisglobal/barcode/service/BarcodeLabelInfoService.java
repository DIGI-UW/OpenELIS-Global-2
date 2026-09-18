package org.openelisglobal.barcode.service;

import org.openelisglobal.barcode.valueholder.BarcodeLabelInfo;
import org.openelisglobal.common.service.BaseObjectService;
import org.springframework.security.access.prepost.PreAuthorize;

public interface BarcodeLabelInfoService extends BaseObjectService<BarcodeLabelInfo, String> {

    @PreAuthorize("hasAuthority('PRIV_BARCODE_VIEW')")
    BarcodeLabelInfo getDataByCode(String code);
}
