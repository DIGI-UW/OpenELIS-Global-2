package org.openelisglobal.dataexchange.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.mock.http.MockHttpOutputMessage;

/**
 * OGC-740 + OGC-741 — verify that the FHIR media-type converter produces
 * spec-compliant FHIR JSON (HAPI's IParser, not Jackson) AND advertises the
 * {@code application/fhir+json} media type so Spring's content negotiator picks
 * it up for that Accept header.
 */
public class FhirMediaTypeMessageConverterTest {

    private static FhirContext fhirContext;
    private static FhirMediaTypeMessageConverter converter;

    @BeforeClass
    public static void setUpFhir() {
        fhirContext = FhirContext.forR4();
        converter = new FhirMediaTypeMessageConverter(fhirContext);
    }

    @Test
    public void supportsApplicationFhirJsonMediaType() {
        assertTrue("converter must advertise application/fhir+json",
                converter.getSupportedMediaTypes().contains(MediaType.parseMediaType("application/fhir+json")));
        assertTrue("converter must also accept application/json so legacy callers keep working",
                converter.getSupportedMediaTypes().contains(MediaType.APPLICATION_JSON));
    }

    @Test
    public void supportsAnyIBaseResourceSubclass() {
        assertTrue(converter.canRead(Bundle.class, MediaType.parseMediaType("application/fhir+json")));
        assertTrue(converter.canRead(CapabilityStatement.class, MediaType.parseMediaType("application/fhir+json")));
        assertFalse("non-FHIR types must NOT route through this converter — Jackson handles them",
                converter.canWrite(java.util.Map.class, MediaType.APPLICATION_JSON));
    }

    @Test
    public void writeProducesSpecCompliantFhirJson_noHapiInternals() throws IOException {
        Bundle bundle = new Bundle();
        bundle.setId("test-bundle");
        bundle.setType(Bundle.BundleType.SEARCHSET);

        MockHttpOutputMessage out = new MockHttpOutputMessage();
        converter.write(bundle, MediaType.parseMediaType("application/fhir+json"), out);

        String body = out.getBodyAsString();
        assertFalse("must not leak HAPI internal field 'formatCommentsPre'", body.contains("formatCommentsPre"));
        assertFalse("must not leak HAPI internal field 'valueAsCalendar'", body.contains("valueAsCalendar"));
        assertFalse("must not leak HAPI internal field 'idElement'", body.contains("idElement"));
        assertTrue("must include spec-compliant resourceType", body.contains("\"resourceType\":\"Bundle\""));
        assertTrue("must include the bundle type field", body.contains("\"type\":\"searchset\""));
        assertEquals("application/fhir+json", out.getHeaders().getContentType().toString());
    }

    @Test
    public void writePreservesNegotiatedApplicationJsonContentType() throws IOException {
        // Lock the OGC-740/741 follow-up: when a legacy client sends
        // Accept: application/json, the converter is still picked (because we
        // advertise application/json too), but it must NOT overwrite the
        // response Content-Type with application/fhir+json — that would lie
        // about what the client got back.
        Bundle bundle = new Bundle();
        bundle.setId("legacy");
        bundle.setType(Bundle.BundleType.SEARCHSET);

        MockHttpOutputMessage out = new MockHttpOutputMessage();
        converter.write(bundle, MediaType.APPLICATION_JSON, out);

        assertEquals("legacy Accept: application/json must round-trip Content-Type: application/json",
                "application/json", out.getHeaders().getContentType().toString());
        assertTrue("body must still be valid FHIR JSON regardless of Content-Type",
                out.getBodyAsString().contains("\"resourceType\":\"Bundle\""));
    }

    @Test
    public void readParsesSpecCompliantFhirJson() throws IOException {
        String json = "{\"resourceType\":\"Bundle\",\"id\":\"abc\",\"type\":\"searchset\"}";
        HttpInputMessage in = new HttpInputMessage() {
            @Override
            public java.io.InputStream getBody() {
                return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public HttpHeaders getHeaders() {
                return new HttpHeaders();
            }
        };
        Bundle parsed = (Bundle) converter.read(Bundle.class, in);
        assertEquals("abc", parsed.getIdElement().getIdPart());
        assertEquals(Bundle.BundleType.SEARCHSET, parsed.getType());
    }
}
