package org.openelisglobal.odoo.client;

import java.util.Map;
import org.apache.ws.commons.util.NamespaceContextImpl;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.common.TypeFactoryImpl;
import org.apache.xmlrpc.common.XmlRpcController;
import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.apache.xmlrpc.parser.MapParser;
import org.apache.xmlrpc.parser.TypeParser;

/** Keeps Odoo responses as protocol data, including error responses. */
final class OdooXmlRpcTypeFactory extends TypeFactoryImpl {
    OdooXmlRpcTypeFactory(XmlRpcController controller) {
        super(controller);
    }

    @Override
    public TypeParser getParser(XmlRpcStreamConfig config, NamespaceContextImpl context, String namespace,
            String localName) {
        if ("".equals(namespace) && "struct".equals(localName)) {
            return new MapParser(config, context, this) {
                @Override
                public Object getResult() throws XmlRpcException {
                    Object result = super.getResult();
                    // Apache's response parser deserializes a binary faultCause even with
                    // extensions disabled. Reject it before the struct reaches that parser.
                    if (result instanceof Map<?, ?> map && map.get("faultCause") instanceof byte[]) {
                        throw new XmlRpcException("Serialized XML-RPC fault causes are not supported");
                    }
                    return result;
                }
            };
        }
        return super.getParser(config, context, namespace, localName);
    }
}
