package org.openelisglobal.microbiology.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.form.MicroPatientReportForm;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MicroEnvironmentalReportService {
    private final MicroCaseDAO cases;
    private final MicroPatientReportService reports;
    private final MicrobiologyCaseAccessService access;

    public MicroEnvironmentalReportService(MicroCaseDAO cases, MicroPatientReportService reports,
            MicrobiologyCaseAccessService access) {
        this.cases = cases;
        this.reports = reports;
        this.access = access;
    }

    public List<MicroPatientReportForm> released(String orderId, String actor) {
        var result = new ArrayList<MicroPatientReportForm>();
        for (var c : cases.getByOrder(orderId)) {
            if (c.getSiteId() == null)
                throw new IllegalArgumentException("MICROBIOLOGY_ENVIRONMENTAL_CASE_REQUIRED");
            if (!access.hasLabUnitRole(actor, c.getLabUnitId(), Constants.ROLE_RESULTS)
                    && !access.hasLabUnitRole(actor, c.getLabUnitId(), Constants.ROLE_VALIDATION)
                    && !access.hasLabUnitRole(actor, c.getLabUnitId(), Constants.ROLE_REPORTS))
                throw new AccessDeniedException("Case lab unit access required");
            if (c.getStatus() != org.openelisglobal.microbiology.valueholder.MicroCaseStatus.ACTIVE)
                continue;
            if (!"FINAL_RELEASED".equals(c.getFinalReleaseState()))
                throw new IllegalStateException("MICROBIOLOGY_CERTIFICATE_RELEASE_REQUIRED");
            var version = reports.versions(c.getId()).stream().filter(v -> !"PRELIMINARY".equals(v.type()))
                    .max(Comparator.comparingInt(MicroPatientReportForm.Version::number))
                    .orElseThrow(() -> new IllegalStateException("MICROBIOLOGY_REPORT_SNAPSHOT_REQUIRED"));
            result.add(version.report());
        }
        return result;
    }
}
