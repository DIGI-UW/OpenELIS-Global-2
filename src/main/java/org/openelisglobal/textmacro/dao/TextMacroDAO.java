package org.openelisglobal.textmacro.dao;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.textmacro.valueholder.TextMacro;
import org.openelisglobal.textmacro.valueholder.TextMacroContext;

public interface TextMacroDAO extends BaseDAO<TextMacro, String> {

    Optional<TextMacro> findByCode(String code);

    List<TextMacro> findActiveByContext(TextMacroContext context, String query, int limit);

    List<TextMacro> search(String query, TextMacroContext context, String status, String sort, int offset, int limit);

    long countSearch(String query, TextMacroContext context, String status);

    List<TextMacro> findAllWithContexts();

    List<TextMacro> findByIdsWithContexts(Set<String> ids);
}
