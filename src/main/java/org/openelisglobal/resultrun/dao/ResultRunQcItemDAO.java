package org.openelisglobal.resultrun.dao;

import java.util.Collection;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.resultrun.valueholder.ResultRunQcItem;

public interface ResultRunQcItemDAO extends BaseDAO<ResultRunQcItem, Long> {

    /** The blanks and duplicates recorded on runs, oldest first. */
    List<ResultRunQcItem> getItems(Collection<Long> runIds);

    /** The duplicates of these analyses, on any run. */
    List<ResultRunQcItem> getDuplicatesOfAnalyses(Collection<String> parentAnalysisIds);
}
