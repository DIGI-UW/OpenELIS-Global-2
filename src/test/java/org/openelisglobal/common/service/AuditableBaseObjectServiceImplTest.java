package org.openelisglobal.common.service;

import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.security.WithDaemonUser;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuditableBaseObjectServiceImplTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SiteInformationService siteInformationService;

    @After
    public void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test(expected = LIMSRuntimeException.class)
    public void insert_noContextAndMissingSysUserId_throws() {
        SecurityContextHolder.clearContext();
        SiteInformation si = createTestSiteInfo();
        si.setSysUserId(null);
        siteInformationService.insert(si);
    }

    @Test
    @WithDaemonUser
    public void insert_withDaemonContext_succeeds() {
        SiteInformation si = createTestSiteInfo();
        String id = siteInformationService.insert(si);
        assertNotNull(id);
    }

    @Test
    public void insert_withHumanContext_succeeds() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin", "pass",
                AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
        SiteInformation si = createTestSiteInfo();
        si.setSysUserId("1");
        String id = siteInformationService.insert(si);
        assertNotNull(id);
    }

    private SiteInformation createTestSiteInfo() {
        SiteInformation si = new SiteInformation();
        si.setName("test_phase4_" + System.nanoTime());
        si.setValue("test");
        si.setDescription("Phase 4 test");
        si.setValueType("text");
        si.setSysUserId("1");
        return si;
    }
}
