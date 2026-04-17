package org.openelisglobal.label.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.label.valueholder.Label;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_BARCODE_VIEW')")
public interface LabelService extends BaseObjectService<Label, String> {
}
