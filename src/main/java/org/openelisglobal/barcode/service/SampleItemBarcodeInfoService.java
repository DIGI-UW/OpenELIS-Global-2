package org.openelisglobal.barcode.service;

import org.openelisglobal.barcode.valueholder.SampleItemBarcodeInfo;
import org.openelisglobal.common.service.BaseObjectService;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_BARCODE_VIEW')")
public interface SampleItemBarcodeInfoService extends BaseObjectService<SampleItemBarcodeInfo, Integer> {

}
