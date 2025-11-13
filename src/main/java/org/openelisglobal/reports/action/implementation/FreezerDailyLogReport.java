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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.coldstorage.service.FreezerReadingService;
import org.openelisglobal.coldstorage.service.FreezerService;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.coldstorage.valueholder.FreezerReading;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.reports.action.implementation.reportBeans.FreezerReadingReportData;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.spring.util.SpringContext;

/**
 * Report implementation for daily/weekly/monthly freezer temperature logs.
 * Generates a detailed log of all temperature readings within a specified date
 * range.
 */
public class FreezerDailyLogReport extends Report implements IReportCreator {

    private FreezerReadingService freezerReadingService = SpringContext.getBean(FreezerReadingService.class);
    private FreezerService freezerService = SpringContext.getBean(FreezerService.class);

    private List<FreezerReadingReportData> reportItems;
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
            OffsetDateTime start = OffsetDateTime.parse(startDate + "T00:00:00Z");
            OffsetDateTime end = OffsetDateTime.parse(endDate + "T23:59:59Z");

            if (freezerId != null) {
                // Single freezer report
                Freezer freezer = freezerService.findById(freezerId).orElse(null);
                if (freezer != null) {
                    List<FreezerReading> readings = freezerReadingService.getReadingsBetween(freezerId, start, end);
                    processReadings(readings, freezer);
                } else {
                    add1LineErrorMessage("report.error.message.noPrintableItems");
                }
            } else {
                // All freezers report
                List<Freezer> allFreezers = freezerService.getAllFreezers("");
                for (Freezer freezer : allFreezers) {
                    List<FreezerReading> readings = freezerReadingService.getReadingsBetween(freezer.getId(), start,
                            end);
                    processReadings(readings, freezer);
                }
            }

            if (reportItems.isEmpty()) {
                add1LineErrorMessage("report.error.message.noPrintableItems");
            }

        } catch (Exception e) {
            add1LineErrorMessage("report.error.message.general");
        }
    }

    private void processReadings(List<FreezerReading> readings, Freezer freezer) {
        for (FreezerReading reading : readings) {
            FreezerReadingReportData data = new FreezerReadingReportData();

            data.setFreezerId(String.valueOf(freezer.getId()));
            data.setFreezerName(freezer.getName() != null ? freezer.getName() : "Freezer " + freezer.getId());
            data.setLocation(freezer.getRoom() != null ? freezer.getRoom() : "Unknown");

            // Format timestamp
            if (reading.getRecordedAt() != null) {
                data.setTimestamp(
                        reading.getRecordedAt().atZoneSameInstant(ZoneId.systemDefault()).format(DATE_FORMATTER));
            } else {
                data.setTimestamp("");
            }

            // Temperature data
            data.setTemperature(reading.getTemperatureCelsius());
            data.setTemperatureFormatted(formatTemperature(reading.getTemperatureCelsius()));

            // Humidity data
            data.setHumidity(reading.getHumidityPercentage());
            data.setHumidityFormatted(formatHumidity(reading.getHumidityPercentage()));

            // Status
            if (reading.getStatus() != null) {
                data.setStatus(reading.getStatus().name());
                data.setStatusSeverity(getStatusSeverity(reading.getStatus()));
            } else {
                data.setStatus("UNKNOWN");
                data.setStatusSeverity("NORMAL");
            }

            // Thresholds
            if (freezer.getCriticalThreshold() != null) {
                data.setMinThreshold(formatTemperature(freezer.getCriticalThreshold()));
            }
            if (freezer.getWarningThreshold() != null) {
                data.setMaxThreshold(formatTemperature(freezer.getWarningThreshold()));
            }

            reportItems.add(data);
        }
    }

    private String formatTemperature(BigDecimal temperature) {
        if (temperature == null) {
            return "—";
        }
        return String.format("%.1f°C", temperature.doubleValue());
    }

    private String formatHumidity(BigDecimal humidity) {
        if (humidity == null) {
            return "—";
        }
        return String.format("%.1f%%", humidity.doubleValue());
    }

    private String getStatusSeverity(FreezerReading.Status status) {
        switch (status) {
        case CRITICAL:
            return "CRITICAL";
        case WARNING:
            return "WARNING";
        case NORMAL:
        default:
            return "NORMAL";
        }
    }

    @Override
    protected void createReportParameters() {
        super.createReportParameters();

        reportParameters.put("reportTitle", MessageUtil.getMessage("report.freezer.dailylog.title"));
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
        return "FreezerDailyLogReport";
    }
}
