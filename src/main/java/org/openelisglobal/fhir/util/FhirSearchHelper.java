package org.openelisglobal.fhir.util;

import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.openelisglobal.common.util.ConfigurationProperties;

public final class FhirSearchHelper {

    private FhirSearchHelper() {

    }

    public static String extractString(StringAndListParam param) {
        if (param == null || param.getValuesAsQueryTokens().isEmpty()) {
            return null;
        }
        StringParam stringParam = (StringParam) param.getValuesAsQueryTokens().get(0).getValuesAsQueryTokens().get(0);
        return stringParam.getValue();
    }

    public static String extractToken(TokenAndListParam param) {
        if (param == null || param.getValuesAsQueryTokens().isEmpty()) {
            return null;
        }
        TokenParam token = (TokenParam) param.getValuesAsQueryTokens().get(0).getValuesAsQueryTokens().get(0);
        return token.getValue();
    }

    public static Date extractFromDate(DateRangeParam param) {
        if (param == null || param.getLowerBound() == null) {
            return null;
        }
        return param.getLowerBound().getValue();
    }

    public static Date extractToDate(DateRangeParam param) {
        if (param == null || param.getUpperBound() == null) {
            return null;
        }
        return param.getUpperBound().getValue();
    }

    public static String buildSearchTerm(String givenName, String familyName) {
        if (givenName != null && familyName != null) {
            return givenName + " " + familyName;
        }
        return givenName != null ? givenName : familyName;
    }

    public static int getPageStart(HttpServletRequest request) {
        String pageParam = request.getParameter("_page");
        int pageSize = getPageSize(request);

        if (pageParam != null) {
            try {
                int page = Integer.parseInt(pageParam);
                return (page - 1) * pageSize + 1;
            } catch (NumberFormatException e) {

            }
        }
        return 1;
    }

    public static int getPageSize(HttpServletRequest request) {
        String countParam = request.getParameter("_count");
        if (countParam != null) {
            try {
                return Integer.parseInt(countParam);
            } catch (NumberFormatException e) {

            }
        }
        return Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
    }

    public static <T> List<T> applyPagination(List<T> items, int pageStart, int pageSize) {
        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }

        int fromIndex = Math.max(0, pageStart - 1);
        if (fromIndex >= items.size()) {
            return new ArrayList<>();
        }

        int toIndex = Math.min(fromIndex + pageSize, items.size());

        List<T> pagedResults = new ArrayList<>();
        for (int i = fromIndex; i < toIndex; i++) {
            pagedResults.add(items.get(i));
        }

        return pagedResults;
    }

    public static <T> List<T> filterActiveOnly(List<T> items) {

        return items;
    }
}