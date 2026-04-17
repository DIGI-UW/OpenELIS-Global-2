package org.openelisglobal.program.service;

import org.openelisglobal.program.valueholder.OrderProgramDisplayItem;
import org.springframework.security.access.prepost.PreAuthorize;

public interface OrderProgramsDisplayService {

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    OrderProgramDisplayItem getOrderProgramById(Integer programSampleId);
}
