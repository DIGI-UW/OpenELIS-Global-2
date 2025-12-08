package org.openelisglobal.common.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.form.AdminOptionMenuForm;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.menu.service.AdminMenuItemService;
import org.openelisglobal.spring.util.SpringContext;

public abstract class BaseMenuController<T> extends BaseController {

    protected static final int PREVIOUS = 1;

    protected static final int NEXT = 2;

    protected static final int NONE = -1;

    protected int getPageSize() {
        return Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
    }

    protected String performMenuAction(AdminOptionMenuForm<T> form, HttpServletRequest request)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        String forward = FWD_SUCCESS;

        int action = -1;
        if (!StringUtil.isNullorNill(request.getParameter("paging"))) {
            action = Integer.parseInt(request.getParameter("paging"));
        }
        List<T> menuList = null;

        try {
            switch (action) {
            case PREVIOUS:
                menuList = doPreviousPage(form, request);
                break;
            case NEXT:
                menuList = doNextPage(form, request);
                break;
            default:
                menuList = doNone(form, request);
            }
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            forward = FWD_FAIL;
        }

        form.setMenuList(menuList);
        if (!isRestCall()) {
            form.setAdminMenuItems(SpringContext.getBean(AdminMenuItemService.class).getActiveItemsSorted());
        }
        request.setAttribute(DEACTIVATE_DISABLED, getDeactivateDisabled());
        request.setAttribute(ADD_DISABLED, getAddDisabled());
        request.setAttribute(EDIT_DISABLED, getEditDisabled());

        List<String> selectedIDs = new ArrayList<>();

        form.setSelectedIDs(selectedIDs);

        return forward;
    }

    protected List<T> doNextPage(AdminOptionMenuForm<T> form, HttpServletRequest request) {
        int startingRecNo = getCurrentStartingRecNo(request);

        LogEvent.logTrace("BaseMenuAction", "performAction()", "current start " + startingRecNo);
        int nextStartingRecNo = startingRecNo + getPageSize();

        LogEvent.logTrace("BaseMenuAction", "performAction()", "next start " + nextStartingRecNo);
        String stringNextStartingRecNo = String.valueOf(nextStartingRecNo);
        request.setAttribute("startingRecNo", stringNextStartingRecNo);

        List<T> nextPageList = createMenuList(form, request);

        request.setAttribute(PREVIOUS_DISABLED, "false");

        if (getPageSize() > 0 && nextPageList.size() > getPageSize()) {
            request.setAttribute(NEXT_DISABLED, "false");
            // chop off last record (this was only to indicate that there are
            // more records
            nextPageList = nextPageList.subList(0, getPageSize());
        } else {
            request.setAttribute(NEXT_DISABLED, "true");
        }

        return nextPageList;
    }

    protected List<T> doPreviousPage(AdminOptionMenuForm<T> form, HttpServletRequest request) {

        int startingRecNo = getCurrentStartingRecNo(request);

        int nextStartingRecNo = startingRecNo - getPageSize();
        String stringNextStartingRecNo = String.valueOf(nextStartingRecNo);
        request.setAttribute("startingRecNo", stringNextStartingRecNo);

        List<T> previousPageList = createMenuList(form, request);

        request.setAttribute(NEXT_DISABLED, "false");

        if (getPageSize() > 0 && previousPageList.size() > getPageSize()) {
            request.setAttribute(PREVIOUS_DISABLED, "false");
            // chop off last record (this was only to indicate that there are
            // more records
            previousPageList = previousPageList.subList(0, getPageSize());
        } else {
            request.setAttribute(PREVIOUS_DISABLED, "true");
        }

        if (nextStartingRecNo <= 1) {
            request.setAttribute(PREVIOUS_DISABLED, "true");
        }

        return previousPageList;
    }

    protected List<T> doNone(AdminOptionMenuForm<T> form, HttpServletRequest request) {

        int nextStartingRecNo = getCurrentStartingRecNo(request);
        String stringNextStartingRecNo = String.valueOf(nextStartingRecNo);
        request.setAttribute("startingRecNo", stringNextStartingRecNo);

        List<T> samePageList = createMenuList(form, request);

        // this is first page: don't enable previous button

        if (nextStartingRecNo <= 1) {
            request.setAttribute(PREVIOUS_DISABLED, "true");
        }

        if (getPageSize() > 0 && samePageList.size() > getPageSize()) {
            request.setAttribute(NEXT_DISABLED, "false");
            // chop off last record (this was only to indicate that there are more records
            samePageList = samePageList.subList(0, getPageSize());
        } else {
            request.setAttribute(NEXT_DISABLED, "true");
        }
        return samePageList;
    }

    protected int getCurrentStartingRecNo(HttpServletRequest request) {

        String stringStartingRecNo = "1";

        if (!StringUtil.isNullorNill((String) request.getAttribute("startingRecNo"))) {
            stringStartingRecNo = (String) request.getAttribute("startingRecNo");
        } else if (!StringUtil.isNullorNill(request.getParameter("startingRecNo"))) {
            stringStartingRecNo = request.getParameter("startingRecNo");
        }

        // Make sure it's a valid value
        int startingRecNo = Integer.parseInt(stringStartingRecNo);
        if (startingRecNo <= 0) {
            startingRecNo = 1;
        }
        return startingRecNo;
    }

    protected abstract List<T> createMenuList(AdminOptionMenuForm<T> form, HttpServletRequest request);

    protected abstract String getDeactivateDisabled();

    protected String getAddDisabled() {
        return "false";
    }

    protected String getEditDisabled() {
        return "false";
    }

    protected void setDisplayPageBounds(HttpServletRequest request, int listSize, int startingRecNo, int totalRecords)
            throws LIMSRuntimeException {
        request.setAttribute(MENU_TOTAL_RECORDS, String.valueOf(totalRecords));
        request.setAttribute(MENU_FROM_RECORD, String.valueOf(startingRecNo));

        int numOfRecs = 0;
        if (getPageSize() <= 0) {
            numOfRecs = listSize;
            numOfRecs--;
        } else if (listSize != 0) {
            numOfRecs = Math.min(listSize, getPageSize());
            numOfRecs--;
        }

        int endingRecNo = startingRecNo + numOfRecs;
        request.setAttribute(MENU_TO_RECORD, String.valueOf(endingRecNo));
    }
}
