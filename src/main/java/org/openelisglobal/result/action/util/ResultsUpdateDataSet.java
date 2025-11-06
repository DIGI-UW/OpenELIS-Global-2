/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
 */

package org.openelisglobal.result.action.util;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IResultSaveService;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.referral.valueholder.ReferralSet;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.springframework.validation.Errors;

/** */
public class ResultsUpdateDataSet implements IResultSaveService {
    private List<TestResultItem> modifiedItems = new ArrayList<>();
    private List<ResultSet> modifiedResults = new ArrayList<>();
    private List<TestResultItem> analysisOnlyChangeResults = new ArrayList<>();
    private List<ResultSet> newResults = new ArrayList<>();
    private List<Analysis> modifiedAnalysis = new ArrayList<>();
    private List<Result> deletableResults = new ArrayList<>();
    private List<ReferralSet> savableReferralSets = new ArrayList<>();
    private List<String> referredAnalysisIds = new ArrayList<>();
    private Analysis previousAnalysis = new Analysis();
    private ResultsValidation resultValidation = SpringContext.getBean(ResultsValidation.class);
    private List<Note> noteList = new ArrayList<>();

    private final String currentUserId;

    public ResultsUpdateDataSet(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public List<TestResultItem> getModifiedItems() {
        return modifiedItems;
    }

    @Override
    public List<ResultSet> getModifiedResults() {
        return modifiedResults;
    }

    public void setModifiedResults(List<ResultSet> modifiedResults) {
        this.modifiedResults = modifiedResults;
    }

    @Override
    public String getCurrentUserId() {
        return currentUserId;
    }

    @Override
    public List<ResultSet> getNewResults() {
        return newResults;
    }

    public List<TestResultItem> getAnalysisOnlyChangeResults() {
        return analysisOnlyChangeResults;
    }

    public List<Analysis> getModifiedAnalysis() {
        return modifiedAnalysis;
    }

    public List<Result> getDeletableResults() {
        return deletableResults;
    }

    public List<ReferralSet> getSavableReferralSets() {
        return savableReferralSets;
    }

    public List<String> getReferredAnalysisIds() {
        return referredAnalysisIds;
    }

    public Analysis getPreviousAnalysis() {
        return previousAnalysis;
    }

    public List<Note> getNoteList() {
        return noteList;
    }

    public void addToNoteList(Note note) {
        if (note != null) {
            noteList.add(note);
        }
    }

    public void filterModifiedItems(List<TestResultItem> allItems) {
        for (TestResultItem item : allItems) {
            if (isUpdated(item)) {
                modifiedItems.add(item);
            } else if (item.getIsModified()) {
                // this covers cases such as test date change or test method change w/o data
                // update
                analysisOnlyChangeResults.add(item);
            }
        }
    }

    public Errors validateModifiedItems() {
        return resultValidation.validateModifiedItems(getModifiedItems());
    }

    private boolean isUpdated(TestResultItem item) {
        return item.getIsModified() && (ResultUtil.areResults(item) || ResultUtil.areNotes(item)
                || ResultUtil.isReferred(item) || ResultUtil.isForcedToAcceptance(item) || ResultUtil.isRejected(item));
    }

    public void setPreviousAnalysis(Analysis previousAnalysis) {
        this.previousAnalysis = previousAnalysis;
    }
}
