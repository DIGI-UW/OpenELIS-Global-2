package org.openelisglobal.common.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.openelisglobal.common.service.BaseObjectService;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Authorization for the CRUD every service inherits from
 * {@link BaseObjectService}. Referenced from that interface's
 * {@code @PreAuthorize} expressions as a static call, so it needs no bean and
 * works in every context that has method security, slice tests included.
 *
 * <p>
 * Resolution order, per invocation:
 * <ol>
 * <li>A {@link CrudPrivileges} annotation on the target's service interface →
 * require the declared read/write privilege (empty = open, transitional).</li>
 * <li>Else a <em>type-level</em> {@code @PreAuthorize} on the target's service
 * interface → evaluate that expression against the same expression root. This
 * keeps the ~90 services that were already covered by a type-level gate exactly
 * as they were: once {@code BaseObjectService} carries a method-level
 * annotation, Spring Security no longer consults the type-level one for
 * inherited methods, so the fallback re-applies it.</li>
 * <li>Else open — the pre-existing behaviour for a method-gated-only service.
 * Tracked by {@code InheritedCrudGateCoverageTest} until each declares its
 * privileges.</li>
 * </ol>
 *
 * <p>
 * All checks go through the supplied root, so {@code ROLE_SYSTEM} and the
 * startup flag ({@code SystemAwareSecurityExpressionRoot}) apply as everywhere.
 */
public final class CrudGate {

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();
    private static final Map<Class<?>, Expression> TYPE_LEVEL = new ConcurrentHashMap<>();
    private static final Map<Class<?>, String[]> DECLARED = new ConcurrentHashMap<>();
    private static final String[] UNDECLARED = new String[0];
    private static final Expression NONE = PARSER.parseExpression("true");

    private CrudGate() {
    }

    public static boolean read(MethodSecurityExpressionOperations root) {
        return permits(root, true);
    }

    public static boolean write(MethodSecurityExpressionOperations root) {
        return permits(root, false);
    }

    private static boolean permits(MethodSecurityExpressionOperations root, boolean read) {
        Object target = root.getThis();
        if (target == null) {
            return true;
        }
        String[] declared = DECLARED.computeIfAbsent(target.getClass(), CrudGate::declaredPrivileges);
        if (declared != UNDECLARED) {
            String privilege = declared[read ? 0 : 1];
            return privilege.isEmpty() || root.hasAuthority(privilege);
        }
        Expression typeLevel = TYPE_LEVEL.computeIfAbsent(target.getClass(), CrudGate::typeLevelExpression);
        if (typeLevel == NONE) {
            return true;
        }
        Boolean allowed = typeLevel.getValue(new StandardEvaluationContext(root), Boolean.class);
        return Boolean.TRUE.equals(allowed);
    }

    /**
     * {read, write} from the unique {@link CrudPrivileges} in the hierarchy, or
     * UNDECLARED.
     */
    private static String[] declaredPrivileges(Class<?> targetClass) {
        String[] found = null;
        for (MergedAnnotation<CrudPrivileges> a : MergedAnnotations.from(targetClass, SearchStrategy.TYPE_HIERARCHY)
                .stream(CrudPrivileges.class).toList()) {
            String[] value = { a.getString("read"), a.getString("write") };
            if (found != null && !java.util.Arrays.equals(found, value)) {
                throw new IllegalStateException(targetClass.getName() + " has conflicting @CrudPrivileges");
            }
            found = value;
        }
        return found == null ? UNDECLARED : found;
    }

    /**
     * The unique type-level {@code @PreAuthorize} across the target's class and
     * interface hierarchy, or {@link #NONE}. Two different type-level expressions
     * on one bean would be ambiguous; that shape does not occur and is refused
     * loudly rather than guessed.
     */
    private static Expression typeLevelExpression(Class<?> targetClass) {
        String found = null;
        for (MergedAnnotation<PreAuthorize> a : MergedAnnotations.from(targetClass, SearchStrategy.TYPE_HIERARCHY)
                .stream(PreAuthorize.class).toList()) {
            String value = a.getString("value");
            if (found != null && !found.equals(value)) {
                throw new IllegalStateException(targetClass.getName() + " has conflicting type-level @PreAuthorize: '"
                        + found + "' and '" + value + "'");
            }
            found = value;
        }
        return found == null ? NONE : PARSER.parseExpression(found);
    }
}
