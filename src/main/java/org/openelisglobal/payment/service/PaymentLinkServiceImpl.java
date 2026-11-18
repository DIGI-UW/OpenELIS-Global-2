package org.openelisglobal.payment.service;

import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.payment.PaymentAmounts;
import org.openelisglobal.payment.PaymentTaxSettings;
import org.openelisglobal.payment.gateway.CustomerContact;
import org.openelisglobal.payment.gateway.PaymentGatewayClient;
import org.openelisglobal.payment.gateway.PaymentGatewayDisabledException;
import org.openelisglobal.payment.gateway.PaymentGatewayProvider;
import org.openelisglobal.payment.gateway.PaymentLinkRequest;
import org.openelisglobal.payment.gateway.PaymentLinkResult;
import org.openelisglobal.payment.valueholder.Payment;
import org.openelisglobal.payment.valueholder.PaymentStatus;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Creates the gateway payment link for an order. Deliberately not
 * {@code @Transactional} as a whole: the gateway call is a network request and
 * must not hold a database transaction open. Each database step runs in its own
 * short transaction inside {@link PaymentService}.
 */
@Service
public class PaymentLinkServiceImpl implements PaymentLinkService {

    private final PaymentService paymentService;
    private final PaymentGatewayProvider gatewayProvider;
    private final OrderPricingService pricing;
    private final SampleService sampleService;
    private final PaymentContactService contactService;
    private final PaymentTaxSettings taxSettings;

    @Autowired
    public PaymentLinkServiceImpl(PaymentService paymentService, PaymentGatewayProvider gatewayProvider,
            OrderPricingService pricing, SampleService sampleService, PaymentContactService contactService,
            PaymentTaxSettings taxSettings) {
        this.paymentService = paymentService;
        this.gatewayProvider = gatewayProvider;
        this.pricing = pricing;
        this.sampleService = sampleService;
        this.contactService = contactService;
        this.taxSettings = taxSettings;
    }

    @Override
    public Payment requestLink(String sampleId, String sysUserId) {
        PaymentGatewayClient gateway = gatewayProvider.current();
        if (!gateway.isEnabled()) {
            throw new PaymentGatewayDisabledException();
        }
        Sample sample;
        try {
            // get() throws for a missing row; this method is not transactional,
            // so the exception can be turned into "not found" here.
            sample = sampleService.get(sampleId);
        } catch (ObjectNotFoundException e) {
            sample = null;
        }
        if (sample == null) {
            throw new IllegalArgumentException("Sample not found: " + sampleId);
        }

        // Never open a second link while one is waiting to be paid or is paid.
        for (Payment existing : paymentService.getPaymentsForSample(sampleId)) {
            if (existing.getStatus() == PaymentStatus.PENDING || existing.getStatus() == PaymentStatus.PAID) {
                return existing;
            }
        }

        PriceQuote quote = pricing.quoteSample(sampleId);
        if (!quote.isComplete()) {
            throw new IllegalStateException("Tests without a price: " + String.join(", ", quote.unpricedTestNames()));
        }
        if (quote.totalPaise() <= 0) {
            throw new IllegalStateException("Order has no priced tests to charge");
        }
        // The patient pays the subtotal plus GST; the subtotal and GST are kept apart.
        PaymentAmounts amounts = PaymentAmounts.of(quote.totalPaise(), taxSettings);
        // The patient's phone and email are read only when the gateway will use them.
        CustomerContact contact = gateway.notifiesCustomer() ? contactService.contactForSample(sampleId)
                : CustomerContact.NONE;

        Payment payment = new Payment();
        payment.setSample(sample);
        payment.setSubtotalPaise(amounts.subtotalPaise());
        payment.setGstPaise(amounts.gstPaise());
        payment.setAmountPaise(amounts.totalPaise());
        payment.setSysUserId(sysUserId);
        payment = paymentService.save(payment);

        PaymentLinkResult link;
        try {
            link = gateway.createPaymentLink(
                    new PaymentLinkRequest(sample.getAccessionNumber(), amounts.totalPaise(), contact));
        } catch (RuntimeException e) {
            paymentService.transition(payment.getId(), PaymentStatus.FAILED, sysUserId);
            throw e;
        }

        payment.setGatewayLinkId(link.linkId());
        payment.setShortUrl(link.shortUrl());
        payment.setSysUserId(sysUserId);
        paymentService.update(payment);
        return paymentService.transition(payment.getId(), PaymentStatus.PENDING, sysUserId);
    }
}
