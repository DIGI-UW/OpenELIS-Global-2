/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.reports.action.implementation;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.alert.service.AlertService;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.coldstorage.service.CorrectiveActionService;
import org.openelisglobal.coldstorage.service.FreezerService;
import org.openelisglobal.coldstorage.valueholder.CorrectiveAction;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.reports.action.implementation.reportBeans.FreezerAuditTrailReportData;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.spring.util.SpringContext;

public class FreezerAuditTrailReport extends Report implements IReportCreator {

    private AlertService alertService = SpringContext.getBean(AlertService.class);
    private CorrectiveActionService correctiveActionService = SpringContext.getBean(CorrectiveActionService.class);
    private FreezerService freezerService = SpringContext.getBean(FreezerService.class);

    private List<FreezerAuditTrailReportData> reportItems;
    private String startDate;
    private String endDate;
    private Long freezerId;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void initializeReport(ReportForm form) {
        super.initializeReport();

        // Extract parameters from form
        startDate = form.getLowerDateRange();
        endDate = form.getUpperDateRange();

        // Extract freezerId from form (reusing projectCode field per OpenELIS pattern)
        String freezerIdStr = form.getProjectCode();
        if (!GenericValidator.isBlankOrNull(freezerIdStr)) {
            try {
                freezerId = Long.parseLong(freezerIdStr);
            } catch (NumberFormatException e) {
                freezerId = null;
            }
        }

        createReportParameters();
        createReportData();
    }

    private void createReportData() {
        reportItems = new ArrayList<>();

        if (GenericValidator.isBlankOrNull(startDate) || GenericValidator.isBlankOrNull(endDate)) {
            add1LineErrorMessage("report.error.message.noPrintableItems");
            return;
        }

        try {
            if (freezerId != null) {
                // Single freezer audit trail
                Freezer freezer = freezerService.findById(freezerId).orElse(null);
                if (freezer != null) {
                    processAuditEvents(freezer);
                } else {
                    add1LineErrorMessage("report.error.message.noPrintableItems");
                }
            } else {
                // All freezers audit trail
                List<Freezer> allFreezers = freezerService.getAllFreezers("");
                for (Freezer freezer : allFreezers) {
                    processAuditEvents(freezer);
                }
            }

            // Sort by timestamp descending (most recent first)
            reportItems.sort(Comparator.comparing(FreezerAuditTrailReportData::getTimestamp).reversed());

            if (reportItems.isEmpty()) {
                add1LineErrorMessage("report.error.message.noPrintableItems");
            }

        } catch (Exception e) {
            add1LineErrorMessage("report.error.message.general");
        }
    }

