package org.openelisglobal.referral;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.referral.dto.ReferenceLabMetricsDTO;
import org.openelisglobal.referral.service.ReferenceLabResultsService;
import org.openelisglobal.siteinformation.service.SiteInformationDomainService;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.openelisglobal.siteinformation.valueholder.SiteInformationDomain;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for the stuck-referral threshold surfaced via the Reference
 * Lab Results /metrics endpoint (OGC-801).
 *
 * <p>
 * Verifies the three-layer contract:
 * <ol>
 * <li>Liquibase seeds the {@code referralStuckThresholdDays} row in
 * {@code site_information} with the documented default of {@code 7}.</li>
 * <li>{@code getDashboardMetrics()} reads that row and returns the value via
 * {@link ReferenceLabMetricsDTO#getReferralStuckThresholdDays()}.</li>
 * <li>If the row is absent or malformed, the service falls back to {@code 7}
 * rather than throwing or returning {@code 0}.</li>
 * </ol>
 */
public class ReferenceLabResultsThresholdTest extends BaseWebContextSensitiveTest {

    private static final String CONFIG_NAME = "referralStuckThresholdDays";
    private static final int DEFAULT_THRESHOLD = 7;
    private static final String ACTOR_USER_ID = "1";

    @Autowired
    private ReferenceLabResultsService referenceLabResultsService;

    @Autowired
    private SiteInformationService siteInformationService;

    @Autowired
    private SiteInformationDomainService siteInformationDomainService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/referral.xml");
    }

    /**
     * BaseWebContextSensitiveTest disables rollback
     * ({@code Propagation.NOT_SUPPORTED}), so mutations to {@code site_information}
     * persist across test methods. Each test must leave the threshold row in its
     * seeded state (name=referralStuckThresholdDays, value=7) to keep tests
     * order-independent.
     */
    @After
    public void restoreSeededThresholdRow() {
        SiteInformation row = siteInformationService.getSiteInformationByName(CONFIG_NAME);
        if (row == null) {
            SiteInformation fresh = new SiteInformation();
            fresh.setName(CONFIG_NAME);
            fresh.setValue(String.valueOf(DEFAULT_THRESHOLD));
            fresh.setValueType("int");
            fresh.setDescription("Days a referral can wait at the reference lab before being flagged as stuck on"
                    + " the Reference Lab Results page.");
            SiteInformationDomain domain = siteInformationDomainService.getByName("siteIdentity");
            if (domain != null) {
                fresh.setDomain(domain);
            }
            fresh.setSysUserId(ACTOR_USER_ID);
            siteInformationService.save(fresh);
        } else if (!String.valueOf(DEFAULT_THRESHOLD).equals(row.getValue())) {
            row.setValue(String.valueOf(DEFAULT_THRESHOLD));
            row.setSysUserId(ACTOR_USER_ID);
            siteInformationService.save(row);
        }
    }

    @Test
    public void metrics_carriesSeededThresholdValue() {
        ReferenceLabMetricsDTO metrics = referenceLabResultsService.getDashboardMetrics();
        Assert.assertEquals(
                "Liquibase shipment-017 must seed site_information.referralStuckThresholdDays=7 and"
                        + " getDashboardMetrics() must surface it on the DTO",
                DEFAULT_THRESHOLD, metrics.getReferralStuckThresholdDays());
    }

    @Test
    public void metrics_reflectsUpdatedThresholdValue() {
        setThresholdValue("14");

        ReferenceLabMetricsDTO metrics = referenceLabResultsService.getDashboardMetrics();
        Assert.assertEquals("getDashboardMetrics() must read the current site_information row, not a cached value", 14,
                metrics.getReferralStuckThresholdDays());
    }

    @Test
    public void metrics_fallsBackToDefaultWhenValueIsMalformed() {
        setThresholdValue("not-a-number");

        ReferenceLabMetricsDTO metrics = referenceLabResultsService.getDashboardMetrics();
        Assert.assertEquals("Malformed config values must fall back to the default rather than throwing",
                DEFAULT_THRESHOLD, metrics.getReferralStuckThresholdDays());
    }

    @Test
    public void metrics_fallsBackToDefaultWhenConfigRowAbsent() {
        SiteInformation row = siteInformationService.getSiteInformationByName(CONFIG_NAME);
        Assert.assertNotNull("precondition: Liquibase shipment-017 must seed the row before this test deletes it", row);
        siteInformationService.delete(row.getId(), ACTOR_USER_ID);

        ReferenceLabMetricsDTO metrics = referenceLabResultsService.getDashboardMetrics();
        Assert.assertEquals("Absent site_information row must fall back to the documented default of 7",
                DEFAULT_THRESHOLD, metrics.getReferralStuckThresholdDays());
    }

    @Test
    public void metrics_thresholdAdditive_otherFieldsStillPopulated() {
        ReferenceLabMetricsDTO metrics = referenceLabResultsService.getDashboardMetrics();

        // Outstanding bucket counts referrals in REQUESTED/RECEIVED/IN_PROGRESS — the
        // referral.xml fixture seeds at least one such row (REQUESTED legacy referral),
        // so the count must be > 0.
        Assert.assertTrue("outstanding must remain populated alongside the new threshold field",
                metrics.getOutstanding() >= 1L);
        Assert.assertEquals("reconciledToday must still be the v1 stub value of 0", 0L, metrics.getReconciledToday());
        Assert.assertTrue("returned must be a non-negative count", metrics.getReturned() >= 0L);
        Assert.assertTrue("rejectedThisWeek must be a non-negative count", metrics.getRejectedThisWeek() >= 0L);
        Assert.assertEquals("threshold field must coexist with the four original metrics", DEFAULT_THRESHOLD,
                metrics.getReferralStuckThresholdDays());
    }

    private void setThresholdValue(String value) {
        SiteInformation row = siteInformationService.getSiteInformationByName(CONFIG_NAME);
        Assert.assertNotNull("precondition: Liquibase shipment-017 must seed the row before this test mutates it", row);
        row.setValue(value);
        row.setSysUserId(ACTOR_USER_ID);
        siteInformationService.save(row);
    }
}
