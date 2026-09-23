package org.openelisglobal.reports.dataexport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.openelisglobal.reports.dataexport.service.ReportingAudit;

/**
 * Captures the real logging boundary, restoring its configuration after each
 * test.
 */
final class ReportingAuditCapture extends AbstractAppender implements AutoCloseable {
    private final List<JsonNode> events = new CopyOnWriteArrayList<>();
    private final Logger logger = (Logger) LogManager.getLogger(ReportingAudit.class);
    private final Level previousLevel = logger.getLevel();
    private final boolean previousAdditivity = logger.isAdditive();

    ReportingAuditCapture() {
        super("reporting-audit-capture", null, null, false, Property.EMPTY_ARRAY);
        start();
        logger.addAppender(this);
        logger.setLevel(Level.INFO);
        logger.setAdditive(false);
    }

    @Override
    public void append(LogEvent event) {
        String message = event.getMessage().getFormattedMessage();
        if (!message.startsWith("REPORTING_AUDIT "))
            return;
        try {
            events.add(new ObjectMapper().readTree(message.substring("REPORTING_AUDIT ".length())));
        } catch (Exception error) {
            throw new AssertionError("Audit output must be one valid JSON event", error);
        }
    }

    List<JsonNode> events() {
        return List.copyOf(events);
    }

    List<String> actions() {
        return events.stream().map(event -> event.path("action").asText()).toList();
    }

    @Override
    public void close() {
        logger.removeAppender(this);
        logger.setLevel(previousLevel);
        logger.setAdditive(previousAdditivity);
        stop();
    }
}
