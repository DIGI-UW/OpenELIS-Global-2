package org.openelisglobal.common.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import org.hibernate.HibernateException;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.DatabaseCleanService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.patient.util.PatientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class DeletePatientTestDataController extends BaseController {

    @Autowired
    private DatabaseCleanService databaseCleanService;
    @Autowired
    private HistoryService historyService;

    @GetMapping(value = "/DatabaseCleaningRequest")
    public String confirmCleanSamplePatientDatabaseEntries() {
        return findForward(FWD_SUCCESS);
    }

    @PostMapping(value = "/DatabaseCleaningRequest")
    public String cleanSamplePatientDatabaseEntries(HttpServletRequest request) {

        if (!"true".equals(ConfigurationProperties.getInstance()
                .getPropertyValueLowerCase(ConfigurationProperties.Property.TrainingInstallation))) {
            return findForward(FWD_FAIL_DELETE);
        }
        databaseCleanService.cleanDatabase();

        try {
            History history = new History();
            history.setActivity("T");
            history.setTimestamp(new Timestamp(System.currentTimeMillis()));

            history.setNameKey("Database");
            history.setReferenceId("0");
            history.setReferenceTable("0");
            history.setSysUserId(getSysUserId(request));
            historyService.save(history);

            PatientUtil.invalidateUnknownPatients();
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in DeletePatientTestData performAction()", e);
        }

        return findForward(FWD_SUCCESS_DELETE);
    }

    @Override
    protected String findLocalForward(String forward) {

        if (FWD_SUCCESS_DELETE.equals(forward)) {
            return "masterListsPageDefinition";
        } else if (FWD_SUCCESS.equals(forward)) {
            return "databaseCleaningDefinition";
        }
        return "redirect:/Home";
    }

    @Override
    protected String getPageTitleKey() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getPageSubtitleKey() {
        // TODO Auto-generated method stub
        return null;
    }
}
