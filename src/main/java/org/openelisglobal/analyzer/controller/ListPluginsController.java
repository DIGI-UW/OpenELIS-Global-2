package org.openelisglobal.analyzer.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.openelisglobal.analyzer.form.ListPluginForm;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.plugin.PluginLoader;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ListPluginsController extends BaseController {

    // form isn't submitted back
    private static final String[] ALLOWED_FIELDS = new String[] {};

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = "/ListPlugins", method = RequestMethod.GET)
    public ModelAndView showListPlugins(HttpServletRequest request) {
        ListPluginForm form = new ListPluginForm();

        List<String> pluginNames = PluginLoader.getCurrentPlugins();

        if (pluginNames.isEmpty()) {
            pluginNames.add(MessageUtil.getContextualMessage("plugin.no.plugins"));
        }
        form.setPluginList(pluginNames);

        return findForward(FWD_SUCCESS, form);
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "ListPluginsPageDefinition";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        return "plugin.installed.plugins";
    }

    @Override
    protected String getPageSubtitleKey() {
        return "plugin.installed.plugins";
    }
}
