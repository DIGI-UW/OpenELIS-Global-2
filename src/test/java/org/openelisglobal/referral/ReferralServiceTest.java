package org.openelisglobal.referral;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.referral.action.beanitems.ReferralDisplayItem;
import org.openelisglobal.referral.form.ReferredOutTestsForm;
import org.openelisglobal.referral.form.ReferredOutTestsForm.ReferDateType;
import org.openelisglobal.referral.service.ReferralService;
import org.openelisglobal.referral.valueholder.Referral;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class ReferralServiceTest extends BaseWebContextSensitiveTest {
    @Autowired
    ReferralService rService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/referral.xml");
    }

    @Test
    public void getReferralById_shouldReturnReferralById() {
        Assert.assertEquals("African Health Org", rService.getReferralById("1").getOrganizationName());
    }

    @Test
    public void getReferralByAnalysisId_shouldReturnReferralByAnalysisId() {
        Assert.assertEquals("Health Corporations Ltd", rService.getReferralByAnalysisId("2").getOrganizationName());
    }

    @Test
    public void getReferralsBySampleId_shouldReturnReferralsBySampleId() {
        List<Referral> referrals = rService.getReferralsBySampleId("1");

        Assert.assertEquals(1, referrals.size());
        Assert.assertEquals("African Health Org", referrals.get(0).getOrganizationName());
    }

    @Test
    public void getUncanceledOpenReferrals_shouldReturnUncanceledOpenReferrals() {
        List<Referral> referrals = rService.getUncanceledOpenReferrals();

        Assert.assertEquals(2, referrals.size());
        Assert.assertEquals("African Health Org", referrals.get(0).getOrganizationName());
    }

    @Test
    public void getSentReferrals_shouldReturnSentReferrals() {
        List<Referral> referrals = rService.getSentReferrals();

        Assert.assertEquals(1, referrals.size());
        Assert.assertEquals("African Health Org", referrals.get(0).getOrganizationName());
    }

    @Test
    public void getSentReferralUuids_shouldSentReferralUuids() {
        List<UUID> referrals = rService.getSentReferralUuids();

        Assert.assertEquals(1, referrals.size());
        Assert.assertEquals("436a353f-8ac9-4a30-86db-bb6eda979b57", referrals.get(0).toString());
    }

    @Test
    public void getReferralsByOrganization_shouldReturnReferralsByOrganization() {
        String lDate = "2018-02-15";
        String hDate = "2018-02-18";
        java.sql.Date lowDate = java.sql.Date.valueOf(lDate);
        java.sql.Date highDate = java.sql.Date.valueOf(hDate);

        List<Referral> referrals = rService.getReferralsByOrganization("1", lowDate, highDate);

        Assert.assertEquals(1, referrals.size());
        Assert.assertEquals("African Health Org", referrals.get(0).getOrganizationName());
    }

    @Test
    public void getReferralsByAccessionNumber_shouldReturnReferralsByAccessionNumber() {
        List<Referral> referrals = rService.getReferralsByAccessionNumber("12345");

        Assert.assertEquals(1, referrals.size());
        Assert.assertEquals("African Health Org", referrals.get(0).getOrganizationName());
    }

    @Test
    public void getReferralByPatientId_shouldReturnReferralByPatientId() {
        List<Referral> referrals = rService.getReferralByPatientId("2");

        Assert.assertEquals(1, referrals.size());
        Assert.assertEquals("Health Corporations Ltd", referrals.get(0).getOrganizationName());
    }

    @Test
    @Transactional
    public void convertToDisplayItem_shouldconvertToDisplayItem() {
        Referral referral = rService.get("1");
        ReferralDisplayItem item = rService.convertToDisplayItem(referral);
        Assert.assertEquals("John", item.getPatientFirstName());
        Assert.assertEquals("Doe", item.getPatientLastName());
    }

    @Test
    @Transactional
    public void getReferralItems_shouldReturnReferralItems() {

        ReferredOutTestsForm form = new ReferredOutTestsForm();
        form.setSearchType(ReferredOutTestsForm.SearchType.PATIENT);
        form.setStartDate("2018-02-16");
        form.setEndDate("2018-02-17");
        form.setSelPatient("2");
        form.setDateType(ReferredOutTestsForm.ReferDateType.SENT);
        form.setTestUnitIds(List.of("2"));
        form.setTestIds(List.of("2"));

        List<ReferralDisplayItem> items = rService.getReferralItems(form);

        assertNotNull(items);
        assertFalse(items.isEmpty());
        assertEquals("13333", items.get(0).getAccessionNumber());
    }

    @Test
    public void getReferralsByTestAndDate_shouldReturnFilteredReferrals() {

        ReferDateType dateType = ReferDateType.SENT;
        Timestamp startDate = Timestamp.valueOf("2018-02-16 00:00:00");
        Timestamp endDate = Timestamp.valueOf("2018-02-16 23:59:59");

        List<String> testUnitIds = Arrays.asList("1");
        List<String> testIds = Arrays.asList("1");

        List<Referral> referrals = rService.getReferralsByTestAndDate(dateType, startDate, endDate, testUnitIds,
                testIds);

        Assert.assertNotNull(referrals);
        Assert.assertFalse(referrals.isEmpty());
        referrals.forEach(ref -> {
            Assert.assertNotNull(ref.getId());
            Assert.assertNotNull(ref.getAnalysis());
        });
        Assert.assertEquals("African Health Org", referrals.get(0).getOrganizationName());
    }
}