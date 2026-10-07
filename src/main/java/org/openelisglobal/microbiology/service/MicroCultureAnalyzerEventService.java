package org.openelisglobal.microbiology.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzer.service.AnalyzerEventPersistenceService;
import org.openelisglobal.analyzer.service.AnalyzerEventRegistration;
import org.openelisglobal.analyzer.valueholder.AnalyzerEvent;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroCaseInoculationDAO;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MicroCultureAnalyzerEventService {

    private static final String POSITIVE_SIGNAL = MicroCaseStage.POSITIVE_SIGNAL.name();

    private final AnalyzerEventPersistenceService persistenceService;
    private final MicroCaseDAO caseDAO;
    private final MicroCaseInoculationDAO inoculationDAO;
    private final MicroCaseStateService stateService;
    private final ObjectMapper objectMapper;

    @Autowired
    public MicroCultureAnalyzerEventService(AnalyzerEventPersistenceService persistenceService, MicroCaseDAO caseDAO,
            MicroCaseInoculationDAO inoculationDAO, MicroCaseStateService stateService) {
        this(persistenceService, caseDAO, inoculationDAO, stateService, new ObjectMapper());
    }

    MicroCultureAnalyzerEventService(AnalyzerEventPersistenceService persistenceService, MicroCaseDAO caseDAO,
            MicroCaseInoculationDAO inoculationDAO, MicroCaseStateService stateService, ObjectMapper objectMapper) {
        this.persistenceService = persistenceService;
        this.caseDAO = caseDAO;
        this.inoculationDAO = inoculationDAO;
        this.stateService = stateService;
        this.objectMapper = objectMapper;
    }

    public AnalyzerEvent receive(MicroCultureAnalyzerEventCommand command, String performedBy) {
        validate(command);
        AnalyzerEventRegistration registration = persistenceService.createIfAbsent(toEvent(command));
        AnalyzerEvent event = registration.event();
        if (!registration.created() || !"RECEIVED".equals(event.getStatus())) {
            return event;
        }
        try {
            CultureTarget target = resolveTarget(command);
            MicroCase microCase = target.microCase();
            if (!MicroCaseStage.INCUBATING.name().equals(microCase.getStage())) {
                throw new IllegalArgumentException("CULTURE_ANALYZER_CASE_NOT_INCUBATING");
            }
            String note = text(command.note()).isEmpty() ? "Analyzer reported a positive culture signal"
                    : command.note().trim();
            stateService.advanceStage(microCase.getId(), MicroCaseStage.POSITIVE_SIGNAL, performedBy, note, List.of(),
                    target.sourceSampleItemId());
            persistenceService.markApplied(event, microCase.getId());
        } catch (RuntimeException exception) {
            persistenceService.markFailed(event, failureReason(exception));
        }
        return event;
    }

    private CultureTarget resolveTarget(MicroCultureAnalyzerEventCommand command) {
        if (text(command.sourceId()).isEmpty()) {
            throw new IllegalArgumentException("CULTURE_ANALYZER_SOURCE_SAMPLE_REQUIRED");
        }
        String targetCaseId = text(command.targetCaseId());
        var sources = inoculationDAO.getByContainerIdentifier(command.sourceId().trim()).stream()
                .filter(value -> targetCaseId.isEmpty() || targetCaseId.equals(value.getCaseId()))
                .map(value -> new CultureSource(value.getCaseId(), value.getSourceSampleItemId())).distinct().toList();
        if (sources.isEmpty()) {
            throw new IllegalArgumentException("CULTURE_ANALYZER_SOURCE_NOT_MATCHED");
        }
        if (sources.size() > 1) {
            throw new IllegalArgumentException("CULTURE_ANALYZER_SOURCE_AMBIGUOUS");
        }
        CultureSource source = sources.get(0);
        if (text(source.sourceSampleItemId()).isEmpty()) {
            throw new IllegalArgumentException("CULTURE_ANALYZER_SOURCE_SAMPLE_REQUIRED");
        }
        MicroCase microCase = caseDAO.get(source.caseId())
                .orElseThrow(() -> new IllegalArgumentException("CULTURE_ANALYZER_CASE_NOT_MATCHED"));
        return new CultureTarget(microCase, source.sourceSampleItemId());
    }

    private record CultureSource(String caseId, String sourceSampleItemId) {
    }

    private record CultureTarget(MicroCase microCase, String sourceSampleItemId) {
    }

    private AnalyzerEvent toEvent(MicroCultureAnalyzerEventCommand command) {
        AnalyzerEvent event = new AnalyzerEvent();
        event.setExternalEventId(command.externalEventId().trim());
        event.setEventType(POSITIVE_SIGNAL);
        event.setAnalyzerId(trimToNull(command.analyzerId()));
        event.setSourceId(trimToNull(command.sourceId()));
        event.setTargetReference(trimToNull(command.targetCaseId()));
        try {
            event.setPayload(objectMapper.writeValueAsString(Map.of("note", text(command.note()))));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("CULTURE_ANALYZER_EVENT_PAYLOAD_INVALID");
        }
        return event;
    }

    private void validate(MicroCultureAnalyzerEventCommand command) {
        if (command == null || text(command.externalEventId()).isEmpty()) {
            throw new IllegalArgumentException("CULTURE_ANALYZER_EVENT_ID_REQUIRED");
        }
        if (!POSITIVE_SIGNAL.equals(command.eventType())) {
            throw new IllegalArgumentException("CULTURE_ANALYZER_EVENT_TYPE_UNSUPPORTED");
        }
        if (text(command.targetCaseId()).isEmpty() && text(command.sourceId()).isEmpty()) {
            throw new IllegalArgumentException("CULTURE_ANALYZER_EVENT_ROUTE_REQUIRED");
        }
    }

    private String failureReason(RuntimeException exception) {
        return text(exception.getMessage()).isEmpty() ? "CULTURE_ANALYZER_EVENT_PROCESSING_FAILED"
                : exception.getMessage();
    }

    private String trimToNull(String value) {
        return text(value).isEmpty() ? null : value.trim();
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }
}
