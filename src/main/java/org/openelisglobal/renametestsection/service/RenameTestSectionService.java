package org.openelisglobal.renametestsection.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.renametestsection.valueholder.RenameTestSection;
import org.springframework.security.access.prepost.PreAuthorize;

public interface RenameTestSectionService extends BaseObjectService<RenameTestSection, String> {

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    void getData(RenameTestSection testSection);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    List<RenameTestSection> getTestSections(String filter);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    RenameTestSection getTestSectionByName(RenameTestSection testSection);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    List<RenameTestSection> getPageOfTestSections(int startingRecNo);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    Integer getTotalTestSectionCount();

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    List<RenameTestSection> getAllTestSections();

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    RenameTestSection getTestSectionById(String id);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    Localization getLocalizationForRenameTestSection(String id);
}
