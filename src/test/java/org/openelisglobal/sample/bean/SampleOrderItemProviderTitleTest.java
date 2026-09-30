package org.openelisglobal.sample.bean;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.openelisglobal.config.AppConfig;

/**
 * Order entry echoes the picked provider's title (OGC-1223). The order form
 * refused unknown fields, so picking any provider made every order save fail
 * with a 400 before it reached the controller.
 */
public class SampleOrderItemProviderTitleTest {

    @Test
    public void anOrderWithAPickedProvidersTitleIsRead() throws Exception {
        ObjectMapper mapper = new AppConfig().jacksonMessageConverter().getObjectMapper();

        SampleOrderItem order = mapper.readValue("{\"labNo\":\"DEV01260000000000100\",\"providerPersonId\":\"12\","
                + "\"providerTitleCode\":\"DR\",\"providerTitleAbbreviation\":\"Dr\"}", SampleOrderItem.class);

        assertEquals("DR", order.getProviderTitleCode());
        assertEquals("Dr", order.getProviderTitleAbbreviation());
    }
}
