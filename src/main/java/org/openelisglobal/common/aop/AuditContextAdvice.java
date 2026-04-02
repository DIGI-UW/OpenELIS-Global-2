package org.openelisglobal.common.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.openelisglobal.common.util.UserContextHolder;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.security.DaemonContextAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * AOP advice that enforces the Daemon Eligibility Contract on all data
 * mutations through {@code AuditableBaseObjectServiceImpl}.
 *
 * <p>
 * <b>Rule 1:</b> Every mutation must have an authenticated SecurityContext
 * (human or daemon). If not, an {@link IllegalStateException} is thrown — no
 * silent fallback to daemon.
 *
 * <p>
 * <b>Rule 2:</b> If the entity's {@code sysUserId} is not already set, it is
 * auto-stamped from the current SecurityContext.
 *
 * <p>
 * Runs at {@code @Order(200)} — after any future {@code @PreAuthorize} (order
 * 100) but before {@code @Transactional} (order MAX_VALUE). This ensures no
 * transaction is opened if the context check fails.
 */
@Aspect
@Component
@Order(200)
public class AuditContextAdvice {

    @Autowired
    private UserContextHolder userContextHolder;

    @Before("execution(public * org.openelisglobal.common.service.AuditableBaseObjectServiceImpl+.insert(..)) || "
            + "execution(public * org.openelisglobal.common.service.AuditableBaseObjectServiceImpl+.insertAll(..)) || "
            + "execution(public * org.openelisglobal.common.service.AuditableBaseObjectServiceImpl+.update(..)) || "
            + "execution(public * org.openelisglobal.common.service.AuditableBaseObjectServiceImpl+.updateAll(..)) || "
            + "execution(public * org.openelisglobal.common.service.AuditableBaseObjectServiceImpl+.save(..)) || "
            + "execution(public * org.openelisglobal.common.service.AuditableBaseObjectServiceImpl+.saveAll(..)) || "
            + "execution(public * org.openelisglobal.common.service.AuditableBaseObjectServiceImpl+.delete(..)) || "
            + "execution(public * org.openelisglobal.common.service.AuditableBaseObjectServiceImpl+.deleteAll(..))")
    public void enforceAndStampAuditContext(JoinPoint jp) {
        DaemonContextAssert.assertAuthenticatedContext();

        String userId = userContextHolder.getCurrentSysUserId();
        for (Object arg : jp.getArgs()) {
            if (arg instanceof BaseObject<?> bo) {
                if (bo.getSysUserId() == null || bo.getSysUserId().isEmpty()) {
                    bo.setSysUserId(userId);
                }
            }
        }
    }
}
