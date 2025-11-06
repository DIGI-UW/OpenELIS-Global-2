package org.openelisglobal.gender.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.gender.valueholder.Gender;

public interface GenderService extends BaseObjectService<Gender, Integer> {
    Gender getGenderByType(String type);
}
