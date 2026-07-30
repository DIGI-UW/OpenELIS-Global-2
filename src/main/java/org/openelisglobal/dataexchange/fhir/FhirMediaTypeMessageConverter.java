package org.openelisglobal.dataexchange.fhir;

import ca.uhn.fhir.context.FhirContext;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 * OGC-740 + OGC-741 — serialize FHIR resources via HAPI's IParser instead of
 * Jackson, and register the {@code application/fhir+json} media type with
 * Spring's content negotiator.
 *
 * <p>
 * Before this converter, GET /rest/fhir/Observation returned a 150KB blob that
 * started with HAPI internals ({@code formatCommentsPre},
 * {@code idElement.idElement}, {@code valueAsCalendar}, {@code nanos}) —
 * Jackson serializing HAPI domain objects directly. Requests with
 * {@code Accept: application/fhir+json} returned 406.
 *
 * <p>
 * One converter fixes both: HAPI's JsonParser produces a spec-compliant FHIR
 * payload (OGC-740 size + correctness), and registering it for
 * {@code application/fhir+json} lets Spring's content negotiation pick it up
 * for that header (OGC-741).
 */
public class FhirMediaTypeMessageConverter extends AbstractHttpMessageConverter<IBaseResource> {

    public static final MediaType FHIR_JSON = MediaType.parseMediaType("application/fhir+json");

    private final FhirContext fhirContext;

    public FhirMediaTypeMessageConverter(FhirContext fhirContext) {
        super(FHIR_JSON, MediaType.APPLICATION_JSON);
        this.fhirContext = fhirContext;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return IBaseResource.class.isAssignableFrom(clazz);
    }

    @Override
    protected IBaseResource readInternal(Class<? extends IBaseResource> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        try (InputStreamReader reader = new InputStreamReader(inputMessage.getBody(), StandardCharsets.UTF_8)) {
            return fhirContext.newJsonParser().parseResource(clazz, reader);
        }
    }

    @Override
    protected void writeInternal(IBaseResource resource, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        // Compact output (no pretty-print) — matches FHIR spec defaults and
        // keeps payload size down (OGC-740 was a 150KB blob; HAPI's parser
        // emits ~3-4KB for the same Bundle). Content-Type is set by
        // AbstractHttpMessageConverter.write() from the negotiated MediaType
        // before this method runs — don't overwrite it, or a legacy
        // Accept: application/json caller gets application/fhir+json back.
        fhirContext.newJsonParser().setPrettyPrint(false).encodeResourceToWriter(resource,
                new java.io.OutputStreamWriter(outputMessage.getBody(), StandardCharsets.UTF_8));
    }
}
