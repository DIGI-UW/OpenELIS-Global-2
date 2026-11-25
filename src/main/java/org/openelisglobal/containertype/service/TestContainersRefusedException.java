package org.openelisglobal.containertype.service;

/**
 * Expected containers the editor sent that cannot be saved, naming the field
 * and, for a container row, its position in the list.
 */
public class TestContainersRefusedException extends ContainerTypeRefusedException {

    private static final long serialVersionUID = 1L;

    private final Integer row;

    public TestContainersRefusedException(String field, String code, Integer row) {
        super(field, code);
        this.row = row;
    }

    public Integer getRow() {
        return row;
    }
}
