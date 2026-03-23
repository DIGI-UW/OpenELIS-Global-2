package org.openelisglobal.coldstorage.service;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperRunManager;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.openelisglobal.alert.service.AlertService;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.coldstorage.service.dto.FreezerDailyLogData;
import org.openelisglobal.coldstorage.service.dto.FreezerMonthlyLogData;
import org.openelisglobal.coldstorage.service.dto.FreezerWeeklyLogData;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.coldstorage.valueholder.FreezerReading;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FreezerReportServiceImpl implements FreezerReportService {

    @Autowired
    private FreezerReadingService freezerReadingService;

    @Autowired
    private FreezerService freezerService;

    @Autowired
    private AlertService alertService;

    @Autowired
    private SiteInformationService siteInformationService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy",
            Locale.ENGLISH);
    private static final DateTimeFormatter MONTH_DAY_FORMATTER = DateTimeFormatter.ofPattern("MMM dd", Locale.ENGLISH);
    private static final WeekFields WEEK_FIELDS = WeekFields.of(Locale.getDefault());

    @Override
    public List<FreezerDailyLogData> generateDailyLogData(Long freezerId, LocalDate startDate, LocalDate endDate) {
        OffsetDateTime startDateTime = startDate.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
        OffsetDateTime endDateTime = endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toOffsetDateTime();

        List<FreezerReading> readings = new ArrayList<>();
        if (freezerId != null) {
            readings = freezerReadingService.getReadingsBetween(freezerId, startDateTime, endDateTime);
        } else {
            List<Freezer> allFreezers = freezerService.getAllFreezers("");
            for (Freezer freezer : allFreezers) {
                readings.addAll(freezerReadingService.getReadingsBetween(freezer.getId(), startDateTime, endDateTime));
            }
        }

        return readings.stream().map(this::mapToDailyLogData).collect(Collectors.toList());
    }

    @Override
    public byte[] generatePdfReport(String reportType, Long freezerId, LocalDate startDate, LocalDate endDate) {
        try {
            LogEvent.logInfo(this.getClass().getSimpleName(), "generatePdfReport",
                    "Starting PDF generation - reportType: " + reportType + ", freezerId: " + freezerId);

            String reportPath = getReportPath(reportType);
            LogEvent.logInfo(this.getClass().getSimpleName(), "generatePdfReport", "Report path: " + reportPath);

            Map<String, Object> parameters = buildReportParameters(freezerId, startDate, endDate, reportType);
            LogEvent.logInfo(this.getClass().getSimpleName(), "generatePdfReport",
                    "Parameters built: " + parameters.size() + " parameters");

            JRBeanCollectionDataSource dataSource = buildDataSource(reportType, freezerId, startDate, endDate);
            LogEvent.logInfo(this.getClass().getSimpleName(), "generatePdfReport",
                    "Data source built with " + dataSource.getRecordCount() + " records");

            File reportFile = new ClassPathResource(reportPath).getFile();
            LogEvent.logInfo(this.getClass().getSimpleName(), "generatePdfReport",
                    "Report file exists: " + reportFile.exists() + ", path: " + reportFile.getAbsolutePath());

            byte[] pdfBytes = JasperRunManager.runReportToPdf(reportFile.getAbsolutePath(), parameters, dataSource);
            LogEvent.logInfo(this.getClass().getSimpleName(), "generatePdfReport",
                    "PDF generated successfully: " + pdfBytes.length + " bytes");

            return pdfBytes;
        } catch (JRException | java.io.IOException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "generatePdfReport",
                    "Error generating report: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    @Override
    public List<FreezerWeeklyLogData> generateWeeklyLogData(Long freezerId, LocalDate startDate, LocalDate endDate) {
        // Get all daily readings first
        List<FreezerDailyLogData> dailyData = generateDailyLogData(freezerId, startDate, endDate);

        // Group by month-year and week number
        Map<String, Map<Integer, List<FreezerDailyLogData>>> groupedData = dailyData.stream()
                .collect(Collectors.groupingBy(FreezerDailyLogData::getMonthYear,
                        Collectors.groupingBy(FreezerDailyLogData::getWeekNumber)));

        List<FreezerWeeklyLogData> weeklyData = new ArrayList<>();

        for (Map.Entry<String, Map<Integer, List<FreezerDailyLogData>>> monthEntry : groupedData.entrySet()) {
            for (Map.Entry<Integer, List<FreezerDailyLogData>> weekEntry : monthEntry.getValue().entrySet()) {
                FreezerWeeklyLogData weekly = aggregateWeeklyData(weekEntry.getValue());
                weeklyData.add(weekly);
            }
        }

        // Sort by year, month, week number
        weeklyData.sort((a, b) -> {
            int yearCompare = extractYear(a.getMonthYear()).compareTo(extractYear(b.getMonthYear()));
            if (yearCompare != 0)
                return yearCompare;

            int monthCompare = extractMonth(a.getMonthYear()).compareTo(extractMonth(b.getMonthYear()));
            if (monthCompare != 0)
                return monthCompare;

            return a.getWeekNumber().compareTo(b.getWeekNumber());
        });

        return weeklyData;
    }

    @Override
    public List<FreezerMonthlyLogData> generateMonthlyLogData(Long freezerId, LocalDate startDate, LocalDate endDate) {
        // Get all daily readings first
        List<FreezerDailyLogData> dailyData = generateDailyLogData(freezerId, startDate, endDate);

        // Group by month-year
        Map<String, List<FreezerDailyLogData>> groupedData = dailyData.stream()
                .collect(Collectors.groupingBy(FreezerDailyLogData::getMonthYear));

        List<FreezerMonthlyLogData> monthlyData = new ArrayList<>();

        for (Map.Entry<String, List<FreezerDailyLogData>> entry : groupedData.entrySet()) {
            FreezerMonthlyLogData monthly = aggregateMonthlyData(entry.getValue());
            monthlyData.add(monthly);
        }

        // Sort by year, month
        monthlyData.sort((a, b) -> {
            int yearCompare = a.getYear().compareTo(b.getYear());
            if (yearCompare != 0)
                return yearCompare;
            return a.getMonth().compareTo(b.getMonth());
        });

        return monthlyData;
    }

    private FreezerWeeklyLogData aggregateWeeklyData(List<FreezerDailyLogData> dailyReadings) {
        FreezerWeeklyLogData weekly = new FreezerWeeklyLogData();

        if (dailyReadings.isEmpty()) {
            return weekly;
        }

        // Use the first reading for grouping info
        FreezerDailyLogData first = dailyReadings.get(0);
        weekly.setMonthYear(first.getMonthYear());
        weekly.setWeekNumber(first.getWeekNumber());
        weekly.setWeekPeriod(first.getWeekPeriod());
        weekly.setYear(first.getYear());
        weekly.setMonth(first.getMonth());

        // Calculate aggregates
        weekly.setReadingCount(dailyReadings.size());

        List<BigDecimal> temperatures = new ArrayList<>();
        List<BigDecimal> humidities = new ArrayList<>();
        int normalCount = 0;
        int warningCount = 0;
        int criticalCount = 0;
        int alertCount = 0;

        for (FreezerDailyLogData reading : dailyReadings) {
            if (reading.getTemperature() != null) {
                temperatures.add(reading.getTemperature());
            }
            if (reading.getHumidity() != null) {
                humidities.add(reading.getHumidity());
            }

            String status = reading.getStatus();
            if ("NORMAL".equals(status))
                normalCount++;
            else if ("WARNING".equals(status))
                warningCount++;
            else if ("CRITICAL".equals(status))
                criticalCount++;

            if (Boolean.TRUE.equals(reading.getAlertTriggered())) {
                alertCount++;
            }
        }

        // Calculate temperature statistics
        if (!temperatures.isEmpty()) {
            weekly.setAvgTemperature(average(temperatures));
            weekly.setMinTemperature(temperatures.stream().min(BigDecimal::compareTo).orElse(null));
            weekly.setMaxTemperature(temperatures.stream().max(BigDecimal::compareTo).orElse(null));
        }

        // Calculate humidity average
        if (!humidities.isEmpty()) {
            weekly.setAvgHumidity(average(humidities));
        }

        weekly.setNormalCount(normalCount);
        weekly.setWarningCount(warningCount);
        weekly.setCriticalCount(criticalCount);
        weekly.setAlertCount(alertCount);

        return weekly;
    }

    private FreezerMonthlyLogData aggregateMonthlyData(List<FreezerDailyLogData> dailyReadings) {
        FreezerMonthlyLogData monthly = new FreezerMonthlyLogData();

        if (dailyReadings.isEmpty()) {
            return monthly;
        }

        // Use the first reading for grouping info
        FreezerDailyLogData first = dailyReadings.get(0);
        monthly.setMonthYear(first.getMonthYear());
        monthly.setYear(first.getYear());
        monthly.setMonth(first.getMonth());

        // Calculate aggregates
        monthly.setReadingCount(dailyReadings.size());

        // Count unique days
        long daysMonitored = dailyReadings.stream().map(FreezerDailyLogData::getDate).distinct().count();
        monthly.setDaysMonitored((int) daysMonitored);

        List<BigDecimal> temperatures = new ArrayList<>();
        List<BigDecimal> humidities = new ArrayList<>();
        int normalCount = 0;
        int warningCount = 0;
        int criticalCount = 0;
        int alertCount = 0;

        for (FreezerDailyLogData reading : dailyReadings) {
            if (reading.getTemperature() != null) {
                temperatures.add(reading.getTemperature());
            }
            if (reading.getHumidity() != null) {
                humidities.add(reading.getHumidity());
            }

            String status = reading.getStatus();
            if ("NORMAL".equals(status))
                normalCount++;
            else if ("WARNING".equals(status))
                warningCount++;
            else if ("CRITICAL".equals(status))
                criticalCount++;

            if (Boolean.TRUE.equals(reading.getAlertTriggered())) {
                alertCount++;
            }
        }

        // Calculate temperature statistics
        if (!temperatures.isEmpty()) {
            monthly.setAvgTemperature(average(temperatures));
            monthly.setMinTemperature(temperatures.stream().min(BigDecimal::compareTo).orElse(null));
            monthly.setMaxTemperature(temperatures.stream().max(BigDecimal::compareTo).orElse(null));
        }

        // Calculate humidity average
        if (!humidities.isEmpty()) {
            monthly.setAvgHumidity(average(humidities));
        }

        monthly.setNormalCount(normalCount);
        monthly.setWarningCount(warningCount);
        monthly.setCriticalCount(criticalCount);
        monthly.setAlertCount(alertCount);

        return monthly;
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return null;
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private Integer extractYear(String monthYear) {
        // Extract year from "December 2025" format
        String[] parts = monthYear.split(" ");
        return Integer.parseInt(parts[parts.length - 1]);
    }

    private Integer extractMonth(String monthYear) {
        // Extract month number from "December 2025" format
        String monthName = monthYear.split(" ")[0];
        return java.time.Month.valueOf(monthName.toUpperCase()).getValue();
    }

    private String getReportPath(String reportType) {
        // Use single unified template for all report types
        return "reports/FreezerTemperatureMonitoringReport.jasper";
    }

    private Map<String, Object> buildReportParameters(Long freezerId, LocalDate startDate, LocalDate endDate,
            String reportType) {
        Map<String, Object> parameters = new HashMap<>();

        String facilityName = siteInformationService.getSiteInformationByName("siteNumber") != null
                ? siteInformationService.getSiteInformationByName("siteNumber").getValue()
                : "Laboratory";

        String freezerName = "All Freezers";
        if (freezerId != null) {
            Freezer freezer = freezerService.findById(freezerId).orElse(null);
            if (freezer != null) {
                freezerName = freezer.getName();
            }
        }

        // Determine report type display name
        String reportTypeDisplay = switch (reportType.toLowerCase()) {
        case "daily", "dailylog", "freezerdailylogreport" -> "Daily Log";
        case "weekly", "weeklylog" -> "Weekly Log";
        case "monthly", "monthlylog" -> "Monthly Log";
        default -> "Temperature Log";
        };

        parameters.put("reportTitle", "Temperature Monitoring Report");
        parameters.put("reportType", reportTypeDisplay);
        parameters.put("labName", facilityName);
        parameters.put("facilityName", facilityName);
        parameters.put("freezerName", freezerName);
        parameters.put("startDate", startDate.format(DATE_FORMATTER));
        parameters.put("endDate", endDate.format(DATE_FORMATTER));
        parameters.put("reportDate", LocalDate.now().format(DATE_FORMATTER));
        parameters.put("complianceFooter",
                "This report complies with CAP, CLIA, FDA, and WHO guidelines for temperature-controlled storage monitoring.");

        return parameters;
    }

    private JRBeanCollectionDataSource buildDataSource(String reportType, Long freezerId, LocalDate startDate,
            LocalDate endDate) {
        return switch (reportType.toLowerCase()) {
        case "daily", "dailylog", "freezerdailylogreport" ->
            new JRBeanCollectionDataSource(generateDailyLogData(freezerId, startDate, endDate));
        case "weekly", "weeklylog" ->
            new JRBeanCollectionDataSource(generateWeeklyLogData(freezerId, startDate, endDate));
        case "monthly", "monthlylog" ->
            new JRBeanCollectionDataSource(generateMonthlyLogData(freezerId, startDate, endDate));
        default -> throw new IllegalArgumentException("Unknown report type: " + reportType);
        };
    }

    private FreezerDailyLogData mapToDailyLogData(FreezerReading reading) {
        FreezerDailyLogData data = new FreezerDailyLogData();

        OffsetDateTime recordedAt = reading.getRecordedAt();
        LocalDate date = recordedAt.toLocalDate();

        // Full timestamp
        data.setRecordedAt(recordedAt.format(DATE_TIME_FORMATTER));

        // Date grouping fields
        data.setDate(date.format(DATE_FORMATTER));
        data.setYear(date.getYear());
        data.setMonth(date.getMonthValue());
        data.setMonthYear(date.format(MONTH_YEAR_FORMATTER));

        // Week grouping fields
        int weekOfMonth = date.get(WEEK_FIELDS.weekOfMonth());
        data.setWeekNumber(weekOfMonth);
        data.setWeekPeriod(calculateWeekPeriod(date, weekOfMonth));

        // Reading data
        data.setTemperature(reading.getTemperatureCelsius());
        data.setHumidity(reading.getHumidityPercentage());
        data.setStatus(reading.getStatus() != null ? reading.getStatus().name() : "NORMAL");
        data.setAlertTriggered(checkIfAlertExistsAtTime(reading));

        return data;
    }

    private String calculateWeekPeriod(LocalDate date, int weekOfMonth) {
        // Calculate the start and end of the week within the month
        LocalDate startOfMonth = date.withDayOfMonth(1);
        LocalDate weekStart = startOfMonth.plusWeeks(weekOfMonth - 1);
        LocalDate weekEnd = weekStart.plusDays(6);

        // Ensure week doesn't go beyond month boundaries
        if (weekEnd.getMonthValue() != date.getMonthValue()) {
            weekEnd = date.withDayOfMonth(date.lengthOfMonth());
        }

        return String.format("Week %d: %s-%s, %d", weekOfMonth, weekStart.format(MONTH_DAY_FORMATTER),
                weekEnd.format(MONTH_DAY_FORMATTER), date.getYear());
    }

    private boolean checkIfAlertExistsAtTime(FreezerReading reading) {
        if (reading.getFreezer() == null || reading.getRecordedAt() == null) {
            return false;
        }
        OffsetDateTime readingTime = reading.getRecordedAt();
        OffsetDateTime startWindow = readingTime.minusMinutes(5);
        OffsetDateTime endWindow = readingTime.plusMinutes(5);

        List<Alert> alerts = alertService.getAlertsByEntity("Freezer", reading.getFreezer().getId());
        return alerts.stream().filter(alert -> alert.getStartTime() != null).anyMatch(
                alert -> !alert.getStartTime().isBefore(startWindow) && !alert.getStartTime().isAfter(endWindow));
    }

}
