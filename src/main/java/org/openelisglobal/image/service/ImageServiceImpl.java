package org.openelisglobal.image.service;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletContext;
import java.io.File;
import java.util.Optional;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.image.dao.ImageDAO;
import org.openelisglobal.image.valueholder.Image;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ImageServiceImpl extends AuditableBaseObjectServiceImpl<Image, String> implements ImageService {
    @Autowired
    protected ImageDAO baseObjectDAO;
    @Autowired
    private SiteInformationService siteInformationService;
    @Autowired
    private ServletContext servletContext;

    public static final String PREVIEW_FILE_PATH = File.separator + "static" + File.separator + "images"
            + File.separator;
    public String FULL_PREVIEW_FILE_PATH;

    ImageServiceImpl() {
        super(Image.class);
        disableLogging();
    }

    @PostConstruct
    public void init() {
        FULL_PREVIEW_FILE_PATH = servletContext.getRealPath("") + PREVIEW_FILE_PATH;
    }

    @Override
    protected ImageDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    public String getFullPreviewPath() {
        return FULL_PREVIEW_FILE_PATH;
    }

    @Override
    public String getImageNameFilePath(String imageName) {
        switch (imageName) {
        case "headerLeftImage":
            return "leftLabLogo.jpg";
        case "labDirectorSignature":
            return "labDirectorSignature.jpg";
        default:
            return "rightLabLogo.jpg";
        }
    }

    @Override
    public Image getImageByDescription(String imageDescription) {
        return baseObjectDAO.getImageByDescription(imageDescription);
    }

    @Override
    public Optional<Image> getImageBySiteInfoName(String imageName) {
        SiteInformation logoInformation = siteInformationService.getSiteInformationByName(imageName);
        if (logoInformation == null || logoInformation.getValue() == null
                || GenericValidator.isBlankOrNull(logoInformation.getValue().trim())) {
            return Optional.empty();
        }
        try {
            Image image = get(logoInformation.getValue());
            return Optional.ofNullable(image);
        } catch (Exception e) {
            LogEvent.logError(e);
            return Optional.empty();
        }
    }
}
