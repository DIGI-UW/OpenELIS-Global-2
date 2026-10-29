package org.openelisglobal.microbiology.service;

import java.lang.reflect.UndeclaredThrowableException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

/**
 * Rechecks request ownership under the case lock in the mutation's transaction.
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class MicroCaseWriteAccessGuard {
    public static final String REQUEST_SCOPE = MicroCaseWriteAccessGuard.class.getName() + ".scope";

    public record WriteScope(String caseId, String actor, String role) {
    }

    private final MicroCaseDAO cases;
    private final MicrobiologyCaseAccessService access;
    private final TransactionTemplate transaction;
    private final AnnotationTransactionAttributeSource attributes = new AnnotationTransactionAttributeSource();

    public MicroCaseWriteAccessGuard(MicroCaseDAO cases, MicrobiologyCaseAccessService access,
            PlatformTransactionManager transactions) {
        this.cases = cases;
        this.access = access;
        this.transaction = new TransactionTemplate(transactions);
    }

    @Around("execution(public * org.openelisglobal.microbiology.service.*Impl.*(..))")
    public Object authorize(ProceedingJoinPoint invocation) throws Throwable {
        var request = RequestContextHolder.getRequestAttributes();
        var scope = request instanceof ServletRequestAttributes servlet
                ? servlet.getRequest().getAttribute(REQUEST_SCOPE)
                : null;
        var method = ((MethodSignature) invocation.getSignature()).getMethod();
        var attribute = attributes.getTransactionAttribute(method, invocation.getTarget().getClass());
        if (!(scope instanceof WriteScope write) || attribute == null || attribute.isReadOnly())
            return invocation.proceed();

        // The service's transaction advice joins this boundary. Transfer takes the
        // same case lock, so ownership cannot change before this mutation commits.
        return transaction.execute(status -> {
            var microCase = cases.getForUpdate(write.caseId());
            if (microCase == null)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            if (!access.hasLabUnitRole(write.actor(), microCase.getLabUnitId(), write.role()))
                throw new AccessDeniedException("Case lab unit access required");
            try {
                return invocation.proceed();
            } catch (RuntimeException | Error failure) {
                throw failure;
            } catch (Throwable failure) {
                throw new UndeclaredThrowableException(failure);
            }
        });
    }
}
