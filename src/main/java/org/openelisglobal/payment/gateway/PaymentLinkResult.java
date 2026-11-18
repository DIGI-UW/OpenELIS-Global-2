package org.openelisglobal.payment.gateway;

/** The gateway's id for the link and the URL the patient pays at. */
public record PaymentLinkResult(String linkId, String shortUrl) {
}
