package org.openelisglobal.externalconnections.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.controller.BaseMenuController;
import org.openelisglobal.common.form.AdminOptionMenuForm;
import org.openelisglobal.common.validator.BaseErrors;
import org.openelisglobal.externalconnections.form.ExternalConnectionMenuForm;
import org.openelisglobal.externalconnections.service.ExternalConnectionService;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ExternalConnectionMenuController extends BaseMenuController<ExternalConnection> {

    private static final String[] ALLOWED_FIELDS = new String[] { "selectedIds*" };

    @Autowired
    private ExternalConnectionService externalConnectionService;

    private static final String TITLE_KEY = "titleKey";

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @GetMapping(value = { "/ExternalConnectionsMenu" })
    public ModelAndView showSiteInformationMenu(HttpServletRequest request, RedirectAttributes redirectAttributes)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        ExternalConnectionMenuForm form = new ExternalConnectionMenuForm();
        String forward = performMenuAction(form, request);
        if (FWD_FAIL.equals(forward)) {
            Errors errors = new BaseErrors();
            errors.reject("error.generic");
            redirectAttributes.addFlashAttribute(Constants.REQUEST_ERRORS, errors);
            return findForward(FWD_FAIL, form);
        } else {
            addFlashMsgsToRequest(request);
            return findForward(forward, form);
        }
    }

    @Override
    protected List<ExternalConnection> createMenuList(AdminOptionMenuForm<ExternalConnection> form,
            HttpServletRequest request) {
        List<ExternalConnection> connections;

        request.setAttribute(TITLE_KEY, "externalConnections.browse.title");

        int startingRecNo = Integer.parseInt((String) request.getAttribute("startingRecNo"));

        request.setAttribute("menuDefinition", "ExternalConnectionMenuDefinition");

        connections = externalConnectionService.getAll();
        setDisplayPageBounds(request, connections == null ? 0 : connections.size(), startingRecNo,
                externalConnectionService.getCount());

        return connections;
    }

    @Override
    protected String getDeactivateDisabled() {
        return "true";
    }

    @Override
    protected String getAddDisabled() {
        return "false";
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "externalConnectionsMasterListsPageDefinition";
        } else if (FWD_FAIL.equals(forward)) {
            return "redirect:/MasterListsPage";
        } else if (FWD_SUCCESS_DELETE.equals(forward)) {
            return "redirect:/MasterListsPage";
        } else if (FWD_FAIL_DELETE.equals(forward)) {
            return "redirect:/MasterListsPage";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        return (String) request.getAttribute(TITLE_KEY);
    }

    @Override
    protected String getPageSubtitleKey() {
        return (String) request.getAttribute(TITLE_KEY);
    }
}
