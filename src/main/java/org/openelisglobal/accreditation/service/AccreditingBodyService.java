package org.openelisglobal.accreditation.service;

import java.util.List;
import org.openelisglobal.accreditation.dto.AccreditationSummary;
import org.openelisglobal.accreditation.dto.AccreditingBodyView;
import org.openelisglobal.accreditation.valueholder.AccreditingBody;
import org.openelisglobal.common.service.BaseObjectService;

public interface AccreditingBodyService extends BaseObjectService<AccreditingBody, Long> {

    /** All bodies in report-logo order, with enrolled counts and derived status. */
    List<AccreditingBodyView> getBodyViews();

    /**
     * Portfolio counts + active body names, for the page banner and QA Overview.
     */
    AccreditationSummary getSummary();

    /** Create a body. Rejects a duplicate or malformed code. */
    AccreditingBody createBody(AccreditingBody body, String sysUserId);

    /**
     * Update a body. {@code code} is immutable — supplying a different one is
     * rejected rather than silently ignored.
     */
    AccreditingBody updateBody(Long id, AccreditingBody incoming, String sysUserId);

    /** Delete a body. Rejected while any test is still enrolled under it. */
    void deleteBody(Long id, String sysUserId);

    /** Attach (or clear, with a null image id) this body's report logo. */
    AccreditingBody setLogo(Long id, String logoImageId, String sysUserId);
}
