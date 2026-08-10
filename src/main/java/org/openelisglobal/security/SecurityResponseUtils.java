package org.openelisglobal.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;

/**
 * Content negotiation helper for the security subsystem. Determines whether a
 * request wants an HTML (browser navigation) response or a JSON (API/Fetch)
 * response based on the {@code Accept} header, rather than the URL path.
 */
public final class SecurityResponseUtils {

    private SecurityResponseUtils() {
    }

    public static boolean isHtmlRequest(HttpServletRequest request) {
        MediaTypeRequestMatcher htmlMatcher = new MediaTypeRequestMatcher(new HeaderContentNegotiationStrategy(),
                MediaType.TEXT_HTML);
        htmlMatcher.setIgnoredMediaTypes(Set.of(MediaType.ALL));
        return htmlMatcher.matches(request);
    }
}
