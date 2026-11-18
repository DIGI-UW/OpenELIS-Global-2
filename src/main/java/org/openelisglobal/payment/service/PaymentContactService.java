package org.openelisglobal.payment.service;

import org.openelisglobal.payment.gateway.CustomerContact;

/** Reads the phone and email a payment link can be sent to. */
public interface PaymentContactService {

    /**
     * The order's patient's phone and email, cleaned up;
     * {@link CustomerContact#NONE} when the sample, patient or both values are
     * missing or invalid.
     */
    CustomerContact contactForSample(String sampleId);
}
