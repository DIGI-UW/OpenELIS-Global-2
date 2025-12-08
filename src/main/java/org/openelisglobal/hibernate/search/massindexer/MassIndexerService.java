package org.openelisglobal.hibernate.search.massindexer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.stereotype.Service;

@Service
public class MassIndexerService {
    @PersistenceContext
    EntityManager entityManager;

    // parameters to allow tuning the MassIndexer for optimal performance

    private int idFetchSize = 100;

    private int batchSizeToLoadObjects = 10;

    private int threadsToLoadObjects = 6;

    @Transactional
    public void reindex() throws Exception {
        SearchSession searchSession = Search.session(entityManager);
        MassIndexer indexer = searchSession.massIndexer();
        indexer.idFetchSize(idFetchSize).batchSizeToLoadObjects(batchSizeToLoadObjects)
                .threadsToLoadObjects(threadsToLoadObjects).startAndWait();
    }
}
