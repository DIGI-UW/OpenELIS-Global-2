package org.openelisglobal.resultvalidation.util;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.services.IResultSaveService;
import org.openelisglobal.result.action.util.ResultSet;

public class ResultValidationSaveService implements IResultSaveService {

    private String currentUserId;
    private List<ResultSet> newResults = new ArrayList<>();
    private List<ResultSet> modifiedResults = new ArrayList<>();

    @Override
    public String getCurrentUserId() {
        return currentUserId;
    }

    @Override
    public List<ResultSet> getNewResults() {
        return newResults;
    }

    @Override
    public List<ResultSet> getModifiedResults() {
        return modifiedResults;
    }

    public void setNewResults(List<ResultSet> newResults) {
        this.newResults = newResults;
    }

    public void setModifiedResults(List<ResultSet> modifiedResults) {
        this.modifiedResults = modifiedResults;
    }

    public void addNewResultSet(ResultSet resultSet) {
        newResults.add(resultSet);
    }

    public void addModifiedResultSet(ResultSet resultSet) {
        modifiedResults.add(resultSet);
    }
}
