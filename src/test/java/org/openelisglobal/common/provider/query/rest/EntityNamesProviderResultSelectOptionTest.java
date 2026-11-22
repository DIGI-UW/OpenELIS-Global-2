package org.openelisglobal.common.provider.query.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.renamemethod.service.RenameMethodService;
import org.openelisglobal.renametestsection.service.RenameTestSectionService;
import org.openelisglobal.testconfiguration.service.ResultSelectListService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Rename Existing Result List Options prefills a field per language from this
 * endpoint. It is the only way that screen can learn what the languages it is
 * not being read in currently say, and an entity name this endpoint does not
 * answer is what left it sending the displayed name as every language.
 *
 * <p>
 * Unit-level rather than through MockMvc: the shared test context registers
 * controllers from an allowlist that does not include this package, so a
 * request for this path would be answered by no handler at all — a 404 that
 * looks like the endpoint's own "not found" and passes for the wrong reason.
 */
@RunWith(MockitoJUnitRunner.class)
public class EntityNamesProviderResultSelectOptionTest {

    @Mock
    private PanelService panelService;
    @Mock
    private RenameTestSectionService renameTestSectionService;
    @Mock
    private TypeOfSampleService typeOfSampleService;
    @Mock
    private UnitOfMeasureService unitOfMeasureService;
    @Mock
    private RenameMethodService renameMethodService;
    @Mock
    private ResultSelectListService resultSelectListService;

    @InjectMocks
    private EntityNamesProviderRestController controller;

    private Localization withBothLanguages() {
        Localization localization = new Localization();
        localization.setLocalizedValue("en", "Reactive");
        localization.setLocalizedValue("fr", "Reactif");
        return localization;
    }

    @Test
    public void answersForAResultSelectOptionWithEveryLanguageItHas() {
        when(resultSelectListService.getLocalizationForResultSelectOption("1598")).thenReturn(withBothLanguages());

        ResponseEntity<JSONObject> response = controller.processRequest("1598", "resultSelectOption");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JSONObject names = (JSONObject) response.getBody().get("name");
        assertNotNull("the screen has no other source for these", names);
        assertEquals("Reactive", names.get("english"));
        assertEquals("Reactif", names.get("french"));
    }

    @Test
    public void asksTheResultSelectListServiceRatherThanAnotherEntitysService() {
        when(resultSelectListService.getLocalizationForResultSelectOption("1598")).thenReturn(withBothLanguages());

        controller.processRequest("1598", "resultSelectOption");

        verify(resultSelectListService).getLocalizationForResultSelectOption("1598");
    }

    @Test
    public void reportsNotFoundForAnOptionWithNothingStored() {
        when(resultSelectListService.getLocalizationForResultSelectOption(anyString())).thenReturn(null);

        ResponseEntity<JSONObject> response = controller.processRequest("9662399", "resultSelectOption");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void stillRejectsAnEntityNameItDoesNotKnow() {
        ResponseEntity<JSONObject> response = controller.processRequest("1598", "somethingElse");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
