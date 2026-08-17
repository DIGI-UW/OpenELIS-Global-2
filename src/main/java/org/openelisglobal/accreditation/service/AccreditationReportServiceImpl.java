package org.openelisglobal.accreditation.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.accreditation.dao.AccreditingBodyDAO;
import org.openelisglobal.accreditation.dao.TestAccreditationDAO;
import org.openelisglobal.accreditation.dto.AccreditationReportData;
import org.openelisglobal.accreditation.valueholder.AccreditationStatus;
import org.openelisglobal.accreditation.valueholder.AccreditingBody;
import org.openelisglobal.accreditation.valueholder.LogoVisibilityMode;
import org.openelisglobal.accreditation.valueholder.TestAccreditation;
import org.openelisglobal.image.service.ImageService;
import org.openelisglobal.image.valueholder.Image;
import org.openelisglobal.internationalization.MessageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * OGC-686 — resolves the accreditation logos and notes line for one rendered
 * patient report.
 *
 * <p>
 * Two indexed queries regardless of report size: one for the enrollment rows of
 * every printed test, one for the bodies in display order. Bodies are evaluated
 * independently of each other, so a PERCENTAGE body can be suppressed on the
 * same report where an ANY_ACCREDITED_TEST body prints.
 *
 * <p>
 * The logo gate and the notes line deliberately differ: a logo is a claim about
 * <i>this report</i> and must clear the body's visibility rule, while the notes
 * line only states which accreditations the lab holds that touch the report at
 * all. A body with one accredited test out of twenty is named in the line but
 * shows no logo under an 80% threshold.
 */
@Service
public class AccreditationReportServiceImpl implements AccreditationReportService {

    @Autowired
    private AccreditingBodyDAO accreditingBodyDAO;

    @Autowired
    private TestAccreditationDAO testAccreditationDAO;

    @Autowired
    private ImageService imageService;

    @Override
    @Transactional(readOnly = true)
    public AccreditationReportData resolve(Collection<String> testIds, LocalDate asOf) {
        if (testIds == null || testIds.isEmpty()) {
            return AccreditationReportData.EMPTY;
        }
        // The same test can appear on several samples of one report; the gate counts
        // distinct tests, otherwise a repeated test would inflate the ratio.
        Set<String> distinctTestIds = new HashSet<>(testIds);
        LocalDate evaluationDate = asOf == null ? LocalDate.now() : asOf;

        Map<Long, Integer> accreditedCountByBody = new HashMap<>();
        for (TestAccreditation enrollment : testAccreditationDAO.getByTestIds(distinctTestIds)) {
            accreditedCountByBody.merge(enrollment.getAccreditingBodyId(), 1, Integer::sum);
        }
        if (accreditedCountByBody.isEmpty()) {
            return AccreditationReportData.EMPTY;
        }

        List<byte[]> logos = new ArrayList<>();
        List<String> claimedBodyNames = new ArrayList<>();
        for (AccreditingBody body : accreditingBodyDAO.getAllOrdered()) {
            int accredited = accreditedCountByBody.getOrDefault(body.getId(), 0);
            if (accredited == 0) {
                continue;
            }
            if (!AccreditationStatus.of(body.getActive(), body.getExpiresOn(), evaluationDate).isValidForReporting()) {
                continue;
            }
            claimedBodyNames.add(body.getName());
            if (logos.size() < MAX_LOGOS && body.getLogoImageId() != null
                    && passesVisibilityGate(body, accredited, distinctTestIds.size())) {
                Image logo = imageService.get(body.getLogoImageId());
                if (logo != null && logo.getImage() != null) {
                    logos.add(logo.getImage());
                }
            }
        }

        if (claimedBodyNames.isEmpty()) {
            return AccreditationReportData.EMPTY;
        }
        return new AccreditationReportData(logos,
                MessageUtil.getMessage("report.accreditation.notesLine", String.join(", ", claimedBodyNames)));
    }

    /**
     * Logo visibility gate, evaluated per body against the tests printed on this
     * report.
     */
    private boolean passesVisibilityGate(AccreditingBody body, int accredited, int totalTests) {
        if (body.getLogoVisibilityMode() == LogoVisibilityMode.PERCENTAGE) {
            short threshold = body.getThresholdPct() == null ? 0 : body.getThresholdPct();
            // Integer arithmetic on both sides: no rounding to argue about when a
            // lab asks why 3 of 4 tests did not clear an 80% threshold.
            return accredited * 100 >= threshold * totalTests;
        }
        return accredited > 0;
    }
}
