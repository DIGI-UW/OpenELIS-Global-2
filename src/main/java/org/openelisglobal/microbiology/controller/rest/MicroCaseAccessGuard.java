package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.form.MicroAstRunRequestForm;
import org.openelisglobal.microbiology.form.MicroIsolateRequestForm;
import org.openelisglobal.microbiology.service.MicrobiologyCaseAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

/**
 * Applies the case's current unit to retained case and child-resource
 * endpoints.
 */
@Aspect
@Component
public class MicroCaseAccessGuard extends MicrobiologyRestControllerSupport {
    private final MicrobiologyCaseAccessService access;
    private final MicroCaseDAO cases;
    private final MicroIsolateDAO isolates;
    private final MicroAstRunDAO runs;
    private final MicroAstReadingDAO readings;
    private final MicroCriticalCommunicationDAO communications;

    public MicroCaseAccessGuard(MicrobiologyCaseAccessService access, MicroCaseDAO cases, MicroIsolateDAO isolates,
            MicroAstRunDAO runs, MicroAstReadingDAO readings, MicroCriticalCommunicationDAO communications) {
        this.access = access;
        this.cases = cases;
        this.isolates = isolates;
        this.runs = runs;
        this.readings = readings;
        this.communications = communications;
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
                String resolved = resolve(name, (String) args[i]);
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
                    caseId = resolve("isolateId", f.isolateId);
            }
        if (caseId == null)
            return; // Catalog/search endpoints have their own scoped query.
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null)
            throw new AccessDeniedException("Authenticated request required");
        HttpServletRequest request = attributes.getRequest();
        String actor = authenticatedUserId(request);
        var c = cases.get(caseId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        boolean read = "GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod());
        PreAuthorize rule = method.getAnnotation(PreAuthorize.class);
        String role = rule != null && SUPERVISOR_ACCESS.equals(rule.value()) ? Constants.ROLE_VALIDATION
                : Constants.ROLE_RESULTS;
        boolean allowed = read ? access.canReadLabUnit(actor, c.getLabUnitId())
                : access.hasLabUnitRole(actor, c.getLabUnitId(), role);
        if (!allowed)
            throw new AccessDeniedException("Case lab unit access required");
    }

    private String resolve(String kind, String id) {
        if (id == null)
            return null;
        return switch (kind) {
        case "isolateId" ->
            isolates.get(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)).getCaseId();
        case "runId", "sourceRunId" -> resolve("isolateId",
                runs.get(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)).getIsolateId());
        case "readingId" -> resolve("runId",
                readings.get(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)).getAstRunId());
        case "communicationId" ->
            communications.get(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)).getCaseId();
        default -> null;
        };
    }
}
