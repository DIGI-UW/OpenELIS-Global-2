package org.openelisglobal.barcode.service;

import jakarta.validation.Valid;
import org.openelisglobal.barcode.form.BarcodeConfigurationForm;
import org.springframework.security.access.prepost.PreAuthorize;

public interface BarcodeConfigService {

    @PreAuthorize("hasAuthority('PRIV_BARCODE_MANAGE')")
    void updateBarcodeInfoFromForm(@Valid BarcodeConfigurationForm form, String sysUserId);
}
