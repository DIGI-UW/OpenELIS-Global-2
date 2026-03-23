package org.openelisglobal.genericsample.service;

import java.io.InputStream;
import java.util.Map;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.genericsample.form.GenericSampleImportResult;
import org.openelisglobal.genericsample.form.GenericSampleOrderForm;

public interface GenericSampleOrderService {
    Map<String, Object> saveGenericSampleOrder(GenericSampleOrderForm form, String sysUserId)
            throws FhirLocalPersistingException;

    Map<String, Object> saveGenericSampleOrderInternal(GenericSampleOrderForm form, String sysUserId)
            throws FhirLocalPersistingException;

    GenericSampleOrderForm getGenericSampleOrderByAccessionNumber(String accessionNumber);

    Map<String, Object> updateGenericSampleOrder(String accessionNumber, GenericSampleOrderForm form, String sysUserId);

    GenericSampleImportResult validateImportFile(InputStream inputStream, String fileName, String contentType);

    Map<String, Object> importSamplesFromFile(InputStream inputStream, String fileName, String contentType,
            String sysUserId);
}
