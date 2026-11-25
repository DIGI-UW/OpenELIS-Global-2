package org.openelisglobal.program.dao;

import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
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

    @Override
    @Transactional(readOnly = true)
    public Optional<PathologySlide> getByBarcode(String barcode) {
        Query<PathologySlide> query = entityManager.unwrap(Session.class)
                .createQuery("from PathologySlide s where s.barcode = :barcode order by s.id", PathologySlide.class);
        query.setParameter("barcode", barcode);
        query.setMaxResults(1);
        return query.uniqueResultOptional();
    }
}
