package org.openelisglobal.referral;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.referral.action.beanitems.ReferralItem;
import org.openelisglobal.referral.service.ReferralItemService;
import org.springframework.beans.factory.annotation.Autowired;

public class ReferralItemServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ReferralItemService referralItemService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/referral-item.xml");
    }

    @Test
    public void getReferralItems_ShouldReturnAllReferralItems() {
        List<ReferralItem> referralItems = referralItemService.getReferralItems();
        assertNotNull(referralItems);
        assertEquals(3, referralItems.size());
        assertEquals("101", referralItems.get(0).getReferralId());
        assertEquals("12345", referralItems.get(1).getAccessionNumber());
    }
}
