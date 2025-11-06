/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.common.paging;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.groups.Default;
import java.io.Serializable;
import java.util.List;
import org.openelisglobal.common.util.IdValuePair;

public class PagingBean implements Serializable {

    public interface Paging {
    }

    static final long serialVersionUID = 1L;

    @Pattern(regexp = "^[0-9]*$", groups = { Default.class, Paging.class })
    private String totalPages;

    @Pattern(regexp = "^[0-9]*$", groups = { Default.class, Paging.class })
    private String currentPage;

    // for display
    private List<IdValuePair> searchTermToPage;

    public String getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(String currentPage) {
        this.currentPage = currentPage;
    }

    public void setTotalPages(String totalPages) {
        this.totalPages = totalPages;
    }

    public String getTotalPages() {
        return totalPages;
    }

    public void setSearchTermToPage(List<IdValuePair> searchTermToPage) {
        this.searchTermToPage = searchTermToPage;
    }

    public List<IdValuePair> getSearchTermToPage() {
        return searchTermToPage;
    }
}
