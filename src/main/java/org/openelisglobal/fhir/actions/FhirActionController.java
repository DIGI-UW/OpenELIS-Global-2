package org.openelisglobal.fhir.actions;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FhirActionController {

    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private CloseableHttpClient httpClient;

    @PostMapping("/fhir/optimizeStorage")
    public ResponseEntity<String> triggerOptimizeStorage() throws ClientProtocolException, IOException {
        HttpPost httpPost = new HttpPost(fhirConfig.getLocalFhirStorePath() + "/$reindex");
        String json = "{\n" + //
                "  \"resourceType\": \"Parameters\",\n" + //
                "  \"parameter\": [ {\n" + //
                "    \"name\": \"optimizeStorage\",\n" + //
                "    \"valueString\": \"ALL_VERSIONS\"\n" + //
                "  } ]\n" + //
                "}";
        StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");
        try (CloseableHttpResponse res = httpClient.execute(httpPost)) {
            return ResponseEntity.status(res.getStatusLine().getStatusCode())
                    .body(EntityUtils.toString(res.getEntity(), StandardCharsets.UTF_8));
        }
    }
}
