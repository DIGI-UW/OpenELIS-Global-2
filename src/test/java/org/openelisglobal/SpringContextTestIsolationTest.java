package org.openelisglobal;

import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.context.support.GenericApplicationContext;

public class SpringContextTestIsolationTest extends BaseWebContextSensitiveTest {

    @Test
    public void cachedTestContextRestoresLegacyBeanLookupAfterAnotherContextCloses() throws Exception {
        SpringContext bridge = webApplicationContext.getBean(SpringContext.class);
        try {
            try (GenericApplicationContext otherContext = new GenericApplicationContext()) {
                otherContext.refresh();
                bridge.setApplicationContext(otherContext);
            }

            setDefaultTestAuthentication();

            assertSame(webApplicationContext.getBean(SiteInformationService.class),
                    SpringContext.getBean(SiteInformationService.class));
        } finally {
            bridge.setApplicationContext(webApplicationContext);
        }
    }
}
