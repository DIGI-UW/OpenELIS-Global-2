package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.controller.AnalyzerRestController;
import org.openelisglobal.analyzer.dao.AnalyzerProfileBindingDAO;
import org.openelisglobal.analyzer.form.AnalyzerForm;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzerimport.action.AnalyzerFhirImportController;
import org.springframework.web.bind.annotation.GetMapping;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerProfileCutoverContractTest {

    private static final String PROFILE_ID = "site.mock-hematology";
    private static final int PROFILE_REVISION = 3;
    private static final String FINGERPRINT = "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @Mock
    private AnalyzerProfileBindingDAO bindingDAO;

    @Mock
    private BridgeProfileCatalogService catalogService;

    private AnalyzerProfileBindingServiceImpl service;

    @Before
    public void setUp() throws Exception {
        service = new AnalyzerProfileBindingServiceImpl(bindingDAO, catalogService);
        when(catalogService.getCatalog()).thenReturn(new BridgeProfileCatalog("1.0", FINGERPRINT,
                java.util.List.of(new BridgeProfileCatalog.ProfileRevision(
                        new ObjectMapper().readTree(
                                """
                                        {
                                          "profileId":"site.mock-hematology",
                                          "revision":3,
                                          "revisionFingerprint":"sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                                          "status":"ACTIVE"
                                        }
                                        """),
                        new ObjectMapper().createObjectNode()))));
    }

    @Test
    public void analyzerFormUsesExactProfileRevisionAndHasNoLegacyBootstrapHint() {
        assertTrue(hasField(AnalyzerForm.class, "profileId"));
        assertTrue(hasField(AnalyzerForm.class, "profileRevision"));
        assertFalse(hasField(AnalyzerForm.class, "defaultConfigId"));
    }

    @Test
    public void assignmentPinsTheResolvedRevisionWithoutCopyingProfileContent() throws Exception {
        AnalyzerProfileBinding binding = binding();
        when(bindingDAO.findByProfileIdAndRevision(PROFILE_ID, PROFILE_REVISION)).thenReturn(Optional.of(binding));
        Analyzer analyzer = new Analyzer();

        Method assign = AnalyzerProfileBindingServiceImpl.class.getMethod("assignProfile", Analyzer.class, String.class,
                int.class, String.class);
        Object result = invoke(assign, service, analyzer, PROFILE_ID, PROFILE_REVISION, "17");

        assertSame(binding, result);
        assertSame(binding, analyzer.getProfileBinding());
        assertFalse(Arrays.stream(Analyzer.class.getDeclaredFields())
                .anyMatch(field -> field.getName().toLowerCase().contains("profilesnapshot")));
    }

    @Test
    public void analyzerControllerHasNoFilesystemProfileCatalogHandlers() {
        for (Method method : AnalyzerRestController.class.getDeclaredMethods()) {
            GetMapping mapping = method.getAnnotation(GetMapping.class);
            if (mapping == null) {
                continue;
            }
            for (String path : mapping.value()) {
                assertFalse("Legacy filesystem profile route remains: " + path,
                        path.startsWith("/profiles") || path.startsWith("/defaults"));
            }
        }
    }

    @Test
    public void fhirImportDoesNotInferOrApplyProfilesFromFilesystem() {
        for (Method method : AnalyzerFhirImportController.class.getDeclaredMethods()) {
            String name = method.getName();
            assertFalse("FHIR import still owns profile selection: " + name, name.equals("applyMatchedProfile")
                    || name.equals("matchProfileFromDevice") || name.equals("resolveProfilesBaseDir"));
        }
    }

    private static Object invoke(Method method, Object target, Object... args) throws Exception {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw exception;
        }
    }

    private static boolean hasField(Class<?> type, String fieldName) {
        return Arrays.stream(type.getDeclaredFields()).anyMatch(field -> fieldName.equals(field.getName()));
    }

    private static AnalyzerProfileBinding binding() {
        AnalyzerProfileBinding binding = new AnalyzerProfileBinding();
        binding.setId("41");
        binding.setProfileId(PROFILE_ID);
        binding.setProfileRevision(PROFILE_REVISION);
        binding.setProfileFingerprint(FINGERPRINT);
        return binding;
    }
}
