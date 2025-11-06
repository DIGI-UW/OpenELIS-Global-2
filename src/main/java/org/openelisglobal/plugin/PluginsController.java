package org.openelisglobal.plugin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.ServletWrappingController;

@Controller
public class PluginsController {

    @RequestMapping("/pluginServlet/{servletName}")
    public void runPluginServlet(
            @Valid @Pattern(regexp = "[a-zA-Z0-9]*") @PathVariable("servletName") String servletName,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        ServletWrappingController controller = (ServletWrappingController) SpringContext.getBean(servletName);
        controller.handleRequest(request, response);
    }
}
