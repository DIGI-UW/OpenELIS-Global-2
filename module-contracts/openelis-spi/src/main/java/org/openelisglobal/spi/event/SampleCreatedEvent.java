package org.openelisglobal.spi.event;

import java.time.Instant;
import java.util.Objects;

/**
 * Published by the core after a sample (an order with its specimens) has been
 * created, inside the creating transaction, and forwarded by the module runtime
 * into every started module context. A module receives it with an ordinary
 * Spring {@code @EventListener} or {@code @TransactionalEventListener} method.
 *
 * <p>
 * Only identifiers cross the boundary; a module that needs more reads it
 * through the core services it injects.
 */
public final class SampleCreatedEvent {

    private final String sampleId;
    private final String accessionNumber;
    private final String patientId;
    private final Instant createdAt;

    public SampleCreatedEvent(String sampleId, String accessionNumber, String patientId, Instant createdAt) {
        this.sampleId = Objects.requireNonNull(sampleId, "sampleId");
        this.accessionNumber = accessionNumber;
        this.patientId = patientId;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    /** The core sample id (primary key of {@code clinlims.sample}). */
    public String getSampleId() {
        return sampleId;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    /**
     * The core patient id the sample belongs to; {@code null} for a sample without
     * a patient.
     */
    public String getPatientId() {
        return patientId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "SampleCreatedEvent[sampleId=" + sampleId + ", accessionNumber=" + accessionNumber + ", patientId="
                + patientId + "]";
    }
}
