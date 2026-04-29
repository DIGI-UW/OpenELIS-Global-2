package org.openelisglobal.security;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Service-layer privilege coverage scan.
 *
 * <p>
 * Authorization lives exclusively at the service layer. Every public abstract
 * method on every service interface (in a {@code *.service.*} package) must
 * carry exactly one {@code @PreAuthorize("hasAuthority('PRIV_*')")} annotation.
 *
 * <p>
 * Exclusion rules are structural, not name-based:
 * <ol>
 * <li>The interface itself is {@code BaseObjectService} or
 * {@code AuditableBaseObjectService} — skip (they are the generic
 * scaffold).</li>
 * <li>The interface extends <em>only</em> {@code BaseObjectService} /
 * {@code AuditableBaseObjectService} and declares <strong>no additional
 * methods</strong> — this is an Option-C violation: the service is incomplete
 * and must be annotated before it passes.</li>
 * <li>The interface is in a {@code common.service} or {@code common.dao}
 * package — skip (pure infra).</li>
 * </ol>
 *
 * <p>
 * Uses ASM ({@code org.springframework.asm}) to inspect bytecode directly — no
 * class loading, no static initializers triggered.
 */
public class ServicePrivilegeCoverageTest {

    private static final String BASE_PACKAGE_PATH = "org/openelisglobal";
    private static final String PRE_AUTHORIZE_DESC = "Lorg/springframework/security/access/prepost/PreAuthorize;";
    private static final String CROSS_DOMAIN_DESC = "Lorg/openelisglobal/common/service/CrossDomainService;";

    // Internal ASM names of the two base scaffold interfaces
    private static final String BASE_OBJECT_SERVICE = "org/openelisglobal/common/service/BaseObjectService";
    private static final String AUDITABLE_BASE_OBJECT_SERVICE = "org/openelisglobal/common/service/AuditableBaseObjectService";

    @Test
    public void allServiceMethods_shouldHaveHasAuthorityPreAuthorize() throws Exception {
        List<String> violations = new ArrayList<>();

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath*:" + BASE_PACKAGE_PATH + "/**/service/**/*.class");

        for (Resource resource : resources) {
            try (InputStream in = resource.getInputStream()) {
                ServiceInterfaceVisitor visitor = new ServiceInterfaceVisitor();
                new ClassReader(in).accept(visitor, ClassReader.SKIP_FRAMES);
                violations.addAll(visitor.violations());
            } catch (IOException e) {
                // unreadable bytecode — skip
            }
        }

        assertTrue(violations.size() + " service method(s) missing @PreAuthorize(\"hasAuthority('PRIV_*')\"):\n"
                + String.join("\n", violations), violations.isEmpty());
    }

    // -----------------------------------------------------------------------
    // ASM visitor
    // -----------------------------------------------------------------------

    static class ServiceInterfaceVisitor extends ClassVisitor {

        private String className;
        private String simpleClassName;
        private boolean isInterface;
        private boolean isScaffoldBase; // BaseObjectService itself — skip entirely
        private boolean isCommonInfra; // common.service / common.dao — skip entirely
        private boolean isCrossDomain; // @CrossDomainService — skip, exempt by design
        private boolean extendsOnlyScaffold; // extends Base(Auditable)ObjectService only
        private boolean classHasPreAuthorize;

        private final List<MethodEntry> methods = new ArrayList<>();
        private final List<String> collectedViolations = new ArrayList<>();