    private void processAuditEvents(Freezer freezer) {
        Long freezerIdLong = freezer.getId();

        // Get all alerts for this freezer
        List<Alert> alerts = alertService.getAlertsByEntity("Freezer", freezerIdLong);

        // Process alerts
        for (Alert alert : alerts) {
            // Filter by date range if timestamps are within range
            if (alert.getStartTime() != null) {
                FreezerAuditTrailReportData data = new FreezerAuditTrailReportData();

                data.setEventId("ALERT-" + alert.getId());
                data.setTimestamp(
                        alert.getStartTime().atZoneSameInstant(ZoneId.systemDefault()).format(DATE_FORMATTER));
                data.setFreezerId(String.valueOf(freezer.getId()));
                data.setFreezerName(freezer.getName() != null ? freezer.getName() : "Freezer " + freezer.getId());
                data.setEventType("ALERT");
                data.setDescription(alert.getMessage() != null ? alert.getMessage() : "Temperature alert");
                data.setPerformedBy("System");
                data.setSeverity(alert.getSeverity() != null ? alert.getSeverity().name() : "UNKNOWN");
                data.setStatus(alert.getStatus() != null ? alert.getStatus().name() : "UNKNOWN");

                // Build details
                StringBuilder details = new StringBuilder();
                details.append("Alert Type: ").append(alert.getAlertType()).append("\n");
                if (alert.getContextData() != null) {
                    details.append("Context: ").append(alert.getContextData()).append("\n");
                }
                if (alert.getAcknowledgedAt() != null) {
                    details.append("Acknowledged: ").append(
                            alert.getAcknowledgedAt().atZoneSameInstant(ZoneId.systemDefault()).format(DATE_FORMATTER))
                            .append("\n");
                }
                if (alert.getResolvedAt() != null) {
                    details.append("Resolved: ").append(
                            alert.getResolvedAt().atZoneSameInstant(ZoneId.systemDefault()).format(DATE_FORMATTER))
                            .append("\n");
                }
                data.setDetails(details.toString());

                reportItems.add(data);
            }
        }

        // Get all corrective actions for this freezer
        List<CorrectiveAction> actions = correctiveActionService.getCorrectiveActionsByFreezerId(freezerIdLong);
        for (CorrectiveAction action : actions) {
            FreezerAuditTrailReportData actionData = new FreezerAuditTrailReportData();

            actionData.setEventId("ACTION-" + action.getId());
            if (action.getCreatedAt() != null) {
                actionData.setTimestamp(
                        action.getCreatedAt().atZoneSameInstant(ZoneId.systemDefault()).format(DATE_FORMATTER));
            }
            actionData.setFreezerId(String.valueOf(freezer.getId()));
            actionData.setFreezerName(freezer.getName() != null ? freezer.getName() : "Freezer " + freezer.getId());
            actionData.setEventType("CORRECTIVE_ACTION");
            actionData.setDescription(
                    action.getDescription() != null ? action.getDescription() : "Corrective action taken");

            // Get user name
            String performedBy = "Unknown";
            if (action.getCreatedBy() != null && action.getCreatedBy().getLoginName() != null) {
                performedBy = action.getCreatedBy().getLoginName();
            }
            actionData.setPerformedBy(performedBy);

            actionData.setStatus(action.getStatus() != null ? action.getStatus().name() : "UNKNOWN");
            actionData.setSeverity("INFO");

            // Build action details
            StringBuilder actionDetails = new StringBuilder();
            actionDetails.append("Action Type: ").append(action.getActionType()).append("\n");
            actionDetails.append("Status: ").append(action.getStatus()).append("\n");
            if (action.getCompletedAt() != null) {
                actionDetails.append("Completed: ").append(
                        action.getCompletedAt().atZoneSameInstant(ZoneId.systemDefault()).format(DATE_FORMATTER))
                        .append("\n");
            }
            if (action.getCompletionNotes() != null) {
                actionDetails.append("Notes: ").append(action.getCompletionNotes()).append("\n");
            }
            actionData.setDetails(actionDetails.toString());

            reportItems.add(actionData);
        }
    }

    @Override
    protected void createReportParameters() {
        super.createReportParameters();

        reportParameters.put("reportTitle", MessageUtil.getMessage("report.freezer.audittrail.title"));
        reportParameters.put("startDate", startDate != null ? startDate : "");
        reportParameters.put("endDate", endDate != null ? endDate : "");
        reportParameters.put("labName", ConfigurationProperties.getInstance().getPropertyValue(Property.SiteName));
        reportParameters.put("complianceFooter",
                "CAP (College of American Pathologists), CLIA (Clinical Laboratory Improvement Amendments), "
                        + "FDA (Food and Drug Administration), and WHO (World Health Organization) compliant");
    }

    @Override
    public JRDataSource getReportDataSource() throws IllegalStateException {
        if (!initialized) {
            throw new IllegalStateException("initializeReport not called first");
        }
        return errorFound ? new JRBeanCollectionDataSource(errorMsgs) : new JRBeanCollectionDataSource(reportItems);
    }

    @Override
    protected String reportFileName() {
        return "FreezerAuditTrailReport";
    }
}
