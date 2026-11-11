package org.openelisglobal.provider.service;

import java.util.List;
import org.openelisglobal.dictionary.valueholder.Dictionary;

/**
 * The provider titles a site has configured, held as the {@code providerTitle}
 * dictionary category (OGC-1223). A provider record stores a title's local
 * abbreviation, so this is what turns that code back into something to read.
 */
public interface ProviderTitleService {

    /** The dictionary category the titles live in. */
    String CATEGORY_NAME = "providerTitle";

    /** Every title, active or not, in sort order - what the admin page lists. */
    List<Dictionary> getAllTitles();

    /** Only the titles a provider may be given now, in sort order. */
    List<Dictionary> getActiveTitles();

    /**
     * The title with this local abbreviation, active or not, or null. A record
     * keeps a title that has since been deactivated, so a lookup for display must
     * not filter on the active flag.
     */
    Dictionary getByCode(String code);

    /**
     * How many providers carry this title. The admin page shows it, and the
     * deactivate confirmation names it.
     */
    int countProvidersUsing(String code);
}
