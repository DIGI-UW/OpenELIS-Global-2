package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.microbiology.form.MicroAstRunRequestForm;
import org.openelisglobal.microbiology.form.MicroIsolateRequestForm;
import org.openelisglobal.microbiology.service.MicroCaseResourceAccessService;
import org.openelisglobal.microbiology.service.MicroCaseWriteAccessGuard;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Applies the case's current unit to retained case and child-resource
 * endpoints.
 */
@Aspect
@Component
public class MicroCaseAccessGuard extends MicrobiologyRestControllerSupport {
    private final MicroCaseResourceAccessService resources;

    public MicroCaseAccessGuard(MicroCaseResourceAccessService resources) {
        this.resources = resources;
    }

    @Before("execution(public * org.openelisglobal.microbiology.controller.rest.*.*(..)) && @within(org.springframework.web.bind.annotation.RestController)")
    public void check(JoinPoint invocation) {
        Method method = ((MethodSignature) invocation.getSignature()).getMethod();
        String caseId = null, childCase = null;
        var parameters = method.getParameters();
        var args = invocation.getArgs();
        for (int i = 0; i < args.length; i++) {
            String name = null;
            PathVariable path = parameters[i].getAnnotation(PathVariable.class);
            RequestParam query = parameters[i].getAnnotation(RequestParam.class);
            if (path != null)
                name = path.value().isEmpty() ? parameters[i].getName() : path.value();
            else if (query != null)
                name = query.value().isEmpty() ? parameters[i].getName() : query.value();
            if ("caseId".equals(name))
                caseId = (String) args[i];
            else if (name != null && args[i] instanceof String) {
                String resolved = resources.resolveCaseId(name, (String) args[i]);
                if (resolved != null)
                    childCase = resolved;
            }
        }
        if (caseId == null)
            caseId = childCase;
        if (caseId == null)
            for (Object arg : args) {
                if (arg instanceof MicroIsolateRequestForm f)
                    caseId = f.caseId;
                if (arg instanceof MicroAstRunRequestForm f)
                    caseId = resources.resolveCaseId("isolateId", f.isolateId);
            }
        if (caseId == null)
            return; // Catalog/search endpoints have their own scoped query.
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null)
            throw new AccessDeniedException("Authenticated request required");
        HttpServletRequest request = attributes.getRequest();
        String actor = authenticatedUserId(request);
        boolean read = "GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod());
        PreAuthorize rule = method.getAnnotation(PreAuthorize.class);
        String role = rule != null && SUPERVISOR_ACCESS.equals(rule.value()) ? Constants.ROLE_VALIDATION
                : Constants.ROLE_RESULTS;
        for (Object arg : args) {
            if (arg instanceof MicroIsolateRequestForm form
                    && "CONFIRMED".equalsIgnoreCase(form.identificationStatus)) {
                role = Constants.ROLE_VALIDATION;
            }
        }
        resources.requireAccess(caseId, actor, role, read);
        if (!read)
            request.setAttribute(MicroCaseWriteAccessGuard.REQUEST_SCOPE,
                    new MicroCaseWriteAccessGuard.WriteScope(caseId, actor, role));
    }

}
