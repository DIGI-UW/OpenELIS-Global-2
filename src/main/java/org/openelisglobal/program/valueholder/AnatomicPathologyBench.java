package org.openelisglobal.program.valueholder;

import org.openelisglobal.program.valueholder.cytology.CytologySample;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample;
import org.openelisglobal.program.valueholder.pathology.PathologySample;

/**
 * The three anatomic-pathology benches whose cases a case view reads, each with
 * the entity its cases are stored as. Declared in the order rows of one order
 * and one day are listed: a pathology case above the IHC case it referred.
 */
public enum AnatomicPathologyBench {
    PATHOLOGY(PathologySample.class), IMMUNOHISTOCHEMISTRY(ImmunohistochemistrySample.class),
    CYTOLOGY(CytologySample.class);

    private final Class<? extends ProgramSample> entityClass;

    AnatomicPathologyBench(Class<? extends ProgramSample> entityClass) {
        this.entityClass = entityClass;
    }

    public Class<? extends ProgramSample> entityClass() {
        return entityClass;
    }
}
