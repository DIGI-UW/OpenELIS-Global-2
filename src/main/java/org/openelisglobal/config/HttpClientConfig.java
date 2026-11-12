package org.openelisglobal.config;

import javax.net.ssl.SSLContext;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;

@Configuration
public class HttpClientConfig {

    @Value("${server.ssl.trust-store}")
    private Resource trustStore;

    @Value("${server.ssl.trust-store-password}")
    private String trustStorePassword;

    @Value("${server.ssl.key-store}")
    private Resource keyStore;

    @Value("${server.ssl.key-store-password}")
    private String keyStorePassword;

    @Value("${server.ssl.key-password}")
    private String keyPassword;

    @Value("${org.openelisglobal.httpclient.connectionRequestTimeout:0}")
    private Integer connectionRequestTimeout;

    @Value("${org.openelisglobal.httpclient.connectionTimeout:0}")
    private Integer connectionTimeout;

    @Value("${org.openelisglobal.httpclient.socketTimeout:0}")
    private Integer socketTimeout;

    @Bean
    @Primary
    public CloseableHttpClient httpClient() throws Exception {
        return httpClientBuilder(sslConnectionSocketFactory()).build();
    }

    @Bean
    public CloseableHttpClient localFhirHttpClient() throws Exception {
        return httpClientBuilder(sslConnectionSocketFactory()).disableRedirectHandling().build();
    }

    @Bean
    public CloseableHttpClient partnerFhirHttpClient() throws Exception {
        SSLContext trustOnly = SSLContextBuilder.create()
                .loadTrustMaterial(trustStore.getFile(), trustStorePassword.toCharArray()).build();
        return httpClientBuilder(new SSLConnectionSocketFactory(trustOnly)).disableRedirectHandling().build();
    }

    private HttpClientBuilder httpClientBuilder(SSLConnectionSocketFactory socketFactory) {

        HttpClientBuilder httpBuilder = HttpClientBuilder.create().setSSLSocketFactory(socketFactory);

        if (connectionRequestTimeout != 0 || connectionTimeout != 0 || socketTimeout != 0) {
            final RequestConfig.Builder configBuilder = RequestConfig.custom();
            if (connectionRequestTimeout != 0) {
                configBuilder.setConnectionRequestTimeout(this.connectionRequestTimeout);
            }
            if (connectionTimeout != 0) {
                configBuilder.setConnectTimeout(connectionTimeout);
            }
            if (socketTimeout != 0) {
                configBuilder.setSocketTimeout(socketTimeout);
            }
            httpBuilder.setDefaultRequestConfig(configBuilder.build());
        }

        return httpBuilder;
    }

    public SSLConnectionSocketFactory sslConnectionSocketFactory() throws Exception {
        return new SSLConnectionSocketFactory(sslContext());
    }

    public SSLContext sslContext() throws Exception {
        return SSLContextBuilder.create()
                .loadKeyMaterial(keyStore.getFile(), keyStorePassword.toCharArray(), keyPassword.toCharArray())
                .loadTrustMaterial(trustStore.getFile(), trustStorePassword.toCharArray()).build();
    }
}
