package org.openelisglobal.reports.action.implementation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.spi.report.ReportException;
import org.openelisglobal.spi.report.ReportOutput;
import org.openelisglobal.spi.report.ReportProvider;
import org.openelisglobal.spi.report.ReportRequest;

/**
 * Adapts a module's {@link ReportProvider} to the core's {@link IReportCreator}
 * contract, so the report controllers serve a module report exactly like a
 * built-in one. The report form's scalar fields become the request parameters
 * (lists are joined with commas, nested objects are skipped), which is what a
 * site-specific report needs: the accession range, the dates, the selections.
 */
public class ModuleReportCreator implements IReportCreator {

    private static final ObjectMapper FORM_MAPPER = new ObjectMapper()
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private final ReportProvider provider;
    private String requestedReport;
    private String systemUserId;
    private Map<String, String> parameters = new LinkedHashMap<>();
    private ReportOutput output;

    public ModuleReportCreator(ReportProvider provider) {
        this.provider = provider;
    }

    @Override
    public void initializeReport(ReportForm form) {
        parameters = flatten(form);
    }

    @Override
    public String getResponseHeaderName() {
        return "Content-Disposition";
    }

    @Override
    public String getContentType() {
        return output == null ? "application/pdf" : output.getContentType();
    }

    @Override
    public String getResponseHeaderContent() {
        String fileName = output != null && output.getFileName() != null ? output.getFileName()
                : requestedReport + ".pdf";
        return "inline; filename=\"" + fileName + "\"";
    }

    @Override
    public HashMap<String, ?> getReportParameters() {
        return new HashMap<>(parameters);
    }

    @Override
    public byte[] runReport() {
        Locale locale = org.springframework.context.i18n.LocaleContextHolder.getLocale();
        try {
            output = provider.run(new ReportRequest(requestedReport, parameters, systemUserId, locale));
        } catch (ReportException e) {
            LogEvent.logError("ModuleReportCreator", "runReport", "report " + requestedReport + " failed: " + e);
            throw new IllegalStateException("report " + requestedReport + " failed: " + e.getMessage(), e);
        }
        return output.getContent();
    }

    @Override
    public void setReportPath(String path) {
        // module reports resolve their own templates
    }

    @Override
    public void setRequestedReport(String report) {
        this.requestedReport = report;
    }

    @Override
    public List<String> getReportedOrders() {
        return new ArrayList<>();
    }

    @Override
    public void setSystemUserId(String id) {
        this.systemUserId = id;
    }

    static Map<String, String> flatten(ReportForm form) {
        Map<String, String> flat = new LinkedHashMap<>();
        if (form == null) {
            return flat;
        }
        Map<String, Object> tree = FORM_MAPPER.convertValue(form, new TypeReference<Map<String, Object>>() {
        });
        for (Map.Entry<String, Object> entry : tree.entrySet()) {
            Object value = entry.getValue();
            if (value == null || value instanceof Map) {
                continue;
            }
            if (value instanceof List<?> list) {
                List<String> items = new ArrayList<>();
                for (Object item : list) {
                    if (item != null && !(item instanceof Map) && !(item instanceof List)) {
                        items.add(String.valueOf(item));
                    }
                }
                if (!items.isEmpty()) {
                    flat.put(entry.getKey(), String.join(",", items));
                }
                continue;
            }
            flat.put(entry.getKey(), String.valueOf(value));
        }
        return flat;
    }
}
