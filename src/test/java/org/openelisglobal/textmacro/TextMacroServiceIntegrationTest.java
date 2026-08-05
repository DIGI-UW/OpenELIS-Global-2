package org.openelisglobal.textmacro;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.textmacro.form.TextMacroAdminForm;
import org.openelisglobal.textmacro.form.TextMacroAdminQueryForm;
import org.openelisglobal.textmacro.form.TextMacroPageForm;
import org.openelisglobal.textmacro.service.TextMacroConflictException;
import org.openelisglobal.textmacro.service.TextMacroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class TextMacroServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private TextMacroService service;

    @Test
    public void serviceCreatesGeneratedIdsAndFiltersActiveMacrosByContext() {
        String suffix = uniqueSuffix();
        TextMacroAdminForm culture = service.save(null,
                request(".culture" + suffix, "Culture phrase " + suffix, Set.of("MICROBIOLOGY_CULTURE_ACTIVITY"), true),
                TEST_SYS_USER_ID);
        TextMacroAdminForm history = service.save(null,
                request(".history" + suffix, "History phrase " + suffix, Set.of("MICROBIOLOGY_CLINICAL_HISTORY"), true),
                TEST_SYS_USER_ID);
        TextMacroAdminForm inactive = service.save(null, request(".inactive" + suffix, "Inactive phrase " + suffix,
                Set.of("MICROBIOLOGY_CULTURE_ACTIVITY"), false), TEST_SYS_USER_ID);
        assertNotNull(culture.id);
        assertFalse(culture.id.isBlank());
        assertNotEquals(culture.id, history.id);
        assertEquals(1, service.findActive("MICROBIOLOGY_CULTURE_ACTIVITY", suffix, 50).size());
        assertEquals(culture.id, service.findActive("MICROBIOLOGY_CULTURE_ACTIVITY", suffix, 50).get(0).id);
        assertTrue(service.findActive("MICROBIOLOGY_ANTIBIOTIC_EXPOSURE", suffix, 50).isEmpty());
        assertNotNull(inactive.id);
    }

    @Test
    public void canonicalDuplicateIsRejectedThroughService() {
        String suffix = uniqueSuffix();
        service.save(null, request(".duplicate" + suffix, "First", Set.of("MICROBIOLOGY_CULTURE_ACTIVITY"), true),
                TEST_SYS_USER_ID);

        assertThrows(TextMacroConflictException.class, () -> service.save(null,
                request(("DUPLICATE" + suffix).toUpperCase(), "Second", Set.of("MICROBIOLOGY_CULTURE_ACTIVITY"), true),
                TEST_SYS_USER_ID));
    }

    @Test
    public void fiveHundredMacrosRemainDeterministicallyPageable() {
        String suffix = uniqueSuffix();
        Set<String> generatedIds = new HashSet<>();
        for (int index = 0; index < 500; index++) {
            TextMacroAdminForm saved = service.save(null, request(String.format(".scale%s%03d", suffix, index),
                    "Scale phrase " + suffix + " " + index, Set.of("MICROBIOLOGY_CULTURE_ACTIVITY"), true),
                    TEST_SYS_USER_ID);
            generatedIds.add(saved.id);
        }
        TextMacroAdminQueryForm query = new TextMacroAdminQueryForm();
        query.q = suffix;
        query.context = "MICROBIOLOGY_CULTURE_ACTIVITY";
        query.status = "active";
        query.sort = "code:desc";
        query.page = 5;
        query.pageSize = 100;
        TextMacroPageForm page = service.searchAdmin(query);

        assertEquals(500, page.total);
        assertEquals(100, page.items.size());
        assertEquals(500, generatedIds.size());
        assertTrue(page.items.stream().allMatch(item -> generatedIds.contains(item.id)));
        assertTrue(page.items.get(0).code.compareTo(page.items.get(99).code) > 0);
    }

    private TextMacroAdminForm request(String code, String text, Set<String> contexts, boolean active) {
        TextMacroAdminForm request = new TextMacroAdminForm();
        request.code = code;
        request.expansionText = text;
        request.contexts = contexts;
        request.active = active;
        return request;
    }

    private String uniqueSuffix() {
        return Long.toUnsignedString(System.nanoTime(), 36);
    }
}
