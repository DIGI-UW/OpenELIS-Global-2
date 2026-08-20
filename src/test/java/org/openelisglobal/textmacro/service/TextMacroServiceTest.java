package org.openelisglobal.textmacro.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openelisglobal.textmacro.dao.TextMacroDAO;
import org.openelisglobal.textmacro.form.TextMacroAdminForm;
import org.openelisglobal.textmacro.form.TextMacroAdminQueryForm;
import org.openelisglobal.textmacro.form.TextMacroPageForm;
import org.openelisglobal.textmacro.valueholder.TextMacro;
import org.openelisglobal.textmacro.valueholder.TextMacroContext;

public class TextMacroServiceTest {

    private TextMacroDAO macroDAO;
    private TextMacroService service;

    @Before
    public void setUp() {
        macroDAO = mock(TextMacroDAO.class);
        service = new TextMacroServiceImpl(macroDAO);
    }

    @Test
    public void createNormalizesCodeAndAttributesAuthenticatedActor() {
        TextMacroAdminForm request = request(" GPC ", "  Gram-positive cocci  ",
                Set.of("MICROBIOLOGY_CULTURE_ACTIVITY"));
        when(macroDAO.findByCode(".gpc")).thenReturn(Optional.empty());

        service.save(null, request, "42");

        ArgumentCaptor<TextMacro> saved = ArgumentCaptor.forClass(TextMacro.class);
        verify(macroDAO).insert(saved.capture());
        assertEquals(".gpc", saved.getValue().getCode());
        assertEquals("Gram-positive cocci", saved.getValue().getExpansionText());
        assertEquals(Set.of(TextMacroContext.MICROBIOLOGY_CULTURE_ACTIVITY), saved.getValue().getContexts());
        assertEquals("42", saved.getValue().getLastUpdatedBy());
        assertEquals("LOCAL", saved.getValue().getProvenance());
        assertTrue(saved.getValue().isActive());
    }

    @Test
    public void createRejectsDuplicateCanonicalCode() {
        TextMacro existing = macro("existing", ".gpc", TextMacroContext.MICROBIOLOGY_CULTURE_ACTIVITY);
        when(macroDAO.findByCode(".gpc")).thenReturn(Optional.of(existing));

        assertThrows(TextMacroConflictException.class, () -> service.save(null,
                request(".GPC", "Different text", Set.of("MICROBIOLOGY_CULTURE_ACTIVITY")), "42"));
    }

    @Test
    public void createRejectsUnknownContextAndMissingActor() {
        TextMacroRequestException invalidContext = assertThrows(TextMacroRequestException.class,
                () -> service.save(null, request(".gpc", "Text", Set.of("NOT_A_CONTEXT")), "42"));
        assertEquals("INVALID_MACRO_CONTEXT", invalidContext.getCode());
        TextMacroRequestException missingActor = assertThrows(TextMacroRequestException.class,
                () -> service.save(null, request(".gpc", "Text", Set.of("MICROBIOLOGY_CULTURE_ACTIVITY")), " "));
        assertEquals("AUTHENTICATED_ACTOR_REQUIRED", missingActor.getCode());
    }

    @Test
    public void validationErrorsExposeStableContractCodes() {
        TextMacroRequestException invalidCode = assertThrows(TextMacroRequestException.class,
                () -> service.save(null, request("not valid!", "Text", Set.of("MICROBIOLOGY_CULTURE_ACTIVITY")), "42"));
        assertEquals("INVALID_MACRO_CODE", invalidCode.getCode());

        TextMacroRequestException missingText = assertThrows(TextMacroRequestException.class,
                () -> service.save(null, request(".valid", " ", Set.of("MICROBIOLOGY_CULTURE_ACTIVITY")), "42"));
        assertEquals("MACRO_TEXT_REQUIRED", missingText.getCode());

        TextMacroRequestException missingContext = assertThrows(TextMacroRequestException.class,
                () -> service.save(null, request(".valid", "Text", Set.of()), "42"));
        assertEquals("MACRO_CONTEXT_REQUIRED", missingContext.getCode());
    }

    @Test
    public void runtimeLookupUsesRecognizedContextAndCapsLimit() {
        TextMacro macro = macro("macro-1", ".ng24", TextMacroContext.MICROBIOLOGY_CULTURE_ACTIVITY);
        when(macroDAO.findActiveByContext(TextMacroContext.MICROBIOLOGY_CULTURE_ACTIVITY, "ng", 50))
                .thenReturn(List.of(macro));

        assertEquals(".ng24", service.findActive("MICROBIOLOGY_CULTURE_ACTIVITY", " ng ", 500).get(0).code);

        verify(macroDAO).findActiveByContext(TextMacroContext.MICROBIOLOGY_CULTURE_ACTIVITY, "ng", 50);
    }

    @Test
    public void adminSearchNormalizesStablePageState() {
        TextMacroAdminQueryForm query = new TextMacroAdminQueryForm();
        query.q = " gpc ";
        query.context = "MICROBIOLOGY_CULTURE_ACTIVITY";
        query.status = "inactive";
        query.sort = "updated:desc";
        query.page = 0;
        query.pageSize = 999;
        when(macroDAO.search("gpc", TextMacroContext.MICROBIOLOGY_CULTURE_ACTIVITY, "INACTIVE", "updated:desc", 0, 100))
                .thenReturn(List.of());
        when(macroDAO.countSearch("gpc", TextMacroContext.MICROBIOLOGY_CULTURE_ACTIVITY, "INACTIVE")).thenReturn(0L);

        TextMacroPageForm result = service.searchAdmin(query);

        assertEquals(1, result.page);
        assertEquals(20, result.pageSize);
        assertEquals(0, result.total);
    }

    private TextMacroAdminForm request(String code, String text, Set<String> contexts) {
        TextMacroAdminForm request = new TextMacroAdminForm();
        request.code = code;
        request.expansionText = text;
        request.contexts = contexts;
        request.active = true;
        return request;
    }

    private TextMacro macro(String id, String code, TextMacroContext context) {
        TextMacro macro = new TextMacro();
        macro.setId(id);
        macro.setCode(code);
        macro.setExpansionText("Expansion");
        macro.setContexts(Set.of(context));
        macro.setActive(true);
        macro.setProvenance("LOCAL");
        return macro;
    }
}
