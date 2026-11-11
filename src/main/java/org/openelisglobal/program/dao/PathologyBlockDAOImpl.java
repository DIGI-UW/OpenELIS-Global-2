package org.openelisglobal.program.dao;

import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
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

    @Override
    @Transactional(readOnly = true)
    public Optional<PathologyBlock> getByBarcode(String barcode) {
        Query<PathologyBlock> query = entityManager.unwrap(Session.class)
                .createQuery("from PathologyBlock b where b.barcode = :barcode order by b.id", PathologyBlock.class);
        query.setParameter("barcode", barcode);
        query.setMaxResults(1);
        return query.uniqueResultOptional();
    }
}
