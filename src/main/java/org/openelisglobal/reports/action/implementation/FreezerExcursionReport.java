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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
import org.openelisglobal.reports.action.implementation.reportBeans.FreezerExcursionReportData;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.spring.util.SpringContext;

/**
 * Report implementation for freezer temperature excursions (threshold
 * violations). Groups consecutive readings that exceed thresholds into
 * excursion events.
 */
public class FreezerExcursionReport extends Report implements IReportCreator {

    private FreezerReadingService freezerReadingService = SpringContext.getBean(FreezerReadingService.class);
    private FreezerService freezerService = SpringContext.getBean(FreezerService.class);

    private List<FreezerExcursionReportData> reportItems;
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
            // Parse dates in system timezone (not UTC) to match local calendar days
            ZoneId systemZone = ZoneId.systemDefault();
            LocalDate startLocalDate = LocalDate.parse(startDate);
            LocalDate endLocalDate = LocalDate.parse(endDate);

            OffsetDateTime start = OffsetDateTime.of(startLocalDate, LocalTime.MIN,
                    systemZone.getRules().getOffset(startLocalDate.atStartOfDay()));
            OffsetDateTime end = OffsetDateTime.of(endLocalDate, LocalTime.MAX,
                    systemZone.getRules().getOffset(endLocalDate.atTime(LocalTime.MAX)));

            if (freezerId != null) {
                // Single freezer report
                Freezer freezer = freezerService.findById(freezerId).orElse(null);
                if (freezer != null) {
                    List<FreezerReading> readings = freezerReadingService.getReadingsBetween(freezerId, start, end);
                    processExcursions(readings, freezer);
                } else {
                    add1LineErrorMessage("report.error.message.noPrintableItems");
                }
            } else {
                // All freezers report
                List<Freezer> allFreezers = freezerService.getAllFreezers("");
                for (Freezer freezer : allFreezers) {
                    List<FreezerReading> readings = freezerReadingService.getReadingsBetween(freezer.getId(), start,
                            end);
                    processExcursions(readings, freezer);
                }
            }

            if (reportItems.isEmpty()) {
                add1LineErrorMessage("report.error.message.noPrintableItems");
            }

        } catch (Exception e) {
            add1LineErrorMessage("report.error.message.general");
        }
    }

    private void processExcursions(List<FreezerReading> readings, Freezer freezer) {
        if (readings.isEmpty()) {
            return;
        }

        // Group consecutive excursion readings into excursion events
        List<FreezerReading> currentExcursion = new ArrayList<>();
        FreezerReading.Status currentStatus = null;

        for (FreezerReading reading : readings) {
            // Only process WARNING and CRITICAL status readings
            if (reading.getStatus() == FreezerReading.Status.WARNING
                    || reading.getStatus() == FreezerReading.Status.CRITICAL) {

                if (currentExcursion.isEmpty() || reading.getStatus() == currentStatus) {
                    // Continue current excursion
                    currentExcursion.add(reading);
                    currentStatus = reading.getStatus();
                } else {
                    // Status changed - save current excursion and start new one
                    saveExcursion(currentExcursion, freezer);
                    currentExcursion = new ArrayList<>();
                    currentExcursion.add(reading);
                    currentStatus = reading.getStatus();
                }
            } else {
                // Normal reading - save any ongoing excursion
                if (!currentExcursion.isEmpty()) {
                    saveExcursion(currentExcursion, freezer);
                    currentExcursion = new ArrayList<>();
                    currentStatus = null;
                }
            }
        }

        // Save final excursion if any
        if (!currentExcursion.isEmpty()) {
            saveExcursion(currentExcursion, freezer);
        }
    }

    private void saveExcursion(List<FreezerReading> excursionReadings, Freezer freezer) {
        if (excursionReadings.isEmpty()) {
            return;
        }

        FreezerExcursionReportData data = new FreezerExcursionReportData();

        FreezerReading firstReading = excursionReadings.get(0);
        FreezerReading lastReading = excursionReadings.get(excursionReadings.size() - 1);

        // Generate excursion ID
        data.setExcursionId("EXC-" + freezer.getId() + "-" + firstReading.getId());

        data.setFreezerId(String.valueOf(freezer.getId()));
        data.setFreezerName(freezer.getName() != null ? freezer.getName() : "Freezer " + freezer.getId());
        data.setLocation(freezer.getRoom() != null ? freezer.getRoom() : "Unknown");

        // Timestamps
        if (firstReading.getRecordedAt() != null) {
            data.setStartTime(
                    firstReading.getRecordedAt().atZoneSameInstant(ZoneId.systemDefault()).format(DATE_FORMATTER));
        }
        if (lastReading.getRecordedAt() != null) {
            data.setEndTime(
                    lastReading.getRecordedAt().atZoneSameInstant(ZoneId.systemDefault()).format(DATE_FORMATTER));
        }

        // Calculate duration
        if (firstReading.getRecordedAt() != null && lastReading.getRecordedAt() != null) {
            long minutes = ChronoUnit.MINUTES.between(firstReading.getRecordedAt(), lastReading.getRecordedAt());
            data.setDuration(formatDuration(minutes));
        }

        // Find min/max temperatures during excursion
        BigDecimal minTemp = null;
        BigDecimal maxTemp = null;
        for (FreezerReading reading : excursionReadings) {
            if (reading.getTemperatureCelsius() != null) {
                if (minTemp == null || reading.getTemperatureCelsius().compareTo(minTemp) < 0) {
                    minTemp = reading.getTemperatureCelsius();
                }
                if (maxTemp == null || reading.getTemperatureCelsius().compareTo(maxTemp) > 0) {
                    maxTemp = reading.getTemperatureCelsius();
                }
            }
        }
        data.setMinTemperature(minTemp);
        data.setMaxTemperature(maxTemp);
        data.setTemperatureRange(formatTemperatureRange(minTemp, maxTemp));

        // Severity (use the highest severity from the excursion)
        data.setSeverity(firstReading.getStatus().name());

        // Status (for now, all excursions are considered RESOLVED after the excursion
        // ends)
        data.setStatus("RESOLVED");

        reportItems.add(data);
    }

    private String formatDuration(long minutes) {
        if (minutes < 60) {
            return minutes + " minutes";
        } else {
            long hours = minutes / 60;
            long remainingMinutes = minutes % 60;
            return hours + " hours " + remainingMinutes + " minutes";
        }
    }

    private String formatTemperatureRange(BigDecimal min, BigDecimal max) {
        if (min == null && max == null) {
            return "—";
        }
        if (min == null) {
            return String.format("Max %.1f°C", max.doubleValue());
        }
        if (max == null) {
            return String.format("Min %.1f°C", min.doubleValue());
        }
        if (min.compareTo(max) == 0) {
            return String.format("%.1f°C", min.doubleValue());
        }
        return String.format("%.1f°C to %.1f°C", min.doubleValue(), max.doubleValue());
    }

    @Override
    protected void createReportParameters() {
        super.createReportParameters();

        reportParameters.put("reportTitle", MessageUtil.getMessage("report.freezer.excursion.title"));
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
        return "FreezerExcursionReport";
    }
}
