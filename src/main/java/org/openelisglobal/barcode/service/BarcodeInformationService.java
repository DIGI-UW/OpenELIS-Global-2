package org.openelisglobal.barcode.service;

import jakarta.validation.Valid;
import org.openelisglobal.barcode.form.BarcodeConfigurationForm;

public interface BarcodeInformationService {

    void updateBarcodeInfoFromForm(@Valid BarcodeConfigurationForm form, String sysUserId);
}
