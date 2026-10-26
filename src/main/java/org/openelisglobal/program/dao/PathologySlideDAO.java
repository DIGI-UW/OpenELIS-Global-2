package org.openelisglobal.program.dao;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.program.valueholder.pathology.PathologySlide;

/**
 * Reads and writes a single slide, which the case as a whole does not cover.
 */
public interface PathologySlideDAO extends BaseDAO<PathologySlide, Integer> {
}
