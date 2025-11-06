package org.openelisglobal.common.service;

import org.openelisglobal.common.dao.AccessionDAO;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat;
import org.openelisglobal.common.valueholder.AccessionNumberInfo;
import org.openelisglobal.common.valueholder.AccessionNumberInfo.AccessionIdentity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AccessionServiceImpl implements AccessionService {

    @Autowired
    private AccessionDAO accessionDAO;

    @Override
    public long getNextNumberNoIncrement(String prefix, AccessionFormat accessionFormat) {
        long value;
        if (accessionDAO.exists(new AccessionIdentity(prefix, accessionFormat))) {
            value = accessionDAO.getNextNumberNoIncrement(prefix, accessionFormat);
        } else {
            value = createAccessionInfo(prefix, accessionFormat, 0L).getCurVal();
        }
        return value;
    }

    @Override
    public long getNextNumberIncrement(String prefix, AccessionFormat accessionFormat) {
        long value;
        if (accessionDAO.exists(new AccessionIdentity(prefix, accessionFormat))) {
            value = accessionDAO.getNextNumberIncrement(prefix, accessionFormat);
        } else {
            value = createAccessionInfo(prefix, accessionFormat, 1L).getCurVal();
        }
        return value;
    }

    @Override
    public long getNextNumberIncrement(AccessionIdentity accessionIdentity) {
        long value;
        if (accessionDAO.exists(accessionIdentity)) {
            value = accessionDAO.getNextNumberIncrement(accessionIdentity.getPrefix(), accessionIdentity.getType());
        } else {
            value = createAccessionInfo(accessionIdentity.getPrefix(), accessionIdentity.getType(), 1L).getCurVal();
        }
        return value;
    }

    @Override
    public long getNextNumberNoIncrement(AccessionIdentity accessionIdentity) {
        long value;
        if (accessionDAO.exists(accessionIdentity)) {
            value = accessionDAO.getNextNumberIncrement(accessionIdentity.getPrefix(), accessionIdentity.getType());
        } else {
            value = createAccessionInfo(accessionIdentity.getPrefix(), accessionIdentity.getType(), 0L).getCurVal();
        }
        return value;
    }

    private AccessionNumberInfo createAccessionInfo(String prefix, AccessionFormat accessionFormat, long value) {
        AccessionNumberInfo info = new AccessionNumberInfo();
        info.setAccessionIdentity(new AccessionIdentity(prefix, accessionFormat));
        info.setCurVal(value);

        return accessionDAO.save(info);
    }

    @Override
    public void setCurVal(String prefix, AccessionFormat accessionFormat, long curVal) {
        AccessionNumberInfo info = accessionDAO.get(new AccessionIdentity(prefix, accessionFormat));
        info.setCurVal(curVal);
        accessionDAO.save(info);
    }
}
