package org.openelisglobal.microbiology.service;

import java.sql.Date;
import org.openelisglobal.microbiology.form.MicroBreakpointRuleAdminForm;
import org.openelisglobal.microbiology.form.MicroBreakpointStandardAdminForm;
import org.openelisglobal.microbiology.form.MicroReferenceAdminPageForm;
import org.openelisglobal.microbiology.form.MicroReferenceAdminQueryForm;

public interface MicroBreakpointAdminService {

    MicroReferenceAdminPageForm<MicroBreakpointStandardAdminForm> getStandards(MicroReferenceAdminQueryForm query);

    MicroReferenceAdminPageForm<MicroBreakpointRuleAdminForm> getRules(String standardId,
            MicroReferenceAdminQueryForm query);

    MicroBreakpointRuleAdminForm saveRule(String standardId, String ruleId, MicroBreakpointRuleAdminForm request,
            String actorId);

    void activate(String standardId, Date effectiveDate, String actorId);

    void archive(String standardId, String actorId);
}
