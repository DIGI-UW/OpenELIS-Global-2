package org.openelisglobal.analyzer.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AnalyzerBridgeTransportServiceImpl implements AnalyzerBridgeTransportService {

    @Value("${analyzer.bridge.url:}")
    private String analyzerBridgeUrl;

    @Value("${analyzer.bridge.insecure-tls:false}")
    private boolean insecureTls;

    @Override
    public String sendMessage(Analyzer analyzer, String message) {
        if (analyzerBridgeUrl == null || analyzerBridgeUrl.isBlank()) {
            throw new LIMSRuntimeException("Analyzer bridge URL is not configured");
        }
        if (analyzer == null || analyzer.getIpAddress() == null || analyzer.getPort() == null) {
            throw new LIMSRuntimeException("Analyzer IP/port is not configured");
        }

        String bridgeEndpoint = analyzerBridgeUrl.replaceAll("/+$", "") + "/?forwardAddress=" + analyzer.getIpAddress()
                + "&forwardPort=" + analyzer.getPort();

        try {
            URL url = new URL(bridgeEndpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            if (conn instanceof HttpsURLConnection && insecureTls) {
                HttpsURLConnection httpsConn = (HttpsURLConnection) conn;
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new javax.net.ssl.TrustManager[] { new X509TrustManager() {
                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }
                } }, new java.security.SecureRandom());
                httpsConn.setSSLSocketFactory(sslContext.getSocketFactory());
                httpsConn.setHostnameVerifier((hostname, session) -> true);
            }

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(45000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(message.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            String body = "";
            try (InputStream is = (status < 400) ? conn.getInputStream() : conn.getErrorStream()) {
                if (is != null) {
                    body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
            if (status >= 200 && status < 300) {
                return body;
            }
            throw new LIMSRuntimeException("Bridge returned HTTP " + status + ": " + body);
        } catch (SocketTimeoutException e) {
            throw new LIMSRuntimeException("Bridge timeout during analyzer communication", e);
        } catch (LIMSRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new LIMSRuntimeException("Bridge communication error: " + e.getMessage(), e);
        }
    }
}
