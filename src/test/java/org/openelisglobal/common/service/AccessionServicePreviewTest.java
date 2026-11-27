package org.openelisglobal.common.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.dao.AccessionDAO;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat;
import org.openelisglobal.common.valueholder.AccessionNumberInfo.AccessionIdentity;

@RunWith(MockitoJUnitRunner.class)
public class AccessionServicePreviewTest {
    @Mock
    private AccessionDAO accessionDAO;
    @InjectMocks
    private AccessionServiceImpl service;

    @Test
    public void repeatedFirstPreviewsDoNotCreateOrReserveTheCounter() {
        AccessionIdentity identity = new AccessionIdentity("DEV0126", AccessionFormat.SITEYEARNUM);
        assertEquals(1L, service.getNextNumberNoIncrement("DEV0126", AccessionFormat.SITEYEARNUM));
        assertEquals(1L, service.getNextNumberNoIncrement(identity));
        verify(accessionDAO, never()).save(any());
        verify(accessionDAO, never()).getNextNumberIncrement(any(), any());
    }

    @Test
    public void bothPreviewEntryPointsReadTheNextNumberWithoutIncrementingIt() {
        when(accessionDAO.exists(any())).thenReturn(true);
        when(accessionDAO.getNextNumberNoIncrement("DEV0126", AccessionFormat.SITEYEARNUM)).thenReturn(42L);
        AccessionIdentity identity = new AccessionIdentity("DEV0126", AccessionFormat.SITEYEARNUM);
        assertEquals(42L, service.getNextNumberNoIncrement("DEV0126", AccessionFormat.SITEYEARNUM));
        assertEquals(42L, service.getNextNumberNoIncrement(identity));
        verify(accessionDAO, never()).save(any());
        verify(accessionDAO, never()).getNextNumberIncrement(any(), any());
    }
}
