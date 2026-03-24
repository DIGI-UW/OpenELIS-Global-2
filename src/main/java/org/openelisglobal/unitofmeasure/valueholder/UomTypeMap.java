package org.openelisglobal.unitofmeasure.valueholder;

import org.openelisglobal.common.valueholder.BaseObject;

public class UomTypeMap extends BaseObject<String> {

    private String id;
    private UnitOfMeasure unitOfMeasure;
    private String uomType;

    public UomTypeMap() {
        super();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public UnitOfMeasure getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(UnitOfMeasure unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public String getUomType() {
        return uomType;
    }

    public void setUomType(String uomType) {
        this.uomType = uomType;
    }
}
