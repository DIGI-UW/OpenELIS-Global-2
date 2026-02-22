package org.openelisglobal.dataexchange.externalorders.service;

import org.openelisglobal.dataexchange.externalorders.dto.ExternalOrderRequest;
import org.openelisglobal.sample.form.SamplePatientEntryForm;

public interface ExternalOrderFormMapperService {

    SamplePatientEntryForm buildForm(ExternalOrderRequest externalOrderRequest);
}
