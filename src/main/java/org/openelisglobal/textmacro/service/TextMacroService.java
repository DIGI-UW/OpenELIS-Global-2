package org.openelisglobal.textmacro.service;

import java.util.List;
import org.openelisglobal.textmacro.form.TextMacroAdminForm;
import org.openelisglobal.textmacro.form.TextMacroAdminQueryForm;
import org.openelisglobal.textmacro.form.TextMacroBulkRequestForm;
import org.openelisglobal.textmacro.form.TextMacroBulkResultForm;
import org.openelisglobal.textmacro.form.TextMacroPageForm;
import org.openelisglobal.textmacro.form.TextMacroSummaryForm;

public interface TextMacroService {
    List<TextMacroSummaryForm> findActive(String context, String query, int limit);

    TextMacroPageForm searchAdmin(TextMacroAdminQueryForm query);

    TextMacroAdminForm getAdmin(String id);

    TextMacroAdminForm save(String id, TextMacroAdminForm request, String actorId);

    String exportCsv();

    TextMacroBulkResultForm bulk(TextMacroBulkRequestForm request, String actorId);
}
