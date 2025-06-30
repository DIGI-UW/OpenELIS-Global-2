package org.openelisglobal.audittrail.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.audittrail.action.workers.AuditTrailItem;
import org.openelisglobal.audittrail.action.workers.AuditTrailViewWorker;
import org.openelisglobal.audittrail.form.AuditTrailViewForm;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class AuditTrailReportController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "accessionNumberSearch" };

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = "/AuditTrailReport", method = RequestMethod.GET)
    public ModelAndView showAuditTrailReport(HttpServletRequest request,
            @ModelAttribute("form") @Valid AuditTrailViewForm form)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        form.setFormMethod(RequestMethod.GET);

        String accessionNumber = form.getAccessionNumberSearch();
        if (!GenericValidator.isBlankOrNull(accessionNumber)) {
            AuditTrailViewWorker worker = SpringContext.getBean(AuditTrailViewWorker.class);
            worker.setAccessionNumber(accessionNumber);
            List<AuditTrailItem> items = worker.getAuditTrail();
            form.setLog(items);
            form.setAccessionNumber(accessionNumber);
            form.setSampleOrderItems(worker.getSampleOrderSnapshot());
            form.setPatientProperties(worker.getPatientSnapshot());
        }

        return findForward(FWD_SUCCESS, form);
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "auditTrailViewDefinition";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        return "reports.auditTrail";
    }

    @Override
    protected String getPageSubtitleKey() {
        return "reports.auditTrail";
    }
}
