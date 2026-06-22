package org.openelisglobal.sample.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.referral.action.beanitems.ReferralItem;
import org.openelisglobal.referral.service.ReferralSetService;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Covers {@link SamplePatientEntryServiceImpl#persistOrderEntryReferrals},
 * which persists Refer Out / Subcontract rows synchronously inside the sample
 * save transaction so they cannot be silently lost by the async FHIR transform
 * listener. The helper is invoked uniformly for every sample domain (clinical,
 * environmental, vector) — capture happens on Step 3 (Label) of the order-entry
 * wizard for all workflows.
 *
 * <p>
 * Pattern mirrors {@code SamplePatientEntryServiceImplTest}: pure Mockito unit
 * test, no Spring context. Spring-context integration coverage for the
 * downstream DB writes lives in {@code ReferralSetServiceTest} and
 * {@code ReferralSubcontractTransitionsTest}.
 */
@RunWith(MockitoJUnitRunner.class)
public class SamplePatientEntryServiceReferralPersistenceTest {

    @Mock
    private ReferralSetService referralSetService;

    private SamplePatientEntryServiceImpl service;

    @Before
    public void setUp() {
        service = new SamplePatientEntryServiceImpl();
        ReflectionTestUtils.setField(service, "referralSetService", referralSetService);
    }

    @Test
    public void envDomain_withUseReferralAndNonEmptyItems_invokesDraftSaveAndMarksFlag() {
        SamplePatientUpdateData updateData = updateDataWithDomain("E");
        SamplePatientEntryForm form = formWithReferrals(true, oneReferralItem());

        service.persistOrderEntryReferrals(updateData, form);

        verify(referralSetService, times(1)).createDraftReferralSetsForOrderEntry(eq(form.getReferralItems()),
                same(updateData));
        assertTrue("Env workflow must mark referrals persisted so async path short-circuits",
                updateData.isReferralsPersistedSynchronously());
    }

    @Test
    public void vectorDomain_withUseReferralAndNonEmptyItems_invokesDraftSaveAndMarksFlag() {
        SamplePatientUpdateData updateData = updateDataWithDomain("V");
        SamplePatientEntryForm form = formWithReferrals(true, oneReferralItem());

        service.persistOrderEntryReferrals(updateData, form);

        verify(referralSetService, times(1)).createDraftReferralSetsForOrderEntry(eq(form.getReferralItems()),
                same(updateData));
        assertTrue(updateData.isReferralsPersistedSynchronously());
    }

    @Test
    public void clinicalDomain_withUseReferralAndNonEmptyItems_invokesDraftSaveAndMarksFlag() {
        // Clinical (domain="H") referrals now flow through the same sync helper
        // as env/vector. The Step-3 Refer Out surface is the single canonical
        // capture point across every order-entry workflow.
        SamplePatientUpdateData updateData = updateDataWithDomain("H");
        SamplePatientEntryForm form = formWithReferrals(true, oneReferralItem());

        service.persistOrderEntryReferrals(updateData, form);

        verify(referralSetService, times(1)).createDraftReferralSetsForOrderEntry(eq(form.getReferralItems()),
                same(updateData));
        assertTrue("Clinical workflow must mark referrals persisted so async path short-circuits",
                updateData.isReferralsPersistedSynchronously());
    }

    @Test
    public void envDomain_withUseReferralFalse_doesNotInvokeDraftSave() {
        SamplePatientUpdateData updateData = updateDataWithDomain("E");
        SamplePatientEntryForm form = formWithReferrals(false, oneReferralItem());

        service.persistOrderEntryReferrals(updateData, form);

        verify(referralSetService, never()).createDraftReferralSetsForOrderEntry(any(), any());
        assertFalse(updateData.isReferralsPersistedSynchronously());
    }

    @Test
    public void envDomain_withEmptyReferralItems_doesNotInvokeDraftSaveOrSetFlag() {
        // useReferral=true but items=[] is the "nothing selected" UI state. We
        // skip the call entirely so the async leg's own empty-list short-circuit
        // is still the source of truth for that case (preserves clinical parity).
        SamplePatientUpdateData updateData = updateDataWithDomain("E");
        SamplePatientEntryForm form = formWithReferrals(true, new ArrayList<>());

        service.persistOrderEntryReferrals(updateData, form);

        verify(referralSetService, never()).createDraftReferralSetsForOrderEntry(any(), any());
        assertFalse("Flag must remain false on empty input so async leg is free to run if needed",
                updateData.isReferralsPersistedSynchronously());
    }

    @Test
    public void envDomain_withNullReferralItems_doesNotThrowOrInvokeDraftSave() {
        SamplePatientUpdateData updateData = updateDataWithDomain("E");
        SamplePatientEntryForm form = new SamplePatientEntryForm();
        form.setUseReferral(true);
        // form.referralItems left as null intentionally — defensive guard.

        service.persistOrderEntryReferrals(updateData, form);

        verify(referralSetService, never()).createDraftReferralSetsForOrderEntry(any(), any());
        assertFalse(updateData.isReferralsPersistedSynchronously());
    }

    @Test
    public void nullSample_doesNotPreventDraftSave() {
        // The helper no longer inspects Sample.domain, so a null sample on
        // updateData must not stop referral persistence — guards against the
        // earlier domain-gated NPE risk and confirms domain-agnostic behaviour.
        SamplePatientUpdateData updateData = new SamplePatientUpdateData("3901"); // no sample attached
        SamplePatientEntryForm form = formWithReferrals(true, oneReferralItem());

        service.persistOrderEntryReferrals(updateData, form);

        verify(referralSetService, times(1)).createDraftReferralSetsForOrderEntry(eq(form.getReferralItems()),
                same(updateData));
        assertTrue(updateData.isReferralsPersistedSynchronously());
    }

    private static SamplePatientUpdateData updateDataWithDomain(String domain) {
        SamplePatientUpdateData updateData = new SamplePatientUpdateData("3901");
        Sample sample = new Sample();
        sample.setDomain(domain);
        updateData.setSample(sample);
        return updateData;
    }

    private static SamplePatientEntryForm formWithReferrals(boolean useReferral, List<ReferralItem> items) {
        SamplePatientEntryForm form = new SamplePatientEntryForm();
        form.setUseReferral(useReferral);
        form.setReferralItems(items);
        return form;
    }

    private static List<ReferralItem> oneReferralItem() {
        List<ReferralItem> items = new ArrayList<>();
        ReferralItem item = new ReferralItem();
        item.setReferredInstituteId("ORG-1");
        item.setReferredTestId("TEST-1");
        items.add(item);
        return items;
    }
}
