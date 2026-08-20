package org.openelisglobal.microbiology.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.openelisglobal.analyzer.service.AnalyzerEventPersistenceService;
import org.openelisglobal.analyzer.valueholder.AnalyzerEvent;
import org.openelisglobal.microbiology.dao.MicroAstRunDAO;
import org.openelisglobal.microbiology.form.MicroAstAnalyzerResultRequestForm;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MicroAstAnalyzerEventService {

    private static final String RESULT_AVAILABLE = "AST_RESULT_AVAILABLE";
    private static final String QC_FAIL = "AST_QC_FAIL";

    private final AnalyzerEventPersistenceService persistenceService;
    private final MicroAstRunDAO runDAO;
    private final MicroAstService astService;
    private final ObjectMapper objectMapper;

    @Autowired
    public MicroAstAnalyzerEventService(AnalyzerEventPersistenceService persistenceService, MicroAstRunDAO runDAO,
            MicroAstService astService) {
        this(persistenceService, runDAO, astService, new ObjectMapper());
    }

    MicroAstAnalyzerEventService(AnalyzerEventPersistenceService persistenceService, MicroAstRunDAO runDAO,
            MicroAstService astService, ObjectMapper objectMapper) {
        this.persistenceService = persistenceService;
        this.runDAO = runDAO;
        this.astService = astService;
        this.objectMapper = objectMapper;
    }

    public AnalyzerEvent receive(MicroAstAnalyzerEventCommand command, String performedBy) {
        validate(command);
        AnalyzerEvent event = persistenceService.createIfAbsent(toEvent(command));
        if (!"RECEIVED".equals(event.getStatus())) {
            return event;
        }
        try {
            MicroAstRun run = resolveRun(command)
                    .orElseThrow(() -> new MicroAstConflictException("AST_ANALYZER_RUN_NOT_MATCHED"));
            if (QC_FAIL.equals(command.eventType())) {
                astService.recordAnalyzerQcFailure(run.getId(), command.payload().instrumentQcReference,
                        command.payload().analyzerMessageCodes, command.externalEventId(), performedBy);
            } else {
                astService.applyAnalyzerResults(toBatch(run.getId(), command), performedBy);
            }
            persistenceService.markApplied(event, run.getId());
        } catch (RuntimeException exception) {
            persistenceService.markFailed(event, failureReason(exception));
        }
        return event;
    }

    private Optional<MicroAstRun> resolveRun(MicroAstAnalyzerEventCommand command) {
        if (command.targetRunId() != null && !command.targetRunId().isBlank()) {
            return runDAO.get(command.targetRunId());
        }
        return runDAO.getByAnalyzerAndCard(command.analyzerId(), command.sourceId());
    }

    private MicroAstAnalyzerResultBatch toBatch(String runId, MicroAstAnalyzerEventCommand command) {
        MicroAstAnalyzerResultRequestForm request = command.payload();
        return new MicroAstAnalyzerResultBatch(runId, command.externalEventId(), command.analyzerId(),
                command.sourceId(), request.analyzerSoftwareVersion, request.analyzerOrganismId,
                request.analyzerOrganismName, request.analyzerOrganismConfidence, request.analyzerExpertFlags,
                request.instrumentQcReference, request.qcPassed, request.loadedAt, request.completedAt,
                request.analyzerMessageCodes,
                request.readings.stream()
                        .map(reading -> new MicroAstAnalyzerReading(reading.antibioticId, reading.rawValue,
                                reading.units, reading.instrumentInterpretation, reading.analyzerResultReference))
                        .toList());
    }

    private AnalyzerEvent toEvent(MicroAstAnalyzerEventCommand command) {
        AnalyzerEvent event = new AnalyzerEvent();
        event.setExternalEventId(command.externalEventId().trim());
        event.setEventType(command.eventType().trim());
        event.setAnalyzerId(trimToNull(command.analyzerId()));
        event.setSourceId(trimToNull(command.sourceId()));
        event.setTargetReference(trimToNull(command.targetRunId()));
        try {
            event.setPayload(objectMapper.writeValueAsString(command.payload()));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("AST_ANALYZER_EVENT_PAYLOAD_INVALID");
        }
        return event;
    }

    private void validate(MicroAstAnalyzerEventCommand command) {
        if (command == null || command.externalEventId() == null || command.externalEventId().isBlank()) {
            throw new IllegalArgumentException("AST_ANALYZER_EVENT_ID_REQUIRED");
        }
        if (!RESULT_AVAILABLE.equals(command.eventType()) && !QC_FAIL.equals(command.eventType())) {
            throw new IllegalArgumentException("AST_ANALYZER_EVENT_TYPE_UNSUPPORTED");
        }
        if (command.payload() == null) {
            throw new IllegalArgumentException("AST_ANALYZER_EVENT_PAYLOAD_REQUIRED");
        }
        boolean hasTarget = command.targetRunId() != null && !command.targetRunId().isBlank();
        if (!hasTarget && (command.analyzerId() == null || command.analyzerId().isBlank() || command.sourceId() == null
                || command.sourceId().isBlank())) {
            throw new IllegalArgumentException("AST_ANALYZER_EVENT_ROUTE_REQUIRED");
        }
    }

    private String failureReason(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "AST_ANALYZER_EVENT_PROCESSING_FAILED" : message;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
