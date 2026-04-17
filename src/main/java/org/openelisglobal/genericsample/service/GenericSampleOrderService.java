package org.openelisglobal.genericsample.service;

import java.io.InputStream;
import java.util.Map;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.genericsample.form.GenericSampleImportResult;
import org.openelisglobal.genericsample.form.GenericSampleOrderForm;
import org.springframework.security.access.prepost.PreAuthorize;

public interface GenericSampleOrderService {
    @PreAuthorize("hasAuthority('PRIV_ORDER_CREATE')")
    Map<String, Object> saveGenericSampleOrder(GenericSampleOrderForm form, String sysUserId)
            throws FhirLocalPersistingException;

    @PreAuthorize("hasAuthority('PRIV_ORDER_CREATE')")
    Map<String, Object> saveGenericSampleOrderInternal(GenericSampleOrderForm form, String sysUserId)
            throws FhirLocalPersistingException;

    @PreAuthorize("hasAuthority('PRIV_ORDER_CREATE')")
    GenericSampleOrderForm getGenericSampleOrderByAccessionNumber(String accessionNumber);

    @PreAuthorize("hasAuthority('PRIV_ORDER_CREATE')")
    Map<String, Object> updateGenericSampleOrder(String accessionNumber, GenericSampleOrderForm form, String sysUserId);

    @PreAuthorize("hasAuthority('PRIV_ORDER_CREATE')")
    GenericSampleImportResult validateImportFile(InputStream inputStream, String fileName, String contentType);

    @PreAuthorize("hasAuthority('PRIV_ORDER_CREATE')")
    Map<String, Object> importSamplesFromFile(InputStream inputStream, String fileName, String contentType,
            String sysUserId);
}
