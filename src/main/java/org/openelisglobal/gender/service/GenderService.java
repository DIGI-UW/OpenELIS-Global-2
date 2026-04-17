package org.openelisglobal.gender.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.gender.valueholder.Gender;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
public interface GenderService extends BaseObjectService<Gender, Integer> {
    Gender getGenderByType(String type);
}
