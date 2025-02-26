package org.openelisglobal.common.servlet.query;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.json.XML;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.provider.query.BaseQueryProvider;
import org.openelisglobal.common.provider.query.QueryProviderFactory;
import org.openelisglobal.common.servlet.validation.AjaxServlet;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

public class AjaxQueryXMLServlet extends AjaxServlet {

    /** */
    private static final long serialVersionUID = -7346331231442794642L;

    @Override
    public void sendData(String field, String message, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        response.setCharacterEncoding("utf-8");

        if (!StringUtil.isNullorNill(field)) {
            StringBuilder sb = new StringBuilder().append("<fieldmessage>").append("<formfield>").append(field)
                    .append("</formfield>").append("<message>").append(message).append("</message>")
                    .append("</fieldmessage>");
            if ("true".equals(request.getParameter("asJSON"))) {
                response.setContentType("application/json");
                response.setHeader("Cache-Control", "no-cache");
                response.getWriter().write(XML.toJSONObject(sb.toString()).toString());
            } else {
                response.setContentType("text/xml");
                response.setHeader("Cache-Control", "no-cache");
                response.getWriter().write(sb.toString());
            }
        } else {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException, LIMSRuntimeException {
        boolean unauthorized = false;

        // check for module authentication
        UserModuleService userModuleService = SpringContext.getBean(UserModuleService.class);
        unauthorized |= userModuleService.isSessionExpired(request);

        // check for csrf token to prevent js hijacking since we employ callback
        // functions
        CsrfToken officialToken = new HttpSessionCsrfTokenRepository().loadToken(request);
        String clientSuppliedToken = request.getHeader("X-CSRF-Token");
        // unauthorized |= !officialToken.getToken().equals(clientSuppliedToken);
        unauthorized |= clientSuppliedToken == null;

        if (unauthorized) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("text/html; charset=utf-8");
            response.getWriter().println(MessageUtil.getMessage("message.error.unauthorized"));
            return;
        }

        String queryProvider = request.getParameter("provider");
        BaseQueryProvider provider = QueryProviderFactory.getInstance().getQueryProvider(queryProvider);
        provider.setServlet(this);
        provider.processRequest(request, response);
    }
}
