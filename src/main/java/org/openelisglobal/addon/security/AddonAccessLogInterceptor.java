package org.openelisglobal.addon.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import org.openelisglobal.addon.dao.AddonAccessLogDAO.Entry;
import org.openelisglobal.addon.service.AddonAccessLogService;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Writes one access log row per authenticated add-on API request, after the
 * answer: the route template (not the raw path) as the endpoint, the order or
 * patient the controller named, the status code. Requests without valid
 * credentials have no add-on to attribute and are not recorded here.
 *
 * <p>
 * Nothing the add-on sends can keep its request out of the log: a header the
 * request firewall refuses to hand out (control or non-ASCII characters) is
 * recorded as {@link #REFUSED_HEADER} instead.
 */
@Component
public class AddonAccessLogInterceptor implements HandlerInterceptor {

    public static final String ACTOR_HEADER = "X-Addon-Actor";
    public static final String PURPOSE_HEADER = "X-Addon-Purpose";
    static final String REFUSED_HEADER = "(header refused)";

    @Autowired
    private AddonAccessLogService accessLog;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception ex) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof AddonAuthenticationToken token)) {
            return;
        }
        AddonToken.Claims claims = token.getClaims();
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String endpoint = pattern == null ? request.getRequestURI() : pattern.toString();
        endpoint = endpoint.replaceFirst("^/rest/addon-api/v1", "");
        Object type = request.getAttribute(AddonAccessLogService.SUBJECT_TYPE);
        Object subject = request.getAttribute(AddonAccessLogService.SUBJECT);
        Object rows = request.getAttribute(AddonAccessLogService.ROWS);
        int status = ex != null && response.getStatus() < 400 ? 500 : response.getStatus();
        String actor = header(request, ACTOR_HEADER);
        String purpose = header(request, PURPOSE_HEADER);
        accessLog.record(new Entry(Instant.now(), claims.addonId(), claims.isService() ? "service" : "user",
                claims.userId(), REFUSED_HEADER.equals(actor) ? actor : AddonAccessLogService.clean(actor, 100),
                REFUSED_HEADER.equals(purpose) ? purpose : purpose(purpose), request.getMethod(),
                AddonAccessLogService.clean(endpoint, 120), type == null ? AddonAccessLogService.NONE : type.toString(),
                subject == null ? null : AddonAccessLogService.cleanText(subject.toString(), 200), status,
                rows instanceof Integer n ? n : null));
    }

    private static String header(HttpServletRequest request, String name) {
        try {
            return request.getHeader(name);
        } catch (RuntimeException e) {
            // StrictHttpFirewall: a value it does not allow
            LogEvent.logWarn(AddonAccessLogInterceptor.class.getSimpleName(), "header",
                    "add-on API request with a refused " + name + " header");
            return REFUSED_HEADER;
        }
    }

    static String purpose(String value) {
        String clean = AddonAccessLogService.clean(value, 64);
        return clean != null && clean.matches("[A-Za-z0-9._:-]+") ? clean : null;
    }
}
