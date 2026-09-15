package org.openelisglobal.reports.dataexport.service;

import java.io.IOException;
import java.io.Writer;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.reports.dataexport.dao.ReferralExportDAO;
import org.openelisglobal.reports.dataexport.form.ExportRecord;
import org.openelisglobal.reports.dataexport.form.ExportSnapshot;
import org.openelisglobal.reports.dataexport.form.ReportSourceConfig;
import org.openelisglobal.reports.dataexport.form.ReportingVariable;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl.ResultType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReferralSource implements ReportingSource {
    private final ReferralExportDAO referrals;
    private final ReportingResultValues values;
    private final ReportingCsvWriter csv;

    public ReferralSource(ReferralExportDAO referrals, ReportingResultValues values, ReportingCsvWriter csv) {
        this.referrals = referrals;
        this.values = values;
        this.csv = csv;
    }

    @Override
    public String id() {
        return "REFERRALS";
    }

    @Override
    public List<ReportingVariable> catalog() {
        return List.of(field("referralId", "Referral ID", "text"),
                field("referralResultId", "Referral Result ID", "text"), field("resultId", "Result ID", "text"),
                field("referralAccessionNumber", "Accession Number", "text"),
                field("referringLab", "Referring Lab", "text"), field("referredLab", "Referred Lab", "text"),
                field("referredTestName", "Referred Test Name", "text"),
                field("returnedTestName", "Returned Test Name", "text"), field("requestDate", "Request Date", "date"),
                field("referralDate", "Referral Date", "date"),
                field("referralResultValue", "Referral Result Value", "text"),
                field("referralResultDate", "Referral Result Date", "date"),
                field("referralStatus", "Referral Status", "text"), field("resultUnit", "Result Unit", "text"));
    }

    private static ReportingVariable field(String id, String label, String type) {
        return new ReportingVariable(id, label, type, "referrals", false, List.of("TABLE"));
    }

    @Override
    public void validateConfiguration(ReportSourceConfig configuration) {
        if (!List.of("sentDate", "requestDate").contains(configuration.dateAnchor())
                || !configuration.layouts().equals(List.of("TABLE")) || !configuration.catalogs().isEmpty()
                || !List.of("labSectionIds", "testIds").containsAll(configuration.filters()))
            throw new IllegalArgumentException("reporting.definition.unsupportedMapping");
        if (!catalog().stream().map(ReportingVariable::id).collect(Collectors.toSet())
                .containsAll(configuration.attributes()))
            throw new IllegalArgumentException("reporting.definition.attributesInvalid");
    }

    @Override
    public long write(Writer output, ExportSnapshot request) throws IOException {
        ZoneId zone = ZoneId.of(request.timezone());
        String referringLab = ConfigurationProperties.getInstance()
                .getPropertyValue(ConfigurationProperties.Property.SiteName);
        try (var stream = referrals.stream(request)) {
            var input = stream.iterator();
            Iterator<ExportRecord> records = new Iterator<>() {
                private Compiled pending;
                private int count;

                private Compiled read() {
                    while (input.hasNext()) {
                        var row = input.next();
                        var result = row.returnedResult() == null ? null : row.returnedResult().getResult();
                        if (result != null && values.isQualifier(result))
                            continue;
                        var compiled = compile(row, zone, referringLab);
                        if (++count % 250 == 0)
                            referrals.clearReadBatch();
                        return compiled;
                    }
                    return null;
                }

                @Override
                public boolean hasNext() {
                    if (pending == null)
                        pending = read();
                    return pending != null;
                }

                @Override
                public ExportRecord next() {
                    if (!hasNext())
                        throw new NoSuchElementException();
                    var first = pending;
                    var fields = new LinkedHashMap<>(first.fields());
                    pending = read();
                    while (first.multiKey() != null && pending != null
                            && Objects.equals(first.multiKey(), pending.multiKey())) {
                        for (String field : List.of("referralResultId", "resultId", "referralResultValue"))
                            fields.put(field, Objects.toString(fields.get(field), "") + "; "
                                    + Objects.toString(pending.fields().get(field), ""));
                        pending = read();
                    }
                    return new ExportRecord(first.id(), fields.get("referralId"), null, fields, Map.of());
                }
            };
            return csv.write(output, ReportingCsvWriter.Layout.TABLE,
                    request.variables().stream().map(ReportingVariable::exportField).toList(), records);
        }
    }

    private Compiled compile(ReferralExportDAO.Row row, ZoneId zone, String referringLab) {
        var referral = row.referral();
        var returned = row.returnedResult();
        var result = returned == null ? null : returned.getResult();
        var test = row.returnedTest();
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("referralId", referral.getId());
        fields.put("referralAccessionNumber", referral.getAnalysis().getSampleItem().getSample().getAccessionNumber());
        fields.put("referringLab", referringLab);
        fields.put("referredLab", referral.getOrganization() == null ? referral.getOrganizationName()
                : referral.getOrganization().getOrganizationName());
        fields.put("referredTestName", referral.getAnalysis().getTest().getDescription());
        fields.put("requestDate", date(referral.getRequestDate(), zone));
        fields.put("referralDate", date(referral.getSentDate(), zone));
        fields.put("referralStatus", referral.getStatus() == null ? null : referral.getStatus().name());
        fields.put("referralResultId", returned == null ? null : returned.getId());
        fields.put("referralResultDate", returned == null ? null : date(returned.getReferralReportDate(), zone));
        fields.put("resultId", result == null ? null : result.getId());
        fields.put("referralResultValue", result == null ? null : values.format(result));
        fields.put("returnedTestName", test == null ? null : test.getDescription());
        fields.put("resultUnit", test == null || test.getUnitOfMeasure() == null ? null
                : test.getUnitOfMeasure().getUnitOfMeasureName());
        String component = result == null || result.getTestResult() == null ? null
                : result.getTestResult().getComponentId();
        String multiKey = result != null && ResultType.isMultiSelectVariant(result.getResultType())
                ? referral.getId() + ":" + returned.getTestId() + ":" + component + ":" + result.getGrouping() + ":"
                        + result.getResultType() + ":" + returned.getReferralReportDate()
                : null;
        return new Compiled(returned == null ? "referral:" + referral.getId() : "returned:" + returned.getId(), fields,
                multiKey);
    }

    private static String date(Timestamp value, ZoneId zone) {
        return value == null ? null : value.toInstant().atZone(zone).toLocalDate().toString();
    }

    private record Compiled(String id, Map<String, String> fields, String multiKey) {
    }
}
