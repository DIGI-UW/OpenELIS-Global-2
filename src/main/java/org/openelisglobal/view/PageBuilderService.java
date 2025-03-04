package org.openelisglobal.view;

import jakarta.servlet.http.HttpServletRequest;

public interface PageBuilderService {

    String setupJSPPage(String view, HttpServletRequest request) throws ViewConfigurationException;
}
