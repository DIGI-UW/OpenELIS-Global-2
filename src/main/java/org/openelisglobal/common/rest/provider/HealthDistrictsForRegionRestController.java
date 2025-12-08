package org.openelisglobal.common.rest.provider;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.util.validator.GenericValidator;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/rest/")
public class HealthDistrictsForRegionRestController {
    @Autowired
    OrganizationService organizationService;

    @GetMapping(value = "health-districts-for-region", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<IdValuePair> getHealthDistrictsForRegion(HttpServletRequest request, @RequestParam String regionId) {
        if (GenericValidator.isBlankOrNull(regionId)) {
            return Collections.<IdValuePair>emptyList();
        }
        List<Organization> districts = organizationService.getOrganizationsByParentId(regionId);

        List<IdValuePair> districtIdValues = new ArrayList<>();
        if (!districts.isEmpty()) {
            districts.forEach(org -> {
                IdValuePair district = new IdValuePair(org.getId(), org.getOrganizationName());
                districtIdValues.add(district);
            });
            return districtIdValues;
        } else {
            return Collections.<IdValuePair>emptyList();
        }
    }
}
