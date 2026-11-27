package org.openelisglobal.program.service;

import jakarta.servlet.ServletContext;
import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Where the compiled report templates and the images they print are found, as
 * the report print endpoint resolves them.
 */
public record ReportPaths(String reportPath, String imagesPath) {

    public static ReportPaths of(ServletContext servletContext) {
        String reportPath = URLDecoder.decode(ReportPaths.class.getClassLoader().getResource("reports").getPath(),
                StandardCharsets.UTF_8);
        if (!reportPath.endsWith(File.separator)) {
            reportPath = reportPath + File.separator;
        }
        String imagesPath = URLDecoder.decode(
                servletContext.getRealPath("") + "static" + File.separator + "images" + File.separator,
                StandardCharsets.UTF_8);
        return new ReportPaths(reportPath, imagesPath);
    }
}
