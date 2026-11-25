package org.openelisglobal.module.kitfixture.report;

import java.util.Set;
import org.openelisglobal.spi.report.ReportOutput;
import org.openelisglobal.spi.report.ReportProvider;
import org.openelisglobal.spi.report.ReportRequest;

public class ThingReport implements ReportProvider {

    @Override
    public Set<String> reportIds() {
        return Set.of("kitfixtureThings");
    }

    @Override
    public ReportOutput run(ReportRequest request) {
        return ReportOutput.pdf(new byte[0], "things.pdf");
    }
}
