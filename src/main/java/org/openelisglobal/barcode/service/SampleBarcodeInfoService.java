package org.openelisglobal.barcode.service;

import org.openelisglobal.barcode.valueholder.SampleBarcodeInfo;
import org.openelisglobal.common.service.BaseObjectService;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_BARCODE_VIEW')")
public interface SampleBarcodeInfoService extends BaseObjectService<SampleBarcodeInfo, Integer> {

}
