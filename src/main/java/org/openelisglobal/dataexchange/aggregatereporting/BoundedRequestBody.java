package org.openelisglobal.dataexchange.aggregatereporting;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Reads a request body up to a bound, sized by what arrives rather than by the
 * Content-Length header the client declares.
 */
final class BoundedRequestBody {

    private BoundedRequestBody() {
    }

    /**
     * @return the body, or null when the declared or the actual length exceeds
     *         {@code maxBytes}
     */
    static byte[] read(InputStream in, int declaredLength, int maxBytes) throws IOException {
        if (declaredLength > maxBytes) {
            return null;
        }
        ByteArrayOutputStream body = new ByteArrayOutputStream(Math.max(0, Math.min(declaredLength, 64 * 1024)));
        byte[] chunk = new byte[8 * 1024];
        int read;
        while ((read = in.read(chunk)) != -1) {
            if (body.size() + read > maxBytes) {
                return null;
            }
            body.write(chunk, 0, read);
        }
        return body.toByteArray();
    }
}
