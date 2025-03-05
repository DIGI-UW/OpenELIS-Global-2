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
 * <p>Copyright (C) ITECH, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.logo.controller.rest;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.servlet.reports.LogoUploadService;
import org.openelisglobal.image.service.ImageService;
import org.openelisglobal.image.valueholder.Image;
import org.openelisglobal.logo.form.LogoUploadForm;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/rest/logoUpload")
public class LogoUploadRestController {

    static final long serialVersionUID = 1L;

    @Autowired
    private ImageService imageService;
    @Autowired
    private SiteInformationService siteInformationService;
    @Autowired
    private LogoUploadService logoUploadService;

    private static final String[] ALLOWED_FIELDS = new String[] { "logoFile", "removeImage", "logoName" };

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @PostMapping
    protected ResponseEntity<Void> doPost(HttpServletRequest request,
            @ModelAttribute("logoUploadform") LogoUploadForm form) throws ServletException, IOException {
        String whichLogo = form.getLogoName();
        boolean removeImage = "true".equals(request.getParameter("removeImage"));

        if (removeImage) {
            removeImage(whichLogo);
        } else {
            updateImage(form.getLogoFile(), whichLogo);
        }
        return ResponseEntity.ok().build();
    }

    private void removeImage(String logoName) {
        File previewFile = new File(imageService.getFullPreviewPath() + imageService.getImageNameFilePath(logoName));

        boolean deleteSuccess = previewFile.delete();
        if (!deleteSuccess) {
            LogEvent.logError(this.getClass().getSimpleName(), "removeImage", "could not delete preview file");
        }

        SiteInformation logoInformation = siteInformationService.getSiteInformationByName(logoName);

        if (logoInformation == null) {
            return;
        }

        String imageId = logoInformation.getValue();

        if (!GenericValidator.isBlankOrNull(imageId)) {
            Image image = imageService.get(imageId);

            try {
                logoUploadService.removeImage(image, logoInformation);
            } catch (LIMSRuntimeException e) {
                LogEvent.logError(e);
            }
        }
    }

    private void updateImage(MultipartFile logoFile, String whichLogo) throws ServletException {
        try {
            if (validToWrite(logoFile)) {

                File previewFile = new File(
                        imageService.getFullPreviewPath() + imageService.getImageNameFilePath(whichLogo));

                logoFile.transferTo(previewFile);

                writeFileImageToDatabase(previewFile, whichLogo);
            }
        } catch (RuntimeException e) {
            throw new ServletException(e);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private void writeFileImageToDatabase(File file, String logoName) {
        if (!fileInImageDirectory(file)) {
            return;
        }
        SiteInformation logoInformation = siteInformationService.getSiteInformationByName(logoName);

        if (logoInformation == null) {
            return;
        }

        String imageId = logoInformation.getValue();

        boolean newImage = GenericValidator.isBlankOrNull(imageId);

        long fileSize = file.length();
        byte[] imageData = new byte[(int) fileSize];

        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            int bytesRead = fileInputStream.read(imageData);
            if (bytesRead != fileSize) {
                throw new IOException("file size changed between array allocation and file read, suspected attack");
            }
        } catch (IOException e) {
            LogEvent.logError(e);
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    LogEvent.logError(e.getMessage(), e);
                }
            }
        }

        Image image = new Image();
        image.setImage(imageData);
        image.setDescription(logoName);

        try {
            logoUploadService.saveImage(image, newImage, imageId, logoInformation);
        } catch (LIMSRuntimeException e) {
            LogEvent.logError(e);
        }
    }

    private boolean fileInImageDirectory(File file) {
        String filePath;
        try {
            filePath = file.getCanonicalPath();
            return filePath.startsWith((new File(imageService.getFullPreviewPath()).getCanonicalPath()));
        } catch (IOException e) {
            LogEvent.logError(e);
            return false;
        }
    }

    private boolean validToWrite(MultipartFile logoFile) {
        boolean valid = logoFile.getSize() > 0 && !GenericValidator.isBlankOrNull(logoFile.getOriginalFilename())
                && (logoFile.getOriginalFilename().contains("jpg") || logoFile.getOriginalFilename().contains("png")
                        || logoFile.getOriginalFilename().contains("gif"));

        try (InputStream input = logoFile.getInputStream()) {
            ImageIO.read(input);
        } catch (IOException e) {
            valid = false;
        }
        return valid;
    }
}
