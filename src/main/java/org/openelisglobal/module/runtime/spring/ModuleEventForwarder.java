package org.openelisglobal.module.runtime.spring;

import java.time.Instant;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.sample.event.SamplePatientUpdateDataCreatedEvent;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spi.event.SampleCreatedEvent;
import org.springframework.context.event.EventListener;

/**
 * Spring events never travel from a parent context to its children, so the
 * core's events reach modules only through this forwarder: it listens in the
 * root context, turns the core event into its SPI counterpart (ids only, never
 * core entities) and publishes that into every started module context, inside
 * the publishing transaction, so a module's {@code @EventListener} or
 * {@code @TransactionalEventListener} sees the same commit boundary the core
 * does.
 */
public class ModuleEventForwarder {

    private static final String LOG = "ModuleEventForwarder";

    private final ModuleContextFactory factory;

    public ModuleEventForwarder(ModuleContextFactory factory) {
        this.factory = factory;
    }

    @EventListener
    public void onSampleCreated(SamplePatientUpdateDataCreatedEvent event) {
        if (factory.contexts().isEmpty() || event.getUpdateData() == null) {
            return;
        }
        Sample sample = event.getUpdateData().getSample();
        if (sample == null || sample.getId() == null) {
            return;
        }
        forward(new SampleCreatedEvent(sample.getId(), sample.getAccessionNumber(),
                event.getUpdateData().getPatientId(), Instant.now()));
    }

    /** Publishes an SPI event to every started module. */
    public void forward(Object spiEvent) {
        for (ModuleApplicationContext context : factory.contexts().values()) {
            try {
                context.publishForwarded(spiEvent);
            } catch (RuntimeException e) {
                LogEvent.logError(LOG, "forward", "[module:" + context.getModuleId() + "] listener for "
                        + spiEvent.getClass().getSimpleName() + " failed: " + e);
                throw e;
            }
        }
    }
}
