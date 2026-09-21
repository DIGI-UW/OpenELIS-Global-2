package org.openelisglobal.provider.service;

import java.util.List;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * The provider titles a site has configured, held as the {@code providerTitle}
 * dictionary category (OGC-1223). A provider record stores a title's local
 * abbreviation, so this is what turns that code back into something to read.
 */
public interface ProviderTitleService {

    /** The dictionary category the titles live in. */
    String CATEGORY_NAME = "providerTitle";

    /** Every title, active or not, in sort order - what the admin page lists. */
    @PreAuthorize("hasAuthority('PRIV_PROVIDER_VIEW')")
    List<Dictionary> getAllTitles();

    /** Only the titles a provider may be given now, in sort order. */
    @PreAuthorize("hasAuthority('PRIV_PROVIDER_VIEW')")
    List<Dictionary> getActiveTitles();

    /**
     * The title with this local abbreviation, active or not, or null. A record
     * keeps a title that has since been deactivated, so a lookup for display must
     * not filter on the active flag.
     */
    @PreAuthorize("hasAuthority('PRIV_PROVIDER_VIEW')")
    Dictionary getByCode(String code);

    /**
     * How many providers carry this title. The admin page shows it, and the
     * deactivate confirmation names it.
     */
    @PreAuthorize("hasAuthority('PRIV_PROVIDER_MANAGE')")
    int countProvidersUsing(String code);
}
