package org.openelisglobal.referral;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.common.services.SampleAddService;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.referral.action.beanitems.ReferralItem;
import org.openelisglobal.referral.service.ReferralItemService;
import org.openelisglobal.referral.service.ReferralResultService;
import org.openelisglobal.referral.service.ReferralService;
import org.openelisglobal.referral.service.ReferralSetService;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.referral.valueholder.ReferralResult;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;

public class ReferralSetServiceTest extends BaseWebContextSensitiveTest {
    @Autowired
    private ReferralSetService referralSetService;
    @Autowired
    private ReferralItemService referralItemService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private ReferralResultService referralResultService;
    @Autowired
    private ResultService resultService;
    @Autowired
    private ReferralService referralService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/referral-set.xml");
    }

    @Test
    public void createSaveReferralSetsSamplePatientEntry_ShouldInsertAReferralSets() {
        SampleAddService.SampleTestCollection sampleTestCollection = getSampleTestCollection();
        sampleTestCollection.analysises = analysisService.getAll();
        List<SampleAddService.SampleTestCollection> sampleItemsTests = new ArrayList<>(List.of(sampleTestCollection));

        List<ReferralItem> referralItems = referralItemService.getReferralItems();
        SamplePatientUpdateData updateData = new SamplePatientUpdateData("3901");
        updateData.setSampleItemsTests(sampleItemsTests);
        int initialReferralItemsCount = referralItems.size();
        assertEquals(3, initialReferralItemsCount);

        List<Referral> initialReferrals = referralService.getAll();
        int initialReferralCount = initialReferrals.size();
        assertEquals(3, initialReferralCount);

        List<Result> initialResultsList = resultService.getAllResults();
        int initialResultCount = initialResultsList.size();
        assertEquals(2, initialResultCount);

        List<ReferralResult> initialReferralResults = referralResultService.getAll();
        int initialReferralResultCount = initialReferralResults.size();
        assertEquals(4, initialReferralResultCount);

        referralSetService.createSaveReferralSetsSamplePatientEntry(referralItems, updateData);

        List<Referral> newReferrals = referralService.getAll();
        int newReferralCount = newReferrals.size();
        assertEquals(initialReferralCount + 3, newReferralCount);

        List<Result> newResultsList = resultService.getAllResults();
        int newResultCount = newResultsList.size();
        assertEquals(initialResultCount + 3, newResultCount);

        List<ReferralResult> newReferralResults = referralResultService.getAll();
        int newReferralResultCount = newReferralResults.size();
        assertEquals(initialReferralResultCount + 3, newReferralResultCount);

        List<ReferralItem> newReferralItems = referralItemService.getReferralItems();
        assertEquals(initialReferralItemsCount + 3, newReferralItems.size());
    }

    private static SampleAddService.SampleTestCollection getSampleTestCollection() {
        SampleAddService sampleAddService = new SampleAddService("xml", "3901", new Sample(), "2024-06-03");
        List<org.openelisglobal.test.valueholder.Test> tests = new ArrayList<>();
        List<ObservationHistory> initialConditionList = new ArrayList<>();
        Map<String, String> testIdToUserSectionMap = new HashMap<>();
        Map<String, String> testIdToUserSampleTypeMap = new HashMap<>();
        return sampleAddService.new SampleTestCollection(new SampleItem(), tests, "2024-06-04", initialConditionList,
                testIdToUserSectionMap, testIdToUserSampleTypeMap, new ObservationHistory());
    }
}
