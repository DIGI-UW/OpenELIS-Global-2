package org.openelisglobal.common.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import org.hibernate.HibernateException;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.DatabaseCleanService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.patient.util.PatientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest")
public class DatabaseCleaningRestController {

    @Autowired
    private DatabaseCleanService databaseCleanService;
    @Autowired
    private HistoryService historyService;

    @GetMapping(value = "/database-cleaning/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getStatus() {
        boolean isTrainingInstallation = "true".equals(ConfigurationProperties.getInstance()
                .getPropertyValueLowerCase(ConfigurationProperties.Property.TrainingInstallation));

        return ResponseEntity.ok(new StatusResponse(isTrainingInstallation));
    }

    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    @PostMapping(value = "/database-cleaning", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> cleanDatabase(HttpServletRequest request) {

        if (!"true".equals(ConfigurationProperties.getInstance()
                .getPropertyValueLowerCase(ConfigurationProperties.Property.TrainingInstallation))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Database cleaning is only allowed on training installations"));
        }

        try {
            databaseCleanService.cleanDatabase();

            History history = new History();
            history.setActivity("T");
            history.setTimestamp(new Timestamp(System.currentTimeMillis()));
            history.setNameKey("Database");
            history.setReferenceId("0");
            history.setReferenceTable("0");
            history.setSysUserId(ControllerUtills.getSysUserId(request));
            historyService.save(history);

            PatientUtil.invalidateUnknownPatients();

            return ResponseEntity.ok(new SuccessResponse("Database cleaned successfully"));
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error cleaning database", e);
        }
    }

    private static class StatusResponse {
        public final boolean trainingInstallation;

        public StatusResponse(boolean trainingInstallation) {
            this.trainingInstallation = trainingInstallation;
        }

        public boolean isTrainingInstallation() {
            return trainingInstallation;
        }
    }

    private static class SuccessResponse {
        public final String message;

        public SuccessResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    private static class ErrorResponse {
        public final String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }
    }
}
