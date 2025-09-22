package org.openelisglobal.odoo.client;

import java.util.List;
import java.util.Map;

public interface OdooConnection {

    boolean isAvailable();

    Integer create(String model, List<Map<String, Object>> dataParams);

    Object[] searchAndRead(String model, List<Object> criteria, List<String> fields);

}
