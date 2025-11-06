package org.openelisglobal.referral;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.Timestamp;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.referral.service.ReferralResultService;
import org.openelisglobal.referral.valueholder.ReferralResult;
import org.springframework.beans.factory.annotation.Autowired;

public class ReferralResultServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ReferralResultService referralResultService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/referral-result.xml");
    }

    @Test
    public void getReferralResultById_ShouldReturnResultReferralMatchingTheIdPassedAsParameter() {
        ReferralResult referralResult = referralResultService.getReferralResultById("4");
        assertNotNull(referralResult);
        assertEquals("103", referralResult.getReferralId());
        assertEquals(Timestamp.valueOf("2024-07-04 12:00:00"), referralResult.getLastupdated());
        assertEquals("Y", referralResult.getResult().getIsReportable());
    }

    @Test
    public void getReferralResultsForReferral_ShouldReturnAllReferralResultsWithASpecificReferral() {
        List<ReferralResult> referralResults = referralResultService.getReferralResultsForReferral("103");
        assertNotNull(referralResults);
        assertEquals(2, referralResults.size());
        assertEquals("3", referralResults.get(0).getId());
        assertEquals("4", referralResults.get(1).getId());
    }

    @Test
    public void getReferralsByResultId_ShouldReturnReferralResultsUsingAResultId() {
        List<ReferralResult> referralResults = referralResultService.getReferralsByResultId("2002");
        assertNotNull(referralResults);
        assertEquals(2, referralResults.size());
        assertEquals("3", referralResults.get(1).getId());
        assertEquals("2", referralResults.get(0).getId());
    }
}
