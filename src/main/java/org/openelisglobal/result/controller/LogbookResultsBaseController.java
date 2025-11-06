package org.openelisglobal.result.controller;

import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.internationalization.MessageUtil;

public abstract class LogbookResultsBaseController extends BaseController {

    private String titleKey = "";

    @Override
    protected String getPageTitleKey() {
        return titleKey;
    }

    @Override
    protected String getPageSubtitleKey() {
        return titleKey;
    }

    @Override
    protected String getMessageForKey(String messageKey) {
        return MessageUtil.getMessage("results.title", messageKey);
    }

    protected void setRequestType(String section) {
        if (!GenericValidator.isBlankOrNull(section)) {
            titleKey = section;
        }
    }
}
