package org.openelisglobal.resultvalidation.controller;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.qc.service.QcHoldService;
import org.openelisglobal.resultvalidation.bean.AnalysisItem;
import org.openelisglobal.spring.util.SpringContext;

public abstract class BaseResultValidationController extends BaseController {

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
        return MessageUtil.getMessage("validation.title", messageKey);
    }

    protected void setRequestType(String section) {
        if (!GenericValidator.isBlankOrNull(section)) {
            titleKey = section;
        }
    }

    /** The analysis ids named by a validation list, nulls dropped. */
    protected List<String> analysisIdsOf(List<AnalysisItem> items) {
        return items.stream().map(AnalysisItem::getAnalysisId).filter(Objects::nonNull).toList();
    }

    /**
     * Of the analyses in a validation list, the ones whose release is withheld by
     * an open QC failure. Every screen that finalizes analyses asks here, so none
     * of them can become a sidestep around the block the lab opted into.
     *
     * <p>
     * Re-resolved from the database rather than trusted from the submitted rows:
     * the hold is a safety control, and a client could otherwise clear it by
     * posting {@code qcHold=false}.
     */
    protected Set<String> analysisIdsBlockedFromRelease(List<AnalysisItem> items) {
        return SpringContext.getBean(QcHoldService.class).analysisIdsBlockedFromRelease(analysisIdsOf(items));
    }
}
