package org.openelisglobal.module.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Entity;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.openelisglobal.module.api.ModuleKind;
import org.openelisglobal.module.api.OpenElisModule;
import org.openelisglobal.module.api.manifest.BackendSection;
import org.openelisglobal.module.api.manifest.ModuleManifest;
import org.openelisglobal.module.manifest.ManifestValidation;
import org.openelisglobal.module.manifest.ManifestValidator;
import org.openelisglobal.module.runtime.ExtensionPoints;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * The contract the core enforces when it loads a module, checked at the
 * module's own build time with the core's validator and against the module's
 * own classpath. A module extends this class in its test sources:
 *
 * <pre>
 * public class MyModuleContractTest extends ModuleContractTest {
 * }
 * </pre>
 *
 * <p>
 * Checked: the manifest is valid and its kind matches its declarations; every
 * declared class exists and plays its role (activator, entities, extension
 * points); the changelog exists and its changesets carry the module's id as
 * prefix and author; every REST controller under the base packages maps under
 * {@code /rest/modules/<id>/} and every handler is guarded by
 * {@code @PreAuthorize}; the frontend bundle ships its entry and its English
 * messages use only {@code mod.<id>.} keys. Set {@code -DskipFrontend=true} to
 * skip the bundle checks in a backend-only build.
 */
