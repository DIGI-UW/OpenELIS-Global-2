package org.openelisglobal.result.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.result.valueholder.ResultInventory;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
public interface ResultInventoryService extends BaseObjectService<ResultInventory, String> {
    void getData(ResultInventory resultInventory);

    ResultInventory getResultInventoryById(ResultInventory resultInventory);

    List<ResultInventory> getAllResultInventoryss();

    List<ResultInventory> getResultInventorysByResult(Result result);
}
