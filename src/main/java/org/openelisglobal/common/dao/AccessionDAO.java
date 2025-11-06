package org.openelisglobal.common.dao;

import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat;
import org.openelisglobal.common.valueholder.AccessionNumberInfo;
import org.openelisglobal.common.valueholder.AccessionNumberInfo.AccessionIdentity;

public interface AccessionDAO {

    long getNextNumberNoIncrement(String prefix, AccessionFormat accessionFormat);

    long getNextNumberIncrement(String prefix, AccessionFormat accessionFormat);

    AccessionNumberInfo save(AccessionNumberInfo info);

    AccessionNumberInfo get(AccessionIdentity accessionIdentity);

    boolean exists(AccessionIdentity accessionIdentity);
}
