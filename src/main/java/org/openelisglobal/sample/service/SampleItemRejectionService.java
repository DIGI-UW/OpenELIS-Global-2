package org.openelisglobal.sample.service;

import org.springframework.security.access.prepost.PreAuthorize;

public interface SampleItemRejectionService {

    @PreAuthorize("hasAuthority('PRIV_SAMPLE_STATUS_VIEW')")
    void reject(String sampleItemId, String reason, String authenticatedUserId);
}
