package org.openelisglobal.documentation.controller;

import java.io.IOException;
import java.util.Locale;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DocumentationController {

    @GetMapping(value = "/docs/UserManual", produces = MediaType.APPLICATION_PDF_VALUE)
    public String getUserManual() throws IOException {
        Locale locale = LocaleContextHolder.getLocale();
        String filename = "";
        if (locale.getLanguage().equals("en")) {
            filename = "/documentation/OEGlobal_UserManual_en.pdf";
        } else {
            filename = "/documentation/OEGlobal_UserManual_en.pdf";
        }

        return "redirect:" + filename;
    }
}
