package org.openelisglobal.referral;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.Timestamp;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.referral.service.ReferralService;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.referral.valueholder.ReferralStatus;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An analysis can carry more than one referral: a rejected or cancelled one
 * gets re-raised, or the sample goes out to a second lab.
 *
 * <p>
 * The lookup used to go through {@code getMatch}, which returns nothing when
 * more than one row matches. Every caller then behaved as though the test had
 * never been referred: Results Entry showed no referral and offered to raise
 * another, and the manual-entry hook stopped completing referrals for that
 * analysis.
 */
public class ReferralByAnalysisLookupTest extends BaseWebContextSensitiveTest {

    private static final String ANALYSIS_ID = "1";
    private static final String ORGANIZATION_ID = "1";
    private static final String ACTOR = "1";

    @Autowired
    private ReferralService referralService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private OrganizationService organizationService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/referral.xml");
        resyncSequence("clinlims.referral_seq", "clinlims.referral");
        jdbcTemplate.update("DELETE FROM clinlims.referral_status_history WHERE referral_id IN"
                + " (SELECT id FROM clinlims.referral WHERE analysis_id = CAST(? AS numeric))", ANALYSIS_ID);
        jdbcTemplate.update("DELETE FROM clinlims.referral WHERE analysis_id = CAST(? AS numeric)", ANALYSIS_ID);
    }

    @Test
    public void oneReferralOnTheAnalysisIsFound() {
        String id = insertReferral(ReferralStatus.REQUESTED);

        Referral found = referralService.getReferralByAnalysisId(ANALYSIS_ID);

        assertNotNull("a single referral must still be found", found);
        assertEquals(id, found.getId());
    }

    @Test
    public void aReReferredAnalysisReturnsTheReferralCurrentlyInPlay() {
        insertReferral(ReferralStatus.CANCELLED);
        String reRaised = insertReferral(ReferralStatus.REQUESTED);

        Referral found = referralService.getReferralByAnalysisId(ANALYSIS_ID);

        assertNotNull("a second referral on the analysis must not read as no referral at all", found);
        assertEquals("the newest referral is the one in play", reRaised, found.getId());
    }

    private String insertReferral(ReferralStatus status) {
        Analysis analysis = analysisService.get(ANALYSIS_ID);
        assertNotNull("precondition: testdata/referral.xml seeds analysis " + ANALYSIS_ID, analysis);

        Referral referral = new Referral();
        referral.setFhirUuid(UUID.randomUUID());
        referral.setStatus(status);
        referral.setAnalysis(analysis);
        referral.setOrganization(organizationService.get(ORGANIZATION_ID));
        referral.setReferralTypeId("1");
        referral.setRequesterName("bench");
        referral.setRequestDate(new Timestamp(System.currentTimeMillis()));
        referral.setSysUserId(ACTOR);
        return referralService.insert(referral);
    }
}
