package org.openelisglobal.qc.report;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.qc.service.QCControlLotService;
import org.openelisglobal.qc.service.QCResultService;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCSource;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Materialises the bench QC register (OGC-1147) for export: the runs in the
 * window with their lab unit, test, control and technician names already
 * resolved, so the controller never touches a lazy association outside a
 * transaction. The sibling chart export does the same in
 * {@code QCChartDataService.getExportModel}.
 */
@Service
public class QCBenchExportService {

    @Autowired
    private QCResultService qcResultService;

    @Autowired
    private QCControlLotService controlLotService;

    @Autowired
    private TestService testService;

    @Autowired
    private TestSectionService testSectionService;

    @Autowired
    private SystemUserService systemUserService;

    /** One bench control run, every name already resolved. */
    public record BenchExportRow(Timestamp runDateTime, String source, String labUnitName, String testName,
            String controlLabel, BigDecimal expectedValue, BigDecimal uncertainty, BigDecimal resultValue,
            String outcome, String technicianName) {
    }

    /** The register rows, plus whether the row cap cut the window short. */
    public record BenchExport(List<BenchExportRow> rows, boolean truncated) {
    }

    @Transactional(readOnly = true)
    public BenchExport getBenchExport(Timestamp startDate, Timestamp endDate, QCSource source, int maxRows) {
        // One row over the cap tells us the cap bit, without a second count query.
        List<QCResult> results = qcResultService.findBenchResults(startDate, endDate, source, maxRows + 1);
        boolean truncated = results.size() > maxRows;
        if (truncated) {
            results = results.subList(0, maxRows);
        }
        return new BenchExport(results.stream().map(this::toRow).toList(), truncated);
    }

    private BenchExportRow toRow(QCResult result) {
        return new BenchExportRow(result.getRunDateTime(), String.valueOf(result.getSource()), labUnitName(result),
                testName(result), controlLabel(result), result.getExpectedValue(), result.getUncertainty(),
                result.getResultValue(), String.valueOf(result.getQualitativeOutcome()), technicianName(result));
    }

    /**
     * Names are context on a compliance export, never a reason to fail one. Each
     * lookup is a separate service, so they share this guard rather than four
     * copies of it.
     */
    private String resolveOrBlank(Supplier<String> lookup) {
        try {
            String value = lookup.get();
            return value == null ? "" : value;
        } catch (RuntimeException e) {
            return "";
        }
    }

    private String labUnitName(QCResult result) {
        if (result.getTestSectionId() == null) {
            return "";
        }
        return resolveOrBlank(() -> {
            TestSection section = testSectionService.get(result.getTestSectionId());
            return section == null ? null : section.getLocalizedName();
        });
    }

    private String testName(QCResult result) {
        if (result.getTestId() == null) {
            return "";
        }
        return resolveOrBlank(() -> {
            Test test = testService.get(result.getTestId());
            return test == null ? null : test.getName();
        });
    }

    /** The kit label for an RDT, or the lot number for a levelled control. */
    private String controlLabel(QCResult result) {
        if (StringUtils.isNotBlank(result.getControlLabel())) {
            return result.getControlLabel();
        }
        if (result.getControlLotId() == null) {
            return "";
        }
        return resolveOrBlank(() -> {
            QCControlLot lot = controlLotService.get(result.getControlLotId());
            return lot == null ? null : lot.getLotNumber() + " (" + lot.getControlLevel() + ")";
        });
    }

    /**
     * Who ran the control — the point of recording the acting user, not automation.
     */
    private String technicianName(QCResult result) {
        if (result.getTechnicianId() == null) {
            return "";
        }
        return resolveOrBlank(() -> {
            SystemUser user = systemUserService.get(String.valueOf(result.getTechnicianId()));
            return user == null ? null : user.getDisplayName();
        });
    }
}
