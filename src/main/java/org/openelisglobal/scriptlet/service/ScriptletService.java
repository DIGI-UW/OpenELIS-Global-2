package org.openelisglobal.scriptlet.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.scriptlet.valueholder.Scriptlet;

public interface ScriptletService extends BaseObjectService<Scriptlet, String> {
    void getData(Scriptlet scriptlet);

    Integer getTotalScriptletCount();

    List<Scriptlet> getPageOfScriptlets(int startingRecNo);

    Scriptlet getScriptletByName(Scriptlet scriptlet);

    Scriptlet getScriptletById(String scriptletId);

    List<Scriptlet> getScriptlets(String filter);

    List<Scriptlet> getAllScriptlets();
}
