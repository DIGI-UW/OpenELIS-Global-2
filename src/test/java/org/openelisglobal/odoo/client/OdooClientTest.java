package org.openelisglobal.odoo.client;

import static org.junit.Assert.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.xmlrpc.XmlRpcException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class OdooClientTest {
    private HttpServer server;
    private OdooClient client;
    private final AtomicReference<String> commonResponse = new AtomicReference<>();
    private final AtomicReference<String> objectResponse = new AtomicReference<>();
    private final List<String> commonRequests = new CopyOnWriteArrayList<>();
    private final List<String> objectRequests = new CopyOnWriteArrayList<>();

    @Before
    public void startServer() throws IOException {
        ReadMarker.reads.set(0);
        commonResponse.set(response("<int>7</int>"));
        objectResponse.set(response("<int>42</int>"));
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/xmlrpc/2/common", exchange -> respond(exchange, commonResponse, commonRequests));
        server.createContext("/xmlrpc/2/object", exchange -> respond(exchange, objectResponse, objectRequests));
        server.start();
        client = new OdooClient("http://127.0.0.1:" + server.getAddress().getPort(), "test-database", "test-user",
                "synthetic-test-password");
    }

    @After
    public void stopServer() {
        if (server != null)
            server.stop(0);
        ReadMarker.reads.set(0);
    }

    @Test
    public void authenticationAndCrudUseStandardXmlRpc() {
        objectResponse.set(response("<int>42</int>"));
        Map<String, Object> invoice = Map.of("move_type", "out_invoice", "partner_id", 12, "invoice_line_ids",
                List.of(List.of(0, 0, Map.of("quantity", 1.0, "price_unit", 12.5))));
        assertEquals(Integer.valueOf(42), client.create("account.move", List.of(invoice)));
        objectResponse.set(response("<boolean>1</boolean>"));
        assertTrue(client.write("account.move", List.of(List.of(42), Map.of("ref", "Synthetic invoice"))));
        assertTrue(client.delete("account.move", List.of(List.of(42))));
        objectResponse.set(response("<array><data><value><int>42</int></value></data></array>"));
        assertArrayEquals(new Object[] { 42 }, client.search("account.move", List.of("ref", "=", "Synthetic invoice")));

        assertEquals(1, commonRequests.size());
        assertTrue(commonRequests.get(0).contains("<methodName>authenticate</methodName>"));
        assertTrue(commonRequests.get(0).contains("test-database"));
        assertTrue(commonRequests.get(0).contains("test-user"));
        assertEquals(4, objectRequests.size());
        String[] methods = { "create", "write", "unlink", "search" };
        for (int i = 0; i < methods.length; i++) {
            String request = objectRequests.get(i);
            assertTrue(request.contains("<methodName>execute_kw</methodName>"));
            assertTrue(request.contains("<value>" + methods[i] + "</value>"));
            assertTrue(request.contains("<value>account.move</value>"));
            assertFalse(request.contains("http://ws.apache.org/xmlrpc/namespaces/extensions"));
        }
        assertTrue(objectRequests.get(0).contains("<double>12.5</double>"));
    }

    @Test
    public void searchReadPreservesStandardNestedDataAndBinaryFields() {
        objectResponse.set(response("""
                <array><data><value><struct>
                  <member><name>id</name><value><int>12</int></value></member>
                  <member><name>name</name><value><string>Synthetic &amp; Partner</string></value></member>
                  <member><name>active</name><value><boolean>0</boolean></value></member>
                  <member><name>credit</name><value><double>12.5</double></value></member>
                  <member><name>image</name><value><base64>AQID</base64></value></member>
                  <member><name>category_id</name><value><array><data>
                    <value><int>3</int></value><value><string>Example</string></value>
                  </data></array></value></member>
                </struct></value></data></array>
                """));
        Object[] records = client.searchAndRead("res.partner", List.of("id", "=", 12), List.of("name", "active"));
        assertEquals(1, records.length);
        Map<?, ?> record = (Map<?, ?>) records[0];
        assertEquals(12, record.get("id"));
        assertEquals("Synthetic & Partner", record.get("name"));
        assertEquals(false, record.get("active"));
        assertEquals(12.5, record.get("credit"));
        assertArrayEquals(new byte[] { 1, 2, 3 }, (byte[]) record.get("image"));
        assertArrayEquals(new Object[] { 3, "Example" }, (Object[]) record.get("category_id"));
        assertTrue(objectRequests.get(0).contains("<value>search_read</value>"));
        assertTrue(objectRequests.get(0).contains("<name>fields</name>"));
        client.searchAndRead("res.partner", List.of("id", "=", 12), null);
        assertFalse(objectRequests.get(1).contains("<name>fields</name>"));
    }

    @Test
    public void ordinaryFaultsKeepTheirCodeAndMessage() {
        objectResponse.set(fault(""));
        RuntimeException error = assertThrows(RuntimeException.class,
                () -> client.create("account.move", List.of(Map.of("ref", "Synthetic"))));
        assertTrue(error.getCause() instanceof XmlRpcException);
        assertEquals(123, ((XmlRpcException) error.getCause()).code);
        assertEquals("Synthetic rejected request", error.getCause().getMessage());
    }

    @Test
    public void objectResponsesCannotDeserializeExtensionValues() throws IOException {
        objectResponse.set(response(serializedExtension()));
        assertRejectedWithoutDeserialization(() -> client.search("res.partner", List.of("id", "=", 12)));
    }

    @Test
    public void authenticationResponsesCannotDeserializeExtensionValues() throws IOException {
        commonResponse.set(response(serializedExtension()));
        assertRejectedWithoutDeserialization(client::init);
    }

    @Test
    public void objectFaultsCannotDeserializeExceptionCauses() throws IOException {
        objectResponse.set(fault(serializedCause()));
        assertRejectedWithoutDeserialization(() -> client.search("res.partner", List.of("id", "=", 12)));
    }

    @Test
    public void authenticationFaultsCannotDeserializeExceptionCauses() throws IOException {
        commonResponse.set(fault(serializedCause()));
        assertRejectedWithoutDeserialization(client::init);
    }

    @Test
    public void nestedExtensionValuesAreRejected() throws IOException {
        objectResponse.set(response("<array><data><value><struct><member><name>payload</name><value>"
                + serializedExtension() + "</value></member></struct></value></data></array>"));
        assertRejectedWithoutDeserialization(() -> client.searchAndRead("res.partner", List.of("id", "=", 12), null));
    }

    @Test
    public void unsupportedOutgoingValuesAreNotSerialized() {
        Map<String, Object> values = Map.of("unsupported", new ReadMarker());
        assertThrows(RuntimeException.class, () -> client.create("account.move", List.of(values)));
        assertEquals(0, ReadMarker.reads.get());
        assertEquals(0, objectRequests.size());
    }

    private void assertRejectedWithoutDeserialization(Runnable action) {
        assertThrows(RuntimeException.class, action::run);
        assertEquals("No Java object should be reconstructed from a response", 0, ReadMarker.reads.get());
    }

    private static String response(String value) {
        return "<?xml version=\"1.0\"?><methodResponse><params><param><value>" + value
                + "</value></param></params></methodResponse>";
    }

    private static String fault(String extraMember) {
        return "<?xml version=\"1.0\"?><methodResponse><fault><value><struct>"
                + "<member><name>faultCode</name><value><int>123</int></value></member>"
                + "<member><name>faultString</name><value><string>Synthetic rejected request</string></value></member>"
                + extraMember + "</struct></value></fault></methodResponse>";
    }

    private static String serializedExtension() throws IOException {
        return "<ex:serializable xmlns:ex=\"http://ws.apache.org/xmlrpc/namespaces/extensions\">" + serializedMarker()
                + "</ex:serializable>";
    }

    private static String serializedCause() throws IOException {
        return "<member><name>faultCause</name><value><base64>" + serializedMarker() + "</base64></value></member>";
    }

    private static String serializedMarker() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(new ReadMarker());
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray());
    }

    /** Harmless probe: deserialization only increments this test's counter. */
    private static final class ReadMarker extends Exception {
        private static final long serialVersionUID = 1L;
        private static final AtomicInteger reads = new AtomicInteger();

        private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
            input.defaultReadObject();
            reads.incrementAndGet();
        }
    }

    private static void respond(HttpExchange exchange, AtomicReference<String> response, List<String> requests)
            throws IOException {
        try (exchange) {
            requests.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = response.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/xml; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
        }
    }
}
