package org.openelisglobal.barcode.service;

import jakarta.validation.Valid;
import org.openelisglobal.barcode.form.BarcodeConfigurationForm;

public interface BarcodeConfigService {

    void updateBarcodeInfoFromForm(@Valid BarcodeConfigurationForm form, String sysUserId);
}
