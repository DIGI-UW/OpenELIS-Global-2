package org.openelisglobal.program.dao;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.program.valueholder.pathology.PathologyBlock;

/**
 * Reads and writes a single block, which the case as a whole does not cover.
 */
public interface PathologyBlockDAO extends BaseDAO<PathologyBlock, Integer> {
}
