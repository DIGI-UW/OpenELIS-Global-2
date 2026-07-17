package org.openelisglobal.externalconnections.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.controller.BaseMenuController;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.form.AdminOptionMenuForm;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.validator.BaseErrors;
import org.openelisglobal.externalconnections.form.ExternalConnectionMenuForm;
import org.openelisglobal.externalconnections.service.ExternalConnectionService;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest")
public class ExternalConnectionMenuRestController extends BaseMenuController<ExternalConnection> {

    private static final String[] ALLOWED_FIELDS = new String[] { "selectedIds*" };

    @Autowired
    private ExternalConnectionService externalConnectionService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @GetMapping(value = { "/ExternalConnectionMenu", "/SearchExternalConnectionMenu" })
    public ResponseEntity<Object> showExternalConnectionMenu(HttpServletRequest request)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        ExternalConnectionMenuForm form = new ExternalConnectionMenuForm();
        String forward = performMenuAction(form, request);
        if (FWD_FAIL.equals(forward)) {
            Errors errors = new BaseErrors();
            errors.reject("error.generic");
            return ResponseEntity.badRequest().body(errors.getAllErrors());
        } else {
            addFlashMsgsToRequest(request);
            return ResponseEntity.ok(form);
        }
    }

    @PostMapping(value = "/DeactivateExternalConnection")
    public ResponseEntity<Object> deactivateExternalConnection(HttpServletRequest request,
            @RequestParam(value = ID, required = false) String id) {
        if (id == null || id.isEmpty()) {
            return ResponseEntity.badRequest().body("No ID provided.");
        }
        String[] IDs = id.split(",");
        try {
            for (String connId : IDs) {
                ExternalConnection connection = externalConnectionService.get(Integer.valueOf(connId.trim()));
                if (connection != null) {
                    connection.setActive(false);
                    connection.setSysUserId(getSysUserId(request));
                    externalConnectionService.update(connection);
                }
            }
            return ResponseEntity.ok("External connection(s) deactivated successfully.");
        } catch (LIMSRuntimeException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to deactivate external connection(s).");
        }
    }

    @Override
    protected List<ExternalConnection> createMenuList(AdminOptionMenuForm<ExternalConnection> form,
            HttpServletRequest request) throws LIMSRuntimeException {

        int startingRecNo = this.getCurrentStartingRecNo(request);

        List<ExternalConnection> connections;
        String searchString = request.getParameter("searchString");
        if (YES.equals(request.getParameter("search")) && searchString != null && !searchString.isEmpty()) {
            connections = externalConnectionService.getAll();
            String lowerSearch = searchString.toLowerCase();
            connections = connections.stream().filter(c -> {
                String name = c.getNameLocalization() != null ? c.getNameLocalization().getLocalizedValue() : "";
                String pc = c.getProgrammedConnection() != null ? c.getProgrammedConnection().getValue() : "";
                return (name != null && name.toLowerCase().contains(lowerSearch))
                        || pc.toLowerCase().contains(lowerSearch);
            }).collect(java.util.stream.Collectors.toList());
        } else {
            connections = externalConnectionService.getAll();
        }

        if (connections == null) {
            connections = new ArrayList<>();
        }

        int totalCount = connections.size();

        request.setAttribute(MENU_TOTAL_RECORDS, String.valueOf(totalCount));
        request.setAttribute(MENU_FROM_RECORD, String.valueOf(startingRecNo));
        int pageSize = getPageSize();
        int endingRecNo = Math.min(startingRecNo + pageSize - 1, totalCount);
        request.setAttribute(MENU_TO_RECORD, String.valueOf(endingRecNo));

        form.setFromRecordCount(String.valueOf(startingRecNo));
        form.setToRecordCount(String.valueOf(endingRecNo));
        form.setTotalRecordCount(String.valueOf(totalCount));

        return connections;
    }

    @Override
    protected String getDeactivateDisabled() {
        return "false";
    }

    @Override
    protected int getPageSize() {
        return Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "externalConnectionsMasterListsPageDefinition";
        } else if (FWD_FAIL.equals(forward)) {
            return "redirect:/MasterListsPage";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        return "externalConnections.browse.title";
    }

    @Override
    protected String getPageSubtitleKey() {
        return "externalConnections.browse.title";
    }
}
