package org.openelisglobal.scriptlet.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.scriptlet.valueholder.Scriptlet;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ScriptletService extends BaseObjectService<Scriptlet, String> {

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    void getData(Scriptlet scriptlet);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    Integer getTotalScriptletCount();

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    List<Scriptlet> getPageOfScriptlets(int startingRecNo);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    Scriptlet getScriptletByName(Scriptlet scriptlet);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    Scriptlet getScriptletById(String scriptletId);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    List<Scriptlet> getScriptlets(String filter);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    List<Scriptlet> getAllScriptlets();
}
