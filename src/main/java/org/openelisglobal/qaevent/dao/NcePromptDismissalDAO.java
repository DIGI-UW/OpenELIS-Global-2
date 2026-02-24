package org.openelisglobal.qaevent.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.qaevent.valueholder.NcePromptDismissal;

/**
 * DAO interface for prompt dismissal audit records.
 */
public interface NcePromptDismissalDAO extends BaseDAO<NcePromptDismissal, Integer> {

    List<NcePromptDismissal> getByResultId(String resultId);
}
