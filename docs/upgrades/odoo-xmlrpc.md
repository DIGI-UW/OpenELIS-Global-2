# Odoo requests use standard XML-RPC types

The Odoo client now disables Apache XML-RPC vendor extensions for authentication
and object requests. The existing endpoints and authentication settings remain
unchanged. There is no database migration.

Supported values include strings, integers, doubles, booleans, arrays,
structures, dates and binary data. Custom integrations must not send Java
serialized objects or other Apache-specific extension types. Structures
containing a binary `faultCause` are rejected before the library can reconstruct
a Java exception. Ordinary XML-RPC errors retain their numeric code and text
message.

Local protocol tests cover authentication, invoice-shaped requests, create,
write, delete, search and read operations, ordinary errors, and rejection of
serialized values and exception causes. These tests use a synthetic server;
validate against your configured Odoo version before deployment.

The Apache XML-RPC dependency remains in this change. Replacing the unmaintained
library is separate work; this is not a general audit of the Odoo integration.
