package org.openelisglobal.citystatezip.service;

import java.util.List;
import org.openelisglobal.citystatezip.dao.CityStateZipDAO;
import org.openelisglobal.citystatezip.valueholder.CityStateZip;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CityStateZipServiceImpl extends AuditableBaseObjectServiceImpl<CityStateZip, String>
        implements CityStateZipService {
    @Autowired
    protected CityStateZipDAO baseObjectDAO;

    CityStateZipServiceImpl() {
        super(CityStateZip.class);
    }

    @Override
    protected CityStateZipDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CityStateZip> getAllStateCodes() {
        return baseObjectDAO.getAllStateCodes();
    }

    @Override
    @Transactional(readOnly = true)
    public CityStateZip getState(CityStateZip cityStateZip) {
        return getBaseObjectDAO().getState(cityStateZip);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CityStateZip> getValidCityStateZipCombosForHumanSampleEntry(CityStateZip cityStateZip) {
        return getBaseObjectDAO().getValidCityStateZipCombosForHumanSampleEntry(cityStateZip);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CityStateZip> getCities(String filter) {
        return getBaseObjectDAO().getCities(filter);
    }

    @Override
    @Transactional(readOnly = true)
    public CityStateZip getZipCode(CityStateZip cityStateZip) {
        return getBaseObjectDAO().getZipCode(cityStateZip);
    }

    @Override
    @Transactional(readOnly = true)
    public CityStateZip getCity(CityStateZip cityStateZip) {
        return getBaseObjectDAO().getCity(cityStateZip);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CityStateZip> getCitiesByZipCode(CityStateZip cityStateZip) {
        return getBaseObjectDAO().getCitiesByZipCode(cityStateZip);
    }

    @Override
    @Transactional(readOnly = true)
    public String getCountyCodeByStateAndZipCode(CityStateZip cityStateZip) {
        return getBaseObjectDAO().getCountyCodeByStateAndZipCode(cityStateZip);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CityStateZip> getZipCodesByCity(CityStateZip cityStateZip) {
        return getBaseObjectDAO().getZipCodesByCity(cityStateZip);
    }

    @Override
    public boolean isCityStateZipComboValid(CityStateZip cityStateZip) {
        return getBaseObjectDAO().isCityStateZipComboValid(cityStateZip);
    }

    @Override
    @Transactional(readOnly = true)
    public CityStateZip getCityStateZipByCityAndZipCode(CityStateZip cityStateZip) {
        return getBaseObjectDAO().getCityStateZipByCityAndZipCode(cityStateZip);
    }
}
