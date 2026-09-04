package org.openelisglobal.common.security;

import java.util.function.Supplier;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

/**
 * Plugs SystemAwareSecurityExpressionRoot into Spring Security's method
 * security pipeline so every @PreAuthorize expression evaluation passes through
 * our system-init short-circuit.
 *
 * Spring Security 6 uses createEvaluationContext(Supplier, MethodInvocation)
 * via a private method that bypasses the overridable
 * createSecurityExpressionRoot(Authentication, MethodInvocation). We intercept
 * at the EvaluationContext level instead.
 */
public class SystemAwareMethodSecurityExpressionHandler extends DefaultMethodSecurityExpressionHandler {

    @Override
    public EvaluationContext createEvaluationContext(Supplier<Authentication> authentication,
            MethodInvocation invocation) {
        EvaluationContext ctx = super.createEvaluationContext(authentication, invocation);
        if (ctx instanceof StandardEvaluationContext stdCtx) {
            Object root = stdCtx.getRootObject().getValue();
            if (root instanceof MethodSecurityExpressionOperations ops
                    && !(root instanceof SystemAwareSecurityExpressionRoot)) {
                stdCtx.setRootObject(new SystemAwareSecurityExpressionRoot(ops));
            }
        }
        return ctx;
    }

    @Override
    protected MethodSecurityExpressionOperations createSecurityExpressionRoot(Authentication authentication,
            MethodInvocation invocation) {
        MethodSecurityExpressionOperations delegate = super.createSecurityExpressionRoot(authentication, invocation);
        return new SystemAwareSecurityExpressionRoot(delegate);
    }
}
