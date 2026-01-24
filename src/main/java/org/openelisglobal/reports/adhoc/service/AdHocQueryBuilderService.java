package org.openelisglobal.reports.adhoc.service;

import java.util.Map;
import org.openelisglobal.reports.adhoc.dto.AdHocReportDefinitionDTO;

public interface AdHocQueryBuilderService {

    QueryResult buildQuery(AdHocReportDefinitionDTO definition);

    QueryResult buildCountQuery(AdHocReportDefinitionDTO definition);

    class QueryResult {
        private final String hql;
        private final Map<String, Object> parameters;

        public QueryResult(String hql, Map<String, Object> parameters) {
            this.hql = hql;
            this.parameters = parameters;
        }

        public String getHql() {
            return hql;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }
    }
}
