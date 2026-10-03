package org.openelisglobal.qaevent.service;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.qaevent.dao.NceCategoryDAO;
import org.openelisglobal.qaevent.dao.NceTypeDAO;
import org.openelisglobal.qaevent.valueholder.NceCategory;
import org.openelisglobal.qaevent.valueholder.NceType;
import org.springframework.test.util.ReflectionTestUtils;

public class NceHistoricalCatalogLabelsTest {
    @Test
    public void inactiveCategoryLabelsRemainVisibleButCannotBeSelectedForNewEvents() {
        NceCategory row = new NceCategory();
        row.setId(1);
        row.setName("Historical category");
        row.setActive(false);
        NceCategoryDAO dao = mock(NceCategoryDAO.class);
        when(dao.getAllNceCategory()).thenReturn(List.of(row));
        NceCategoryServiceImpl service = new NceCategoryServiceImpl();
        ReflectionTestUtils.setField(service, "baseObjectDAO", dao);
        assertEquals("Historical category", service.getAllCategoriesAsIdValuePairs().get(0).getValue());
        assertTrue(service.getActiveCategoriesAsIdValuePairs().isEmpty());
    }

    @Test
    public void inactiveTypeLabelsRemainVisibleButCannotBeSelectedForNewEvents() {
        NceType row = new NceType();
        row.setId(1);
        row.setName("Historical type");
        row.setActive(false);
        NceTypeDAO dao = mock(NceTypeDAO.class);
        when(dao.getAllNceType()).thenReturn(List.of(row));
        NceTypeServiceImpl service = new NceTypeServiceImpl();
        ReflectionTestUtils.setField(service, "baseObjectDAO", dao);
        assertEquals("Historical type", service.getAllTypesAsIdValuePairs().get(0).getValue());
        assertTrue(service.getActiveTypesAsIdValuePairs().isEmpty());
    }
}
