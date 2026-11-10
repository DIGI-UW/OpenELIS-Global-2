# External patient-search transport

The configured external patient-search endpoint must use HTTPS. Its certificate
must match the endpoint hostname and chain to a certificate in the installation
trust store (`server.ssl.trust-store`). For a private certificate authority,
import its public certificate through the existing certificate-management
workflow before enabling the connection. Keep the endpoint's own search account
and password.

Patient search uses the same configured trust store as the other installation
clients. It does not send the installation's client certificate or borrow
another destination's credentials. A self-signed server is accepted only when
explicitly trusted and its hostname matches. Recreate the client by restarting
the application after updating trust.

Redirects are not followed. Configure the final HTTPS endpoint directly. Failed
TLS, HTTP-only endpoints and wrong-host certificates return a failed search
without patient results; local patient search remains available.

This preserves the existing partner query protocol, including its query-string
credentials. Those credentials and patient search terms must not be logged by
the receiving service or its reverse proxy. Changing that partner protocol
requires coordination with its owner and is a separate task.
