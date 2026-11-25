package org.openelisglobal.dashboard.management.quality;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.testsupport.SliceSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebAppConfiguration
@ContextConfiguration(classes = { SliceSecurityConfig.class, QiWeightsRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class QiWeightsRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    private static final String URL = "/rest/dashboards/management/quality-weights";

    private static final String VALID = "{\"qc\":0.40,\"eqa\":0.30,\"nceInverse\":0.20,\"amendmentInverse\":0.10}";

    @Autowired
    private RecordingService service;

    @Before
    public void clearSaves() {
        service.saved.clear();
    }

    @Test
    public void dashboardViewerReadsTheWeights() throws Exception {
        mockMvc.perform(
                get(URL).with(user("manager").authorities(new SimpleGrantedAuthority("dashboards.view.management"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.qc").value(0.3));
    }

    @Test
    public void qiManagerReadsTheWeights() throws Exception {
        mockMvc.perform(get(URL).with(user("qa").authorities(new SimpleGrantedAuthority("qa.manage.qi"))))
                .andExpect(status().isOk());
    }

    @Test
    public void benchUserCannotReadTheWeights() throws Exception {
        mockMvc.perform(get(URL).with(user("bench").roles("RESULTS"))).andExpect(status().isForbidden());
    }

    @Test
    public void dashboardViewerCannotChangeTheWeights() throws Exception {
        mockMvc.perform(putWeights(VALID)
                .with(user("manager").authorities(new SimpleGrantedAuthority("dashboards.view.management"))))
                .andExpect(status().isForbidden());
        assertTrue(service.saved.isEmpty());
    }

    @Test
    public void qiManagerSavesValidWeights() throws Exception {
        mockMvc.perform(putWeights(VALID).with(user("qa").authorities(new SimpleGrantedAuthority("qa.manage.qi"))))
                .andExpect(status().isNoContent());
        assertEquals(1, service.saved.size());
    }

    @Test
    public void weightsThatDoNotSumToOneAreABadRequest() throws Exception {
        mockMvc.perform(putWeights("{\"qc\":0.5,\"eqa\":0.5,\"nceInverse\":0.5,\"amendmentInverse\":0.5}")
                .with(user("qa").authorities(new SimpleGrantedAuthority("qa.manage.qi"))))
                .andExpect(status().isBadRequest());
        assertTrue(service.saved.isEmpty());
    }

    private static MockHttpServletRequestBuilder putWeights(String body) {
        UserSessionData session = new UserSessionData();
        session.setSytemUserId(1);
        return put(URL).contentType(MediaType.APPLICATION_JSON).content(body)
                .requestAttr(IActionConstants.USER_SESSION_DATA, session);
    }

    static class RecordingService implements QiWeightsService {
        final List<WeightsView> saved = new ArrayList<>();

        @Override
        public WeightsView getWeights() {
            return QiWeightsServiceImpl.DEFAULTS;
        }

        @Override
        public void saveWeights(WeightsView weights, String sysUserId) {
            if (!weights.isValid()) {
                throw new IllegalArgumentException("invalid");
            }
            saved.add(weights);
        }
    }

    @Configuration
    static class TestConfig {

        @Bean
        RecordingService qiWeightsService() {
            return new RecordingService();
        }

        @Bean
        QiWeightsRestController qiWeightsRestController() {
            return new QiWeightsRestController();
        }
    }
}
