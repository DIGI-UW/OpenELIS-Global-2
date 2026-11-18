package org.openelisglobal.payment.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.sample.valueholder.Sample;

/**
 * One gateway-backed payment request for an order. Amounts are integer paise
 * (INR minor units). No card data is stored: only gateway identifiers and
 * status.
 */
@Entity
@Table(name = "payment", schema = "clinlims")
public class Payment extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 40)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_id", nullable = false)
    private Sample sample;

    @Column(name = "amount_paise", nullable = false)
    private Long amountPaise;

    /**
     * Test subtotal before GST, in paise. Null for payments made before the GST
     * split.
     */
    @Column(name = "subtotal_paise")
    private Long subtotalPaise;

    /**
     * GST on the subtotal, in paise. Null for payments made before the GST split.
     */
    @Column(name = "gst_paise")
    private Long gstPaise;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.CREATED;

    @Column(name = "gateway", nullable = false, length = 20)
    private String gateway = "RAZORPAY";

    @Column(name = "gateway_link_id", length = 100)
    private String gatewayLinkId;

    @Column(name = "gateway_payment_id", length = 100)
    private String gatewayPaymentId;

    @Column(name = "short_url", length = 255)
    private String shortUrl;

    @Column(name = "gateway_event_id", length = 100, unique = true)
    private String gatewayEventId;

    /** Gateway-independent id, assigned before the first insert. */
    @PrePersist
    void assignIdIfMissing() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public Sample getSample() {
        return sample;
    }

    public void setSample(Sample sample) {
        this.sample = sample;
    }

    public Long getSubtotalPaise() {
        return subtotalPaise;
    }

    public void setSubtotalPaise(Long subtotalPaise) {
        this.subtotalPaise = subtotalPaise;
    }

    public Long getGstPaise() {
        return gstPaise;
    }

    public void setGstPaise(Long gstPaise) {
        this.gstPaise = gstPaise;
    }

    public Long getAmountPaise() {
        return amountPaise;
    }

    public void setAmountPaise(Long amountPaise) {
        this.amountPaise = amountPaise;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getGatewayLinkId() {
        return gatewayLinkId;
    }

    public void setGatewayLinkId(String gatewayLinkId) {
        this.gatewayLinkId = gatewayLinkId;
    }

    public String getGatewayPaymentId() {
        return gatewayPaymentId;
    }

    public void setGatewayPaymentId(String gatewayPaymentId) {
        this.gatewayPaymentId = gatewayPaymentId;
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public void setShortUrl(String shortUrl) {
        this.shortUrl = shortUrl;
    }

    public String getGatewayEventId() {
        return gatewayEventId;
    }

    public void setGatewayEventId(String gatewayEventId) {
        this.gatewayEventId = gatewayEventId;
    }
}
