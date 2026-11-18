package org.openelisglobal.payment.service;

/** Prices an order from its tests. */
public interface OrderPricingService {

    /** Quotes the sample's active (not cancelled) tests. */
    PriceQuote quoteSample(String sampleId);
}
