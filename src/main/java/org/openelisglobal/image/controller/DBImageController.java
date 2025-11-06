package org.openelisglobal.image.controller;

import java.util.Base64;
import java.util.Optional;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.image.service.ImageService;
import org.openelisglobal.image.valueholder.Image;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DBImageController {

    @Autowired
    private ImageService imageService;

    @GetMapping(value = "/dbImage/siteInformation/{imageName}")
    public IdValuePair getImage(@PathVariable String imageName) {
        Optional<Image> image = imageService.getImageBySiteInfoName(imageName);
        if (image.isEmpty()) {
            return new IdValuePair(imageName, "");
        }
        String imageData = "data:image/jpg;base64," + Base64.getEncoder().encodeToString(image.get().getImage());
        return new IdValuePair(imageName, imageData);
    }
}
