package org.openelisglobal.biorepository.daoimpl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.Collections;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.biorepository.dao.BioSampleRetrievalSearchCriteria;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BioSample.WorkflowStatus;

@RunWith(MockitoJUnitRunner.class)
public class BioSampleDAOSearchForRetrievalTest {

    @InjectMocks
    private BioSampleDAOImpl bioSampleDAO;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Session session;

    @Mock
    private Query<BioSample> query;

    @Test
    public void searchForRetrieval_bindsWorkflowStatusAsStringName() {
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(BioSample.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.setMaxResults(anyInt())).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        BioSampleRetrievalSearchCriteria criteria = new BioSampleRetrievalSearchCriteria();
        criteria.setWorkflowStatus(WorkflowStatus.STORED);

        bioSampleDAO.searchForRetrieval(criteria);

        verify(query).setParameter(eq("workflowStatus"), eq("STORED"));
    }
}
