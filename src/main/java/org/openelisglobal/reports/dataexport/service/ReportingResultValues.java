package org.openelisglobal.reports.dataexport.service;

import org.openelisglobal.reports.dataexport.dao.SampleTestingExportDAO;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl.ResultType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Shared stored-result interpretation for each reporting source. */
@Service
@Transactional(readOnly = true)
public class ReportingResultValues {
    private final SampleTestingExportDAO lookup;

    public ReportingResultValues(SampleTestingExportDAO lookup) {
        this.lookup = lookup;
    }

    public boolean isQualifier(Result result) {
        return "A".equals(result.getResultType()) && result.getTestResult() == null && result.getParentResult() != null
                && ResultType.isDictionaryVariant(result.getParentResult().getResultType());
    }

    public String format(Result result) {
        String value = result.getValue();
        if (value == null || value.isBlank())
            return value;
        if (ResultType.isDictionaryVariant(result.getResultType())) {
            value = lookup.dictionary(value);
            var qualifiers = lookup.qualifiers(result.getId());
            if (!qualifiers.isEmpty())
                value += " (" + String.join("; ", qualifiers) + ")";
        } else if ("N".equals(result.getResultType()) && result.getSignificantDigits() >= 0) {
            int digits = result.getSignificantDigits();
            // Preserve the stored reporting precision without rounding or HTML.
            if (digits == 0)
                return value.split("\\.")[0];
            int places = value.contains(".") ? value.length() - value.lastIndexOf('.') - 1 : 0;
            if (!value.contains("."))
                value += ".";
            if (places < digits)
                value += "0".repeat(digits - places);
        }
        return value;
    }
}
