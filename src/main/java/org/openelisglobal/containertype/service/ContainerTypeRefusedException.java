package org.openelisglobal.containertype.service;

/** A container type the page sent that cannot be saved, naming the field. */
public class ContainerTypeRefusedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String field;
    private final String code;

    public ContainerTypeRefusedException(String field, String code) {
        super(code);
        this.field = field;
        this.code = code;
    }

    public String getField() {
        return field;
    }

    public String getCode() {
        return code;
    }
}
