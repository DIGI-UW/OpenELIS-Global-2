package org.openelisglobal.program.dao;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.program.valueholder.pathology.PathologySlide;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads and writes a single slide, which the case as a whole does not cover.
 */
@Component
@Transactional
public class PathologySlideDAOImpl extends BaseDAOImpl<PathologySlide, Integer> implements PathologySlideDAO {

    PathologySlideDAOImpl() {
        super(PathologySlide.class);
    }
}
