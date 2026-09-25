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
import java.util.ArrayList;
import java.util.List;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.coldstorage.service.FreezerReadingService;
import org.openelisglobal.coldstorage.service.FreezerService;
import org.openelisglobal.coldstorage.service.dto.FreezerExcursionData;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.reports.action.implementation.reportBeans.FreezerExcursionReportData;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.spring.util.SpringContext;

/**
 * Jasper rendering of the excursions grouped by
 * FreezerReadingService#findExcursions, which the on-screen preview also reads.
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
                    addExcursions(freezer, start, end);
                } else {
                    add1LineErrorMessage("report.error.message.noPrintableItems");
                }
            } else {
                // All freezers report
                List<Freezer> allFreezers = freezerService.getAllFreezersForReporting();
                for (Freezer freezer : allFreezers) {
                    addExcursions(freezer, start, end);
                }
            }

            if (reportItems.isEmpty()) {
                add1LineErrorMessage("report.error.message.noPrintableItems");
            }

        } catch (Exception e) {
            add1LineErrorMessage("report.error.message.general");
        }
    }

    private void addExcursions(Freezer freezer, OffsetDateTime start, OffsetDateTime end) {
        for (FreezerExcursionData excursion : freezerReadingService.findExcursions(freezer, start, end)) {
            reportItems.add(toReportData(excursion));
        }
    }

    private FreezerExcursionReportData toReportData(FreezerExcursionData excursion) {
        FreezerExcursionReportData data = new FreezerExcursionReportData();

        data.setExcursionId("EXC-" + excursion.getFreezerId() + "-" + excursion.getAlertId());
        data.setFreezerId(String.valueOf(excursion.getFreezerId()));
        data.setFreezerName(excursion.getFreezerName() != null ? excursion.getFreezerName()
                : "Freezer " + excursion.getFreezerId());
        data.setLocation(excursion.getLocationName() != null ? excursion.getLocationName() : "Unknown");
        data.setStartTime(formatTimestamp(excursion.getStartTime()));
        data.setEndTime(formatTimestamp(excursion.getEndTime()));
        if (excursion.getDurationSeconds() != null) {
            data.setDuration(formatDuration(excursion.getDurationSeconds() / 60));
        }
        data.setMinTemperature(excursion.getMinTemperature());
        data.setMaxTemperature(excursion.getMaxTemperature());
        data.setTemperatureRange(formatTemperatureRange(excursion.getMinTemperature(), excursion.getMaxTemperature()));
        data.setSeverity(excursion.getSeverity());
        data.setStatus(excursion.getStatus());

        return data;
    }

    /**
     * FreezerExcursionData carries the boundary instants as ISO-8601 text, which
     * the report columns show in the site's own zone.
     */
    private String formatTimestamp(String isoTimestamp) {
        if (GenericValidator.isBlankOrNull(isoTimestamp)) {
            return null;
        }
        return OffsetDateTime.parse(isoTimestamp).atZoneSameInstant(ZoneId.systemDefault()).format(DATE_FORMATTER);
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
