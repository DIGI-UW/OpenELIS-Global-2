package org.openelisglobal.reports.action.implementation;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.openelisglobal.analyzer.service.AnalyzerExperimentService;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.context.i18n.LocaleContextHolder;

public class MauritiusProtocolSheet extends Report implements IReportCreator {

    private static int ACCESSION_PRINT_MAX_LENGTH = 5;

    private List<TwelveWellRow> wellRows;
    private String title;
    private Instant date;
    private AnalyzerExperimentService analyzerExperimentService = SpringContext
            .getBean(AnalyzerExperimentService.class);

    @Override
    public void initializeReport(ReportForm form) {
        super.initializeReport();
        form.setSelectList(new ReportSpecificationList());
        Map<String, String> analyzerExpermientValues;
        try {
            analyzerExpermientValues = analyzerExperimentService.getWellValuesForId(form.getExperimentId());
        } catch (IOException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException(e);
        }

        wellRows = new ArrayList<>();
        for (Entry<String, String> idValue : analyzerExpermientValues.entrySet()) {
            addValueToRows(idValue);
        }
        wellRows.sort((TwelveWellRow r1, TwelveWellRow r2) -> r1.rowLabel.compareTo(r2.rowLabel));
        title = form.getReportName();
        date = Instant.now();
        createReportParameters();
    }

    private void addValueToRows(Entry<String, String> idValue) {
        TwelveWellRow twelveWellRow = new TwelveWellRow();

        Pattern pattern = Pattern.compile("([A-Z]+)(\\d+)");
        Matcher matcher = pattern.matcher(idValue.getKey());
        matcher.find();
        String alpha = matcher.group(1);
        boolean valueFound = false;
        for (TwelveWellRow row : wellRows) {
            if (alpha.equals(row.getRowLabel())) {
                twelveWellRow = row;
                valueFound = true;
                break;
            }
        }
        if (!valueFound) {
            wellRows.add(twelveWellRow);
        }
        twelveWellRow.setRowLabel(alpha);
        String num = matcher.group(2);
        String printValue = idValue == null ? ""
                : idValue.getValue().substring(Math.max(0, idValue.getValue().length() - ACCESSION_PRINT_MAX_LENGTH));
        switch (num) {
        case "1":
            twelveWellRow.setValue1(printValue);
            break;
        case "2":
            twelveWellRow.setValue2(printValue);
            break;
        case "3":
            twelveWellRow.setValue3(printValue);
            break;
        case "4":
            twelveWellRow.setValue4(printValue);
            break;
        case "5":
            twelveWellRow.setValue5(printValue);
            break;
        case "6":
            twelveWellRow.setValue6(printValue);
            break;
        case "7":
            twelveWellRow.setValue7(printValue);
            break;
        case "8":
            twelveWellRow.setValue8(printValue);
            break;
        case "9":
            twelveWellRow.setValue9(printValue);
            break;
        case "10":
            twelveWellRow.setValue10(printValue);
            break;
        case "11":
            twelveWellRow.setValue11(printValue);
            break;
        case "12":
            twelveWellRow.setValue12(printValue);
            break;
        }
    }

    @Override
    protected void createReportParameters() {
        super.createReportParameters();
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                .withZone(ZoneId.systemDefault()).withLocale(LocaleContextHolder.getLocale());

        reportParameters.put("title", title);
        reportParameters.put("date", formatter.format(date));
        reportParameters.put("datasource1", wellRows.subList(0, 4));
        reportParameters.put("datasource2", wellRows.subList(4, 8));
    }

    @Override
    public JRDataSource getReportDataSource() throws IllegalStateException {
        return errorFound ? new JRBeanCollectionDataSource(errorMsgs) : new JREmptyDataSource();
    }

    @Override
    protected String reportFileName() {
        return "MauritiusProtocolSheet";
    }
}
