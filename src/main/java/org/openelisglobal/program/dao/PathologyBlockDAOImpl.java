package org.openelisglobal.program.dao;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.program.valueholder.pathology.PathologyBlock;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads and writes a single block, which the case as a whole does not cover.
 */
@Component
@Transactional
public class PathologyBlockDAOImpl extends BaseDAOImpl<PathologyBlock, Integer> implements PathologyBlockDAO {

    PathologyBlockDAOImpl() {
        super(PathologyBlock.class);
    }
}
