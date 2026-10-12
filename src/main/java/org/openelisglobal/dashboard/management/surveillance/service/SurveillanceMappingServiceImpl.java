package org.openelisglobal.dashboard.management.surveillance.service;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.dashboard.management.surveillance.dao.SurveillanceProgramDAO;
import org.openelisglobal.dashboard.management.surveillance.dao.TestSurveillanceMappingDAO;
import org.openelisglobal.dashboard.management.surveillance.service.SurveillanceImportResult.SkippedRow;
import org.openelisglobal.dashboard.management.surveillance.valueholder.SurveillanceProgram;
import org.openelisglobal.dashboard.management.surveillance.valueholder.TestSurveillanceMapping;
import org.openelisglobal.dashboard.management.surveillance.valueholder.TestSurveillanceMapping.MatchMode;
import org.openelisglobal.reports.dataexport.service.ReportingAudit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SurveillanceMappingServiceImpl extends AuditableBaseObjectServiceImpl<TestSurveillanceMapping, Long>
        implements SurveillanceMappingService {

    static final List<String> COLUMNS = List.of("program_code", "test", "metric_label", "metric_key", "match_mode",
            "positive_result_codes", "denominator_result_codes", "numeric_threshold", "display_order");

    private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true).build();
    private static final Pattern PROGRAM_CODE = Pattern.compile("[A-Z][A-Z0-9_]{0,31}");
    private static final Pattern METRIC_KEY = Pattern.compile("[A-Z][A-Z0-9_]{0,63}");
    private static final int CODES_MAX = 512;

    private enum Outcome {
        ADDED, UPDATED, UNCHANGED
    }

    private static final class RowRejected extends RuntimeException {
        private static final long serialVersionUID = 1L;

        RowRejected(String reason) {
            super(reason, null, false, false);
        }
    }

    @Autowired
    protected TestSurveillanceMappingDAO baseObjectDAO;

    @Autowired
    private SurveillanceProgramDAO programDAO;

    public SurveillanceMappingServiceImpl() {
        super(TestSurveillanceMapping.class);
        this.auditTrailLog = true;
    }

    @Override
    protected TestSurveillanceMappingDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SurveillanceMappingView> getAllViews() {
        return baseObjectDAO.getAllViews();
    }

    @Override
    @Transactional
    public SurveillanceImportResult importCsv(Reader csv, String sourceFile, String sysUserId) {
        Map<String, SurveillanceProgram> programs = programDAO.getAll().stream()
                .collect(Collectors.toMap(SurveillanceProgram::getCode, p -> p));
        Map<String, Integer> firstRowByKey = new HashMap<>();
        List<SkippedRow> skipped = new ArrayList<>();
        int added = 0;
        int updated = 0;
        try (CSVParser parser = CSVParser.parse(csv, FORMAT)) {
            List<String> missing = COLUMNS.stream().filter(c -> !parser.getHeaderMap().containsKey(c)).toList();
            if (!missing.isEmpty()) {
                throw new IllegalArgumentException("Missing columns: " + String.join(", ", missing));
            }
            for (CSVRecord record : parser) {
                int row = (int) record.getRecordNumber() + 1;
                try {
                    List<TestSurveillanceMapping> mappings = parse(record, programs, sysUserId);
                    for (TestSurveillanceMapping m : mappings) {
                        Integer first = firstRowByKey.get(key(m));
                        reject(first != null, "Repeats row " + first);
                    }
                    for (TestSurveillanceMapping m : mappings) {
                        firstRowByKey.put(key(m), row);
                        switch (upsert(m, sysUserId)) {
                        case ADDED -> added++;
                        case UPDATED -> updated++;
                        case UNCHANGED -> {
                        }
                        }
                    }
                } catch (RowRejected e) {
                    skipped.add(new SkippedRow(row, e.getMessage()));
                }
            }
        } catch (IOException | UncheckedIOException e) {
            throw new IllegalArgumentException("Unreadable CSV: " + e.getMessage(), e);
        }
        ReportingAudit.surveillanceMappingsImported(sysUserId, added, updated, skipped.size(), sourceFile);
        return new SurveillanceImportResult(added, updated, skipped);
    }

    private List<TestSurveillanceMapping> parse(CSVRecord record, Map<String, SurveillanceProgram> programs,
            String sysUserId) {
        reject(!record.isConsistent(),
                "Expected " + record.getParser().getHeaderNames().size() + " columns, found " + record.size());
        String programCode = value(record, "program_code").toUpperCase(Locale.ROOT);
        String programName = record.isMapped("program_name") ? value(record, "program_name") : "";
        SurveillanceProgram program = programs.get(programCode);
        reject(program == null && programName.isEmpty(), "Unknown program: " + programCode);
        reject(program == null && !PROGRAM_CODE.matcher(programCode).matches(),
                "program_code must be upper-case letters, digits and underscores, at most 32: " + programCode);
        reject(program == null && programName.length() > 128, "program_name is at most 128 characters");
        String test = value(record, "test");
        reject(test.isEmpty(), "test is required");
        String label = value(record, "metric_label");
        reject(label.isEmpty() || label.length() > 128, "metric_label is required, at most 128 characters");
        String metricKey = value(record, "metric_key");
        reject(!METRIC_KEY.matcher(metricKey).matches(),
                "metric_key must be upper-case letters, digits and underscores, at most 64: " + metricKey);
        MatchMode mode = matchMode(value(record, "match_mode"));
        String positive = codes(value(record, "positive_result_codes"), "positive_result_codes");
        reject(mode == MatchMode.SPECIFIC_CODES && positive == null, "SPECIFIC_CODES needs positive_result_codes");
        reject(mode != MatchMode.SPECIFIC_CODES && positive != null,
                "positive_result_codes apply only to SPECIFIC_CODES");
        boolean numeric = mode == MatchMode.NUMERIC_BELOW || mode == MatchMode.NUMERIC_ABOVE;
        String thresholdText = value(record, "numeric_threshold");
        reject(numeric && thresholdText.isEmpty(), mode + " needs numeric_threshold");
        reject(!numeric && !thresholdText.isEmpty(),
                "numeric_threshold applies only to NUMERIC_BELOW and NUMERIC_ABOVE");
        BigDecimal threshold = numeric ? decimal(thresholdText) : null;
        String denominator = codes(value(record, "denominator_result_codes"), "denominator_result_codes");
        int displayOrder = displayOrder(value(record, "display_order"));

        List<String> testIds = baseObjectDAO.getActiveTestIds(test);
        reject(testIds.isEmpty(), "Unknown test: " + test);
        Long programId = (program != null ? program : createProgram(programCode, programName, programs, sysUserId))
                .getId();
        return testIds.stream().map(testId -> {
            TestSurveillanceMapping m = new TestSurveillanceMapping();
            m.setProgramId(programId);
            m.setTestId(testId);
            m.setMetricLabel(label);
            m.setMetricKey(metricKey);
            m.setMatchMode(mode);
            m.setPositiveResultCodes(positive);
            m.setDenominatorResultCodes(denominator);
            m.setNumericThreshold(threshold);
            m.setDisplayOrder(displayOrder);
            m.setActive(true);
            return m;
        }).toList();
    }

    private SurveillanceProgram createProgram(String code, String name, Map<String, SurveillanceProgram> programs,
            String sysUserId) {
        SurveillanceProgram program = new SurveillanceProgram();
        program.setCode(code);
        program.setDisplayName(name);
        program.setDisplayOrder(
                programs.values().stream().mapToInt(SurveillanceProgram::getDisplayOrder).max().orElse(0) + 1);
        program.setActive(true);
        program.setSysUserId(sysUserId);
        programDAO.insert(program);
        auditTrailService.saveNewHistory(program, sysUserId, programDAO.getTableName());
        programs.put(code, program);
        return program;
    }

    private Outcome upsert(TestSurveillanceMapping incoming, String sysUserId) {
        incoming.setSysUserId(sysUserId);
        TestSurveillanceMapping existing = baseObjectDAO.find(incoming.getProgramId(), incoming.getTestId(),
                incoming.getMetricKey());
        if (existing == null) {
            insert(incoming);
            return Outcome.ADDED;
        }
        if (definition(existing).equals(definition(incoming))) {
            return Outcome.UNCHANGED;
        }
        incoming.setId(existing.getId());
        incoming.setLastupdated(existing.getLastupdated());
        update(incoming);
        return Outcome.UPDATED;
    }

    private static List<Object> definition(TestSurveillanceMapping m) {
        BigDecimal threshold = m.getNumericThreshold();
        return Arrays.asList(m.getMetricLabel(), m.getMatchMode(), m.getPositiveResultCodes(),
                m.getDenominatorResultCodes(), threshold == null ? null : threshold.stripTrailingZeros(),
                m.getDisplayOrder(), m.getActive());
    }

    private static String key(TestSurveillanceMapping m) {
        return m.getProgramId() + "|" + m.getTestId() + "|" + m.getMetricKey();
    }

    private static String value(CSVRecord record, String column) {
        return record.get(column).trim();
    }

    private static MatchMode matchMode(String text) {
        try {
            return MatchMode.valueOf(text.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new RowRejected("Unknown match_mode: " + text);
        }
    }

    private static String codes(String text, String column) {
        String joined = Arrays.stream(text.split(",")).map(String::trim).filter(c -> !c.isEmpty())
                .collect(Collectors.joining(","));
        reject(joined.length() > CODES_MAX, column + " is longer than " + CODES_MAX + " characters");
        return joined.isEmpty() ? null : joined;
    }

    private static BigDecimal decimal(String text) {
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            throw new RowRejected("numeric_threshold is not a number: " + text);
        }
    }

    private static int displayOrder(String text) {
        if (text.isEmpty()) {
            return 0;
        }
        try {
            int order = Integer.parseInt(text);
            reject(order < 0, "display_order must not be negative");
            return order;
        } catch (NumberFormatException e) {
            throw new RowRejected("display_order is not a whole number: " + text);
        }
    }

    private static void reject(boolean condition, String reason) {
        if (condition) {
            throw new RowRejected(reason);
        }
    }
}
