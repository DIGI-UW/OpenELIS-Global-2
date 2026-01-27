package org.openelisglobal.analyzerimport.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerimport.action.beans.NamedAnalyzerTestMapping;
import org.openelisglobal.analyzerimport.form.AnalyzerTestNameMenuForm;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.analyzerimport.util.AnalyzerTestNameCache;
import org.openelisglobal.analyzerimport.util.MappedTestName;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.controller.BaseMenuController;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.form.AdminOptionMenuForm;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.validator.BaseErrors;
import org.openelisglobal.internationalization.MessageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@RestController
@RequestMapping("/rest")
public class AnalyzerTestNameMenuRestController extends BaseMenuController<NamedAnalyzerTestMapping> {

    private static final String[] ALLOWED_FIELDS = new String[] { "selectedIDs*" };

    @Autowired
    private AnalyzerTestMappingService analyzerTestMappingService;
    @Autowired
    private AnalyzerService analyzerService;

    private static final int ANALYZER_NAME = 0;
    private static final int ANALYZER_TEST = 1;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @GetMapping(value = "/AnalyzerTestNameMenu", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> showAnalyzerTestNameMenu(HttpServletRequest request)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        AnalyzerTestNameMenuForm form = new AnalyzerTestNameMenuForm();

        addFlashMsgsToRequest(request);

        String forward = performMenuAction(form, request);
        if (FWD_FAIL.equals(forward)) {
            Errors errors = new BaseErrors();
            errors.reject("error.generic");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
        } else {
            return ResponseEntity.ok(form);
        }
    }

    @Override
    protected List<NamedAnalyzerTestMapping> createMenuList(AdminOptionMenuForm<NamedAnalyzerTestMapping> form,
            HttpServletRequest request) {

        request.setAttribute("menuDefinition", "AnalyzerTestNameMenuDefinition");

        String stringStartingRecNo = (String) request.getAttribute("startingRecNo");
        int startingRecNo = 0;
        if (stringStartingRecNo != null) {
            startingRecNo = Integer.parseInt(stringStartingRecNo);
            if (startingRecNo < 0) {
                startingRecNo = 0;
            }
        }

        String analyzerId = request.getParameter("analyzerId");
        List<NamedAnalyzerTestMapping> mappedTestNameList = new ArrayList<>();
        List<String> analyzerList = new ArrayList<>();
        if (StringUtils.isNotBlank(analyzerId)) {
            Analyzer analyzer = analyzerService.get(analyzerId);
            if (analyzer != null) {
                analyzerList.add(analyzer.getName());
            }
        } else {
            analyzerList = AnalyzerTestNameCache.getInstance().getAnalyzerNames();
        }

        Analyzer analyzer = new Analyzer();

        for (String analyzerName : analyzerList) {
            Collection<MappedTestName> mappedTestNames = AnalyzerTestNameCache.getInstance()
                    .getMappedTestsForAnalyzer(analyzerName).values();
            if (mappedTestNames.size() > 0) {
                analyzer.setId(((MappedTestName) mappedTestNames.toArray()[0]).getAnalyzerId());
                analyzer = analyzerService.get(analyzer.getId());
                mappedTestNameList.addAll(convertedToNamedList(mappedTestNames, analyzer.getName()));
            }
        }

        setDisplayPageBounds(request, mappedTestNameList.size(), startingRecNo, form);

        return mappedTestNameList.subList(Math.min(mappedTestNameList.size(), startingRecNo - 1),
                Math.min(mappedTestNameList.size(), startingRecNo + getPageSize()));

        // return mappedTestNameList;
    }

    private List<NamedAnalyzerTestMapping> convertedToNamedList(Collection<MappedTestName> mappedTestNameList,
            String analyzerName) {
        List<NamedAnalyzerTestMapping> namedMappingList = new ArrayList<>();

        for (MappedTestName test : mappedTestNameList) {
            NamedAnalyzerTestMapping namedMapping = new NamedAnalyzerTestMapping();
            namedMapping.setActualTestName(test.getOpenElisTestName());
            namedMapping.setAnalyzerTestName(test.getAnalyzerTestName());
            namedMapping.setAnalyzerName(analyzerName);

            namedMappingList.add(namedMapping);
        }

        return namedMappingList;
    }

    private void setDisplayPageBounds(HttpServletRequest request, int listSize, int startingRecNo,
            AdminOptionMenuForm<NamedAnalyzerTestMapping> form) throws LIMSRuntimeException {
        request.setAttribute(MENU_TOTAL_RECORDS, String.valueOf(listSize));
        request.setAttribute(MENU_FROM_RECORD, String.valueOf(startingRecNo));

        int numOfRecs = 0;
        if (listSize != 0) {
            numOfRecs = Math.min(listSize, getPageSize());

            numOfRecs--;
        }

        int endingRecNo = startingRecNo + numOfRecs;
        request.setAttribute(MENU_TO_RECORD, String.valueOf(endingRecNo));
        form.setToRecordCount(String.valueOf(endingRecNo));
        form.setFromRecordCount(String.valueOf(startingRecNo));
        form.setTotalRecordCount(String.valueOf(listSize));
    }

    @Override
    protected String getDeactivateDisabled() {
        return "false";
    }

    @Override
    protected String getEditDisabled() {
        return "true";
    }

    @PostMapping(value = "/DeleteAnalyzerTestName", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> showDeleteAnalyzerTestName(HttpServletRequest request,
            @RequestBody @Valid AnalyzerTestNameMenuForm form, BindingResult result,
            RedirectAttributes redirectAttributes)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        if (result.hasErrors()) {
            saveErrors(result);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }

        List<String> selectedIDs = form.getSelectedIDs();

        // String sysUserId = getSysUserId(request);
        List<AnalyzerTestMapping> testMappingList = new ArrayList<>();

        for (int i = 0; i < selectedIDs.size(); i++) {
            String[] ids = selectedIDs.get(i).split(NamedAnalyzerTestMapping.getUniqueIdSeperator());
            AnalyzerTestMapping testMapping = new AnalyzerTestMapping();
            testMapping.setAnalyzerId(AnalyzerTestNameCache.getInstance().getAnalyzerIdForName(ids[ANALYZER_NAME]));
            testMapping.setAnalyzerTestName(ids[ANALYZER_TEST]);
            testMapping.setSysUserId(getSysUserId(request));
            testMappingList.add(testMapping);
            try {
                analyzerTestMappingService.delete(testMapping);
            } catch (LIMSRuntimeException e) {
                LogEvent.logDebug(e);
                saveErrors(result);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
        }

        AnalyzerTestNameCache.getInstance().reloadCache();
        request.setAttribute("menuDefinition", "AnalyzerTestNameDefinition");
        redirectAttributes.addFlashAttribute(Constants.SUCCESS_MSG, MessageUtil.getMessage("message.success.delete"));
        return ResponseEntity.ok(form);
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "analyzerMasterListsPageDefinition";
        } else if (FWD_FAIL.equals(forward)) {
            return "redirect:/MasterListsPage";
        } else if (FWD_SUCCESS_DELETE.equals(forward)) {
            return "redirect:/AnalyzerTestNameMenu";
        } else if (FWD_FAIL_DELETE.equals(forward)) {
            return "analyzerMasterListsPageDefinition";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        return "analyzerTestName.browse.title";
    }

    @Override
    protected String getPageSubtitleKey() {
        return "analyzerTestName.browse.title";
    }
}
