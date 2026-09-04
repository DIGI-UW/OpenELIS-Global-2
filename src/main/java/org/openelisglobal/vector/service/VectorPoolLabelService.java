package org.openelisglobal.vector.service;

import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.vector.valueholder.VectorPool;
import org.springframework.security.access.prepost.PreAuthorize;

public interface VectorPoolLabelService {

    int MAX_LABEL_LENGTH = 64;
    int MAX_DECON_DEPTH = 4;

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    String intakeLotLabel(String accession, int oneBasedPosition);

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    String intakeLotBase(VectorPool intakePool, Sample sample);

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    String subPoolLabel(String parentBase, int position);
}
