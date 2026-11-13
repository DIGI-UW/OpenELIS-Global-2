package org.openelisglobal.dataexchange.aggregatereporting;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

/**
 * The indicator servlet allocated a buffer of whatever Content-Length the
 * client declared before reading a byte (SCAN-094).
 */
public class BoundedRequestBodyTest {

    private static final int MAX = 1024;

    @Test
    public void aBodyWithinTheBound_isReadWhole() throws Exception {
        byte[] body = "<report/>".getBytes(StandardCharsets.UTF_8);

        assertArrayEquals(body, BoundedRequestBody.read(new ByteArrayInputStream(body), body.length, MAX));
        assertArrayEquals(body, BoundedRequestBody.read(new ByteArrayInputStream(body), -1, MAX));
    }

    @Test
    public void aDeclaredLengthBeyondTheBound_allocatesNothing() throws Exception {
        assertNull(BoundedRequestBody.read(new ByteArrayInputStream(new byte[0]), Integer.MAX_VALUE, MAX));
    }

    @Test
    public void aBodyLongerThanDeclared_stopsAtTheBound() throws Exception {
        byte[] body = new byte[MAX + 1];

        assertNull(BoundedRequestBody.read(new ByteArrayInputStream(body), 10, MAX));
    }
}
