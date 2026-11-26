package org.openelisglobal.dataexchange.order.daoimpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.Collections;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder.SortOrder;

@RunWith(MockitoJUnitRunner.class)
public class ElectronicOrderDAOImplTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Session session;

    @Mock
    private Query<ElectronicOrder> query;

    @InjectMocks
    private ElectronicOrderDAOImpl dao;

    private ElectronicOrder testOrder;

    @Before
    public void setUp() {
        testOrder = new ElectronicOrder();
        testOrder.setId("1");
        testOrder.setExternalId("EXT-001");

        when(entityManager.unwrap(Session.class)).thenReturn(session);
    }

    @Test
    public void getAllElectronicOrdersContainingValueOrderedBy_selectsTypedElectronicOrder() {
        ArgumentCaptor<String> hqlCaptor = ArgumentCaptor.forClass(String.class);
        when(session.createQuery(hqlCaptor.capture(), eq(ElectronicOrder.class))).thenReturn(query);
        when(query.list()).thenReturn(Collections.singletonList(testOrder));

        List<ElectronicOrder> result = dao.getAllElectronicOrdersContainingValueOrderedBy("searchValue",
                SortOrder.STATUS_ID);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("1", result.get(0).getId());

        String capturedHql = hqlCaptor.getValue();
        assertTrue("HQL must start with 'select eo from ElectronicOrder eo'",
                capturedHql.startsWith("select eo from ElectronicOrder eo"));
        assertTrue("HQL must order by statusId", capturedHql.contains("order by eo.statusId asc"));
        verify(query).setParameter("searchValue", "searchValue");
        verify(query).list();
    }

    @Test
    public void getAllElectronicOrdersContainingValueOrderedBy_handlesAllSortOrders() {
        ArgumentCaptor<String> hqlCaptor = ArgumentCaptor.forClass(String.class);
        when(session.createQuery(hqlCaptor.capture(), eq(ElectronicOrder.class))).thenReturn(query);
        when(query.list()).thenReturn(Collections.singletonList(testOrder));

        dao.getAllElectronicOrdersContainingValueOrderedBy("test", SortOrder.LAST_UPDATED_ASC);
        assertTrue(hqlCaptor.getValue().contains("eo.statusId asc, eo.lastupdated asc"));

        dao.getAllElectronicOrdersContainingValueOrderedBy("test", SortOrder.LAST_UPDATED_DESC);
        assertTrue(hqlCaptor.getValue().contains("eo.statusId asc, eo.lastupdated desc"));

        dao.getAllElectronicOrdersContainingValueOrderedBy("test", SortOrder.EXTERNAL_ID);
        assertTrue(hqlCaptor.getValue().contains("eo.externalId asc"));
    }

    @Test
    public void getAllElectronicOrdersMatchingAnyValue_selectsTypedElectronicOrder() {
        ArgumentCaptor<String> hqlCaptor = ArgumentCaptor.forClass(String.class);
        when(session.createQuery(hqlCaptor.capture(), eq(ElectronicOrder.class))).thenReturn(query);
        when(query.list()).thenReturn(Collections.singletonList(testOrder));

        List<String> identifierValues = Collections.singletonList("EXT-001");
        List<ElectronicOrder> result = dao.getAllElectronicOrdersMatchingAnyValue(identifierValues, "John",
                SortOrder.EXTERNAL_ID);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("EXT-001", result.get(0).getExternalId());

        String capturedHql = hqlCaptor.getValue();
        assertTrue("HQL must start with 'select eo from ElectronicOrder eo'",
                capturedHql.startsWith("select eo from ElectronicOrder eo"));
        assertTrue("HQL must order by externalId", capturedHql.contains("order by eo.externalId asc"));
        verify(query).setParameterList("identifierValues", identifierValues);
        verify(query).setParameter("patientValue", "John");
        verify(query).list();
    }

    @Test
    public void getElectronicOrdersContainingValueExludedByOrderedBy_selectsTypedElectronicOrder() {
        ArgumentCaptor<String> hqlCaptor = ArgumentCaptor.forClass(String.class);
        when(session.createQuery(hqlCaptor.capture(), eq(ElectronicOrder.class))).thenReturn(query);
        when(query.list()).thenReturn(Collections.singletonList(testOrder));

        List<String> excludedStatuses = Collections.singletonList("CANCELLED");
        List<ElectronicOrder> result = dao.getElectronicOrdersContainingValueExludedByOrderedBy("val", excludedStatuses,
                SortOrder.LAST_UPDATED_DESC);

        assertNotNull(result);
        assertEquals(1, result.size());

        String capturedHql = hqlCaptor.getValue();
        assertTrue("HQL must start with 'select eo from ElectronicOrder eo'",
                capturedHql.startsWith("select eo from ElectronicOrder eo"));
        assertTrue("HQL must order by lastupdated desc",
                capturedHql.contains("order by eo.statusId asc, eo.lastupdated desc"));
        verify(query).setParameter("searchValue", "val");
        verify(query).setParameter("excludedStatuses", excludedStatuses);
        verify(query).list();
    }

    @Test
    public void getAllElectronicOrdersContainingValuesOrderedBy_selectsTypedElectronicOrder() {
        ArgumentCaptor<String> hqlCaptor = ArgumentCaptor.forClass(String.class);
        when(session.createQuery(hqlCaptor.capture(), eq(ElectronicOrder.class))).thenReturn(query);
        when(query.list()).thenReturn(Collections.singletonList(testOrder));

        List<ElectronicOrder> result = dao.getAllElectronicOrdersContainingValuesOrderedBy("ACC-123", "Doe", "Jane",
                "F", SortOrder.STATUS_ID);

        assertNotNull(result);
        assertEquals(1, result.size());

        String capturedHql = hqlCaptor.getValue();
        assertTrue("HQL must start with 'select eo from ElectronicOrder eo'",
                capturedHql.startsWith("select eo from ElectronicOrder eo"));
        assertTrue("HQL must order by statusId", capturedHql.contains("order by eo.statusId asc"));
        verify(query).setParameter("accessionNumber", "ACC-123");
        verify(query).setParameter("patientLastName", "Doe");
        verify(query).setParameter("patientFirstName", "Jane");
        verify(query).setParameter("gender", "F");
        verify(query).list();
    }
}