public abstract class ModuleContractTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** Where the manifest lives on the test classpath. */
    protected String manifestResource() {
        return "/META-INF/openelis-module.json";
    }

    /** The validated manifest. */
    protected ModuleManifest manifest() {
        ManifestValidation validation = new ManifestValidator().validate(manifestText());
        assertTrue(validation.describe(), validation.isValid());
        return validation.manifest();
    }

    @Test
    public void manifestIsValidForTheCore() {
        manifest();
    }

    @Test
    public void declaredClassesExistAndPlayTheirRole() {
        ModuleManifest manifest = manifest();
        BackendSection backend = manifest.getBackend();
        if (backend == null) {
            return;
        }
        List<String> problems = new ArrayList<>();
        if (backend.getActivator() != null) {
            Class<?> activator = load(backend.getActivator(), problems);
            if (activator != null && !OpenElisModule.class.isAssignableFrom(activator)) {
                problems.add("activator " + backend.getActivator() + " does not implement OpenElisModule");
            }
        }
        for (String entity : backend.getEntities()) {
            Class<?> type = load(entity, problems);
            if (type != null && !type.isAnnotationPresent(Entity.class)) {
                problems.add("entity " + entity + " is not annotated with @Entity");
            }
        }
        for (BackendSection.ExtensionDeclaration extension : backend.getExtensions()) {
            Class<?> type = load(extension.getClassName(), problems);
            Class<?> point = ExtensionPoints.interfaceFor(extension.getSpi());
            if (type != null && point != null && !point.isAssignableFrom(type)) {
                problems.add("extension " + extension.getClassName() + " does not implement " + point.getName());
            }
        }
        assertNoProblems(problems);
    }

    @Test
    public void changelogExistsAndItsChangeSetsBelongToTheModule() throws Exception {
        ModuleManifest manifest = manifest();
        BackendSection backend = manifest.getBackend();
        if (backend == null || backend.getLiquibase() == null) {
            return;
        }
        List<String> problems = new ArrayList<>();
        checkChangeLog(manifest.getId(), backend.getLiquibase(), problems, 0);
        assertNoProblems(problems);
    }

    @Test
    public void everyControllerMapsUnderTheModulePrefixAndIsGuarded() {
        ModuleManifest manifest = manifest();
        BackendSection backend = manifest.getBackend();
        if (backend == null) {
            return;
        }
        String prefix = manifest.getRestPrefix();
        List<String> problems = new ArrayList<>();
        for (Class<?> controller : controllers(backend.getBasePackages())) {
            if (manifest.getKind().equals(ModuleKind.PROVIDER.manifestValue())) {
                problems.add(controller.getName() + ": a provider ships no controllers");
                continue;
            }
            RequestMapping classMapping = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
            List<String> classPaths = paths(classMapping);
            boolean classGuarded = AnnotatedElementUtils.hasAnnotation(controller, PreAuthorize.class);
            for (Method method : controller.getDeclaredMethods()) {
                RequestMapping methodMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                if (methodMapping == null) {
                    continue;
                }
                for (String classPath : classPaths) {
                    for (String methodPath : paths(methodMapping)) {
                        String combined = combine(classPath, methodPath);
                        if (!combined.startsWith(prefix)) {
                            problems.add(controller.getName() + "." + method.getName() + " maps '" + combined
                                    + "' outside " + prefix);
                        }
                    }
                }
                if (!classGuarded && !AnnotatedElementUtils.hasAnnotation(method, PreAuthorize.class)) {
                    problems.add(controller.getName() + "." + method.getName() + " has no @PreAuthorize");
                }
            }
        }
        assertNoProblems(problems);
    }

    /** The paths of a mapping, or one empty path when it declares none. */
    private static List<String> paths(RequestMapping mapping) {
        return mapping == null || mapping.path().length == 0 ? List.of("") : Arrays.asList(mapping.path());
    }

    /** The full pattern of a handler, as the core's handler mapping sees it. */
    static String combine(String classPath, String methodPath) {
        String head = classPath.replaceAll("/+$", "");
        String tail = methodPath.isEmpty() || methodPath.startsWith("/") ? methodPath : "/" + methodPath;
        String combined = head + tail;
        return combined.isEmpty() ? "/" : combined;
    }

    @Test
    public void frontendBundleShipsItsEntryAndOwnsItsMessageKeys() throws Exception {
        ModuleManifest manifest = manifest();
        if (manifest.getFrontend() == null || "true".equals(System.getProperty("skipFrontend"))) {
            return;
        }
        String assetRoot = "/META-INF/resources/modules/" + manifest.getId() + "/";
        assertNotNull(
                "frontend entry " + assetRoot + manifest.getFrontend().getEntry()
                        + " is not on the classpath; build the frontend into the module before packaging",
                getClass().getResource(assetRoot + manifest.getFrontend().getEntry()));
        String english = assetRoot + manifest.getFrontend().getMessages().replace("{locale}", "en");
        try (InputStream in = getClass().getResourceAsStream(english)) {
            assertNotNull("the English message file " + english + " ships in the bundle", in);
            JsonNode tree = JSON.readTree(in);
            List<String> problems = new ArrayList<>();
            for (Iterator<Map.Entry<String, JsonNode>> fields = tree.fields(); fields.hasNext();) {
                String key = fields.next().getKey();
                if (!key.startsWith(manifest.getPermissionPrefix())) {
                    problems.add("message key '" + key + "' is outside " + manifest.getPermissionPrefix());
                }
            }
            assertNoProblems(problems);
        }
    }

    private String manifestText() {
        try (InputStream in = getClass().getResourceAsStream(manifestResource())) {
            assertNotNull(manifestResource() + " is not on the test classpath", in);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(manifestResource() + " could not be read", e);
        }
    }

    private Class<?> load(String className, List<String> problems) {
        try {
            return Class.forName(className, false, getClass().getClassLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            problems.add(className + " is not on the classpath: " + e);
            return null;
        }
    }

    private void checkChangeLog(String moduleId, String path, List<String> problems, int depth) throws Exception {
        if (depth > 5) {
            problems.add("changelog includes nest deeper than 5 levels at " + path);
            return;
        }
        try (InputStream in = getClass().getResourceAsStream("/" + path)) {
            if (in == null) {
                problems.add("changelog " + path + " is not on the classpath");
                return;
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setNamespaceAware(true);
            Document document = factory.newDocumentBuilder().parse(in);
            NodeList changeSets = document.getElementsByTagNameNS("*", "changeSet");
            for (int i = 0; i < changeSets.getLength(); i++) {
                Element changeSet = (Element) changeSets.item(i);
                String id = changeSet.getAttribute("id");
                if (!id.startsWith(moduleId + "-")) {
                    problems.add("changeset '" + id + "' must have an id starting with '" + moduleId + "-'");
                }
                if (!moduleId.equals(changeSet.getAttribute("author"))) {
                    problems.add("changeset '" + id + "' must have author '" + moduleId + "', not '"
                            + changeSet.getAttribute("author") + "'");
                }
            }
            NodeList includes = document.getElementsByTagNameNS("*", "include");
            String directory = path.contains("/") ? path.substring(0, path.lastIndexOf('/') + 1) : "";
            for (int i = 0; i < includes.getLength(); i++) {
                Element include = (Element) includes.item(i);
                String file = include.getAttribute("file");
                boolean relative = "true".equalsIgnoreCase(include.getAttribute("relativeToChangelogFile"));
                checkChangeLog(moduleId, relative ? directory + file : file, problems, depth + 1);
            }
        }
    }

    private List<Class<?>> controllers(List<String> basePackages) {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));
        List<Class<?>> controllers = new ArrayList<>();
        for (String basePackage : basePackages) {
            for (BeanDefinition definition : scanner.findCandidateComponents(basePackage)) {
                try {
                    controllers.add(Class.forName(definition.getBeanClassName(), false, getClass().getClassLoader()));
                } catch (ClassNotFoundException e) {
                    fail(definition.getBeanClassName() + " is not on the classpath");
                }
            }
        }
        return controllers;
    }

    private static void assertNoProblems(List<String> problems) {
        if (!problems.isEmpty()) {
            fail(String.join("\n", problems));
        }
    }
}
