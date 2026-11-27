package org.openelisglobal.program.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.openelisglobal.alert.service.AlertService;
import org.openelisglobal.alert.valueholder.AlertSeverity;
import org.openelisglobal.alert.valueholder.AlertType;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.program.event.CriticalResultEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Records an alert for a case signed out with a critical result, after the
 * sign-out has committed. The alert never undoes the sign-out: a failure here
 * is logged and goes no further. The message holds the lab number and nothing
 * of the diagnosis, because the alerts list is not scoped to a laboratory unit.
 */
@Component
public class CriticalResultAlertListener {

    private static final String ENTITY_TYPE = "PATHOLOGY_SAMPLE";
    private static final ObjectMapper CONTEXT_MAPPER = new ObjectMapper();

    @Autowired
    private AlertService alertService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCriticalResult(CriticalResultEvent event) {
        try {
            Map<String, Object> context = new LinkedHashMap<>();
            context.put("accessionNumber", event.getAccessionNumber());
            context.put("pathologySampleId", event.getCaseId());
            alertService.createAlert(AlertType.CRITICAL_RESULT, ENTITY_TYPE, Long.valueOf(event.getCaseId()),
                    AlertSeverity.CRITICAL, MessageUtil.getMessage("pathology.alert.criticalResult",
                            new Object[] { event.getAccessionNumber() }),
                    CONTEXT_MAPPER.writeValueAsString(context));
        } catch (Exception e) {
            LogEvent.logError(e);
        }
    }
}
