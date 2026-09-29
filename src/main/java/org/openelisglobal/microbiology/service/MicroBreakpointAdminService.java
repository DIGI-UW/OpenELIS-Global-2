package org.openelisglobal.microbiology.service;

import java.sql.Date;
import org.openelisglobal.microbiology.form.MicroBreakpointRuleAdminForm;
import org.openelisglobal.microbiology.form.MicroBreakpointStandardAdminForm;
import org.openelisglobal.microbiology.form.MicroReferenceAdminPageForm;
import org.openelisglobal.microbiology.form.MicroReferenceAdminQueryForm;
import org.springframework.security.access.prepost.PreAuthorize;

public interface MicroBreakpointAdminService {

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    MicroReferenceAdminPageForm<MicroBreakpointStandardAdminForm> getStandards(MicroReferenceAdminQueryForm query);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    MicroBreakpointStandardAdminForm getStandard(String standardId);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    MicroReferenceAdminPageForm<MicroBreakpointRuleAdminForm> getRules(String standardId,
            MicroReferenceAdminQueryForm query);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    MicroBreakpointRuleAdminForm getRule(String standardId, String ruleId);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    MicroBreakpointRuleAdminForm saveRule(String standardId, String ruleId, MicroBreakpointRuleAdminForm request,
            String actorId);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    void activate(String standardId, Date effectiveDate, String actorId);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    void archive(String standardId, String actorId);
}
