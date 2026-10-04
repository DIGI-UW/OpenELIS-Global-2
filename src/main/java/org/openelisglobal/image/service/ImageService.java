package org.openelisglobal.image.service;

import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.image.valueholder.Image;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ImageService extends BaseObjectService<Image, String> {

    @PreAuthorize("hasAuthority('PRIV_SITE_INFO_VIEW')")
    String getFullPreviewPath();

    @PreAuthorize("hasAuthority('PRIV_SITE_INFO_VIEW')")
    String getImageNameFilePath(String imageName);

    @PreAuthorize("hasAuthority('PRIV_SITE_INFO_VIEW')")
    Image getImageByDescription(String imageDescription);

    // Also PRIV_REPORT_RUN: every printed report embeds the lab logo through this
    // read (Report.createReportParameters), and site_info:view is held by no
    // seeded role, so every /ReportPrint for a non-admin 403'd on its header.
    @PreAuthorize("hasAnyAuthority('PRIV_SITE_INFO_VIEW','PRIV_REPORT_RUN')")
    Optional<Image> getImageBySiteInfoName(String imageName);
}