        ServiceInterfaceVisitor() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName,
                String[] interfaces) {
            this.className = name.replace('/', '.');
            this.simpleClassName = className.substring(className.lastIndexOf('.') + 1);

            boolean isActualInterface = (access & Opcodes.ACC_INTERFACE) != 0;

            // Only scan top-level interfaces in a *.service.* package
            if (!isActualInterface || name.contains("$") || !className.contains(".service.")) {
                return;
            }

            this.isInterface = true;

            // The scaffold bases themselves — skip
            isScaffoldBase = name.equals(BASE_OBJECT_SERVICE) || name.equals(AUDITABLE_BASE_OBJECT_SERVICE);

            // Common infra packages — skip
            isCommonInfra = className.contains(".common.service.") || className.contains(".common.dao.");

            // Does this interface extend *only* the scaffold (and nothing else)?
            if (interfaces != null) {
                boolean allScaffold = true;
                boolean extendsScaffold = false;
                for (String iface : interfaces) {
                    if (iface.equals(BASE_OBJECT_SERVICE) || iface.equals(AUDITABLE_BASE_OBJECT_SERVICE)) {
                        extendsScaffold = true;
                    } else if (!iface.equals("java/lang/Object")) {
                        allScaffold = false;
                    }
                }
                extendsOnlyScaffold = allScaffold && extendsScaffold;
            }
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (!isInterface || isScaffoldBase || isCommonInfra) {
                return null;
            }
            if (CROSS_DOMAIN_DESC.equals(descriptor)) {
                isCrossDomain = true;
                return null;
            }
            if (PRE_AUTHORIZE_DESC.equals(descriptor)) {
                return new AnnotationVisitor(Opcodes.ASM9) {
                    @Override
                    public void visit(String name, Object value) {
                        if ("value".equals(name) && hasAuthorityExpression((String) value)) {
                            classHasPreAuthorize = true;
                        }
                    }
                };
            }
            return null;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                String[] exceptions) {
            if (!isInterface || isScaffoldBase || isCommonInfra) {
                return null;
            }
            // Only public abstract methods (interface methods without default or static)
            boolean isPublic = (access & Opcodes.ACC_PUBLIC) != 0;
            boolean isStatic = (access & Opcodes.ACC_STATIC) != 0;
            boolean isAbstract = (access & Opcodes.ACC_ABSTRACT) != 0;
            if (!isPublic || isStatic || !isAbstract) {
                return null;
            }
            // Skip synthetic / bridge
            boolean isSynthetic = (access & Opcodes.ACC_SYNTHETIC) != 0;
            boolean isBridge = (access & Opcodes.ACC_BRIDGE) != 0;
            if (isSynthetic || isBridge) {
                return null;
            }

            MethodEntry entry = new MethodEntry(name);
            methods.add(entry);
            return new MethodVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    if (PRE_AUTHORIZE_DESC.equals(desc)) {
                        return new AnnotationVisitor(Opcodes.ASM9) {
                            @Override
                            public void visit(String attrName, Object value) {
                                if ("value".equals(attrName) && hasAuthorityExpression((String) value)) {
                                    entry.hasPreAuthorize = true;
                                }
                            }
                        };
                    }
                    return null;
                }
            };
        }

        @Override
        public void visitEnd() {
            if (!isInterface || isScaffoldBase || isCommonInfra || isCrossDomain) {
                return;
            }

            // Class-level annotation covers all methods (check before Option C so that
            // empty stubs with @PreAuthorize at class level are accepted)
            if (classHasPreAuthorize) {
                return;
            }

            // Option C: interface extends only BaseObjectService and declares no methods
            // — it is an incomplete stub that has not yet declared its access rules.
            if (extendsOnlyScaffold && methods.isEmpty()) {
                collectedViolations.add(simpleClassName + " - extends BaseObjectService with no declared methods;"
                        + " add per-method @PreAuthorize or @CrossDomainService if shared");
                return;
            }

            // Per-method check
            for (MethodEntry m : methods) {
                if (!m.hasPreAuthorize) {
                    collectedViolations.add(String.format("%s#%s - missing @PreAuthorize(\"hasAuthority('PRIV_*')\")",
                            simpleClassName, m.name));
                }
            }
        }

        List<String> violations() {
            return collectedViolations;
        }

        private static boolean hasAuthorityExpression(String expression) {
            return expression != null && expression.contains("hasAuthority(")
                    && !expression.contains("hasAnyAuthority(");
        }

        static class MethodEntry {
            final String name;
            boolean hasPreAuthorize;

            MethodEntry(String name) {
                this.name = name;
            }
        }
    }
}
